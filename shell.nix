{ pkgs ? import ./dependencies/nix/pinned-nixpkgs.nix {}}:
let
  circt = import ./dependencies/nix/circt.nix { inherit pkgs; };
  llvm = import ./dependencies/nix/llvm-mlir.nix { inherit pkgs; };
  spike = import ./dependencies/nix/spike.nix { inherit pkgs; };
  rvtests = import ./dependencies/nix/riscv-tests.nix { inherit pkgs; };
in
  pkgs.mkShell {
    nativeBuildInputs = [
      circt
      pkgs.llvm
      pkgs.lld
      pkgs.clang
      pkgs.openjdk8
      pkgs.mill
      pkgs.protobuf
      pkgs.antlr4
      pkgs.verilator
      pkgs.dtc
      pkgs.cmake
      pkgs.ninja
    ];
    LOCALE_ARCHIVE = "${pkgs.glibcLocales}/lib/locale/locale-archive";
    LD_LIBRARY_PATH = "${pkgs.stdenv.cc.cc.lib}/lib/";
    SPIKE_ROOT = "${spike}";
    RISCV_TESTS_ROOT = "${rvtests}";
  }
