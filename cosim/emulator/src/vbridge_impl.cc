#include <fmt/core.h>
#include <glog/logging.h>



#include "verilated.h"

#include "exceptions.h"
#include "vbridge_impl.h"
#include "util.h"
#include "tl_interface.h"
#include "simple_sim.h"
#include "vpi.h"

#include "glog_exception_safe.h"

/// convert TL style size to size_by_bytes
inline uint32_t decode_size(uint32_t encoded_size) {
  return 1 << encoded_size;
}

VBridgeImpl::VBridgeImpl() :
    sim(1 << 30),
    isa("rv32gc", "M"),
    _cycles(100),
    proc(
        /*isa*/ &isa,
        /*varch*/ fmt::format("vlen:{},elen:{}", 0, 0).c_str(),
        /*sim*/ &sim,
        /*id*/ 0,
        /*halt on reset*/ true,
        /* endianness*/ memif_endianness_little,
        /*log_file_t*/ nullptr,
        /*sout*/ std::cerr){
}

void VBridgeImpl::setup(const std::string &_bin, const std::string &_wave, uint64_t _reset_vector, uint64_t cycles) {
  this->bin = _bin;
  this->wave = _wave;
  this->reset_vector = _reset_vector;
  this->timeout = cycles;
}



void VBridgeImpl::reset() {
  top.clock = 0;
  top.reset = 1;
  top.resetVector = 0x1000;
  top.eval();
  tfp.dump(0);

  // posedge
  top.clock = 1;
  top.eval();
  tfp.dump(1);


  // negedge
  top.reset = 0;
  top.clock = 0;
  top.eval();
  tfp.dump(2);
  // posedge
  LOG(INFO) << fmt::format("Reset compeleted, now we start");
  top.reset = 0;
  top.clock = 1;
  top.eval();
  tfp.dump(3);
  ctx.time(2);
}

VBridgeImpl::~VBridgeImpl() {
  terminate_simulator();
}

void VBridgeImpl::configure_simulator(int argc, char **argv) {
  ctx.commandArgs(argc, argv);
}

void VBridgeImpl::init_spike() {
  // reset spike CPU
  proc.reset();
  // TODO: remove this line, and use CSR write in the test code to enable this the VS field.
  proc.get_state()->sstatus->write(proc.get_state()->sstatus->read() | SSTATUS_VS);
  // load binary to reset_vector
  sim.load(bin, reset_vector);
}

SpikeEvent *find_se_to_issue();void VBridgeImpl::init_simulator() {
  Verilated::traceEverOn(true);
  top.trace(&tfp, 99);
  tfp.open(wave.c_str());
  _cycles = timeout;
}

void VBridgeImpl::terminate_simulator() {
  if (tfp.isOpen()) {
    tfp.close();
  }
  top.final();
}

uint64_t VBridgeImpl::get_t() {
  return ctx.time();
}

uint8_t VBridgeImpl::load(uint64_t address){
  return *sim.addr_to_mem(address);
}

void VBridgeImpl::run() {

  init_spike();
  //sim.load(bin, reset_vector);
  init_simulator();
  reset();

  // start loop
  while (true) {
    loop_until_se_queue_full();
      return_fetch_response();
      top.clock = 1;
      top.eval();

      top.memory_0_a_ready = 1;
      // negedge

      receive_tl_req();
      top.clock = 0;
      top.eval();
      tfp.dump(2 * ctx.time());
      ctx.timeInc(1);

      // posedge, update registers
      top.clock = 1;
      top.eval();
      tfp.dump(2 * ctx.time() - 1);

      if (get_t() >= timeout) {
        throw TimeoutException();
      }

    }
  }

void VBridgeImpl::loop_until_se_queue_full() {
  while (to_rtl_queue.size() < to_rtl_queue_size) {
    try {
      if (auto spike_event = spike_step()) {
        SpikeEvent &se = spike_event.value();
        to_rtl_queue.push_front(std::move(se));
      }
    } catch (trap_t &trap) {
      LOG(FATAL) << fmt::format("spike trapped with {}", trap.name());
    }
  }
  LOG(INFO) << fmt::format("to_rtl_queue is full now, start to simulate.");
}

// ->log_arch_changes()
// ->create_spike_event()
std::optional<SpikeEvent> VBridgeImpl::spike_step() {
  auto state = proc.get_state();
  auto fetch = proc.get_mmu()->load_insn(state->pc);
  auto event = create_spike_event(fetch);  // event not empty iff fetch is v inst
  auto &xr = proc.get_state()->XPR; // todo: ?

  auto &se = event.value();
  // now event always exists,just func
  // todo: detail ?
  state->pc = fetch.func(&proc, fetch.insn, state->pc);
  se.log_arch_changes();


  return event;
}

// now we take all the instruction as spike event
std::optional<SpikeEvent> VBridgeImpl::create_spike_event(insn_fetch_t fetch) {

  return SpikeEvent{proc, fetch, this};
}

/*SpikeEvent *VBridgeImpl::find_se_to_issue() {
  SpikeEvent *se_to_issue = nullptr;
  for (auto iter = to_rtl_queue.rbegin(); iter != to_rtl_queue.rend(); iter++) {
    if (!iter->is_issued) {
      se_to_issue = &(*iter);
      break;
    }
  }
  CHECK(se_to_issue) << fmt::format("[{}] all events in to_rtl_queue are is_issued", get_t());  // TODO: handle this
  return se_to_issue;
}*/



void VBridgeImpl::receive_tl_req() {
  int miss = top.rootp->DUT__DOT__ldut__DOT__tile__DOT__frontend__DOT__icache__DOT__s2_miss;
  //printf("s2_miss = %d\n",miss);


#define TL(name) (get_tl_##name(top))
  int valid = TL(a_valid);
  if (!TL(a_valid)) return;
  // store A channel req

  uint8_t opcode = TL(a_bits_opcode);
  if (opcode == 4){
    LOG(INFO) << fmt::format("Find Mem req");
  }
  uint32_t addr = TL(a_bits_address);
  uint8_t size = TL(a_bits_size);
  uint8_t src = TL(a_bits_source);   // MSHR id, TODO: be returned in D channel
  // find icache refill request, fill fetch_banks
  if (miss){
    for(int i=0; i<8 ; i++){
      uint64_t insn = 0;
      for (int j = 0; j < 8; ++j) {
        insn += (uint64_t) load(addr + j + i*8) << (j * 8);
      }
      LOG(INFO) << fmt::format("Find insn: {:08X} , at:{:08X}",insn,addr + i*8);
      fetch_banks[i].data = insn;
      fetch_banks[i].source = src;
      fetch_banks[i].remaining = true;
    }



    return;
  }
  // find corresponding SpikeEvent
  SpikeEvent *se;
  for (auto se_iter = to_rtl_queue.rbegin(); se_iter != to_rtl_queue.rend(); se_iter++) {

    auto mem_read = se_iter->mem_access_record.all_reads.find(addr);
    if(mem_read != se_iter->mem_access_record.all_reads.end()){
      se = &(*se_iter);
      LOG(INFO) << fmt::format("se.pc = {:08X}",se_iter->pc);
      break;
    }

  }


  switch (opcode) {

    case TlOpcode::Get: {
      auto mem_read = se->mem_access_record.all_reads.find(addr);
      CHECK_S(mem_read != se->mem_access_record.all_reads.end())
              << fmt::format(": [{}] cannot find mem read of addr {:08X}", get_t(), addr);
      CHECK_EQ_S(mem_read->second.size_by_byte, decode_size(size)) << fmt::format(
            ": [{}] expect mem read of size {}, actual size {} (addr={:08X}, {})",
            get_t(), mem_read->second.size_by_byte, 1 << decode_size(size), addr, se->describe_insn());

      uint64_t data = mem_read->second.val;
      LOG(INFO) << fmt::format("[{}] receive rtl mem get req (addr={}, size={}byte), should return data {}",
                               get_t(), addr, decode_size(size), data);
      tl_banks.emplace(std::make_pair(addr, TLReqRecord{
          data, 1u << size, src, TLReqRecord::opType::Get, get_mem_req_cycles()
      }));
      mem_read->second.executed = true;
      break;
    }

    case TlOpcode::PutFullData: {
      uint32_t data = TL(a_bits_data);
      LOG(INFO) << fmt::format("[{}] receive rtl mem put req (addr={:08X}, size={}byte, data={})",
                               addr, decode_size(size), data);
      auto mem_write = se->mem_access_record.all_writes.find(addr);

      CHECK_S(mem_write != se->mem_access_record.all_writes.end())
              << fmt::format(": [{}] cannot find mem write of addr={:08X}", get_t(), addr);
      CHECK_EQ_S(mem_write->second.size_by_byte, decode_size(size)) << fmt::format(
            ": [{}] expect mem write of size {}, actual size {} (addr={:08X}, insn='{}')",
            get_t(), mem_write->second.size_by_byte, 1 << decode_size(size), addr, se->describe_insn());
      CHECK_EQ_S(mem_write->second.val, data) << fmt::format(
            ": [{}] expect mem write of data {}, actual data {} (addr={:08X}, insn='{}')",
            get_t(), mem_write->second.size_by_byte, 1 << decode_size(size), addr, se->describe_insn());

      tl_banks.emplace(std::make_pair(addr, TLReqRecord{
          data, 1u << size, src, TLReqRecord::opType::PutFullData, get_mem_req_cycles()
      }));
      mem_write->second.executed = true;
      break;
    }
    default: {
      LOG(FATAL) << fmt::format("unknown tl opcode {}", opcode);
    }
#undef TL
  }
}

void VBridgeImpl::return_fetch_response(){
#define TL(name) (get_tl_##name(top))
  bool d_valid = false;
  for (int i=0;i<8;i++){
    if(fetch_banks[i].remaining){
      fetch_banks[i].remaining = false;
      LOG(INFO) << fmt::format("Fetch insn: {:08X}", fetch_banks[i].data);
      TL(d_bits_opcode) = 1;
      TL(d_bits_data) = fetch_banks[i].data;
      TL(d_bits_source) = fetch_banks[i].source;
      TL(d_bits_size) = 6;
      d_valid = true;
      break;
    }
  }
  TL(d_valid) = d_valid;
#undef TL
}

/*void VBridgeImpl::return_tl_response() {
#define TL(i, name) (get_tl_##name(top, (i)))
  for (int i = 0; i < consts::numTL; i++) {
    // update remaining_cycles
    for (auto &[addr, record]: tl_banks[i]) {
      if (record.remaining_cycles > 0) record.remaining_cycles--;
    }

    // find a finished request and return
    bool d_valid = false;
    for (auto &[addr, record]: tl_banks[i]) {
      if (record.remaining_cycles == 0) {
        TL(i, d_bits_opcode) = record.op == TLReqRecord::opType::Get ? TlOpcode::AccessAckData : TlOpcode::AccessAck;
        TL(i, d_bits_data) = record.data;
        TL(i, d_bits_sink) = record.source;
        d_valid = true;
        record.op = TLReqRecord::opType::Nil;
        break;
      }
    }
    TL(i, d_valid) = d_valid;

    // collect garbage
    erase_if(tl_banks[i], [](const auto &record) {
        return record.second.op == TLReqRecord::opType::Nil;
    });

    // welcome new requests all the time
    TL(i, a_ready) = true;
  }
#undef TL
}*/






