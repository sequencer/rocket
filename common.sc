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

  // Use SNAPSHOT chisel by default, downstream users should override this for their own project versions.
  def chisel3IvyDep: T[Option[Dep]] = None

  def chisel3PluginIvyDep: T[Option[Dep]] = None

  def tilelinkIvyDep: T[Option[Dep]] = None

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
