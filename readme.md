# Rocket

This is the undergoing refactor project of rocket core. The final goal is making the **rocket** core a minimal project with these features:
1. not depending on diplomacy or rocket-chip.
2. decouple bus interfaces, providing TL, ACE to users.
3. provide diplomatic Module for diplomacy-based SoC integration.
4. online cosim simulation framework for fuzzing and fault injection.
5. collect and documenting each parameters, for each configuration, providing corresponding cover points. test cases or fuzzer should hit the cover points to validate these configuration.
6. add uarch cover points for performance event tuning.

## Refactor Procedure
Here are rules for refactoring:
There will be two projects: `rocket` and `diplomatic`, `rocket` will depend on `chisel3` project, `diplomatic` is the source code originally pulled from rocket-chip.

1. ~~`git mv` all scala codes from `rocket-chip` to `diplomatic` project.~~
1. ~~Remove codes which should not be maintained by rocket project, however it will still depend on the `rocket-chip` project.~~
1. ~~Add CI and pass the smoketest(hello world elf).~~  
1. Refactor out `cde` from `rocket`, start to `git mv` file by file from `diplomatic` to `rocket` project, see sequencer/rocket#4 for detail. 
1. a `TilePRCIDomain` will be the sham to the instantible `RocketCore`.  
1. There won't be any unrelated functional change during this refactoring.  

## Test & CI
For now, CI is only a bucket of sanity test, running cases from [riscv-tests](https://github.com/riscv-software-src/riscv-tests/) under the default cases.  
In the future, Rocket Core will get rid of the DTM based simulation, since it:
1. requires a correct debug mode, LSU to start test
1. cannot diff with a reference model(spike, qemu, etc)
1. slow to load binary, a large start-up time

A new online difftest should be provided with these features:
1. spike as reference model
1. capture architecture events, for each cycles, comparing event with architecutre events generated from spike

## Pending PRs
Philosophy of this repository is **fast break and fast fix**.
This repository always tracks remote developing branches, it may need some patches to work, `make patch` will append below in sequence:
<!-- BEGIN-PATCH -->
rocket-chip https://github.com/chipsalliance/rocket-chip/pull/2968.diff  
rocket-chip https://github.com/chipsalliance/rocket-chip/pull/3013.diff  
rocket-chip https://github.com/chipsalliance/rocket-chip/pull/3066.diff  
rocket-chip https://github.com/chipsalliance/rocket-chip/pull/3103.diff  
<!-- END-PATCH -->
