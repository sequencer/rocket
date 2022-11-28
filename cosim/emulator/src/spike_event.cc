#include <fmt/core.h>
#include <glog/logging.h>

#include "spike_event.h"
#include "disasm.h"
#include "util.h"
#include "tl_interface.h"
#include "exceptions.h"
#include "glog_exception_safe.h"

std::string SpikeEvent::describe_insn() const {
  return fmt::format("pc={:08X}, bits={:08X}, disasm='{}'", pc, inst_bits, proc.get_disassembler()->disassemble(inst_bits));
}

/*void SpikeEvent::pre_log_arch_changes() {
  rd_bits = proc.get_state()->XPR[rd_idx];
  // uint8_t *vd_bits_start = &proc.VU.elt<uint8_t>(rd_idx, 0);
  // CHECK_LT_S(vlmul, 4) << ": fractional vlmul not supported yet";  // TODO: support fractional vlmul
  // uint32_t len = consts::vlen_in_bits << vlmul / 8;
  vd_write_record.vd_bytes = std::make_unique<uint8_t[]>(len);
  std::memcpy(vd_write_record.vd_bytes.get(), vd_bits_start, len);
}*/

void SpikeEvent::log_arch_changes() {

  state_t *state = proc.get_state();

  for (auto [write_idx, data]: state->log_reg_write) {
    // in spike, log_reg_write is arrange:
    // xx0000 <- x
    // xx0001 <- f
    // xx0010 <- vreg
    // xx0011 <- vec
    // xx0100 <- csr
//    if ((write_idx & 0xf) == 0b0010) {  // vreg
//      auto vd = (write_idx >> 4);
//      CHECK_EQ_S(vd, rd_idx) << fmt::format("expect to write vrf[{}], detect writing vrf[{}]", rd_idx, vd);
//      CHECK_LT_S(vd, 32) << fmt::format("log_reg_write vd ({}) out of bound", vd);
//
//      uint8_t *vd_bits_start = &proc.VU.elt<uint8_t>(rd_idx, 0);
//      uint32_t len = consts::vlen_in_bits << vlmul / 8;
//      for (int i = 0; i < len; i++) {
//        uint8_t origin_byte = vd_write_record.vd_bytes[i], cur_byte = vd_bits_start[i];
//        if (origin_byte != cur_byte) {
//          vrf_access_record.all_writes[vd * consts::vlen_in_bytes + i] = {.byte = cur_byte };
//          LOG(INFO) << fmt::format("spike detect vrf change: vrf[{}, {}] from {} to {} [{}]",
//                                   vd, i, (int) origin_byte, (int) cur_byte, vd * consts::vlen_in_bytes + i);
//        }
//      }
//
//    }
    if ((write_idx & 0xf) == 0b0000) {  // scalar rf
      //LOG(INFO) << fmt::format("idx = {:08X} data = {:08X}", rd_idx, rd_bits);
      uint32_t new_rd_bits = proc.get_state()->XPR[rd_idx];
      if (rd_new_bits != new_rd_bits ) {
        rd_new_bits = new_rd_bits;
        is_rd_written = true;
        LOG(INFO) << fmt::format("Insert Spike {:08X} with scalar rf change: x[{}] from {:08X} to {:08X}", pc, rd_idx, rd_old_bits, rd_new_bits);
      }
    }
//    else if((write_idx & 0xf) == 0b0100){
//        LOG(INFO) << fmt::format("spike detect csr change: idx={:08X}", write_idx);
//    }
//    else {
//        LOG(INFO) << fmt::format("spike detect unknown reg change (idx = {:08X})", write_idx);
//    }
  }

  for (auto mem_write: state->log_mem_write) {
    uint64_t address = std::get<0>(mem_write);
    uint64_t write_addr = address & 0xC0;
    uint64_t value = std::get<1>(mem_write);
    // Byte size_bytes
    uint8_t size_by_byte = std::get<2>(mem_write);
    // record mem block for cache
    for(int i=0; i<8 ; i++){
      uint64_t data = 0;
      //scan 8 bytes to data
      for (int j = 0; j < 8; ++j) {
        data += (uint64_t) impl->load(write_addr + j + i*8) << (j * 8);
      }
      //LOG(INFO) << fmt::format("Find insn: {:08X} , at:{:08X}",insn,addr + i*8);
      block.blocks[i] = data;
      block.addr = write_addr;
      block.remaining = true;
    }
    LOG(INFO) << fmt::format("spike detect mem write {:08X} on mem:{:08X} with size={}byte; block_addr={:08X}", value, address, size_by_byte,write_addr);
    mem_access_record.all_writes[address] = { .size_by_byte = size_by_byte, .val = value };
  }
  // since log_mem_read doesn't record mem data, we need to load manually
  for (auto mem_read: state->log_mem_read) {
    uint64_t address = std::get<0>(mem_read);
    uint64_t read_addr = address & 0xC0;
    // Byte size_bytes
    uint8_t size_by_byte = std::get<2>(mem_read);
    uint64_t value = 0;
    // record mem target
    for (int i = 0; i < size_by_byte; ++i) {
      value += (uint64_t) impl->load(address + i) << (i * 8);
    }
    // record mem block for cache
    for(int i=0; i<8 ; i++){
      uint64_t data = 0;
      //scan 8 bytes to data
      for (int j = 0; j < 8; ++j) {
        data += (uint64_t) impl->load(read_addr + j + i*8) << (j * 8);
      }
      //LOG(INFO) << fmt::format("Find insn: {:08X} , at:{:08X}",insn,addr + i*8);
      block.blocks[i] = data;
      block.addr = read_addr;
      block.remaining = true;
    }
    LOG(INFO) << fmt::format("spike detect mem read {:08X} on mem:{:08X} with size={}byte; block_addr={:08X}", value, address, size_by_byte,read_addr);
    mem_access_record.all_reads[address] = { .size_by_byte = size_by_byte, .val = value };
  }

  //state->log_reg_write.clear();
  state->log_mem_read.clear();
  state->log_mem_write.clear();
}

SpikeEvent::SpikeEvent(processor_t &proc, insn_fetch_t &fetch, VBridgeImpl *impl): proc(proc), impl(impl) {
  auto &xr = proc.get_state()->XPR;
  rs1_bits = xr[fetch.insn.rs1()];
  rs2_bits = xr[fetch.insn.rs2()];
  rd_idx = fetch.insn.rd();
  rd_old_bits = proc.get_state()->XPR[rd_idx];

  pc = proc.get_state()->pc;
  inst_bits = fetch.insn.bits();
  uint32_t opcode = clip(inst_bits, 0, 6);
  is_load = opcode == 0b111;
  is_store = opcode == 0b100011;

  is_issued = false;
  is_committed = false;


}





/*void SpikeEvent::check_is_ready_for_commit() {
  for (auto &[addr, mem_write]: mem_access_record.all_writes) {
    if (!mem_write.executed) {
      LOG(FATAL) << fmt::format("expect to write mem {:08X}, not executed when commit ({})",
                                addr, pc, describe_insn());
    }
  }
  for (auto &[addr, mem_read]: mem_access_record.all_reads) {
    if (!mem_read.executed) {
      LOG(FATAL) << fmt::format("expect to read mem {:08X}, not executed when commit ({})",
                                addr, describe_insn());
    }
  }
  for (auto &[idx, vrf_write]: vrf_access_record.all_writes) {
    CHECK_S(vrf_write.executed) << fmt::format("expect to write vrf {}, not executed when commit ({})",
                              idx, describe_insn());
  }
}*/

/*void SpikeEvent::record_rd_write(VV &top) {
  // TODO: rtl should indicate whether resp_bits_data is valid
  if (is_rd_written) {
    CHECK_EQ_S(top.resp_bits_data, rd_bits) << fmt::format(": expect to write rd[{}] = {}, actual {}",
                                                             rd_idx, rd_bits, top.resp_bits_data);
  }
}*/