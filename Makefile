init:
	git submodule update --init

patch:
	find patches -type f | awk -F/ '{print("(echo "$$0" && cd dependencies/" $$2 " && git apply -3 --ignore-space-change --ignore-whitespace ../../" $$0 ")")}' | sh

depatch:
	git submodule foreach 'git reset --hard && git clean -fdx'

bump:
	git submodule foreach git stash
	git submodule update --remote
	git add dependencies

update-patches:
	rm -rf patches
	sed '/BEGIN-PATCH/,/END-PATCH/!d;//d' readme.md | awk '{print("mkdir -p patches/" $$1 " && wget " $$2 " -P patches/" $$1 )}' | parallel
	git add patches

bsp:
	mill -i mill.bsp.BSP/install

compile:
	mill -i -j 0 __.compile

test:
	mill -i -j 0 tests.run.rv64default.run

#mytest:
	#rm -rf out/cases
	#rm -rf out/cosim/emulator
	#rm -rf out/mytests
	#mill mytests.smoketest --cycles 100
show:
	riscv64-elf-objdump -Dzr out/mycases/cases/riscvtests/rv64mi-p/rv64mi-p-csr | less

smoke:
	riscv64-elf-objdump -Dzr out/cases/smoketest/compile.dest/smoketest.elf | less


clean:
	git clean -fd


