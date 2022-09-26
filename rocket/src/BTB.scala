// See LICENSE.Berkeley for license details.
// See LICENSE.SiFive for license details.

package org.chipsalliance.rocket

import chisel3._
import chisel3.util.{Cat, Mux1H, OHToUInt, Pipe, UIntToOH, Valid, isPow2, log2Ceil, log2Up}
import Operands.CFI
import org.chipsalliance.rocket.todo.{PopCountAtLeast, PseudoLRU}

case class BHTParams(
                      nEntries: Int = 512,
                      counterLength: Int = 1,
                      historyLength: Int = 8,
                      historyBits: Int = 3) {
  require(isPow2(nEntries))

  def nEntriesLog2: Int = log2Ceil(nEntries)

}

case class BTBParams(
                      nEntries: Int = 28,
                      nMatchBits: Int = 14,
                      nPages: Int = 6,
                      nRAS: Int = 6,
                      bhtParams: Option[BHTParams] = Some(BHTParams()),
                      updatesOutOfOrder: Boolean = false)

class RAS(nras: Int) {
  def push(addr: UInt): Unit = {
    when(count < nras.U) {
      count := count + 1
    }
    val nextPos = Mux(isPow2(nras).B || pos < (nras - 1).U, pos + 1, 0.U)
    stack(nextPos) := addr
    pos := nextPos
  }

  def peek: UInt = stack(pos)

  def pop(): Unit = when(!isEmpty) {
    count := count - 1.U
    pos := Mux(isPow2(nras).B || pos > 0.U, pos - 1.U, (nras - 1).U)
  }

  def clear(): Unit = count := 0.U

  def isEmpty: Bool = count === 0.U

  private val count: UInt = RegInit(0.U(log2Up(nras + 1).W))
  private val pos: UInt = RegInit(0.U(log2Up(nras).W))
  private val stack: Vec[UInt] = Reg(Vec(nras, UInt()))
}

class BHTResp(historyLength: Int, counterLength: Int) extends Bundle {
  val history: UInt = UInt(historyLength.W)
  val value: UInt = UInt(counterLength.W)

  def taken: Bool = value(0)

  def strongly_taken: Bool = value === 1.U
}

// BHT contains table of 2-bit counters and a global history register.
// The BHT only predicts and updates when there is a BTB hit.
// The global history:
//    - updated speculatively in fetch (if there's a BTB hit).
//    - on a mispredict, the history register is reset (again, only if BTB hit).
// The counter table:
//    - each counter corresponds with the address of the fetch packet ("fetch pc").
//    - updated when a branch resolves (and BTB was a hit for that branch).
//      The updating branch must provide its "fetch pc".
class BHT(params: BHTParams, fetchBytes: Int, historyLength: Int, counterLength: Int) {
  def index(addr: UInt, history: UInt): UInt = {
    def hashHistory(hist: UInt) = if (params.historyLength == params.historyBits) hist else {
      val k: Double = math.sqrt(3) / 2
      val i: BigInt = BigDecimal(k * math.pow(2, params.historyLength)).toBigInt
      (i.U * hist) (params.historyLength - 1, params.historyLength - params.historyBits)
    }

    def hashAddr(addr: UInt): UInt = {
      val hi: Bits = addr >> log2Ceil(fetchBytes)
      hi(log2Ceil(params.nEntries) - 1, 0) ^ (hi >> log2Ceil(params.nEntries)) (1, 0)
    }

    hashAddr(addr) ^ (hashHistory(history) << (log2Up(params.nEntries) - params.historyBits))
  }

  def get(addr: UInt): BHTResp = {
    val res: BHTResp = Wire(new BHTResp(historyLength, counterLength))
    res.value := Mux(resetting, 0.U, table(index(addr, history)))
    res.history := history
    res
  }

  def updateTable(addr: UInt, d: BHTResp, taken: Bool): Unit = {
    wen := true.B
    when(!resetting) {
      waddr := index(addr, d.history)
      wdata := (params.counterLength match {
        case 1 => taken
        case 2 => Cat(taken ^ d.value(0), d.value === 1.U || d.value(1) && taken)
      })
    }
  }

  def resetHistory(d: BHTResp): Unit = {
    history := d.history
  }

  def updateHistory(addr: UInt, d: BHTResp, taken: Bool): Unit = {
    history := Cat(taken, d.history >> 1)
  }

  def advanceHistory(taken: Bool): Unit = {
    history := Cat(taken, history >> 1)
  }

  private val table = Mem(params.nEntries, UInt(params.counterLength.W))
  val history: UInt = RegInit(0.U(params.historyLength.W))

  private val reset_waddr: UInt = RegInit(0.U((params.nEntriesLog2 + 1).W))
  private val resetting: Bool = !reset_waddr(params.nEntriesLog2)
  private val wen: Bool = WireDefault(resetting)
  private val waddr: UInt = WireDefault(reset_waddr)
  private val wdata: UInt = WireDefault(0.U)
  when(resetting) {
    reset_waddr := reset_waddr + 1
  }
  when(wen) {
    table(waddr) := wdata
  }
}

// BTB update occurs during branch resolution (and only on a mispredict).
//  - "pc" is what future fetch PCs will tag match against.
//  - "br_pc" is the PC of the branch instruction.
class BTBUpdate(vaddrBits: Int, fetchWidth: Int, entries: Int, historyLength: Int, counterLength: Int) extends Bundle {
  val prediction: BTBResp = new BTBResp(fetchWidth, vaddrBits, entries, historyLength, counterLength)
  val pc: UInt = UInt(vaddrBits.W)
  val target: UInt = UInt(vaddrBits.W)
  val taken: Bool = Bool()
  val isValid: Bool = Bool()
  val br_pc: UInt = UInt(vaddrBits.W)
  val cfiType: UInt = UInt(CFI.width.W)
}

// BHT update occurs during branch resolution on all conditional branches.
//  - "pc" is what future fetch PCs will tag match against.
class BHTUpdate(historyLength: Int, counterLength: Int, vaddrBits: Int) extends Bundle {
  val prediction: BHTResp = new BHTResp(historyLength, counterLength)
  val pc: UInt = UInt(vaddrBits.W)
  val branch: Bool = Bool()
  val taken: Bool = Bool()
  val mispredict: Bool = Bool()
}

class RASUpdate(vaddrBits: Int) extends Bundle {
  val cfiType: UInt = UInt(CFI.width.W)
  val returnAddr: UInt = UInt(vaddrBits.W)
}

//  - "bridx" is the low-order PC bits of the predicted branch (after
//     shifting off the lowest log(inst_bytes) bits off).
//  - "mask" provides a mask of valid instructions (instructions are
//     masked off by the predicted taken branch from the BTB).
class BTBResp(fetchWidth: Int, vaddrBits: Int, entries: Int, historyLength: Int, counterLength: Int) extends Bundle {
  val cfiType: UInt = UInt(CFI.width.W)
  val taken: Bool = Bool()
  val mask: UInt = UInt(fetchWidth.W)
  val bridx: UInt = UInt(log2Up(fetchWidth).W)
  val target: UInt = UInt(vaddrBits.W)
  val entry: UInt = UInt(log2Up(entries + 1).W)
  val bht: BHTResp = new BHTResp(historyLength, counterLength)
}

class BTBReq(vaddrBits: Int) extends Bundle {
  val addr: UInt = UInt(vaddrBits.W)
}

class BTBIO(vaddrBits: Int, fetchWidth: Int, entries: Int, historyLength: Int, counterLength: Int, cacheBlockBytes: Int) extends Bundle {
  val req: Valid[BTBReq] = Flipped(Valid(new BTBReq(vaddrBits)))
  val resp: Valid[BTBResp] = Valid(new BTBResp(fetchWidth, vaddrBits, entries, historyLength, counterLength))
  val btb_update: Valid[BTBUpdate] = Flipped(Valid(new BTBUpdate(vaddrBits, fetchWidth, entries, historyLength, counterLength)))
  val bht_update: Valid[BHTUpdate] = Flipped(Valid(new BHTUpdate(historyLength, counterLength, vaddrBits)))
  val bht_advance: Valid[BTBResp] = Flipped(Valid(new BTBResp(fetchWidth, vaddrBits, entries, historyLength, counterLength)))
  val ras_update: Valid[RASUpdate] = Flipped(Valid(new RASUpdate(vaddrBits)))
  val ras_head: Valid[UInt] = Valid(UInt(vaddrBits.W))
  val flush: Bool = Input(Bool())
}

// fully-associative branch target buffer
// Higher-performance processors may cause BTB updates to occur out-of-order,
// which requires an extra CAM port for updates (to ensure no duplicates get
// placed in BTB).
class BTB(btbParams: BTBParams, vaddrBits: Int, fetchWidth: Int, historyLength: Int, counterLength: Int, cacheBlockBytes: Int, iCacheNSets: Int, instBits: Int) extends Module {
  val matchBits: Int = btbParams.nMatchBits max log2Ceil(cacheBlockBytes * iCacheNSets)
  val entries: Int = btbParams.nEntries
  val updatesOutOfOrder: Boolean = btbParams.updatesOutOfOrder
  /** control logic assumes 2 divides pages
    * todo: refactor to force mod 2 == 0
    */
  val nPages: Int = (btbParams.nPages + 1) / 2 * 2
  val coreInstBytes: Int = instBits / 8
  val coreFetchBytes: Int = fetchWidth * coreInstBytes

  val io: BTBIO = IO(new BTBIO(vaddrBits, fetchWidth, entries, historyLength, counterLength, cacheBlockBytes))

  val idxs: Vec[UInt] = Reg(Vec(entries, UInt((matchBits - log2Up(coreInstBytes)).W)))
  val idxPages: Vec[UInt] = Reg(Vec(entries, UInt(log2Up(nPages).W)))
  val tgts: Vec[UInt] = Reg(Vec(entries, UInt((matchBits - log2Up(coreInstBytes)).W)))
  val tgtPages: Vec[UInt] = Reg(Vec(entries, UInt(log2Up(nPages).W)))
  val pages: Vec[UInt] = Reg(Vec(nPages, UInt((vaddrBits - matchBits).W)))
  val pageValid: UInt = RegInit(0.U(nPages.W))
  val pagesMasked: Vec[UInt] = VecInit((pageValid.asBools zip pages).map { case (v, p) => Mux(v, p, 0.U) })

  val isValid: UInt = RegInit(0.U(entries.W))
  val cfiType: Vec[UInt] = Reg(Vec(entries, UInt(CFI.width.W)))
  val brIdx: Vec[UInt] = Reg(Vec(entries, UInt(log2Up(fetchWidth).W)))

  private def page(addr: UInt) = addr >> matchBits

  private def pageMatch(addr: UInt): UInt = {
    val p: UInt = page(addr)
    pageValid & VecInit(pages.map(_ === p)).asUInt
  }

  private def idxMatch(addr: UInt): UInt = {
    val idx: UInt = addr(matchBits - 1, log2Up(coreInstBytes))
    VecInit(idxs.map(_ === idx)).asUInt & isValid
  }

  val r_btb_update: Valid[BTBUpdate] = Pipe(io.btb_update)
  val update_target: UInt = io.req.bits.addr

  val pageHit: UInt = pageMatch(io.req.bits.addr)
  val idxHit: UInt = idxMatch(io.req.bits.addr)

  val updatePageHit: UInt = pageMatch(r_btb_update.bits.pc)
  val (updateHit: Bool, updateHitAddr: UInt) =
    if (updatesOutOfOrder) {
      val updateHits = (pageHit << 1) (Mux1H(idxMatch(r_btb_update.bits.pc), idxPages))
      (updateHits.orR, OHToUInt(updateHits))
    } else (r_btb_update.bits.prediction.entry < entries.U, r_btb_update.bits.prediction.entry)

  val useUpdatePageHit: Bool = updatePageHit.orR
  val usePageHit: Bool = pageHit.orR
  val doIdxPageRepl: Bool = !useUpdatePageHit
  val nextPageRepl: UInt = RegInit(0.U(log2Ceil(nPages).W))
  val idxPageRepl: UInt = Cat(pageHit(nPages - 2, 0), pageHit(nPages - 1)) | Mux(usePageHit, 0.U, UIntToOH(nextPageRepl))
  val idxPageUpdateOH: UInt = Mux(useUpdatePageHit, updatePageHit, idxPageRepl)
  val idxPageUpdate: UInt = OHToUInt(idxPageUpdateOH)
  val idxPageReplEn: UInt = Mux(doIdxPageRepl, idxPageRepl, 0.U)

  val samePage: Bool = page(r_btb_update.bits.pc) === page(update_target)
  val doTgtPageRepl: Bool = !samePage && !usePageHit
  val tgtPageRepl: UInt = Mux(samePage, idxPageUpdateOH, Cat(idxPageUpdateOH(nPages - 2, 0), idxPageUpdateOH(nPages - 1)))
  val tgtPageUpdate: UInt = OHToUInt(pageHit | Mux(usePageHit, 0.U, tgtPageRepl))
  val tgtPageReplEn: UInt = Mux(doTgtPageRepl, tgtPageRepl, 0.U)

  when(r_btb_update.valid && (doIdxPageRepl || doTgtPageRepl)) {
    val both: Bool = doIdxPageRepl && doTgtPageRepl
    val next: UInt = nextPageRepl + Mux(both, 2.U, 1.U)
    nextPageRepl := Mux(next >= nPages.U, next(0), next)
  }

  val repl: PseudoLRU = new PseudoLRU(entries)
  val waddr: UInt = Mux(updateHit, updateHitAddr, repl.way)
  val r_resp: Valid[BTBResp] = Pipe(io.resp)
  when(r_resp.valid && r_resp.bits.taken || r_btb_update.valid) {
    repl.access(Mux(r_btb_update.valid, waddr, r_resp.bits.entry))
  }

  when(r_btb_update.valid) {
    val mask: UInt = UIntToOH(waddr)
    idxs(waddr) := r_btb_update.bits.pc(matchBits - 1, log2Up(coreInstBytes))
    tgts(waddr) := update_target(matchBits - 1, log2Up(coreInstBytes))
    // the +1 corresponds to the <<1 on io.resp.valid
    idxPages(waddr) := idxPageUpdate +& 1.U
    tgtPages(waddr) := tgtPageUpdate
    cfiType(waddr) := r_btb_update.bits.cfiType
    isValid := Mux(r_btb_update.bits.isValid, isValid | mask, isValid & ~mask)
    if (fetchWidth > 1)
      brIdx(waddr) := r_btb_update.bits.br_pc >> log2Up(coreInstBytes)

    require(nPages % 2 == 0)
    val idxWritesEven: Bool = !idxPageUpdate(0)

    def writeBank(i: Int, mod: Int, en: UInt, data: UInt): Unit =
      for (i <- i until nPages by mod)
        when(en(i)) {
          pages(i) := data
        }

    writeBank(0, 2, Mux(idxWritesEven, idxPageReplEn, tgtPageReplEn),
      Mux(idxWritesEven, page(r_btb_update.bits.pc), page(update_target)))
    writeBank(1, 2, Mux(idxWritesEven, tgtPageReplEn, idxPageReplEn),
      Mux(idxWritesEven, page(update_target), page(r_btb_update.bits.pc)))
    pageValid := pageValid | tgtPageReplEn | idxPageReplEn
  }

  io.resp.valid := (pageHit << 1) (Mux1H(idxHit, idxPages))
  io.resp.bits.taken := true.B
  io.resp.bits.target := Cat(pagesMasked(Mux1H(idxHit, tgtPages)), Mux1H(idxHit, tgts) << log2Up(coreInstBytes))
  io.resp.bits.entry := OHToUInt(idxHit)
  io.resp.bits.bridx := (if (fetchWidth > 1) Mux1H(idxHit, brIdx) else 0.U)
  io.resp.bits.mask := Cat((1.U << ~Mux(io.resp.bits.taken, ~io.resp.bits.bridx, 0.U): UInt) - 1.U, 1.U)
  io.resp.bits.cfiType := Mux1H(idxHit, cfiType)

  // if multiple entries for same PC land in BTB, zap them
  when(PopCountAtLeast(idxHit, 2)) {
    isValid := isValid & ~idxHit
  }
  when(io.flush) {
    isValid := 0.U
  }

  btbParams.bhtParams.foreach {bhtParams =>
    val bht: BHT = new BHT(btbParams.bhtParams.get, coreFetchBytes, historyLength, counterLength)
    val isBranch: Bool = (idxHit & VecInit(cfiType.map(_ === CFI.branch)).asUInt).orR
    val res: BHTResp = bht.get(io.req.bits.addr)
    when(io.bht_advance.valid) {
      bht.advanceHistory(io.bht_advance.bits.bht.taken)
    }
    when(io.bht_update.valid) {
      when(io.bht_update.bits.branch) {
        bht.updateTable(io.bht_update.bits.pc, io.bht_update.bits.prediction, io.bht_update.bits.taken)
        when(io.bht_update.bits.mispredict) {
          bht.updateHistory(io.bht_update.bits.pc, io.bht_update.bits.prediction, io.bht_update.bits.taken)
        }
      }.elsewhen(io.bht_update.bits.mispredict) {
        bht.resetHistory(io.bht_update.bits.prediction)
      }
    }
    when(!res.taken && isBranch) {
      io.resp.bits.taken := false.B
    }
    io.resp.bits.bht := res
  }

  if (btbParams.nRAS > 0) {
    val ras: RAS = new RAS(btbParams.nRAS)
    val doPeek: Bool = (idxHit & VecInit(cfiType.map(_ === CFI.ret)).asUInt).orR
    io.ras_head.valid := !ras.isEmpty
    io.ras_head.bits := ras.peek
    when(!ras.isEmpty && doPeek) {
      io.resp.bits.target := ras.peek
    }
    when(io.ras_update.valid) {
      when(io.ras_update.bits.cfiType === CFI.call) {
        ras.push(io.ras_update.bits.returnAddr)
      }.elsewhen(io.ras_update.bits.cfiType === CFI.ret) {
        ras.pop()
      }
    }
  }
}
