package rocket.tests

import chisel3.stage.ChiselGeneratorAnnotation
import circt.stage.{CIRCTTarget, CIRCTTargetAnnotation, ChiselStage, FirtoolOption}
import firrtl.options.TargetDirAnnotation
import firrtl.{AnnotationSeq, ChirrtlEmitter, EmitAllModulesAnnotation}
import freechips.rocketchip.subsystem.{CacheBlockBytes, SystemBusKey, SystemBusParams}
import freechips.rocketchip.util.PlusArgArtefacts
import mainargs._
import org.chipsalliance.cde.config.{Config, Field}
import org.chipsalliance.rocket.{DCacheParams, ICacheParams, MulDivParams, RocketCoreParams}
import org.chipsalliance.rockettile.RocketTileParams


object RocketTileParamsKey extends Field[RocketTileParams]

object Main {
  @main
  def elaborate(
                 @arg(name = "dir") dir: String,
                 @arg(name = "bootrom_file") bootrom: String,
                 @arg(name = "eicg_file") eicgFile: String
               ): Unit = {
    (new ChiselStage).transform(AnnotationSeq(Seq(
      TargetDirAnnotation(dir),
      new ChiselGeneratorAnnotation(() => {
        new DUT(
          new Config((site, here, up) => {
            case freechips.rocketchip.tile.XLen => 64
            case org.chipsalliance.rockettile.XLen => 64
            case org.chipsalliance.rockettile.MaxHartIdBits => 4
            case freechips.rocketchip.tile.MaxHartIdBits => 4
            case org.chipsalliance.rocket.PgLevels => if (site(org.chipsalliance.rockettile.XLen) == 64) 3 else 2
            case freechips.rocketchip.rocket.PgLevels => if (site(org.chipsalliance.rockettile.XLen) == 64) 3 else 2
            case RocketTileParamsKey => RocketTileParams(
              core = RocketCoreParams(mulDiv = Some(MulDivParams(
                mulUnroll = 8,
                mulEarlyOut = true,
                divEarlyOut = true))),
              dcache = Some(DCacheParams(
                rowBits = site(SystemBusKey).beatBits,
                nMSHRs = 0,
                blockBytes = site(CacheBlockBytes))),
              icache = Some(ICacheParams(
                rowBits = site(SystemBusKey).beatBits,
                blockBytes = site(CacheBlockBytes))))
            case SystemBusKey => SystemBusParams(
              beatBytes = site(org.chipsalliance.rockettile.XLen) / 8,
              blockBytes = site(CacheBlockBytes))
          })
        )
      }),
      firrtl.passes.memlib.InferReadWriteAnnotation,
      CIRCTTargetAnnotation(CIRCTTarget.Verilog),
      EmitAllModulesAnnotation(classOf[ChirrtlEmitter]),
      FirtoolOption("--disable-annotation-unknown")
    )))
    // This is super dirty, need refactor
    os.write.over(os.Path(dir) / "plusarg.h", PlusArgArtefacts.serialize_cHeader)
  }

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args)
}
