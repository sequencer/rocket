package org.chipsalliance.rocket

import chisel3._
import chisel3.util.BitPat

/** micro-ops for rocket. */
object Operands {
  def X: BitPat = BitPat.dontCare(1)

  def Y: BitPat = BitPat.Y()

  def N: BitPat = BitPat.N()
  object MEM {
    val width: Int = 5
    val X: BitPat = BitPat.dontCare(width)

    /** atomic ALU uop. */
    object XA {
      val SWAP = "b00100".U(width.W)
      val ADD = "b01000".U(width.W)
      val XOR = "b01001".U(width.W)
      val OR = "b01010".U(width.W)
      val AND = "b01011".U(width.W)
      val MIN = "b01100".U(width.W)
      val MAX = "b01101".U(width.W)
      val MINU = "b01110".U(width.W)
      val MAXU = "b01111".U(width.W)
    }
  }

  /** double word uop. */
  object DW {
    val width: Int = 1
    val X = BitPat.dontCare(width)
    val N = false.B
    val Y = true.B
    // todo: jiuyang thinks here is for X propagation but not for sure.
    val XPR = Y
  }

  /** ALU uop */
  object ALU {
    val width: Int = 4
    val X: BitPat = BitPat.dontCare(width)
    val ADD = 0.U(width.W)
    val SL = 1.U(width.W)
    val SEQ = 2.U(width.W)
    val SNE = 3.U(width.W)
    val XOR = 4.U(width.W)
    val SR = 5.U(width.W)
    val OR = 6.U(width.W)
    val AND = 7.U(width.W)
    val SUB = 10.U(width.W)
    val SRA = 11.U(width.W)
    val SLT = 12.U(width.W)
    val SGE = 13.U(width.W)
    val SLTU = 14.U(width.W)
    val SGEU = 15.U(width.W)
    val DIV = XOR
    val DIVU = SR
    val REM = OR
    val REMU = AND
    val MUL = ADD
    val MULH = SL
    val MULHSU = SEQ
    val MULHU = SNE
  }
}

