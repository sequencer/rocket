// See LICENSE.SiFive for license details.

package org.chipsalliance.rocket

import chisel3._
import chisel3.util.{Cat, log2Ceil}
import org.chipsalliance.rocket.todo.OH1.UIntToOH1

class PMPConfig extends Bundle {
  val l: Bool = Bool()
  val res: UInt = UInt(2.W)
  val a: UInt = UInt(2.W)
  val x: Bool = Bool()
  val w: Bool = Bool()
  val r: Bool = Bool()
}

object PMP {
  def lgAlign: Int = 2

  def apply(reg: PMPReg): PMP = {
    val pmp: PMP = Wire(new PMP(reg.paddrBits, reg.pmpGranularity, reg.pgLevels, reg.pgIdxBits, reg.pgLevelBits))
    pmp.cfg := reg.cfg
    pmp.addr := reg.addr
    pmp.mask := pmp.computeMask
    pmp
  }
}

class PMPReg(val paddrBits: Int, val pmpGranularity: Int, val pgLevels: Int, val pgIdxBits: Int, val pgLevelBits: Int) extends Bundle {
  val cfg = new PMPConfig
  val addr = UInt((paddrBits - PMP.lgAlign).W)

  def reset(): Unit = {
    cfg.a := 0.U
    cfg.l := 0.U
  }

  def readAddr: UInt = if (log2Ceil(pmpGranularity) == PMP.lgAlign) addr else {
    val mask = ((BigInt(1) << (log2Ceil(pmpGranularity) - PMP.lgAlign)) - 1).U
    Mux(napot, addr | (mask >> 1: UInt), ~((~addr: UInt) | mask))
  }
  def napot: Bool = cfg.a(1)
  def torNotNAPOT: Bool = cfg.a(0)
  def tor: Bool = !napot && torNotNAPOT
  def cfgLocked: Bool = cfg.l
  def addrLocked(next: PMPReg): Bool = cfgLocked || next.cfgLocked && next.tor
}

class PMP(paddrBits: Int, pmpGranularity: Int, pgLevels: Int, pgIdxBits: Int, pgLevelBits: Int) extends PMPReg(paddrBits, pmpGranularity,pgLevels, pgIdxBits, pgLevelBits) {
  val mask = UInt(paddrBits.W)

  import PMP._
  def computeMask: UInt = {
    val base = Cat(addr, cfg.a(0)) | ((pmpGranularity - 1) >> lgAlign).U
    Cat(base & ~(base + 1.U): UInt, ((1 << lgAlign) - 1).U)
  }
  private def comparand: UInt = ~((~(addr << lgAlign): UInt) | (pmpGranularity - 1).U)

  private def pow2Match(x: UInt, lgSize: UInt, lgMaxSize: Int): Bool = {
    def eval(a: UInt, b: UInt, m: UInt): Bool = ((a ^ b) & ~m: UInt) === 0.U
    if (lgMaxSize <= log2Ceil(pmpGranularity)) {
      eval(x, comparand, mask)
    } else {
      // break up the circuit; the MSB part will be CSE'd
      val lsbMask: UInt = mask | UIntToOH1(lgSize, lgMaxSize)
      val msbMatch: Bool = eval(x >> lgMaxSize: UInt, comparand >> lgMaxSize, mask >> lgMaxSize)
      val lsbMatch: Bool = eval(x(lgMaxSize-1, 0), comparand(lgMaxSize-1, 0), lsbMask(lgMaxSize-1, 0))
      msbMatch && lsbMatch
    }
  }

  private def boundMatch(x: UInt, lsbMask: UInt, lgMaxSize: Int) = {
    if (lgMaxSize <= log2Ceil(pmpGranularity)) {
      x < comparand
    } else {
      // break up the circuit; the MSB part will be CSE'd
      val msbsLess: Bool = (x >> lgMaxSize: UInt) < (comparand >> lgMaxSize: UInt)
      val msbsEqual: Bool = ((x >> lgMaxSize: UInt) ^ (comparand >> lgMaxSize: UInt)) === 0.U
      val lsbsLess =  (x(lgMaxSize-1, 0) | lsbMask) < comparand(lgMaxSize-1, 0)
      msbsLess || (msbsEqual && lsbsLess)
    }
  }

  private def lowerBoundMatch(x: UInt, lgSize: UInt, lgMaxSize: Int): Bool =
    !boundMatch(x, UIntToOH1(lgSize, lgMaxSize), lgMaxSize)

  private def upperBoundMatch(x: UInt, lgMaxSize: Int): Bool =
    boundMatch(x, 0.U, lgMaxSize)

  private def rangeMatch(x: UInt, lgSize: UInt, lgMaxSize: Int, prev: PMP): Bool =
    prev.lowerBoundMatch(x, lgSize, lgMaxSize) && upperBoundMatch(x, lgMaxSize)

  private def pow2Homogeneous(x: UInt, pgLevel: UInt): Bool = {
    val maskHomogeneous = VecInit(pgLevelMap { idxBits => if (idxBits > paddrBits) false.B else mask(idxBits - 1) }) (pgLevel)
    maskHomogeneous || (pgLevelMap { idxBits => ((x ^ comparand) >> idxBits: UInt) =/= 0.U } (pgLevel))
  }

  private def pgLevelMap[T](f: Int => T): Seq[T] = (0 until pgLevels).map { i =>
    f(pgIdxBits + (pgLevels - 1 - i) * pgLevelBits)
  }

  private def rangeHomogeneous(x: UInt, pgLevel: UInt, prev: PMP): Bool = {
    val beginsAfterLower = !(x < prev.comparand)
    val beginsAfterUpper = !(x < comparand)

    val pgMask = VecInit(pgLevelMap { idxBits => (((BigInt(1) << paddrBits) - (BigInt(1) << idxBits)) max 0).U }) (pgLevel)
    val endsBeforeLower = (x & pgMask) < (prev.comparand & pgMask)
    val endsBeforeUpper = (x & pgMask) < (comparand & pgMask)

    endsBeforeLower || beginsAfterUpper || (beginsAfterLower && endsBeforeUpper)
  }

  // returns whether this PMP completely contains, or contains none of, a page
  def homogeneous(x: UInt, pgLevel: UInt, prev: PMP): Bool =
    Mux(napot, pow2Homogeneous(x, pgLevel), !torNotNAPOT || rangeHomogeneous(x, pgLevel, prev))

  // returns whether this matching PMP fully contains the access
  def aligned(x: UInt, lgSize: UInt, lgMaxSize: Int, prev: PMP): Bool = if (lgMaxSize <= log2Ceil(pmpGranularity)) true.B else {
    val lsbMask = UIntToOH1(lgSize, lgMaxSize)
    val straddlesLowerBound = ((x >> lgMaxSize: UInt) ^ (prev.comparand >> lgMaxSize)) === 0.U && (prev.comparand(lgMaxSize-1, 0) & ~x(lgMaxSize-1, 0): UInt) =/= 0.U
    val straddlesUpperBound = ((x >> lgMaxSize: UInt) ^ (comparand >> lgMaxSize)) === 0.U && (comparand(lgMaxSize-1, 0) & (x(lgMaxSize-1, 0) | lsbMask)) =/= 0.U
    val rangeAligned = !(straddlesLowerBound || straddlesUpperBound)
    val pow2Aligned = (lsbMask & ~mask(lgMaxSize-1, 0): UInt) === 0.U
    Mux(napot, pow2Aligned, rangeAligned)
  }

  // returns whether this PMP matches at least one byte of the access
  def hit(x: UInt, lgSize: UInt, lgMaxSize: Int, prev: PMP): Bool =
    Mux(napot, pow2Match(x, lgSize, lgMaxSize), torNotNAPOT && rangeMatch(x, lgSize, lgMaxSize, prev))
}

class PMPHomogeneityChecker(pmps: Seq[PMP]) {
  def apply(addr: UInt, pgLevel: UInt): Bool = {
    pmps.foldLeft((true.B, 0.U.asTypeOf(pmps.head))) { case ((h, prev), pmp) =>
      (h && pmp.homogeneous(addr, pgLevel, prev), pmp)
    }._1
  }
}

class PMPChecker(lgMaxSize: Int, nPMPs: Int, paddrBits: Int, pmpGranularity: Int, val pgLevels: Int, val pgIdxBits: Int, val pgLevelBits: Int) extends Module {
  val io = IO(new Bundle {
    val prv: UInt = Input(UInt(PRV.SZ.W))
    val pmp: Vec[PMP] = Input(Vec(nPMPs, new PMP(paddrBits, pmpGranularity, pgLevels, pgIdxBits, pgLevelBits)))
    val addr: UInt = Input(UInt(paddrBits.W))
    val size: UInt = Input(UInt(log2Ceil(lgMaxSize + 1).W))
    val r: Bool = Output(Bool())
    val w: Bool = Output(Bool())
    val x: Bool = Output(Bool())
  })

  val default: Bool = if (io.pmp.isEmpty) true.B else io.prv > PRV.S.U
  val pmp0: PMP = WireInit(0.U.asTypeOf(new PMP(paddrBits, pmpGranularity, pgLevels, pgIdxBits, pgLevelBits)))
  pmp0.cfg.r := default
  pmp0.cfg.w := default
  pmp0.cfg.x := default

  val res = (io.pmp zip (pmp0 +: io.pmp)).reverse.foldLeft(pmp0) { case (prev, (pmp, prevPMP)) =>
    val hit: Bool = pmp.hit(io.addr, io.size, lgMaxSize, prevPMP)
    val ignore: Bool = default && !pmp.cfg.l
    val aligned: Bool = pmp.aligned(io.addr, io.size, lgMaxSize, prevPMP)

    for ((name, idx) <- Seq("no", "TOR", if (pmpGranularity <= 4) "NA4" else "", "NAPOT").zipWithIndex; if name.nonEmpty)
      cover(pmp.cfg.a === idx.U, s"The cfg access is set to ${name} access, Cover PMP access mode setting")

    cover(pmp.cfg.l === 0x1.U, s"The cfg lock is set to high, Cover PMP lock mode setting")
   
    // Not including Write and no Read permission as the combination is reserved
    for ((name, idx) <- Seq("no", "RO", "", "RW", "X", "RX", "", "RWX").zipWithIndex; if name.nonEmpty)
      cover((Cat(pmp.cfg.x, pmp.cfg.w, pmp.cfg.r) === idx), s"The permission is set to ${name} access, Cover PMP access permission setting")

    for ((name, idx) <- Seq("", "TOR", if (pmpGranularity <= 4) "NA4" else "", "NAPOT").zipWithIndex; if name.nonEmpty) {
      cover(!ignore && hit && aligned && pmp.cfg.a === idx, s"The access matches ${name} mode, Cover PMP access")
      cover(pmp.cfg.l && hit && aligned && pmp.cfg.a === idx, s"The access matches ${name} mode with lock bit high, Cover PMP access with lock bit")
    }

    val cur = WireInit(pmp)
    cur.cfg.r := aligned && (pmp.cfg.r || ignore)
    cur.cfg.w := aligned && (pmp.cfg.w || ignore)
    cur.cfg.x := aligned && (pmp.cfg.x || ignore)
    Mux(hit, cur, prev)
  }

  io.r := res.cfg.r
  io.w := res.cfg.w
  io.x := res.cfg.x
}
