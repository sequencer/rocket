.global _start
_start:
    lui t0, 0x80000
    slli t0,t0,32
    srli t0,t0,32
    jr t0



