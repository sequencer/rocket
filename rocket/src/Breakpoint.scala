// See LICENSE.SiFive for license details.

package org.chipsalliance.rocket

import chisel3._
import chisel3.util.Cat

class BPControl(useBPWatch: Boolean, xLen: Int) extends Bundle {
  val ttype: UInt = UInt(4.W)
  val dmode: Bool = Bool()
  val maskmax: UInt = UInt(6.W)
  val reserved: UInt = UInt((xLen - (if (useBPWatch) 26 else 24)).W)
  val action: UInt = UInt((if (useBPWatch) 3 else 1).W)
  val chain: Bool = Bool()
  val zero: UInt = UInt(2.W)
  val tmatch: UInt = UInt(2.W)
  val m: Bool = Bool()
  val h: Bool = Bool()
  val s: Bool = Bool()
  val u: Bool = Bool()
  val x: Bool = Bool()
  val w: Bool = Bool()
  val r: Bool = Bool()

  def tType: Int = 2

  def maskMax: Int = 4

  def enabled(mstatus: MStatus): Bool = !mstatus.debug && Cat(m, h, s, u)(mstatus.prv)
}

class TExtra(xLen: Int, mcontextWidth: Int, scontextWidth: Int) extends Bundle {
  def mvalueBits: Int = if (xLen == 32) mcontextWidth min 6 else mcontextWidth min 13

  def svalueBits: Int = if (xLen == 32) scontextWidth min 16 else scontextWidth min 34

  def mselectPos: Int = if (xLen == 32) 25 else 50

  def mvaluePos: Int = mselectPos + 1

  def sselectPos: Int = 0

  def svaluePos: Int = 2

  val mvalue: UInt = UInt(mvalueBits.W)
  val mselect: Bool = Bool()
  val pad2: UInt = UInt((mselectPos - svalueBits - 2).W)
  val svalue: UInt = UInt(svalueBits.W)
  val pad1: UInt = UInt(1.W)
  val sselect: Bool = Bool()
}

class BP(useBPWatch: Boolean, xLen: Int, mcontextWidth: Int, scontextWidth: Int, vaddrBits: Int) extends Bundle {
  val control: BPControl = new BPControl(useBPWatch, xLen)
  val address: UInt = UInt(vaddrBits.W)
  val textra: TExtra = new TExtra(xLen, mcontextWidth, scontextWidth)

  def contextMatch(mcontext: UInt, scontext: UInt) =
    (if (mcontextWidth > 0) (!textra.mselect || (mcontext(textra.mvalueBits - 1, 0) === textra.mvalue)) else true.B) &&
      (if (scontextWidth > 0) (!textra.sselect || (scontext(textra.svalueBits - 1, 0) === textra.svalue)) else true.B)

  def mask: UInt = VecInit((0 until control.maskMax - 1).scanLeft(control.tmatch(0))((m, i) => m && address(i))).asUInt

  def pow2AddressMatch(x: UInt): Bool = (~x | mask) === (~address | mask)

  def rangeAddressMatch(x: UInt): Bool = (x >= address) ^ control.tmatch(0)

  def addressMatch(x: UInt): Bool = Mux(control.tmatch(1), rangeAddressMatch(x), pow2AddressMatch(x))
}

class BPWatch(val n: Int) extends Bundle {
  val valid: Vec[Bool] = Vec(n, Bool())
  val rvalid: Vec[Bool] = Vec(n, Bool())
  val wvalid: Vec[Bool] = Vec(n, Bool())
  val ivalid: Vec[Bool] = Vec(n, Bool())
  val action: UInt = UInt(3.W)
}

class BreakpointUnitIO(n: Int, useBPWatch: Boolean, xLen: Int, mcontextWidth: Int, scontextWidth: Int, vaddrBits: Int) extends Bundle {
  val status: MStatus = Input(new MStatus)
  val bp: Vec[BP] = Input(Vec(n, new BP(useBPWatch, xLen, mcontextWidth, scontextWidth, vaddrBits)))
  val pc: UInt = Input(UInt(vaddrBits.W))
  val ea: UInt = Input(UInt(vaddrBits.W))
  val mcontext: UInt = Input(UInt(mcontextWidth.W))
  val scontext: UInt = Input(UInt(scontextWidth.W))
  val xcpt_if: Bool = Output(Bool())
  val xcpt_ld: Bool = Output(Bool())
  val xcpt_st: Bool = Output(Bool())
  val debug_if: Bool = Output(Bool())
  val debug_ld: Bool = Output(Bool())
  val debug_st: Bool = Output(Bool())
  val bpwatch: Vec[BPWatch] = Output(Vec(n, new BPWatch(1)))
}

class BreakpointUnit(n: Int, useBPWatch: Boolean, xLen: Int, mcontextWidth: Int, scontextWidth: Int, vaddrBits: Int) extends Module {
  val io = IO(new BreakpointUnitIO(n, useBPWatch, xLen, mcontextWidth, scontextWidth, vaddrBits))

  io.xcpt_if := false.B
  io.xcpt_ld := false.B
  io.xcpt_st := false.B
  io.debug_if := false.B
  io.debug_ld := false.B
  io.debug_st := false.B

  (io.bpwatch zip io.bp).foldLeft((true.B, true.B, true.B)) { case ((ri, wi, xi), (bpw, bp)) =>
    val en = bp.control.enabled(io.status)
    val cx = bp.contextMatch(io.mcontext, io.scontext)
    val r = en && bp.control.r && bp.addressMatch(io.ea) && cx
    val w = en && bp.control.w && bp.addressMatch(io.ea) && cx
    val x = en && bp.control.x && bp.addressMatch(io.pc) && cx
    val end = !bp.control.chain
    val action = bp.control.action

    bpw.action := action
    bpw.valid(0) := false.B
    bpw.rvalid(0) := false.B
    bpw.wvalid(0) := false.B
    bpw.ivalid(0) := false.B

    when(end && r && ri) {
      io.xcpt_ld := (action === 0.U)
      io.debug_ld := (action === 1.U)
      bpw.valid(0) := true.B
      bpw.rvalid(0) := true.B
    }
    when(end && w && wi) {
      io.xcpt_st := (action === 0.U)
      io.debug_st := (action === 1.U)
      bpw.valid(0) := true.B
      bpw.wvalid(0) := true.B
    }
    when(end && x && xi) {
      io.xcpt_if := (action === 0.U)
      io.debug_if := (action === 1.U)
      bpw.valid(0) := true.B
      bpw.ivalid(0) := true.B
    }

    (end || r, end || w, end || x)
  }
}
