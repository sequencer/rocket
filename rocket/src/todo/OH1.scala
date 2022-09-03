package org.chipsalliance.rocket.todo

import chisel3._
import chisel3.util.OHToUInt

object OH1 {
  def OH1ToOH(x: UInt): UInt = ((x << 1: UInt) | 1.U(1.W)) & ~(0.U(1.W) ## x)

  def OH1ToUInt(x: UInt): UInt = OHToUInt(OH1ToOH(x))

  def UIntToOH1(x: UInt, width: Int): UInt = ~(-1.S(width.W).asUInt << x)(width - 1, 0)

  def UIntToOH1(x: UInt): UInt = UIntToOH1(x, (1 << x.getWidth) - 1)
}
