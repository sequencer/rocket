#pragma once

#include <queue>
#include <optional>

#include "mmu.h"

#include "VV.h"
#include "verilated_fst_c.h"


class SpikeEvent;



class VBridgeImpl {
public:
  //explicit VBridgeImpl();

  ~VBridgeImpl();


  // todo remove this.
  void configure_simulator(int argc, char **argv);

  void run();

  uint8_t load(uint64_t address);

private:
  VerilatedContext ctx;
  VV top;
  VerilatedFstC tfp;

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

  inline void reset();



  void init_simulator();
  void terminate_simulator();
  uint64_t get_t();


};
