package org.chipsalliance.rocket.todo

import chisel3._
import chisel3.util.Fill

package object implict {
  implicit class AndNot(private val x: UInt) extends AnyVal {
    // like x & ~y, but first truncate or zero-extend y to x's width
    def andNot(y: UInt): UInt = x & ~(y | (x & 0.U))
  }

  implicit class SextTo(private val x: UInt) extends AnyVal {
    def sextTo(n: Int): UInt = {
      require(x.getWidth <= n)
      if (x.getWidth == n) x
      else Fill(n - x.getWidth, x(x.getWidth - 1)) ## x
    }
  }

  implicit class PadTo(private val x: UInt) extends AnyVal {
    def padTo(n: Int): UInt = {
      require(x.getWidth <= n)
      if (x.getWidth == n) x
      else 0.U((n - x.getWidth).W) ## x
    }
  }

  implicit class Grouped(private val x: UInt) extends AnyVal {
    def grouped(width: Int): Vec[UInt] =
      VecInit((0 until x.getWidth by width).map(base => x(base + width - 1, base)))
  }

}