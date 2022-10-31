import mill._
import mill.scalalib._
import mill.scalalib.publish._
import mill.scalalib.scalafmt._
import mill.modules.Util
import mill.define.Sources
import coursier.maven.MavenRepository
import $file.dependencies.cde.build
import $file.dependencies.`berkeley-hardfloat`.build
import $file.dependencies.`rocket-chip`.common
import $file.dependencies.chisel3.build
import $file.dependencies.firrtl.build
import $file.dependencies.treadle.build
import $file.dependencies.chiseltest.build
import $file.dependencies.tilelink.common
import $file.common

object v {
  val scala = "2.12.16"
  val chisel3 = ivy"edu.berkeley.cs::chisel3:3.6-SNAPSHOT"
  val chisel3Plugin = ivy"edu.berkeley.cs::chisel3-plugin:3.6-SNAPSHOT"
  val macroParadise = ivy"org.scalamacros:::paradise:2.1.1"
  val mainargs = ivy"com.lihaoyi::mainargs:0.3.0"
}

object myfirrtl extends dependencies.firrtl.build.firrtlCrossModule(v.scala) {
  override def millSourcePath = os.pwd / "dependencies" / "firrtl"

  override val checkSystemAntlr4Version = false
  override val checkSystemProtocVersion = false
  override val protocVersion = os.proc("protoc", "--version").call().out.text.dropRight(1).split(' ').last
  override val antlr4Version = os.proc("antlr4").call().out.text.split('\n').head.split(' ').last
}

object mytreadle extends dependencies.treadle.build.treadleCrossModule(v.scala) {
  override def millSourcePath = os.pwd / "dependencies" / "treadle"

  def firrtlModule: Option[PublishModule] = Some(myfirrtl)
}

object mychisel3 extends dependencies.chisel3.build.chisel3CrossModule(v.scala) {
  override def millSourcePath = os.pwd / "dependencies" / "chisel3"

  def firrtlModule: Option[PublishModule] = Some(myfirrtl)

  def treadleModule: Option[PublishModule] = Some(mytreadle)

  def chiseltestModule: Option[PublishModule] = Some(mychiseltest)
}

object mychiseltest extends dependencies.chiseltest.build.chiseltestCrossModule(v.scala) {
  override def millSourcePath = os.pwd / "dependencies" / "chiseltest"

  def chisel3Module: Option[PublishModule] = Some(mychisel3)

  def treadleModule: Option[PublishModule] = Some(mytreadle)
}

// TODO: remove and switch to diplomacy
object mycde extends dependencies.cde.build.cde(v.scala) with PublishModule {
  override def millSourcePath = os.pwd / "dependencies" / "cde" / "cde"
}

object myhardfloat extends dependencies.`berkeley-hardfloat`.build.hardfloat {
  override def millSourcePath = os.pwd / "dependencies" / "berkeley-hardfloat"

  override def scalaVersion = v.scala

  def chisel3Module: Option[PublishModule] = Some(mychisel3)

  override def scalacPluginClasspath = T {
    super.scalacPluginClasspath() ++ Agg(mychisel3.plugin.jar())
  }

  override def scalacOptions = T {
    Seq("-Xsource:2.11", s"-Xplugin:${mychisel3.plugin.jar().path}")
  }
}

object myrocketchip extends dependencies.`rocket-chip`.common.CommonRocketChip {
  override def millSourcePath = os.pwd / "dependencies" / "rocket-chip"

  def chisel3Module: Option[PublishModule] = Some(mychisel3)

  def hardfloatModule: PublishModule = myhardfloat

  def configModule: PublishModule = mycde

  override def scalaVersion = v.scala

  override def scalacPluginClasspath = T {
    super.scalacPluginClasspath() ++ Agg(mychisel3.plugin.jar())
  }

  override def scalacOptions = T {
    Seq("-Xsource:2.11", s"-Xplugin:${mychisel3.plugin.jar().path}")
  }
}

object mytilelink extends dependencies.tilelink.common.TileLinkModule {
  override def millSourcePath = os.pwd / "dependencies" / "tilelink" / "tilelink"

  def scalaVersion = T {
    v.scala
  }

  def chisel3Module = Some(mychisel3)

  def chisel3PluginJar = T {
    Some(mychisel3.plugin.jar())
  }
}

object rocket extends common.RocketModule with ScalafmtModule {
  m =>
  def scalaVersion = T {
    v.scala
  }

  def chisel3Module = Some(mychisel3)

  def chisel3PluginJar = T {
    Some(mychisel3.plugin.jar())
  }

  def tilelinkModule = Some(mytilelink)
}

object diplomatic extends common.DiplomaticModule {
  m =>
  def scalaVersion = T {
    v.scala
  }

  // TODO: migrate it to mydiplomacy
  def rocketModule = rocket

  def diplomacyModule = myrocketchip

  def macroParadiseIvy = v.macroParadise

  // TODO: remove -Xsource:2.11
  override def scalacOptions = T {
    Seq("-Xsource:2.11", s"-Xplugin:${mychisel3.plugin.jar().path}")
  }
}

object tests extends Module {
  object elaborate extends ScalaModule with ScalafmtModule {
    def scalaVersion = T {
      v.scala
    }

    override def moduleDeps = Seq(diplomatic)

    override def scalacOptions = T {
      Seq("-Xsource:2.11", s"-Xplugin:${mychisel3.plugin.jar().path}")
    }

    override def ivyDeps = Agg(
      v.mainargs
    )

    object bootrom extends Module {
      def cSrcString = T.input {
        """#define DRAM_BASE 0x80000000
          |
          |.section .text.start, "ax", @progbits
          |.globl _start
          |_start:
          |  csrwi 0x7c1, 0 // disable chicken bits
          |  li s0, DRAM_BASE
          |  csrr a0, mhartid
          |  la a1, _dtb
          |  jr s0
          |
          |.section .text.hang, "ax", @progbits
          |.globl _hang
          |_hang:
          |  csrwi 0x7c1, 0 // disable chicken bits
          |  csrr a0, mhartid
          |  la a1, _dtb
          |  csrwi mie, 0
          |1:
          |  wfi
          |  j 1b
          |
          |.section .rodata.dtb, "a", @progbits
          |.globl _dtb
          |.align 5, 0
          |_dtb:
          |.ascii "DTB goes here"
          |""".stripMargin
      }

      def cSrcPath = T.persistent {
        val path = T.dest / "bootrom.S"
        os.write.over(path, cSrcString())
        PathRef(path)
      }

      def linkScriptString = T.input {
        """SECTIONS
          |{
          |  ROM_BASE = 0x10000; /* ... but actually position independent */
          |  . = ROM_BASE;
          |  .text.start : { *(.text.start) }
          |  . = ROM_BASE + 0x40;
          |  .text.hang : { *(.text.hang) }
          |  . = ROM_BASE + 0x80;
          |  .rodata.dtb : { *(.rodata.dtb) }
          |}
          |""".stripMargin
      }

      def linkScriptPath = T.persistent {
        val path = T.dest / "bootrom.ld"
        os.write.over(path, linkScriptString())
        PathRef(path)
      }

      def elf = T.persistent {
        val path = T.dest / "bootrom.elf"
        mill.modules.Jvm.runSubprocess(Seq(
          "clang",
          "--target=riscv64", "-march=rv64gc",
          "-mno-relax",
          "-static",
          "-nostdlib",
          "-Wl,--no-gc-sections",
          "-fuse-ld=lld", s"-T${linkScriptPath().path}",
          s"${cSrcPath().path}",
          "-o", s"$path"),
          Map[String, String](),
          T.dest
        )
        PathRef(path)
      }

      def bin = T.persistent {
        val path = T.dest / "bootrom.bin"
        mill.modules.Jvm.runSubprocess(Seq(
          "llvm-objcopy",
          "-O", "binary",
          s"${elf().path}",
          s"$path"),
          Map[String, String](),
          T.dest
        )
        PathRef(path)
      }

      def img = T.persistent {
        val path = T.dest / "bootrom.img"
        mill.modules.Jvm.runSubprocess(Seq(
          "dd",
          s"if=${bin().path}",
          s"of=$path",
          "bs=128",
          "count=1"),
          Map[String, String](),
          T.dest
        )
        PathRef(path)
      }
    }

    def eicgPath = T.persistent {
      val path = T.dest / "EICG_wrapper.v"
      os.write.over(path,
        """/* verilator lint_off UNOPTFLAT */
          |module EICG_wrapper(
          |  output out,
          |  input en,
          |  input test_en,
          |  input in
          |);
          |  reg en_latched /*verilator clock_enable*/;
          |  always @(*) begin
          |     if (!in) begin
          |        en_latched = en || test_en;
          |     end
          |  end
          |  assign out = en_latched && in;
          |endmodule
          |""".stripMargin
      )
      PathRef(path)
    }

    def elaborate = T.persistent {
      mill.modules.Jvm.runSubprocess(
        finalMainClass(),
        runClasspath().map(_.path),
        forkArgs(),
        forkEnv(),
        Seq(
          "--dir", T.dest.toString,
          "--bootrom_file", bootrom.img().path.toString,
          "--eicg_file", eicgPath().path.toString,
        ),
        workingDir = forkWorkingDir()
      )
      PathRef(T.dest)
    }

    def rtls = T.persistent {
      os.read(elaborate().path / "filelist.f").split("\n").map(str =>
        try {
          os.Path(str)
        } catch {
          case e: IllegalArgumentException if e.getMessage.contains("is not an absolute path") =>
            elaborate().path / str
        }
      ).filter(p => p.ext == "v" || p.ext == "sv").map(PathRef(_)).toSeq
    }

    def annos = T.persistent {
      os.walk(elaborate().path).filter(p => p.last.endsWith("anno.json")).map(PathRef(_))
    }
  }

  object emulator extends Module {

    object spike extends Module {
      override def millSourcePath = os.pwd / "dependencies" / "riscv-isa-sim"

      // ask make to cache file.
      def compile = T.persistent {
        mill.modules.Jvm.runSubprocess(
          Seq(millSourcePath / "configure",
            "--prefix", "/usr",
            "--without-boost",
            "--without-boost-asio",
            "--without-boost-regex"
          ).map(_.toString),
          Map(
            "CC" -> "clang",
            "CXX" -> "clang++",
            "AR" -> "llvm-ar",
            "RANLIB" -> "llvm-ranlib",
            "LD" -> "lld"
          ), T.ctx.dest)
        mill.modules.Jvm.runSubprocess(Seq("make", "-j", Runtime.getRuntime().availableProcessors()).map(_.toString), Map[String, String](), T.ctx.dest)
        T.ctx.dest
      }
    }

    def csrcDir = T {
      PathRef(millSourcePath / "src")
    }

    def allCSourceFiles = T {
      Lib.findSourceFiles(Seq(csrcDir()), Seq("S", "s", "c", "cpp", "cc")).map(PathRef(_))
    }

    def CMakeListsString = T {
      // format: off
      s"""cmake_minimum_required(VERSION 3.20)
         |project(emulator)
         |include_directories(${csrcDir().path})
         |# plusarg is here
         |include_directories(${elaborate.elaborate().path})
         |link_directories(${spike.compile().toString})
         |include_directories(${spike.compile().toString})
         |include_directories(${spike.millSourcePath.toString})
         |
         |set(CMAKE_CXX_STANDARD 17)
         |set(CMAKE_C_COMPILER "clang")
         |set(CMAKE_CXX_COMPILER "clang++")
         |set(CMAKE_CXX_FLAGS "$${CMAKE_CXX_FLAGS} -DVERILATOR -DTEST_HARNESS=VTestHarness")
         |set(THREADS_PREFER_PTHREAD_FLAG ON)
         |
         |find_package(verilator)
         |find_package(Threads)
         |
         |add_executable(emulator
         |${allCSourceFiles().map(_.path).mkString("\n")}
         |)
         |
         |target_link_libraries(emulator PRIVATE $${CMAKE_THREAD_LIBS_INIT})
         |target_link_libraries(emulator PRIVATE fesvr)
         |verilate(emulator
         |  SOURCES
         |${vsrcs().map(_.path).mkString("\n")}
         |  TOP_MODULE DUT
         |  PREFIX VTestHarness
         |  THREADS ${Runtime.getRuntime().availableProcessors()}
         |  VERILATOR_ARGS ${verilatorArgs().mkString(" ")}
         |)
         |""".stripMargin
      // format: on
    }

    def verilatorArgs = T.input {
      Seq(
        // format: off
        "-Wno-UNOPTTHREADS", "-Wno-STMTDLY", "-Wno-LATCH", "-Wno-WIDTH",
        "--x-assign unique",
        "+define+RANDOMIZE_GARBAGE_ASSIGN",
        "--output-split 20000",
        "--output-split-cfuncs 20000",
        "--max-num-width 1048576"
        // format: on
      )
    }

    def vsrcs = T.persistent {
      elaborate.rtls().filter(p => p.path.ext == "v" || p.path.ext == "sv")
    }

    def cmakefileLists = T.persistent {
      val path = T.dest / "CMakeLists.txt"
      os.write.over(path, CMakeListsString())
      PathRef(T.dest)
    }

    def elf = T.persistent {
      mill.modules.Jvm.runSubprocess(Seq("cmake", "-G", "Ninja", "-S", cmakefileLists().path, "-B", T.dest.toString, "-DCMAKE_BUILD_TYPE=Release").map(_.toString), Map[String, String](), T.dest)
      mill.modules.Jvm.runSubprocess(Seq("ninja", "-C", T.dest).map(_.toString), Map[String, String](), T.dest)
      PathRef(T.dest / "emulator")
    }
  }

  object cases extends Module {
    c =>
    trait Suite extends Module {
      def name: T[String]

      def description: T[String]

      def binaries: T[Seq[PathRef]]
    }

    object riscvtests extends Module {
      trait Suite extends c.Suite {
        def name = T {
          millSourcePath.last
        }

        def description = T {
          s"test suite ${name} from riscv-tests"
        }

        def binaries = T {
          os.walk(untar().path).filter(p => p.last.startsWith(name())).filterNot(p => p.last.endsWith("dump")).map(PathRef(_))
        }
      }

      def commit = T.input {
        "047314c5b0525b86f7d5bb6ffe608f7a8b33ffdb"
      }

      def tgz = T.persistent {
        Util.download(s"https://github.com/ZenithalHourlyRate/riscv-tests-release/releases/download/tag-${commit()}/riscv-tests.tgz")
      }

      def untar = T.persistent {
        mill.modules.Jvm.runSubprocess(Seq("tar", "xzf", tgz().path).map(_.toString), Map[String, String](), T.dest)
        PathRef(T.dest)
      }

      // These bucket are generated via
      // os.walk(os.pwd).map(_.last).filterNot(_.endsWith("dump")).map(_.split('-').dropRight(1).mkString("-")).toSet.toSeq.sorted.foreach(println)
      object `rv32mi-p` extends Suite

      object `rv32mi-p-lh` extends Suite

      object `rv32mi-p-lw` extends Suite

      object `rv32mi-p-sh` extends Suite

      object `rv32mi-p-sw` extends Suite

      object `rv32si-p` extends Suite

      object `rv32ua-p` extends Suite

      object `rv32ua-v` extends Suite

      object `rv32uc-p` extends Suite

      object `rv32uc-v` extends Suite

      object `rv32ud-p` extends Suite

      object `rv32ud-v` extends Suite

      object `rv32uf-p` extends Suite

      object `rv32uf-v` extends Suite

      object `rv32ui-p` extends Suite

      object `rv32ui-v` extends Suite

      object `rv32um-p` extends Suite

      object `rv32um-v` extends Suite

      object `rv32uzfh-p` extends Suite

      object `rv32uzfh-v` extends Suite

      object `rv64mi-p` extends Suite

      object `rv64mi-p-ld` extends Suite

      object `rv64mi-p-lh` extends Suite

      object `rv64mi-p-lw` extends Suite

      object `rv64mi-p-sd` extends Suite

      object `rv64mi-p-sh` extends Suite

      object `rv64mi-p-sw` extends Suite

      object `rv64mzicbo-p` extends Suite

      object `rv64si-p` extends Suite

      object `rv64si-p-icache` extends Suite

      object `rv64ssvnapot-p` extends Suite

      object `rv64ua-p` extends Suite

      object `rv64ua-v` extends Suite

      object `rv64uc-p` extends Suite

      object `rv64uc-v` extends Suite

      object `rv64ud-p` extends Suite

      object `rv64ud-v` extends Suite

      object `rv64uf-p` extends Suite

      object `rv64uf-v` extends Suite

      object `rv64ui-p` extends Suite

      object `rv64ui-v` extends Suite

      object `rv64um-p` extends Suite

      object `rv64um-v` extends Suite

      object `rv64uzfh-p` extends Suite

      object `rv64uzfh-v` extends Suite
    }
  }

  object run extends Module {
    m =>
    trait RunableTest extends Module {
      def elf: T[PathRef]

      def testcases: T[Seq[PathRef]]

      def run = T {
        testcases().map { bin =>
          val name = bin.path.last
          val p = os.proc(emulator.elf().path, bin.path).call(stdout = T.dest / s"$name.running.log", mergeErrIntoOut = true)
          PathRef(if(p.exitCode != 0) {
            os.move(T.dest / s"$name.running.log", T.dest / s"$name.failed.log")
            System.err.println(s"Test $name failed with exit code ${p.exitCode}")
            T.dest / s"$name.failed.log"
          } else {
            os.move(T.dest / s"$name.running.log", T.dest / s"$name.passed.log")
            T.dest / s"$name.passed.log"
          })
        }
      }

      def report = T {
        val failed = run().filter(_.path.last.endsWith("failed.log"))
        assert(failed.isEmpty, s"tests failed in ${failed.map(_.path.last).mkString(", ")}")
      }
    }

    object rv64default extends Module {
      trait RunableTest extends m.RunableTest {
        def elf = T {
          emulator.elf()
        }
      }

      def run = T {
        `rv64mi-p`.run()
        `rv64mi-p-ld`.run()
        `rv64mi-p-lh`.run()
        `rv64mi-p-lw`.run()
        `rv64mi-p-sd`.run()
        `rv64mi-p-sh`.run()
        `rv64mi-p-sw`.run()
        `rv64si-p`.run()
        `rv64si-p-icache`.run()
        `rv64ua-p`.run()
        `rv64ua-v`.run()
        `rv64uc-p`.run()
        `rv64uc-v`.run()
        `rv64ud-p`.run()
        `rv64ud-v`.run()
        `rv64uf-p`.run()
        `rv64uf-v`.run()
        // https://github.com/riscv-software-src/riscv-tests/issues/419
        // `rv64ui-v`.run()
        `rv64um-p`.run()
        `rv64um-v`.run()
      }

      object `rv64mi-p` extends RunableTest {
        def testcases = cases.riscvtests.`rv64mi-p`.binaries()
      }

      object `rv64mi-p-ld` extends RunableTest {
        def testcases = cases.riscvtests.`rv64mi-p-ld`.binaries()
      }

      object `rv64mi-p-lh` extends RunableTest {
        def testcases = cases.riscvtests.`rv64mi-p-lh`.binaries()
      }

      object `rv64mi-p-lw` extends RunableTest {
        def testcases = cases.riscvtests.`rv64mi-p-lw`.binaries()
      }

      object `rv64mi-p-sd` extends RunableTest {
        def testcases = cases.riscvtests.`rv64mi-p-sd`.binaries()
      }

      object `rv64mi-p-sh` extends RunableTest {
        def testcases = cases.riscvtests.`rv64mi-p-sh`.binaries()
      }

      object `rv64mi-p-sw` extends RunableTest {
        def testcases = cases.riscvtests.`rv64mi-p-sw`.binaries()
      }

      object `rv64mzicbo-p` extends RunableTest {
        def testcases = cases.riscvtests.`rv64mzicbo-p`.binaries()
      }

      object `rv64si-p` extends RunableTest {
        def testcases = cases.riscvtests.`rv64si-p`.binaries()
      }

      object `rv64si-p-icache` extends RunableTest {
        def testcases = cases.riscvtests.`rv64si-p-icache`.binaries()
      }

      object `rv64ssvnapot-p` extends RunableTest {
        def testcases = cases.riscvtests.`rv64ssvnapot-p`.binaries()
      }

      object `rv64ua-p` extends RunableTest {
        def testcases = cases.riscvtests.`rv64ua-p`.binaries()
      }

      object `rv64ua-v` extends RunableTest {
        def testcases = cases.riscvtests.`rv64ua-v`.binaries()
      }

      object `rv64uc-p` extends RunableTest {
        def testcases = cases.riscvtests.`rv64uc-p`.binaries()
      }

      object `rv64uc-v` extends RunableTest {
        def testcases = cases.riscvtests.`rv64uc-v`.binaries()
      }

      object `rv64ud-p` extends RunableTest {
        def testcases = cases.riscvtests.`rv64ud-p`.binaries()
      }

      object `rv64ud-v` extends RunableTest {
        def testcases = cases.riscvtests.`rv64ud-v`.binaries()
      }

      object `rv64uf-p` extends RunableTest {
        def testcases = cases.riscvtests.`rv64uf-p`.binaries()
      }

      object `rv64uf-v` extends RunableTest {
        def testcases = cases.riscvtests.`rv64uf-v`.binaries()
      }

      object `rv64ui-p` extends RunableTest {
        def testcases = cases.riscvtests.`rv64ui-p`.binaries()
      }

      object `rv64ui-v` extends RunableTest {
        def testcases = cases.riscvtests.`rv64ui-v`.binaries()
      }

      object `rv64um-p` extends RunableTest {
        def testcases = cases.riscvtests.`rv64um-p`.binaries()
      }

      object `rv64um-v` extends RunableTest {
        def testcases = cases.riscvtests.`rv64um-v`.binaries()
      }
    }
  }

}
