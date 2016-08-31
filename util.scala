// See LICENSE for license details.

package rocket

import Chisel._
import uncore.util._
import scala.math._
import cde.{Parameters, Field}

object Util {
  implicit def uintToBitPat(x: UInt): BitPat = BitPat(x)
  implicit def intToUInt(x: Int): UInt = UInt(x)
  implicit def bigIntToUInt(x: BigInt): UInt = UInt(x)
  implicit def booleanToBool(x: Boolean): Bits = Bool(x)
  implicit def intSeqToUIntSeq(x: Seq[Int]): Seq[UInt] = x.map(UInt(_))
  implicit def wcToUInt(c: WideCounter): UInt = c.value

  implicit class UIntToAugmentedUInt(val x: UInt) extends AnyVal {
    def sextTo(n: Int): UInt =
      if (x.getWidth == n) x
      else Cat(Fill(n - x.getWidth, x(x.getWidth-1)), x)

    def extract(hi: Int, lo: Int): UInt = {
      if (hi == lo-1) UInt(0)
      else x(hi, lo)
    }
  }

  implicit class BooleanToAugmentedBoolean(val x: Boolean) extends AnyVal {
    def toInt: Int = if (x) 1 else 0

    // this one's snagged from scalaz
    def option[T](z: T): Option[T] = if (x) Some(z) else None
  }
}

import Util._

object Str
{
  def apply(s: String): UInt = {
    var i = BigInt(0)
    require(s.forall(validChar _))
    for (c <- s)
      i = (i << 8) | c
    UInt(i, s.length*8)
  }
  def apply(x: Char): UInt = {
    require(validChar(x))
    UInt(x.toInt, 8)
  }
  def apply(x: UInt): UInt = apply(x, 10)
  def apply(x: UInt, radix: Int): UInt = {
    val rad = UInt(radix)
    val w = x.getWidth
    require(w > 0)

    var q = x
    var s = digit(q % rad)
    for (i <- 1 until ceil(log(2)/log(radix)*w).toInt) {
      q = q / rad
      s = Cat(Mux(Bool(radix == 10) && q === UInt(0), Str(' '), digit(q % rad)), s)
    }
    s
  }
  def apply(x: SInt): UInt = apply(x, 10)
  def apply(x: SInt, radix: Int): UInt = {
    val neg = x < SInt(0)
    val abs = x.abs
    if (radix != 10) {
      Cat(Mux(neg, Str('-'), Str(' ')), Str(abs, radix))
    } else {
      val rad = UInt(radix)
      val w = abs.getWidth
      require(w > 0)

      var q = abs
      var s = digit(q % rad)
      var needSign = neg
      for (i <- 1 until ceil(log(2)/log(radix)*w).toInt) {
        q = q / rad
        val placeSpace = q === UInt(0)
        val space = Mux(needSign, Str('-'), Str(' '))
        needSign = needSign && !placeSpace
        s = Cat(Mux(placeSpace, space, digit(q % rad)), s)
      }
      Cat(Mux(needSign, Str('-'), Str(' ')), s)
    }
  }

  private def digit(d: UInt): UInt = Mux(d < UInt(10), Str('0')+d, Str(('a'-10).toChar)+d)(7,0)
  private def validChar(x: Char) = x == (x & 0xFF)
}

object Split
{
  // is there a better way to do do this?
  def apply(x: Bits, n0: Int) = {
    val w = checkWidth(x, n0)
    (x(w-1,n0), x(n0-1,0))
  }
  def apply(x: Bits, n1: Int, n0: Int) = {
    val w = checkWidth(x, n1, n0)
    (x(w-1,n1), x(n1-1,n0), x(n0-1,0))
  }
  def apply(x: Bits, n2: Int, n1: Int, n0: Int) = {
    val w = checkWidth(x, n2, n1, n0)
    (x(w-1,n2), x(n2-1,n1), x(n1-1,n0), x(n0-1,0))
  }

  private def checkWidth(x: Bits, n: Int*) = {
    val w = x.getWidth
    def decreasing(x: Seq[Int]): Boolean =
      if (x.tail.isEmpty) true
      else x.head >= x.tail.head && decreasing(x.tail)
    require(decreasing(w :: n.toList))
    w
  }
}

// a counter that clock gates most of its MSBs using the LSB carry-out
case class WideCounter(width: Int, inc: UInt = UInt(1))
{
  private val isWide = width > 2*inc.getWidth
  private val smallWidth = if (isWide) inc.getWidth max log2Up(width) else width
  private val small = Reg(init=UInt(0, smallWidth))
  private val nextSmall = small +& inc
  small := nextSmall

  private val large = if (isWide) {
    val r = Reg(init=UInt(0, width - smallWidth))
    when (nextSmall(smallWidth)) { r := r + UInt(1) }
    r
  } else null

  val value = if (isWide) Cat(large, small) else small

  def := (x: UInt) = {
    small := x
    if (isWide) large := x >> smallWidth
  }
}

object Random
{
  def apply(mod: Int, random: UInt): UInt = {
    if (isPow2(mod)) random(log2Up(mod)-1,0)
    else PriorityEncoder(partition(apply(1 << log2Up(mod*8), random), mod))
  }
  def apply(mod: Int): UInt = apply(mod, randomizer)
  def oneHot(mod: Int, random: UInt): UInt = {
    if (isPow2(mod)) UIntToOH(random(log2Up(mod)-1,0))
    else PriorityEncoderOH(partition(apply(1 << log2Up(mod*8), random), mod)).asUInt
  }
  def oneHot(mod: Int): UInt = oneHot(mod, randomizer)

  private def randomizer = LFSR16()
  private def round(x: Double): Int =
    if (x.toInt.toDouble == x) x.toInt else (x.toInt + 1) & -2
  private def partition(value: UInt, slices: Int) =
    Seq.tabulate(slices)(i => value < round((i << value.getWidth).toDouble / slices))
}
