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
      def cSrcPath = T {
        val path = T.dest / "bootrom.S"
        os.write(path,
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
            |""".stripMargin)
        PathRef(path)
      }

      def linkScript = T {
        val path = T.dest / "bootrom.ld"
        os.write(path,
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
        )
        PathRef(path)
      }

      def elf = T {
        val path = T.dest / "bootrom.elf"
        os.proc(
          "clang",
          "--target=riscv64", "-march=rv64gc",
          "-mno-relax",
          "-static",
          "-nostdlib",
          "-Wl,--no-gc-sections",
          "-fuse-ld=lld", s"-T${linkScript().path}",
          s"${cSrcPath().path}",
          "-o", path
        ).call(T.dest)
        PathRef(path)
      }

      def bin = T {
        val path = T.dest / "bootrom.bin"
        os.proc(
          "llvm-objcopy",
          "-O", "binary",
          s"${elf().path}",
          s"$path"
        ).call(T.dest)
        PathRef(path)
      }

      def img = T {
        val path = T.dest / "bootrom.img"
        os.proc(
          "dd",
          s"if=${bin().path}",
          s"of=$path",
          "bs=128",
          "count=1"
        ).call(T.dest)
        PathRef(path)
      }
    }

    def eicgPath = T {
      val path = T.dest / "EICG_wrapper.v"
      os.write(path,
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

    def rtls = T {
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
      os.read(T.dest / "filelist.f").split("\n").map(str =>
        try {
          os.Path(str)
        } catch {
          case e: IllegalArgumentException if e.getMessage.contains("is not an absolute path") =>
            T.dest / str
        }
      ).filter(p => p.ext == "v" || p.ext == "sv").map(PathRef(_)) ++
        // find annotation file
        os.walk(T.dest).filter(p => p.last.endsWith("anno.json")).map(PathRef(_))
    }
  }
}


object spike extends Module {
  override def millSourcePath = os.pwd / "dependencies" / "riscv-isa-sim"

  // ask make to cache file.
  def compile = T.persistent {
    os.proc(millSourcePath / "configure", "--prefix", "/usr", "--without-boost", "--without-boost-asio", "--without-boost-regex").call(
      T.ctx.dest, Map(
        "CC" -> "clang",
        "CXX" -> "clang++",
        "AR" -> "llvm-ar",
        "RANLIB" -> "llvm-ranlib",
        "LD" -> "lld",
      )
    )
    os.proc("make", "-j", Runtime.getRuntime().availableProcessors()).call(T.ctx.dest)
    T.ctx.dest
  }
}

object compilerrt extends Module {
  override def millSourcePath = os.pwd / "dependencies" / "llvm-project" / "compiler-rt"

  // ask make to cache file.
  def compile = T.persistent {
    os.proc("cmake", "-S", millSourcePath,
      "-DCOMPILER_RT_BUILD_LIBFUZZER=OFF",
      "-DCOMPILER_RT_BUILD_SANITIZERS=OFF",
      "-DCOMPILER_RT_BUILD_PROFILE=OFF",
      "-DCOMPILER_RT_BUILD_MEMPROF=OFF",
      "-DCOMPILER_RT_BUILD_ORC=OFF",
      "-DCOMPILER_RT_BUILD_BUILTINS=ON",
      "-DCOMPILER_RT_BAREMETAL_BUILD=ON",
      "-DCOMPILER_RT_INCLUDE_TESTS=OFF",
      "-DCOMPILER_RT_HAS_FPIC_FLAG=OFF",
      "-DCOMPILER_RT_DEFAULT_TARGET_ONLY=On",
      "-DCOMPILER_RT_OS_DIR=riscv64",
      "-DCMAKE_BUILD_TYPE=Release",
      "-DCMAKE_SYSTEM_NAME=Generic",
      "-DCMAKE_SYSTEM_PROCESSOR=riscv64",
      "-DCMAKE_TRY_COMPILE_TARGET_TYPE=STATIC_LIBRARY",
      "-DCMAKE_SIZEOF_VOID_P=8",
      "-DCMAKE_ASM_COMPILER_TARGET=riscv64-none-elf",
      "-DCMAKE_C_COMPILER_TARGET=riscv64-none-elf",
      "-DCMAKE_C_COMPILER_WORKS=ON",
      "-DCMAKE_CXX_COMPILER_WORKS=ON",
      "-DCMAKE_C_COMPILER=clang",
      "-DCMAKE_CXX_COMPILER=clang++",
      "-DCMAKE_C_FLAGS=-nodefaultlibs -fno-exceptions -mno-relax -Wno-macro-redefined -fPIC",
      "-DCMAKE_INSTALL_PREFIX=/usr",
      "-Wno-dev",
    ).call(T.ctx.dest)
    os.proc("make", "-j", Runtime.getRuntime().availableProcessors()).call(T.ctx.dest)
    T.ctx.dest
  }
}

object musl extends Module {
  override def millSourcePath = os.pwd / "dependencies" / "musl"

  // ask make to cache file.
  def libraryResources = T.persistent {
    os.proc("make", s"DESTDIR=${T.ctx.dest}", "install").call(compilerrt.compile())
    PathRef(T.ctx.dest)
  }

  def compile = T.persistent {
    val p = libraryResources().path
    os.proc(millSourcePath / "configure", "--target=riscv64-none-elf", "--prefix=/usr").call(
      T.ctx.dest,
      Map(
        "CC" -> "clang",
        "CXX" -> "clang++",
        "AR" -> "llvm-ar",
        "RANLIB" -> "llvm-ranlib",
        "LD" -> "lld",
        "LIBCC" -> "-lclang_rt.builtins-riscv64",
        "CFLAGS" -> "--target=riscv64 -mno-relax -nostdinc",
        "LDFLAGS" -> s"-fuse-ld=lld --target=riscv64 -nostdlib -L${p}/usr/lib/riscv64",
      )
    )
    os.proc("make", "-j", Runtime.getRuntime().availableProcessors()).call(T.ctx.dest)
    T.ctx.dest
  }
}

object pk extends Module {
  override def millSourcePath = os.pwd / "dependencies" / "riscv-pk"

  // ask make to cache file.
  def libraryResources = T.persistent {
    os.proc("make", s"DESTDIR=${T.ctx.dest}", "install").call(musl.compile())
    PathRef(T.ctx.dest)
  }

  def compile = T.persistent {
    val p = libraryResources().path
    val env = Map(
      "CC" -> "clang",
      "CXX" -> "clang++",
      "AR" -> "llvm-ar",
      "RANLIB" -> "llvm-ranlib",
      "LD" -> "lld",
      "CFLAGS" -> s"--target=riscv64 -mno-relax -nostdinc -I${p}/usr/include",
      "LDFLAGS" -> "-fuse-ld=lld --target=riscv64 -nostdlib",
    )
    os.proc("autoreconf", "-fi").call(millSourcePath)
    os.proc(millSourcePath / "configure", "--host=riscv64-none-elf").call(T.ctx.dest, env)
    os.proc("make", "-j", Runtime.getRuntime().availableProcessors(), "pk").call(T.ctx.dest, env)
    T.ctx.dest / "pk"
  }
}
