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
}

