// See LICENSE.SiFive for license details.
// See LICENSE.Berkeley for license details.

package org.chipsalliance.rocket

import chisel3._
import chisel3.util._
import org.chipsalliance.rocket.Operands.MEM

class StoreGen(typ: UInt, addr: UInt, dat: UInt, maxSize: Int) {
  val size = typ(log2Up(log2Up(maxSize)+1)-1,0)
  def misaligned: Bool =
    (addr & ((1.U << size) - 1.U)(log2Up(maxSize)-1,0)).orR

  def mask = {
    var res = 1.U
    for (i <- 0 until log2Up(maxSize)) {
      val upper = Mux(addr(i), res, 0.U) | Mux(size >= (i+1).U, ((BigInt(1) << (1 << i))-1).U, 0.U)
      val lower = Mux(addr(i), 0.U, res)
      res = Cat(upper, lower)
    }
    res
  }

  protected def genData(i: Int): UInt =
    if (i >= log2Up(maxSize)) dat
    else Mux(size === i.U, Fill(1 << (log2Up(maxSize)-i), dat((8 << i)-1,0)), genData(i+1))

  def data = genData(0)
  def wordData = genData(2)
}

class LoadGen(typ: UInt, signed: Bool, addr: UInt, dat: UInt, zero: Bool, maxSize: Int) {
  private val size = new StoreGen(typ, addr, dat, maxSize).size

  private def genData(logMinSize: Int): UInt = {
    var res = dat
    for (i <- log2Up(maxSize)-1 to logMinSize by -1) {
      val pos = 8 << i
      val shifted = Mux(addr(i), res(2*pos-1,pos), res(pos-1,0))
      val doZero = (i == 0).B && zero
      val zeroed = Mux(doZero, 0.U, shifted)
      res = Cat(Mux(size === i.U || doZero, Fill(8*maxSize-pos, signed && zeroed(pos-1)), res(8*maxSize-1,pos)), zeroed)
    }
    res
  }

  def wordData = genData(2)
  def data = genData(0)
}
class AMOALUIO(operandBits: Int) extends Bundle {
  val mask = Input(UInt((operandBits / 8).W))
  val cmd = Input(UInt(MEM.width.W))
  val lhs = Input(UInt(operandBits.W))
  val rhs = Input(UInt(operandBits.W))
  val out = Output(UInt(operandBits.W))
  val out_unmasked = Output(UInt(operandBits.W))
}
class AMOALU(operandBits: Int) extends Module {
  val minXLen = 32
  val widths = (0 to log2Ceil(operandBits / minXLen)).map(minXLen << _)

  val io = IO(new AMOALUIO(operandBits))

  val max = io.cmd === MEM.XA.MAX || io.cmd === MEM.XA.MAXU
  val min = io.cmd === MEM.XA.MIN || io.cmd === MEM.XA.MINU
  val add = io.cmd === MEM.XA.ADD
  val logic_and = io.cmd === MEM.XA.OR || io.cmd === MEM.XA.AND
  val logic_xor = io.cmd === MEM.XA.XOR || io.cmd === MEM.XA.OR

  val adder_out = {
    // partition the carry chain to support sub-xLen addition
    val mask = ~(0.U(operandBits.W) +: widths.init.map(w => !io.mask(w/8-1) << (w-1))).reduce(_|_)
    (io.lhs & mask) + (io.rhs & mask)
  }

  val less = {
    // break up the comparator so the lower parts will be CSE'd
    def isLessUnsigned(x: UInt, y: UInt, n: Int): Bool = {
      if (n == minXLen) x(n-1, 0) < y(n-1, 0)
      else x(n-1, n/2) < y(n-1, n/2) || x(n-1, n/2) === y(n-1, n/2) && isLessUnsigned(x, y, n/2)
    }

    def isLess(x: UInt, y: UInt, n: Int): Bool = {
      val signed = {
        val mask = MEM.XA.MIN ^ MEM.XA.MINU
        (io.cmd & mask) === (MEM.XA.MIN & mask)
      }
      Mux(x(n-1) === y(n-1), isLessUnsigned(x, y, n), Mux(signed, x(n-1), y(n-1)))
    }

    PriorityMux(widths.reverse.map(w => (io.mask(w/8/2), isLess(io.lhs, io.rhs, w))))
  }

  val minmax = Mux(Mux(less, min, max), io.lhs, io.rhs)
  val logic =
    Mux(logic_and, io.lhs & io.rhs, 0.U) |
    Mux(logic_xor, io.lhs ^ io.rhs, 0.U)
  val out =
    Mux(add,                    adder_out,
    Mux(logic_and || logic_xor, logic,
                                minmax))

  val wmask = FillInterleaved(8, io.mask)
  io.out := wmask & out | ~wmask & io.lhs
  io.out_unmasked := out
}
