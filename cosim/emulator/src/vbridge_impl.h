#pragma once

#include <queue>
#include <optional>

#include "mmu.h"

#include "VV.h"
#include "verilated_fst_c.h"

#include "spike_event.h"
#include "simple_sim.h"
#include "vbridge_config.h"


class SpikeEvent;

struct TLReqRecord {
    uint64_t data;
    uint32_t size_by_byte;
    uint16_t source;

    /// when opType set to nil, it means this record is already sent back
    enum class opType {
        Nil, Get, PutFullData
    } op;
    int remaining_cycles;

    TLReqRecord(uint64_t data, uint32_t size_by_byte, uint16_t source, opType op, int cycles) :
        data(data), size_by_byte(size_by_byte), source(source), op(op), remaining_cycles(cycles) {};
};


class VBridgeImpl {
public:
  explicit VBridgeImpl();

  ~VBridgeImpl();

  void setup(const std::string &bin, const std::string &wave, uint64_t reset_vector, uint64_t cycles);
  // todo remove this.
  void configure_simulator(int argc, char **argv);

  void run();

  uint8_t load(uint64_t address);

private:
  // verilator context
  VerilatedContext ctx;
  VV top;
  VerilatedFstC tfp;
  // mem
  simple_sim sim;
  // spike init
  isa_parser_t isa;
  processor_t proc;
  // spike event

  // parameter used in verilator
  uint64_t _cycles;
  /// file path of executeable binary file, which will be executed.
  std::string bin;
  /// generated waveform path.
  std::string wave;
  /// reset vector of
  uint64_t reset_vector{};
  /// RTL timeout cycles
  /// note: this is not the real system cycles, scalar instructions is evaulated via spike, which is not recorded.
  uint64_t timeout{};

  // spike
  const size_t to_rtl_queue_size = 5;
  std::list<SpikeEvent> to_rtl_queue;

  std::map<reg_t, TLReqRecord> tl_banks;

  inline void reset();

  void init_spike();
  void loop_until_se_queue_full();
  std::optional<SpikeEvent> spike_step();
  std::optional<SpikeEvent> create_spike_event(insn_fetch_t fetch);
  SpikeEvent *find_se_to_issue();

  void init_simulator();
  void terminate_simulator();
  uint64_t get_t();

  void return_tl_response();
  void receive_tl_req();

  int get_mem_req_cycles() {
    return 1;
  };


};
