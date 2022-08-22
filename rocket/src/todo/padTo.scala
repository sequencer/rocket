// See LICENSE.Berkeley for license details.
// See LICENSE.SiFive for license details.

package org.chipsalliance.rocket.todo

import chisel3._
import chisel3.util.Cat

object padTo {
  def apply(x: UInt, n: Int): UInt = {
    require(x.getWidth <= n)
    if (x.getWidth == n) x
    else Cat(0.U((n - x.getWidth).W), x)
  }
}