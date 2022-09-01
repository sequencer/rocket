package org.chipsalliance.rocket.todo

import chisel3._
import chisel3.util.log2Up

case class WideCounter(width: Int, inc: UInt = 1.U, reset: Boolean = true, inhibit: Bool = false.B)
{
  private val isWide: Boolean = width > 2*inc.getWidth
  private val smallWidth: Int = if (isWide) inc.getWidth max log2Up(width) else width
  private val small: UInt = if (reset) RegInit(0.U(smallWidth.W)) else Reg(UInt(smallWidth.W))
  private val nextSmall: UInt = small +& inc
  when (!inhibit) { small := nextSmall }

  private val large: UInt = if (isWide) {
    val r = if (reset) RegInit(0.U((width - smallWidth).W)) else Reg(UInt((width - smallWidth).W))
    when (nextSmall(smallWidth) && !inhibit) { r := r + 1.U }
    r
  } else null

  val value: UInt = if (isWide) large ## small else small

  lazy val carryOut: UInt = {
    val lo: UInt = (small ^ nextSmall) >> 1
    if (!isWide) lo else {
      val hi = Mux(nextSmall(smallWidth), large ^ (large +& 1.U), 0.U) >> 1
      hi ## lo
    }
  }

  def := (x: UInt): Unit = {
    small := x
    if (isWide) large := x >> smallWidth
  }
}
