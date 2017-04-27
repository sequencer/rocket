// See LICENSE.SiFive for license details.
// See LICENSE.Berkeley for license details.

package rocket

import Chisel._
import config._
import coreplex._
import diplomacy._
import tile._
import uncore.devices._
import uncore.tilelink2._
import util._

case class RocketTileParams(
    core: RocketCoreParams = RocketCoreParams(),
    icache: Option[ICacheParams] = Some(ICacheParams()),
    dcache: Option[DCacheParams] = Some(DCacheParams()),
    rocc: Seq[RoCCParams] = Nil,
    btb: Option[BTBParams] = Some(BTBParams()),
    dataScratchpadBytes: Int = 0) extends TileParams {
  require(icache.isDefined)
  require(dcache.isDefined)
}
  
class RocketTile(val rocketParams: RocketTileParams, val hartid: Int)(implicit p: Parameters) extends BaseTile(rocketParams)(p)
    with CanHaveLegacyRoccs  // implies CanHaveSharedFPU with CanHavePTW with HasHellaCache
    with CanHaveScratchpad { // implies CanHavePTW with HasHellaCache with HasICacheFrontend

  nDCachePorts += 1 // core TODO dcachePorts += () => module.core.io.dmem ??

  private def ofInt(x: Int) = Seq(ResourceInt(BigInt(x)))
  private def ofStr(x: String) = Seq(ResourceString(x))

  val cpuDevice = new Device {
    def describe(resources: ResourceBindings): Description = {
      val block =  p(CacheBlockBytes)
      val m = if (rocketParams.core.mulDiv.nonEmpty) "m" else ""
      val a = if (rocketParams.core.useAtomics) "a" else ""
      val f = if (rocketParams.core.fpu.nonEmpty) "f" else ""
      val d = if (rocketParams.core.fpu.nonEmpty && p(XLen) > 32) "d" else ""
      val c = if (rocketParams.core.useCompressed) "c" else ""
      val s = if (rocketParams.core.useVM) "s" else ""
      val isa = s"rv${p(XLen)}i$m$a$f$d$c$s"

      val dcache = rocketParams.dcache.map(d => Map(
        "d-cache-block-size"   -> ofInt(block),
        "d-cache-sets"         -> ofInt(d.nSets),
        "d-cache-size"         -> ofInt(d.nSets * d.nWays * block))).getOrElse(Map())

      val icache = rocketParams.icache.map(i => Map(
        "i-cache-block-size"   -> ofInt(block),
        "i-cache-sets"         -> ofInt(i.nSets),
        "i-cache-size"         -> ofInt(i.nSets * i.nWays * block))).getOrElse(Map())

      val dtlb = rocketParams.dcache.filter(_ => rocketParams.core.useVM).map(d => Map(
        "d-tlb-size"           -> ofInt(d.nTLBEntries),
        "d-tlb-sets"           -> ofInt(1))).getOrElse(Map())

      val itlb = rocketParams.icache.filter(_ => rocketParams.core.useVM).map(i => Map(
        "i-tlb-size"           -> ofInt(i.nTLBEntries),
        "i-tlb-sets"           -> ofInt(1))).getOrElse(Map())

      val mmu = if (!rocketParams.core.useVM) Map() else Map(
        "tlb-split" -> Nil,
        "mmu-type"  -> ofStr(p(PgLevels) match {
          case 2 => "riscv,sv32"
          case 3 => "riscv,sv39"
          case 4 => "riscv,sv48"
      }))

      // Find all the caches
      val outer = masterNode.edgesOut
        .flatMap(_.manager.managers)
        .filter(_.supportsAcquireB)
        .flatMap(_.resources.headOption)
        .map(_.owner.label)
        .distinct
      val nextlevel: Option[(String, Seq[ResourceValue])] =
        if (outer.isEmpty) None else
        Some("next-level-cache" -> outer.map(l => ResourceReference(l)).toList)

      Description(s"cpus/cpu@${hartid}", Map(
        "reg"                  -> resources("reg").map(_.value),
        "device_type"          -> ofStr("cpu"),
        "compatible"           -> ofStr("riscv"),
        "status"               -> ofStr("okay"),
        "clock-frequency"      -> Seq(ResourceInt(rocketParams.core.bootFreqHz)),
        "riscv,isa"            -> ofStr(isa))
        ++ dcache ++ icache ++ nextlevel ++ mmu ++ itlb ++ dtlb)
    }
  }
  val intcDevice = new Device {
    def describe(resources: ResourceBindings): Description = {
      Description(s"cpus/cpu@${hartid}/interrupt-controller", Map(
        "compatible"           -> ofStr("riscv,cpu-intc"),
        "interrupt-controller" -> Nil,
        "#interrupt-cells"     -> ofInt(1)))
    }
  }

  ResourceBinding {
    Resource(cpuDevice, "reg").bind(ResourceInt(BigInt(hartid)))
    Resource(intcDevice, "reg").bind(ResourceInt(BigInt(hartid)))

    intNode.edgesIn.flatMap(_.source.sources).map { case s =>
      for (i <- s.range.start until s.range.end) {
       csrIntMap.lift(i).foreach { j =>
          s.resources.foreach { r =>
            r.bind(intcDevice, ResourceInt(j))
          }
        }
      }
    }
  }

  override lazy val module = new RocketTileModule(this)
}

class RocketTileBundle(outer: RocketTile) extends BaseTileBundle(outer)
    with CanHaveScratchpadBundle

class RocketTileModule(outer: RocketTile) extends BaseTileModule(outer, () => new RocketTileBundle(outer))
    with CanHaveLegacyRoccsModule
    with CanHaveScratchpadModule {

  require(outer.p(PAddrBits) >= outer.masterNode.edgesIn(0).bundle.addressBits,
    s"outer.p(PAddrBits) (${outer.p(PAddrBits)}) must be >= outer.masterNode.addressBits (${outer.masterNode.edgesIn(0).bundle.addressBits})")

  val core = Module(p(BuildCore)(outer.p))
  decodeCoreInterrupts(core.io.interrupts) // Decode the interrupt vector
  core.io.hartid := io.hartid // Pass through the hartid
  outer.frontend.module.io.cpu <> core.io.imem
  outer.frontend.module.io.resetVector := io.resetVector
  outer.frontend.module.io.hartid := io.hartid
  outer.dcache.module.io.hartid := io.hartid
  dcachePorts += core.io.dmem // TODO outer.dcachePorts += () => module.core.io.dmem ??
  fpuOpt foreach { fpu => core.io.fpu <> fpu.io }
  core.io.ptw <> ptw.io.dpath
  outer.legacyRocc foreach { lr =>
    lr.module.io.core.cmd <> core.io.rocc.cmd
    lr.module.io.core.exception := core.io.rocc.exception
    core.io.rocc.resp <> lr.module.io.core.resp
    core.io.rocc.busy := lr.module.io.core.busy
    core.io.rocc.interrupt := lr.module.io.core.interrupt
  }


  // TODO eliminate this redundancy
  val h = dcachePorts.size
  val c = core.dcacheArbPorts
  val o = outer.nDCachePorts
  require(h == c, s"port list size was $h, core expected $c")
  require(h == o, s"port list size was $h, outer counted $o")
  // TODO figure out how to move the below into their respective mix-ins
  dcacheArb.io.requestor <> dcachePorts
  ptw.io.requestor <> ptwPorts
}

class SyncRocketTile(rtp: RocketTileParams, hartid: Int)(implicit p: Parameters) extends LazyModule {
  val rocket = LazyModule(new RocketTile(rtp, hartid))

  val masterNode = TLOutputNode()
  masterNode :=* rocket.masterNode

  val slaveNode = new TLInputNode() { override def reverse = true }
  rocket.slaveNode :*= slaveNode

  val intNode = IntInputNode()

  // Some interrupt sources may be completely asynchronous, even
  // if tlClk and coreClk are the same (e.g. Debug Interrupt, which
  // is coming directly from e.g. TCK)
  val xing = LazyModule(new IntXing(3))
  rocket.intNode := xing.intnode
  xing.intnode := intNode

  lazy val module = new LazyModuleImp(this) {
    val io = new CoreBundle {
      val master = masterNode.bundleOut
      val slave = slaveNode.bundleIn
      val interrupts = intNode.bundleIn
      val hartid = UInt(INPUT, hartIdLen)
      val resetVector = UInt(INPUT, p(XLen))
    }
    // signals that do not change:
    rocket.module.io.hartid := io.hartid
    rocket.module.io.resetVector := io.resetVector
  }
}

class AsyncRocketTile(rtp: RocketTileParams, hartid: Int)(implicit p: Parameters) extends LazyModule {
  val rocket = LazyModule(new RocketTile(rtp, hartid))

  val masterNode = TLAsyncOutputNode()
  val source = LazyModule(new TLAsyncCrossingSource)
  source.node :=* rocket.masterNode
  masterNode :=* source.node

  val slaveNode = new TLAsyncInputNode() { override def reverse = true }
  val sink = LazyModule(new TLAsyncCrossingSink)
  rocket.slaveNode :*= sink.node
  sink.node :*= slaveNode

  val intNode = IntInputNode()
  val xing = LazyModule(new IntXing(3))
  rocket.intNode := xing.intnode
  xing.intnode := intNode

  lazy val module = new LazyModuleImp(this) {
    val io = new CoreBundle {
      val master = masterNode.bundleOut
      val slave = slaveNode.bundleIn
      val interrupts = intNode.bundleIn
      val hartid = UInt(INPUT, hartIdLen)
      val resetVector = UInt(INPUT, p(XLen))
    }
    // signals that do not change:
    rocket.module.io.hartid := io.hartid
    rocket.module.io.resetVector := io.resetVector
  }
}

class RationalRocketTile(rtp: RocketTileParams, hartid: Int)(implicit p: Parameters) extends LazyModule {
  val rocket = LazyModule(new RocketTile(rtp, hartid))

  val masterNode = TLRationalOutputNode()
  val source = LazyModule(new TLRationalCrossingSource)
  source.node :=* rocket.masterNode
  masterNode :=* source.node

  val slaveNode = new TLRationalInputNode() { override def reverse = true }
  val sink = LazyModule(new TLRationalCrossingSink(util.SlowToFast))
  rocket.slaveNode :*= sink.node
  sink.node :*= slaveNode

  val intNode = IntInputNode()

  // Some interrupt sources may be completely asynchronous, even
  // if tlClk and coreClk are related (e.g. Debug Interrupt, which
  // is coming directly from e.g. TCK)
  val xing = LazyModule(new IntXing(3))
  rocket.intNode := xing.intnode
  xing.intnode := intNode

  lazy val module = new LazyModuleImp(this) {
    val io = new CoreBundle {
      val master = masterNode.bundleOut
      val slave = slaveNode.bundleIn
      val interrupts = intNode.bundleIn
      val hartid = UInt(INPUT, hartIdLen)
      val resetVector = UInt(INPUT, p(XLen))
    }
    // signals that do not change:
    rocket.module.io.hartid := io.hartid
    rocket.module.io.resetVector := io.resetVector
  }
}
