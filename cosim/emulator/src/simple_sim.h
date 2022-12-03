#pragma once

#include <fstream>

#include <fmt/core.h>
#include <glog/logging.h>

#include "simif.h"

class simple_sim : public simif_t {
private:
    char *mem;

public:
    explicit simple_sim(size_t mem_size) {
      mem = new char[(mem_size*4)];
      LOG(INFO) << fmt::format("mem size = {:08X}",mem_size*4);
    }

    ~simple_sim() override {
      delete[] mem;
    }

    void load(const std::string &fname, size_t reset_vector) {
      std::ifstream fs(fname, std::ifstream::binary);
      assert(fs.is_open());

      size_t offset = 0x80000000;
      while (!fs.eof()) {
        fs.read(&mem[offset], 1024);
        offset += fs.gcount();
      }



      std::ifstream fs_init("/home/yyq/Projects/rocket/out/cases/entrance/compile.dest/entrance", std::ifstream::binary);
      assert(fs_init.is_open());
      size_t cnt = 0x1000;
      while (!fs_init.eof()) {
        fs_init.read(&mem[cnt], 1024);
      }

      //print mem
      uint32_t addr = 0x1000;
      for(int i=0; i<4 ; i++){
        uint32_t insn = 0;
        for (int j = 0; j < 4; j++) {
          insn += (uint64_t) *addr_to_mem(addr + j + i*4) << (j * 8);
        }
        LOG(INFO) << fmt::format("scan mem: {:08X} , at:{:08X}",insn,addr + i*4);
      }


    }

    // should return NULL for MMIO addresses
    char *addr_to_mem(reg_t addr) override {
      return &mem[addr];
    }

    bool mmio_load(reg_t addr, size_t len, uint8_t *bytes) override {
      assert(false && "not implemented");
    }

    bool mmio_store(reg_t addr, size_t len, const uint8_t *bytes) override {
      assert(false && "not implemented");
    }

    // Callback for processors to let the simulation know they were reset.
    void proc_reset(unsigned id) override {
      // maybe nothing to do
    }

    const char *get_symbol(uint64_t addr) override {
      LOG(FATAL) << "not implemented";
    }
};
