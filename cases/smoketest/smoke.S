.global _start
_start:
    addi x1, x1, 1
    vsetivli x0, 16, e8, m1, ta, ma
    lui x30, 1
    auipc x31, 0


