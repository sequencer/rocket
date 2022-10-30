package rocket.tests

import chisel3.stage.ChiselGeneratorAnnotation
import circt.stage.{CIRCTTarget, CIRCTTargetAnnotation, ChiselStage, FirtoolOption}
import firrtl.options.TargetDirAnnotation
import firrtl.{AnnotationSeq, ChirrtlEmitter, EmitAllModulesAnnotation}
import freechips.rocketchip.devices.tilelink.BootROMLocated
import freechips.rocketchip.util.ClockGateModelFile
import mainargs._

object Main {
  @main
  def elaborate(
                 @arg(name = "dir") dir: String,
                 @arg(name = "bootrom_file") bootrom: String,
                 @arg(name = "eicg_file") eicgFile: String
               ): Unit =
    (new ChiselStage).transform(AnnotationSeq(Seq(
      TargetDirAnnotation(dir),
      new ChiselGeneratorAnnotation(() => {
        new DUT((new freechips.rocketchip.system.DefaultConfig).alter((site, here, up) => {
          case BootROMLocated(x) => up(BootROMLocated(x), site).map(_.copy(contentFileName = bootrom))
          case ClockGateModelFile => Some(eicgFile)
        }))
      }),
      firrtl.passes.memlib.InferReadWriteAnnotation,
      CIRCTTargetAnnotation(CIRCTTarget.SystemVerilog),
      EmitAllModulesAnnotation(classOf[ChirrtlEmitter]),
      FirtoolOption("--disable-annotation-unknown")
    )))

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args)
}