import mill._
import mill.scalalib._
import mill.scalalib.publish._
import coursier.maven.MavenRepository

import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version::0.1.4`
import de.tobiasroeser.mill.vcs.version.VcsVersion

trait RocketPublishModule extends ScalaModule with PublishModule {m =>
  def publishVersion = de.tobiasroeser.mill.vcs.version.VcsVersion.vcsState().format()
  def pomSettings = T {
    PomSettings(
      description = artifactName(),
      organization = "org.chipsalliance",
      url = "https://github.com/chipsalliance/rocket",
      licenses = Seq(License.`Apache-2.0`, License.`BSD-3-Clause`),
      versionControl = VersionControl.github("chipsalliance", "rocket"),
      developers = Seq(
        Developer("aswaterman", "Andrew Waterman", "https://github.com/aswaterman")
      )
    )
  }
}

trait RocketModule extends RocketPublishModule {
  // SNAPSHOT of Chisel is published to the SONATYPE
  override def repositoriesTask = T.task { super.repositoriesTask() ++ Seq(
    MavenRepository("https://oss.sonatype.org/content/repositories/snapshots"),
    MavenRepository("https://oss.sonatype.org/content/repositories/releases")
  ) }

  // override to build from source, see the usage of chipsalliance/playground
  def chisel3Module: Option[PublishModule] = None

  // override to build from source, see the usage of chipsalliance/playground
  def chisel3PluginJar: T[Option[PathRef]] = T {
    None
  }

  // override to build from source, see the usage of chipsalliance/playground
  def tilelinkModule: Option[PublishModule] = None

  def opcodesModule: Opcodes

  // Use SNAPSHOT chisel by default, downstream users should override this for their own project versions.
  def chisel3IvyDep: T[Option[Dep]] = None

  def chisel3PluginIvyDep: T[Option[Dep]] = None

  def tilelinkIvyDep: T[Option[Dep]] = None

  override def generatedSources: T[Seq[PathRef]] = T {
    Seq(
      opcodesModule.rvInstruction(),
      opcodesModule.rv32Instruction(),
      opcodesModule.rv64Instruction(),
      opcodesModule.csrs(),
      opcodesModule.causes()
      )
  }

  // User should not override lines below
  override def moduleDeps = Seq() ++ chisel3Module ++ tilelinkModule

  override def scalacPluginClasspath = T {
    super.scalacPluginClasspath() ++ chisel3PluginJar()
  }

  override def scalacPluginIvyDeps = T {
    Agg() ++ chisel3PluginIvyDep()
  }

  override def ivyDeps = T {
    Agg() ++ chisel3IvyDep()
  }
}

trait DiplomaticModule extends RocketPublishModule {
  def rocketModule: PublishModule
  def diplomacyModule: PublishModule
  def macroParadiseIvy: Dep
  override def moduleDeps = super.moduleDeps :+ rocketModule :+ diplomacyModule
  // TODO: refactor out
  override def compileIvyDeps = Agg(macroParadiseIvy)
  override def scalacPluginIvyDeps = Agg(macroParadiseIvy)
}

trait Opcodes extends Module {
  // I really hate riscv-opcode parse.py, So I rewrite it.
  def script = T {
    val f = T.ctx.dest / "rocket.py"
    os.write(f,
      """#!/usr/bin/env python3
        |
        |from constants import *
        |from parse import *
        |
        |if __name__ == "__main__":
        |    tpe = sys.argv[1]
        |    if tpe in ["rv64*", "rv32*", "rv*"]:
        |        match tpe:
        |            case "rv64*":
        |                obj_name = "Instructions64"
        |            case "rv32*":
        |                obj_name = "Instructions32"
        |            case "rv*":
        |                obj_name = "Instructions"
        |        instrs = create_inst_dict([tpe], False)
        |        instrs_str = ("package org.chipsalliance.rocket\n"
        |                      "import chisel3.util.BitPat\n"
        |                      f"object {obj_name} {{\n")
        |        for i in instrs:
        |            instrs_str += f'  def {i.upper().replace(".","_")} = BitPat("b{instrs[i]["encoding"].replace("-","?")}")\n'
        |        instrs_str += "}"
        |        print(instrs_str)
        |    if tpe in ["causes"]:
        |        cause_names_str = ("package org.chipsalliance.rocket\n"
        |                           "object Causes {\n")
        |        for num, name in causes:
        |            cause_names_str += f'  val {name.lower().replace(" ","_")} = {hex(num)}\n'
        |        cause_names_str += '''  val all = {
        |    val res = collection.mutable.ArrayBuffer[Int]()
        |'''
        |        for num, name in causes:
        |            cause_names_str += f'    res += {name.lower().replace(" ","_")}\n'
        |        cause_names_str += '''    res.toArray
        |  }'''
        |        cause_names_str += "}"
        |        print(cause_names_str)
        |    if tpe in ["csrs"]:
        |        csr_names_str = ("package org.chipsalliance.rocket\n"
        |                         "object CSRs {\n")
        |        for num, name in csrs+csrs32:
        |            csr_names_str += f'  val {name} = {hex(num)}\n'
        |        csr_names_str += '''  val all = {
        |    val res = collection.mutable.ArrayBuffer[Int]()
        |'''
        |        for num, name in csrs:
        |            csr_names_str += f'''    res += {name}\n'''
        |        csr_names_str += '''    res.toArray
        |      }
        |  val all32 = {
        |    val res = collection.mutable.ArrayBuffer(all:_*)
        |'''
        |        for num, name in csrs32:
        |            csr_names_str += f'''    res += {name}\n'''
        |        csr_names_str += '''    res.toArray
        |  }
        |'''
        |        csr_names_str += "}"
        |        print(csr_names_str)
        |""".stripMargin
    )
    PathRef(f)
  }
  def rv64Instruction = T {
    val f = T.ctx.dest / "Instructions64.scala"
    os.proc("python", script().path, "rv64*").call(stdout = f, env = Map("PYTHONPATH"->millSourcePath.toString))
    PathRef(f)
  }

  def rv32Instruction = T {
    val f = T.ctx.dest / "Instructions32.scala"
    os.proc("python", script().path, "rv32*").call(stdout = f, env = Map("PYTHONPATH"->millSourcePath.toString))
    PathRef(f)
  }
  def rvInstruction = T {
    val f = T.ctx.dest / "Instructions.scala"
    os.proc("python", script().path, "rv*").call(stdout = f, env = Map("PYTHONPATH"->millSourcePath.toString))
    PathRef(f)
  }
  def causes = T {
    val f = T.ctx.dest / "Causes.scala"
    os.proc("python", script().path, "causes").call(stdout = f, env = Map("PYTHONPATH"->millSourcePath.toString))
    PathRef(f)
  }
  def csrs = T {
    val f = T.ctx.dest / "CSRs.scala"
    os.proc("python", script().path, "csrs").call(stdout = f, env = Map("PYTHONPATH"->millSourcePath.toString))
    PathRef(f)
  }
}
