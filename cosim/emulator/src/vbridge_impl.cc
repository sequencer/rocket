#include <fmt/core.h>
#include <glog/logging.h>



#include "verilated.h"

#include "exceptions.h"
#include "vbridge_impl.h"
#include "glog_exception_safe.h"

/// convert TL style size to size_by_bytes
inline uint32_t decode_size(uint32_t encoded_size) {
  return 1 << encoded_size;
}

//VBridgeImpl::VBridgeImpl() :  {}

void VBridgeImpl::reset() {
  top.clock = 0;
  top.reset = 1;
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


void VBridgeImpl::init_simulator() {
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



void VBridgeImpl::run() {

  // init_spike();
  init_simulator();
  reset();

  // start loop
  while (true) {
    // spike =======> to_rtl_queue =======> rtl
    //loop_until_se_queue_full();

    // loop while there exists unissued insn in queue
    //while (!to_rtl_queue.front().is_issued) {
      // in the RTL thread, for each RTL cycle, valid signals should be checked, generate events, let testbench be able
      // to check the correctness of RTL behavior, benchmark performance signals.
      /*SpikeEvent *se_to_issue = find_se_to_issue();
      se_to_issue->drive_rtl_req(top);
      se_to_issue->drive_rtl_csr(top);

      return_tl_response();*/

      // Make sure any combinatorial logic depending upon inputs that may have changed before we called tick() has settled before the rising edge of the clock.
      //top.clock = 1;
      top.eval();

      // Instruction is_issued, top.req_ready deps on top.req_bits_inst
      /*if (top.req_ready) {
        se_to_issue->is_issued = true;
        se_to_issue->issue_idx = vpi_get_integer("TOP.V.instCount");
        LOG(INFO) << fmt::format("[{}] issue to rtl ({}), , issue index={}", get_t(), se_to_issue->describe_insn(), se_to_issue->issue_idx);
      }*/

      //receive_tl_req();

      // negedge
      top.clock = 0;
      top.eval();
      tfp.dump(2 * ctx.time());
      ctx.timeInc(1);
      // posedge, update registers
      top.clock = 1;
      top.eval();
      tfp.dump(2 * ctx.time() - 1);

      //record_rf_accesses();

      //update_lsu_idx();

      /*if (top.resp_valid) {
        SpikeEvent &se = to_rtl_queue.back();
        se.record_rd_write(top);
        se.check_is_ready_for_commit();
        LOG(INFO) << fmt::format("[{}] rtl commit insn ({})", get_t(), to_rtl_queue.back().describe_insn());
        to_rtl_queue.pop_back();
      }*/

      if (get_t() >= timeout) {
        throw TimeoutException();
      }
    }
   // LOG(INFO) << fmt::format("[{}] all insn in to_rtl_queue is issued, restarting spike", get_t());
  //}
}






