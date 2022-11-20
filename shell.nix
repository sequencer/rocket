let
  packed = builtins.fetchGit {
    url = "https://github.com/CircuitCoder/meow.nix";
    rev = "73a5e6e89c6b68dada14ddd31e78d9b40538f6c5";
  };
in
{ pkgs ? import "${packed}/pinned-nixpkgs.nix" {}}:
let
  circt = import "${packed}/circt.nix" { inherit pkgs; };
  spike = import "${packed}/spike.nix" { inherit pkgs; };
  rvtests = import "${packed}/riscv-tests.nix" { inherit pkgs; };
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
