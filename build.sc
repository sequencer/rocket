import mill._
import mill.scalalib._
import mill.scalalib.publish._
import mill.scalalib.scalafmt._
import coursier.maven.MavenRepository
import $file.dependencies.cde.build
import $file.dependencies.`berkeley-hardfloat`.build
import $file.dependencies.`rocket-chip`.common
import $file.dependencies.chisel3.build
import $file.dependencies.firrtl.build
import $file.dependencies.treadle.build
import $file.dependencies.chiseltest.build
import $file.common

object v {
  val scala = "2.12.16"
  val chisel3 = ivy"edu.berkeley.cs::chisel3:3.6-SNAPSHOT"
  val chisel3Plugin = ivy"edu.berkeley.cs::chisel3-plugin:3.6-SNAPSHOT"
}

object myfirrtl extends dependencies.firrtl.build.firrtlCrossModule(v.scala) {
  override def millSourcePath = os.pwd / "dependencies" / "firrtl"
  override val checkSystemAntlr4Version = false
  override val checkSystemProtocVersion = false
  override val protocVersion = os.proc("protoc", "--version").call().out.text.dropRight(1).split(' ').last
  override val antlr4Version = os.proc("antlr4").call().out.text.split('\n').head.split(' ').last
}
object mytreadle extends dependencies.treadle.build.treadleCrossModule(v.scala) {
  override def millSourcePath = os.pwd /  "dependencies" / "treadle"
  def firrtlModule: Option[PublishModule] = Some(myfirrtl)
}
object mychisel3 extends dependencies.chisel3.build.chisel3CrossModule(v.scala) {
  override def millSourcePath = os.pwd / "dependencies" / "chisel3"
  def firrtlModule: Option[PublishModule] = Some(myfirrtl)
  def treadleModule: Option[PublishModule] = Some(mytreadle)
  def chiseltestModule: Option[PublishModule] = Some(mychiseltest)
}
object mychiseltest extends dependencies.chiseltest.build.chiseltestCrossModule(v.scala) {
  override def millSourcePath = os.pwd /  "dependencies" / "chiseltest"
  def chisel3Module: Option[PublishModule] = Some(mychisel3)
  def treadleModule: Option[PublishModule] = Some(mytreadle)
}
// TODO: remove and switch to diplomacy
object mycde extends dependencies.cde.build.cde(v.scala) with PublishModule {
  override def millSourcePath = os.pwd /  "dependencies" / "cde" / "cde"
}
object myhardfloat extends dependencies.`berkeley-hardfloat`.build.hardfloat {
  override def millSourcePath = os.pwd /  "dependencies" / "berkeley-hardfloat"
  override def scalaVersion = v.scala
  def chisel3Module: Option[PublishModule] = Some(mychisel3)
  override def scalacPluginClasspath = T { super.scalacPluginClasspath() ++ Agg(mychisel3.plugin.jar()) }
  override def scalacOptions = T { Seq("-Xsource:2.11", s"-Xplugin:${mychisel3.plugin.jar().path}", "-P:chiselplugin:genBundleElements") }
}
object myrocketchip extends dependencies.`rocket-chip`.common.CommonRocketChip {
  def chisel3Module: Option[PublishModule] = Some(mychisel3)
  def hardfloatModule: PublishModule = myhardfloat
  def configModule: PublishModule = mycde
  override def scalaVersion = v.scala
  override def millSourcePath = os.pwd /  "dependencies" / "rocket-chip"
  override def scalacPluginClasspath = T { super.scalacPluginClasspath() ++ Agg(mychisel3.plugin.jar()) }
  override def scalacOptions = T { Seq("-Xsource:2.11", s"-Xplugin:${mychisel3.plugin.jar().path}", "-P:chiselplugin:genBundleElements") }
}
object mytilelink extends dependencies.tilelink.common.TileLinkModule {
  def scalaVersion = T { v.scala }
  def chisel3Module = Some(mychisel3)
  def chisel3PluginJar = T { Some(mychisel3.plugin.jar()) } 
}

object rocket extends common.RocketModule with ScalafmtModule { m =>
  def scalaVersion = T { v.scala }
  def rocketchipModule = myrocketchip
  def chisel3Module = Some(mychisel3)
  def chisel3PluginJar = T { Some(mychisel3.plugin.jar()) }
}

object diplomatic extends common.DiplomaticModule { m =>
  def scalaVersion = T { v.scala }
  // TODO: migrate it to mydiplomacy
  def diplomaticModule = myrocketchip
}
