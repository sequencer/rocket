package rocket.tests

import chisel3._
import freechips.rocketchip.diplomacy.{AddressSet, BundleBridgeSource, InModuleBody, LazyModule, RegionType, SimpleLazyModule, TransferSizes}
import freechips.rocketchip.interrupts.{IntSinkNode, IntSinkPortSimple, IntSourceNode, IntSourcePortSimple}
import freechips.rocketchip.subsystem._
import freechips.rocketchip.tilelink.{TLManagerNode, TLSlaveParameters, TLSlavePortParameters}
import org.chipsalliance.cde.config.Parameters
import org.chipsalliance.rockettile.{NMI, PriorityMuxHartIdFromSeq, RocketTile, RocketTileParams, XLen}

class DUT(p: Parameters) extends Module {
  implicit val implicitP = p
  val tileParams = p(RocketTileParamsKey)
  val ldut = LazyModule(new SimpleLazyModule {
    implicit val implicitP = p
    val rocketTile = LazyModule(new RocketTile(tileParams, RocketCrossingParams(), PriorityMuxHartIdFromSeq(Seq(tileParams))))
    val masterNode = TLManagerNode(Seq(TLSlavePortParameters.v1(
      Seq(TLSlaveParameters.v1(
        address = List(AddressSet(0x80000000L, 0x7fffffffL)),
        regionType = RegionType.UNCACHED,
        supportsGet = TransferSizes(1, 64),
        supportsAcquireT = TransferSizes(1, 64),
        supportsAcquireB = TransferSizes(1, 64),
        supportsPutPartial = TransferSizes(1, 64),
        supportsPutFull = TransferSizes(1, 64),
        fifoId = Some(0))),
      beatBytes = 8,
      endSinkId = 4,
      minLatency = 1
    )))
    masterNode :=* rocketTile.masterNode
    val memroy = InModuleBody {
      masterNode.makeIOs()
    }

    val intNode = IntSourceNode(IntSourcePortSimple())
    rocketTile.intInwardNode :=* intNode
    val intIn = InModuleBody {
      intNode.makeIOs()
    }

    val haltNode = IntSinkNode(IntSinkPortSimple())
    haltNode :=* rocketTile.haltNode
    val haltOut = InModuleBody {
      haltNode.makeIOs()
    }

    val ceaseNode = IntSinkNode(IntSinkPortSimple())
    ceaseNode :=* rocketTile.ceaseNode
    val ceaseOut = InModuleBody {
      ceaseNode.makeIOs()
    }

    val wfiNode = IntSinkNode(IntSinkPortSimple())
    wfiNode :=* rocketTile.wfiNode
    val wfiOut = InModuleBody {
      wfiNode.makeIOs()
    }


    val resetVectorNode = BundleBridgeSource(() => UInt(32.W))
    rocketTile.resetVectorNode := resetVectorNode
    val resetVector = InModuleBody {
      resetVectorNode.makeIO()
    }

    val hartidNode = BundleBridgeSource(() => UInt(4.W))
    rocketTile.hartIdNode := hartidNode
    InModuleBody {
      hartidNode.bundle := 0.U
    }

    val nmiNode = BundleBridgeSource(Some(() => new NMI(32)))
    rocketTile.nmiNode := nmiNode
    val nmi = InModuleBody {
      nmiNode.makeIO()
    }


  })
  val dut = Module(ldut.module)
}
