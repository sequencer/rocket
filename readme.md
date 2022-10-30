# Rocket

This is the undergoing refactor project of rocket core.

The porting flow:
1. Compile rocket with rocket-chip.
2. Minimal Verilator-based CI to run riscv-tests.
3. refactor start

## Refactor Procedure
Here are rules for refactoring:
There will be two projects: `rocket` and `diplomatic`, `rocket` will depend on `chisel3` project, `diplomatic` is the source code originally pulled from rocket-chip.

1. ~~`git mv` all scala codes from `rocket-chip` to `diplomatic` project.~~
1. ~~Remove codes which should not be maintained by rocket project, however it will still depend on the `rocket-chip` project.~~
1. Add CI and pass the smoketest(hello world elf).  
1. Refactor out `cde` from `rocket`, start to `git mv` file by file from `diplomatic` to `rocket` project.  
 - sequencer/rocket#1
 - sequencer/rocket#2
1. Leave `RocketTile` in `diplomatic` for SoC integration.  
1. There won't be any unrelated change during this refactoring. 

## Pending PRs
Philosophy of this repository is **fast break and fast fix**.
This repository always tracks remote developing branches, it may need some patches to work, `make patch` will append below in sequence:
<!-- BEGIN-PATCH -->
rocket-chip https://github.com/chipsalliance/rocket-chip/pull/2968.diff  
rocket-chip https://github.com/chipsalliance/rocket-chip/pull/3013.diff  
rocket-chip https://github.com/chipsalliance/rocket-chip/pull/3066.diff  
rocket-chip https://github.com/chipsalliance/rocket-chip/pull/3103.diff  
<!-- END-PATCH -->