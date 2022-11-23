// See LICENSE.SiFive for license details.
// See LICENSE.Berkeley for license details.

package org.chipsalliance.rocket

import chisel3._
import chisel3.util.{Cat, Fill, Reverse}
import org.chipsalliance.rocket.Operands.{DW, ALU}

class ALUIO(xLen: Int) extends Bundle {
  val dw = Input(UInt(DW.width.W))
  val fn = Input(UInt(ALU.width.W))
  val in2 = Input(UInt(xLen.W))
  val in1 = Input(UInt(xLen.W))
  val out = Output(UInt(xLen.W))
  val adder_out = Output(UInt(xLen.W))
  val cmp_out = Output(Bool())
}
class ALU(xLen: Int) extends Module {
  val io: ALUIO = IO(new ALUIO(xLen))

  // ADD, SUB
  val in2_inv = Mux(isSub(io.fn), ~io.in2, io.in2)
  val in1_xor_in2 = io.in1 ^ in2_inv
  io.adder_out := io.in1 + in2_inv + isSub(io.fn)

  // SLT, SLTU
  val slt =
    Mux(io.in1(xLen-1) === io.in2(xLen-1), io.adder_out(xLen-1),
    Mux(cmpUnsigned(io.fn), io.in2(xLen-1), io.in1(xLen-1)))
  io.cmp_out := cmpInverted(io.fn) ^ Mux(cmpEq(io.fn), in1_xor_in2 === 0.U, slt)

  // SLL, SRL, SRA
  val (shamt, shin_r) =
    if (xLen == 32) (io.in2(4,0), io.in1)
    else {
      require(xLen == 64)
      val shin_hi_32 = Fill(32, isSub(io.fn) && io.in1(31))
      val shin_hi = Mux(io.dw === DW.Y, io.in1(63,32), shin_hi_32)
      val shamt = Cat(io.in2(5) & (io.dw === DW.Y), io.in2(4, 0))
      (shamt, Cat(shin_hi, io.in1(31,0)))
    }
  val shin = Mux(io.fn === ALU.SR || io.fn === ALU.SRA, shin_r, Reverse(shin_r))
  val shout_r = (Cat(isSub(io.fn) & shin(xLen-1), shin).asSInt >> shamt)(xLen-1,0)
  val shout_l = Reverse(shout_r)
  val shout = Mux(io.fn === ALU.SR || io.fn === ALU.SRA, shout_r, 0.U) |
              Mux(io.fn === ALU.SL,                      shout_l, 0.U)

  // AND, OR, XOR
  val logic = Mux(io.fn === ALU.XOR || io.fn === ALU.OR, in1_xor_in2, 0.U) |
              Mux(io.fn === ALU.OR || io.fn === ALU.AND, io.in1 & io.in2, 0.U)
  val shift_logic = (isCmp(io.fn) && slt) | logic | shout
  val out = Mux(io.fn === ALU.ADD || io.fn === ALU.SUB, io.adder_out, shift_logic)

  io.out := out
  if (xLen > 32) {
    require(xLen == 64)
    when (io.dw === DW.N) { io.out := Cat(Fill(32, out(31)), out(31, 0)) }
  }

  private def isSub(cmd: UInt): Bool = cmd(3)
  private def isCmp(cmd: UInt): Bool = cmd >= ALU.SLT
  private def cmpUnsigned(cmd: UInt) = cmd(1)
  private def cmpInverted(cmd: UInt) = cmd(0)
  private def cmpEq(cmd: UInt) = !cmd(3)
}
