// See LICENSE.Berkeley for license details.
// See LICENSE.SiFive for license details.

package org.chipsalliance.rocket.todo

import chisel3._
import chisel3.util.{PriorityEncoder, PriorityEncoderOH, UIntToOH, isPow2, log2Ceil, log2Up}
import chisel3.util.random.LFSR

object Random
{
  def apply(mod: Int, random: UInt): UInt = {
    if (isPow2(mod)) random(log2Ceil(mod)-1,0)
    else PriorityEncoder(partition(apply(1 << log2Up(mod*8), random), mod))
  }
  def apply(mod: Int): UInt = apply(mod, randomizer)
  def oneHot(mod: Int, random: UInt): UInt = {
    if (isPow2(mod)) UIntToOH(random(log2Up(mod)-1,0))
    else VecInit(PriorityEncoderOH(partition(apply(1 << log2Up(mod*8), random), mod))).asUInt
  }
  def oneHot(mod: Int): UInt = oneHot(mod, randomizer)

  private def randomizer = LFSR(16)
  private def partition(value: UInt, slices: Int) =
    Seq.tabulate(slices)(i => value < (((i + 1) << value.getWidth) / slices).U)
}