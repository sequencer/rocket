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
  
class RocketTile(val rocketParams: RocketTileParams)(implicit p: Parameters) extends BaseTile(rocketParams)(p)
    with CanHaveLegacyRoccs  // implies CanHaveSharedFPU with CanHavePTW with HasHellaCache
    with CanHaveScratchpad { // implies CanHavePTW with HasHellaCache with HasICacheFrontend

  nDCachePorts += 1 // core TODO dcachePorts += () => module.core.io.dmem ??

  override lazy val module = new RocketTileModule(this)
}

class RocketTileBundle(outer: RocketTile) extends BaseTileBundle(outer)
    with CanHaveScratchpadBundle

class RocketTileModule(outer: RocketTile) extends BaseTileModule(outer, () => new RocketTileBundle(outer))
    with CanHaveLegacyRoccsModule
    with CanHaveScratchpadModule {

  val core = Module(p(BuildCore)(outer.p))
  core.io.interrupts := io.interrupts
  core.io.hartid := io.hartid
  outer.frontend.module.io.cpu <> core.io.imem
  outer.frontend.module.io.resetVector := io.resetVector
  dcachePorts += core.io.dmem // TODO outer.dcachePorts += () => module.core.io.dmem ??
  fpuOpt foreach { fpu => core.io.fpu <> fpu.io }
  ptwOpt foreach { ptw => core.io.ptw <> ptw.io.dpath }
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
  ptwOpt foreach { ptw => ptw.io.requestor <> ptwPorts }
}

class AsyncRocketTile(rtp: RocketTileParams)(implicit p: Parameters) extends LazyModule {
  val rocket = LazyModule(new RocketTile(rtp))

  val masterNode = TLAsyncOutputNode()
  val source = LazyModule(new TLAsyncCrossingSource)
  source.node :=* rocket.masterNode
  masterNode :=* source.node

  val slaveNode = TLAsyncInputNode()
  val sink = LazyModule(new TLAsyncCrossingSink)
  rocket.slaveNode :*= sink.node
  sink.node :*= slaveNode

  lazy val module = new LazyModuleImp(this) {
    val io = new Bundle {
      val master = masterNode.bundleOut
      val slave = slaveNode.bundleIn
      val hartid = UInt(INPUT, p(XLen))
      val interrupts = new TileInterrupts()(p).asInput
      val resetVector = UInt(INPUT, p(XLen))
    }
    rocket.module.io.interrupts := ShiftRegister(io.interrupts, 3)
    // signals that do not change:
    rocket.module.io.hartid := io.hartid
    rocket.module.io.resetVector := io.resetVector
  }
}

class RationalRocketTile(rtp: RocketTileParams)(implicit p: Parameters) extends LazyModule {
  val rocket = LazyModule(new RocketTile(rtp))

  val masterNode = TLRationalOutputNode()
  val source = LazyModule(new TLRationalCrossingSource)
  source.node :=* rocket.masterNode
  masterNode :=* source.node

  val slaveNode = TLRationalInputNode()
  val sink = LazyModule(new TLRationalCrossingSink)
  rocket.slaveNode :*= sink.node
  sink.node :*= slaveNode

  lazy val module = new LazyModuleImp(this) {
    val io = new Bundle {
      val master = masterNode.bundleOut
      val slave = slaveNode.bundleIn
      val hartid = UInt(INPUT, p(XLen))
      val interrupts = new TileInterrupts()(p).asInput
      val resetVector = UInt(INPUT, p(XLen))
    }
    rocket.module.io.interrupts := ShiftRegister(io.interrupts, 1)
    // signals that do not change:
    rocket.module.io.hartid := io.hartid
    rocket.module.io.resetVector := io.resetVector
  }
}
