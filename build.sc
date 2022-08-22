import mill._
import mill.define.Sources
import mill.modules.Util
import scalalib._
import mill.bsp._

import $file.dependencies.chisel3.build
import $file.dependencies.firrtl.build
import $file.dependencies.treadle.build
import $file.dependencies.chiseltest.build
import $file.dependencies.cde.build
import $file.dependencies.diplomacy.build

object v {
  val scala = "2.12.16"
  val upickle = ivy"com.lihaoyi::upickle:2.0.0"
  val oslib = ivy"com.lihaoyi::os-lib:0.8.0"
  val pprint = ivy"com.lihaoyi::pprint:0.7.3"
  val utest = ivy"com.lihaoyi::utest:0.8.0"
}

object myfirrtl extends dependencies.firrtl.build.firrtlCrossModule(v.scala) {
  override def millSourcePath = os.pwd / "dependencies" / "firrtl"
  override def ivyDeps = super.ivyDeps()
  override val checkSystemAntlr4Version = false
  override val checkSystemProtocVersion = false
  override val protocVersion = os.proc("protoc", "--version").call().out.text.dropRight(1).split(' ').last
  override val antlr4Version = os.proc("antlr4").call().out.text.split('\n').head.split(' ').last
}

object mychisel3 extends dependencies.chisel3.build.chisel3CrossModule(v.scala) {
  override def millSourcePath = os.pwd / "dependencies" / "chisel3"
  def firrtlModule: Option[PublishModule] = Some(myfirrtl)
  def treadleModule: Option[PublishModule] = Some(mytreadle)
  def chiseltestModule: Option[PublishModule] = Some(mychiseltest)
}

object mychiseltest extends dependencies.chiseltest.build.chiseltestCrossModule(v.scala) {
  override def millSourcePath = os.pwd / "dependencies" / "chiseltest"
  def chisel3Module = Some(mychisel3)
  def treadleModule = Some(mytreadle)
}

object mytreadle extends dependencies.treadle.build.treadleCrossModule(v.scala) {
  override def millSourcePath = os.pwd /  "dependencies" / "treadle"
  def firrtlModule: Option[PublishModule] = Some(myfirrtl)
}

object mycde extends dependencies.cde.build.cde(v.scala) with PublishModule {
  override def millSourcePath = os.pwd /  "dependencies" / "cde" / "cde"
}

object mydiplomacy extends dependencies.diplomacy.build.diplomacy(v.scala) with PublishModule {
  override def millSourcePath = os.pwd /  "dependencies" / "diplomacy"
  def chisel3Module = Some(mychisel3)
  def cdeModule = Some(mycde)
}

trait rocket extends ScalaModule {
  override def scalaVersion = v.scala
  override def scalacPluginClasspath = T { super.scalacPluginClasspath() ++ Agg(
    mychisel3.plugin.jar()
  ) }
  override def scalacOptions = T {
    super.scalacOptions() ++ Agg(
      s"-Xplugin:${mychisel3.plugin.jar().path}",
    )
  }
  override def moduleDeps: Seq[ScalaModule] = Seq(mychisel3)
}

object rocket extends rocket {
  override def scalaVersion = v.scala
  object tests extends Tests with TestModule.ScalaTest {
    override def ivyDeps = super.ivyDeps() ++ Agg(
      v.utest
    )
    override def moduleDeps = super.moduleDeps ++ Seq(mychiseltest)
  }
}
