// See LICENSE.Berkeley for license details.
// See LICENSE.SiFive for license details.

package rocket

import Chisel._
import config._
import diplomacy._
import uncore.agents._
import uncore.tilelink2._
import uncore.util._
import util._
import Chisel.ImplicitConversions._

trait HasL1CacheParameters extends HasCacheParameters with HasCoreParameters {
  val cacheBlockBytes = p(CacheBlockBytes)
  val lgCacheBlockBytes = log2Up(cacheBlockBytes)
  val cacheDataBits = p(TLCacheEdge).bundle.dataBits
  val cacheDataBeats = (cacheBlockBytes * 8) / cacheDataBits
  val refillCycles = cacheDataBeats
}

class ICacheReq(implicit p: Parameters) extends CoreBundle()(p) with HasL1CacheParameters {
  val addr = UInt(width = vaddrBits)
}

class ICacheResp(implicit p: Parameters) extends CoreBundle()(p) with HasL1CacheParameters {
  val data = Bits(width = coreInstBits)
  val datablock = Bits(width = rowBits)
}

class ICache(val latency: Int)(implicit p: Parameters) extends LazyModule {
  lazy val module = new ICacheModule(this)
  val node = TLClientNode(TLClientParameters(sourceId = IdRange(0,1)))
}

class ICacheBundle(outer: ICache) extends CoreBundle()(outer.p) {
  val req = Valid(new ICacheReq).flip
  val s1_ppn = UInt(INPUT, ppnBits) // delayed one cycle w.r.t. req
  val s1_kill = Bool(INPUT) // delayed one cycle w.r.t. req
  val s2_kill = Bool(INPUT) // delayed two cycles; prevents I$ miss emission

  val resp = Decoupled(new ICacheResp)
  val invalidate = Bool(INPUT)
  val mem = outer.node.bundleOut
}

class ICacheModule(outer: ICache) extends LazyModuleImp(outer)
    with HasCoreParameters
    with HasL1CacheParameters {
  val io = new ICacheBundle(outer)
  val edge = outer.node.edgesOut(0)
  val tl_out = io.mem(0)

  require(isPow2(nSets) && isPow2(nWays))
  require(isPow2(coreInstBytes))
  require(!usingVM || pgIdxBits >= untagBits)

  val s_ready :: s_request :: s_refill_wait :: s_refill :: Nil = Enum(UInt(), 4)
  val state = Reg(init=s_ready)
  val invalidated = Reg(Bool())
  val stall = !io.resp.ready
  val rdy = Wire(Bool())

  val refill_addr = Reg(UInt(width = paddrBits))
  val s1_any_tag_hit = Wire(Bool())

  val s1_valid = Reg(init=Bool(false))
  val s1_vaddr = Reg(UInt())
  val s1_paddr = Cat(io.s1_ppn, s1_vaddr(pgIdxBits-1,0))
  val s1_tag = s1_paddr(tagBits+untagBits-1,untagBits)

  val s0_valid = io.req.valid || s1_valid && stall
  val s0_vaddr = Mux(s1_valid && stall, s1_vaddr, io.req.bits.addr)

  s1_valid := io.req.valid && rdy || s1_valid && stall && !io.s1_kill
  when (io.req.valid && rdy) {
    s1_vaddr := io.req.bits.addr
  }

  val out_valid = s1_valid && !io.s1_kill && state === s_ready
  val s1_idx = s1_vaddr(untagBits-1,blockOffBits)
  val s1_hit = out_valid && s1_any_tag_hit
  val s1_miss = out_valid && !s1_any_tag_hit
  rdy := state === s_ready && !s1_miss

  when (s1_miss && state === s_ready) {
    refill_addr := s1_paddr
  }
  val refill_tag = refill_addr(tagBits+untagBits-1,untagBits)
  val (_, _, refill_done, refill_cnt) = edge.count(tl_out.d)
  tl_out.d.ready := state === s_refill

  val repl_way = if (isDM) UInt(0) else LFSR16(s1_miss)(log2Up(nWays)-1,0)
  val entagbits = code.width(tagBits)
  val tag_array = SeqMem(nSets, Vec(nWays, Bits(width = entagbits)))
  val tag_rdata = tag_array.read(s0_vaddr(untagBits-1,blockOffBits), !refill_done && s0_valid)
  when (refill_done) {
    val tag = code.encode(refill_tag)
    tag_array.write(s1_idx, Vec.fill(nWays)(tag), Vec.tabulate(nWays)(repl_way === _))
  }

  val vb_array = Reg(init=Bits(0, nSets*nWays))
  when (refill_done && !invalidated) {
    vb_array := vb_array.bitSet(Cat(repl_way, s1_idx), Bool(true))
  }
  when (io.invalidate) {
    vb_array := Bits(0)
    invalidated := Bool(true)
  }
  val s1_disparity = Wire(Vec(nWays, Bool()))
  for (i <- 0 until nWays)
    when (s1_valid && s1_disparity(i)) { vb_array := vb_array.bitSet(Cat(UInt(i), s1_idx), Bool(false)) }

  val s1_tag_match = Wire(Vec(nWays, Bool()))
  val s1_tag_hit = Wire(Vec(nWays, Bool()))
  val s1_dout = Wire(Vec(nWays, Bits(width = rowBits)))

  for (i <- 0 until nWays) {
    val s1_vb = !io.invalidate && vb_array(Cat(UInt(i), s1_vaddr(untagBits-1,blockOffBits))).toBool
    val tag_out = tag_rdata(i)
    val s1_tag_disparity = code.decode(tag_out).error
    s1_tag_match(i) := tag_out(tagBits-1,0) === s1_tag
    s1_tag_hit(i) := s1_vb && s1_tag_match(i)
    s1_disparity(i) := s1_vb && (s1_tag_disparity || code.decode(s1_dout(i)).error)
  }
  s1_any_tag_hit := s1_tag_hit.reduceLeft(_||_) && !s1_disparity.reduceLeft(_||_)

  for (i <- 0 until nWays) {
    val data_array = SeqMem(nSets * refillCycles, Bits(width = code.width(rowBits)))
    val wen = tl_out.d.valid && repl_way === UInt(i)
    when (wen) {
      val e_d = code.encode(tl_out.d.bits.data)
      data_array.write((s1_idx << log2Ceil(refillCycles)) | refill_cnt, e_d)
    }
    val s0_raddr = s0_vaddr(untagBits-1,blockOffBits-log2Ceil(refillCycles))
    s1_dout(i) := data_array.read(s0_raddr, !wen && s0_valid)
  }

  // output signals
  outer.latency match {
    case 1 =>
      io.resp.bits.datablock := Mux1H(s1_tag_hit, s1_dout)
      io.resp.valid := s1_hit
    case 2 =>
      val s2_hit = RegEnable(s1_hit, Bool(false), !stall)
      val s2_tag_hit = RegEnable(s1_tag_hit, !stall)
      val s2_dout = RegEnable(s1_dout, !stall)
      io.resp.bits.datablock := Mux1H(s2_tag_hit, s2_dout)
      io.resp.valid := s2_hit
  }
  tl_out.a.valid := state === s_request && !io.s2_kill
  tl_out.a.bits := edge.Get(
                    fromSource = UInt(0),
                    toAddress = (refill_addr >> blockOffBits) << blockOffBits,
                    lgSize = lgCacheBlockBytes)._2
  tl_out.c.valid := Bool(false)
  tl_out.e.valid := Bool(false)

  // control state machine
  switch (state) {
    is (s_ready) {
      when (s1_miss) { state := s_request }
      invalidated := Bool(false)
    }
    is (s_request) {
      when (tl_out.a.ready) { state := s_refill_wait }
      when (io.s2_kill) { state := s_ready }
    }
    is (s_refill_wait) {
      when (tl_out.d.valid) { state := s_refill }
    }
    is (s_refill) {
      when (refill_done) { state := s_ready }
    }
  }
}
