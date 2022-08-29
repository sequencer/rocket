// See LICENSE.Berkeley for license details.
package org.chipsalliance.rocket

import chisel3._
import chisel3.util.BitPat
import chisel3.util.BitPat.bitPatToUInt

object Operands {
  // todo[jiuyang]: These three functions should be add to chisel3.BitPat.
  def X: BitPat = BitPat.dontCare(1)

  def Y: BitPat = BitPat.Y()

  def N: BitPat = BitPat.N()

  object A1 {
    val width: Int = 2
    val X: BitPat = BitPat.dontCare(width)
    val ZERO: BitPat = BitPat(0.U(width.W))
    val RS1: BitPat = BitPat(1.U(width.W))
    val PC: BitPat = BitPat(2.U(width.W))
  }

  object IMM {
    val width: Int = 3
    val X: BitPat = BitPat.dontCare(width)
    val S: BitPat = BitPat(0.U(width.W))
    val SB: BitPat = BitPat(1.U(width.W))
    val U: BitPat = BitPat(2.U(width.W))
    val UJ: BitPat = BitPat(3.U(width.W))
    val I: BitPat = BitPat(4.U(width.W))
    val Z: BitPat = BitPat(5.U(width.W))
  }

  object A2 {
    val width = 2
    val X: BitPat = BitPat.dontCare(width)
    val ZERO: BitPat = BitPat(0.U(width.W))
    val SIZE: BitPat = BitPat(1.U(width.W))
    val RS2: BitPat = BitPat(2.U(width.W))
    val IMM: BitPat = BitPat(3.U(width.W))
  }

  object DW {
    val width: Int = 1
    val X: BitPat = BitPat.dontCare(width)
    val N: BitPat = BitPat.N(width)
    val Y: BitPat = BitPat.Y(width)
    // todo: jiuyang thinks here is for X propagation but not for sure.
    val XPR: BitPat = Y
  }

  object MEM {
    val width: Int = 5
    val X: BitPat = BitPat.dontCare(width)
    /** int load */
    val XRD: BitPat = BitPat("b00000".U(width.W))
    /** int store */
    val XWR: BitPat = BitPat("b00001".U(width.W))
    /** prefetch with intent to read */
    val PFR: BitPat = BitPat("b00010".U(width.W))
    /** prefetch with intent to write */
    val PFW: BitPat = BitPat("b00011".U(width.W))
    /** flush all lines */
    val FLUSH_ALL: BitPat = BitPat("b00101".U(width.W))
    val XLR: BitPat = BitPat("b00110".U(width.W))
    val XSC: BitPat = BitPat("b00111".U(width.W))

    object XA {
      val SWAP: BitPat = BitPat("b00100".U(width.W))
      val ADD: BitPat = BitPat("b01000".U(width.W))
      val XOR: BitPat = BitPat("b01001".U(width.W))
      val OR: BitPat = BitPat("b01010".U(width.W))
      val AND: BitPat = BitPat("b01011".U(width.W))
      val MIN: BitPat = BitPat("b01100".U(width.W))
      val MAX: BitPat = BitPat("b01101".U(width.W))
      val MINU: BitPat = BitPat("b01110".U(width.W))
      val MAXU: BitPat = BitPat("b01111".U(width.W))
    }

    /** write back dirty data and cede R/W permissions */
    val FLUSH: BitPat = BitPat("b10000".U(width.W))
    /** partial (masked) store */
    val PWR: BitPat = BitPat("b10001".U(width.W))
    /** write back dirty data and cede W permissions */
    val PRODUCE: BitPat = BitPat("b10010".U(width.W))
    /** write back dirty data and retain R/W permissions */
    val CLEAN: BitPat = BitPat("b10011".U(width.W))
    /** SFENCE.VMA */
    val SFENCE: BitPat = BitPat("b10100".U(width.W))
    /** HFENCE.VVMA */
    val HFENCEV: BitPat = BitPat("b10101".U(width.W))
    /** HFENCE.GVMA */
    val HFENCEG: BitPat = BitPat("b10110".U(width.W))
    /** check write permissions but don't perform a write */
    val WOK: BitPat = BitPat("b10111".U(width.W))
    /** HLVX instruction */
    val HLVX: BitPat = BitPat("b10000".U(width.W))

    // todo: jiuyang thinks this is not suitable for decoder related logic,
    //       since it leaks of logic optimization by minimizer.
    //       in the future, it should be optimized.
    object helper {
      // todo: remove it and upstream the `isOneOf` function to chisel3.
      private implicit class UIntIsOneOf(private val x: UInt) extends AnyVal {
        def isOneOf(s: Seq[BitPat]): Bool = VecInit(s.map(x === _)).asUInt.orR

        def isOneOf(u1: BitPat, u2: BitPat*): Bool = isOneOf(u1 +: u2.toSeq)
      }

      def isAMOLogical(cmd: UInt): Bool = cmd.isOneOf(XA.SWAP, XA.XOR, XA.OR, XA.AND)

      def isAMOArithmetic(cmd: UInt): Bool = cmd.isOneOf(XA.ADD, XA.MIN, XA.MAX, XA.MINU, XA.MAXU)

      def isAMO(cmd: UInt): Bool = isAMOLogical(cmd) || isAMOArithmetic(cmd)

      def isPrefetch(cmd: UInt): Bool = cmd === PFR || cmd === PFW

      def isRead(cmd: UInt): Bool = cmd.isOneOf(XRD, HLVX, XLR, XSC) || isAMO(cmd)

      def isWrite(cmd: UInt): Bool = cmd.isOneOf(XWR, PWR, XSC) || isAMO(cmd)

      def isWriteIntent(cmd: UInt): Bool = isWrite(cmd) || cmd === PFW || cmd === XLR
    }
  }

  object ALU {
    val width: Int = 4
    val X: BitPat = BitPat.dontCare(width)
    val ADD: BitPat = BitPat(0.U(width.W))
    val SL: BitPat = BitPat(1.U(width.W))
    val SEQ: BitPat = BitPat(2.U(width.W))
    val SNE: BitPat = BitPat(3.U(width.W))
    val XOR: BitPat = BitPat(4.U(width.W))
    val SR: BitPat = BitPat(5.U(width.W))
    val OR: BitPat = BitPat(6.U(width.W))
    val AND: BitPat = BitPat(7.U(width.W))
    val SUB: BitPat = BitPat(10.U(width.W))
    val SRA: BitPat = BitPat(11.U(width.W))
    val SLT: BitPat = BitPat(12.U(width.W))
    val SGE: BitPat = BitPat(13.U(width.W))
    val SLTU: BitPat = BitPat(14.U(width.W))
    val SGEU: BitPat = BitPat(15.U(width.W))
    val DIV: BitPat = XOR
    val DIVU: BitPat = SR
    val REM: BitPat = OR
    val REMU: BitPat = AND
    val MUL: BitPat = ADD
    val MULH: BitPat = SL
    val MULHSU: BitPat = SEQ
    val MULHU: BitPat = SNE

    object helper {
      def isMulFN(fn: UInt, cmp: UInt): Bool = fn(1, 0) === cmp(1, 0)

      def isSub(cmd: UInt): Bool = cmd(3)

      def isCmp(cmd: UInt): Bool = cmd >= bitPatToUInt(SLT)

      def cmpUnsigned(cmd: UInt): Bool = cmd(1)

      def cmpInverted(cmd: UInt): Bool = cmd(0)

      def cmpEq(cmd: UInt): Bool = !cmd(3)
    }
  }

  object CSR {
    val width = 3
    val X: BitPat = BitPat.dontCare(width)
    val N: BitPat = BitPat(0.U(width.W))
    val R: BitPat = BitPat(2.U(width.W))
    val I: BitPat = BitPat(4.U(width.W))
    val W: BitPat = BitPat(5.U(width.W))
    val S: BitPat = BitPat(6.U(width.W))
    val C: BitPat = BitPat(7.U(width.W))

    val addressSize = 12
    def modeLSB: Int = 8

    def mode(addr: Int): Int = (addr >> modeLSB) % (1 << PRV.width)

    def mode(addr: UInt): UInt = addr(modeLSB + PRV.width - 1, modeLSB)

    def busErrorIntCause = 128

    def debugIntCause = 14 // keep in sync with MIP.debug

    def debugTriggerCause = {
      val res = debugIntCause
      require(!(Causes.all contains res))
      res
    }

    def rnmiIntCause = 13 // NMI: Higher numbers = higher priority, must not reuse debugIntCause

    def rnmiBEUCause = 12

    val firstCtr = CSRs.cycle
    val firstCtrH = CSRs.cycleh
    val firstHPC = CSRs.hpmcounter3
    val firstHPCH = CSRs.hpmcounter3h
    val firstHPE = CSRs.mhpmevent3
    val firstMHPC = CSRs.mhpmcounter3
    val firstMHPCH = CSRs.mhpmcounter3h
    val firstHPM = 3
    val nCtr = 32
    val nHPM = nCtr - firstHPM
    val hpmWidth = 40

    val maxPMPs = 16

    object helper {

      /** mask a CSR cmd with a valid bit */
      def maskCmd(valid: Bool, cmd: UInt): UInt = {
        // all commands less than CSR.I are treated by CSRFile as NOPs
        cmd & ~Mux(valid, 0.U, bitPatToUInt(CSR.I))
      }
    }
  }

  object PRV {
    val width = 2
    val U = BitPat(0.U(width.W))
    val S = BitPat(1.U(width.W))
    val H = BitPat(2.U(width.W))
    val M = BitPat(3.U(width.W))
  }

  object CFI {
    val width = 2
    val branch = 0.U(width.W)
    val jump = 1.U(width.W)
    val call = 2.U(width.W)
    val ret = 3.U(width.W)
  }

  object FP {
    val RoundingModeSize = 3
    val FlagsSize = 5
  }
}

