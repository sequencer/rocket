// See LICENSE.SiFive for license details.
// See LICENSE.Berkeley for license details.

package org.chipsalliance.rocket

import chisel3._
import chisel3.util.BitPat
import org.chipsalliance.cde.config.Parameters
import Operands._
import freechips.rocketchip.rocket.Instructions._
import freechips.rocketchip.rocket.CustomInstructions._
import freechips.rocketchip.rocket.ALU._
import freechips.rocketchip.rocket.{DecodeLogic, Instructions32}


trait DecodeConstants
{
  val table: Array[(BitPat, List[BitPat])]
}

class IntCtrlSigs extends Bundle {
  val legal = Bool()
  val fp = Bool()
  val rocc = Bool()
  val branch = Bool()
  val jal = Bool()
  val jalr = Bool()
  val rxs2 = Bool()
  val rxs1 = Bool()
  val scie = Bool()
  val sel_alu2 = UInt(A2.width.W)
  val sel_alu1 = UInt(A1.width.W)
  val sel_imm = UInt(IMM.width.W)
  val alu_dw = Bool()
  val alu_fn = UInt(ALU.width.W)
  val mem = Bool()
  val mem_cmd = UInt(MEM.width.W)
  val rfs1 = Bool()
  val rfs2 = Bool()
  val rfs3 = Bool()
  val wfd = Bool()
  val mul = Bool()
  val div = Bool()
  val wxd = Bool()
  val csr = UInt(CSR.width.W)
  val fence_i = Bool()
  val fence = Bool()
  val amo = Bool()
  val dp = Bool()

  def default: List[BitPat] =
                //           jal                                                             renf1               fence.i
                //   val     | jalr                                                          | renf2             |
                //   | fp_val| | renx2                                                       | | renf3           |
                //   | | rocc| | | renx1       s_alu1                          mem_val       | | | wfd           |
                //   | | | br| | | |   s_alu2  |       imm    dw     alu       | mem_cmd     | | | | mul         |
                //   | | | | | | | |   |       |       |      |      |         | |           | | | | | div       | fence
                //   | | | | | | | |   |       |       |      |      |         | |           | | | | | | wxd     | | amo
                //   | | | | | | | | scie      |       |      |      |         | |           | | | | | | |       | | | dp
                List(N,X,X,X,X,X,X,X,X,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        X,X,X,X,X,X,X,CSR.X,X,X,X,X)

  def decode(inst: UInt, table: Iterable[(BitPat, List[BitPat])]) = {
    val decoder = DecodeLogic(inst, default, table)
    val sigs = Seq(legal, fp, rocc, branch, jal, jalr, rxs2, rxs1, scie, sel_alu2,
                   sel_alu1, sel_imm, alu_dw, alu_fn, mem, mem_cmd,
                   rfs1, rfs2, rfs3, wfd, mul, div, wxd, csr, fence_i, fence, amo, dp)
    sigs zip decoder map {case(s,d) => s := d}
    this
  }
}

class IDecode(implicit val p: Parameters) extends Operands
{
  val table: Array[(BitPat, List[BitPat])] = Array(
    BNE->       List(Y,N,N,Y,N,N,Y,Y,N,A2.RS2, A1.RS1, IMM.SB,DW.X,  ALU.SNE,   N, MEM.X,        N,N,N,N,N,N,N,CSR.N,N,N,N,N),
    BEQ->       List(Y,N,N,Y,N,N,Y,Y,N,A2.RS2, A1.RS1, IMM.SB,DW.X,  ALU.SEQ,   N, MEM.X,        N,N,N,N,N,N,N,CSR.N,N,N,N,N),
    BLT->       List(Y,N,N,Y,N,N,Y,Y,N,A2.RS2, A1.RS1, IMM.SB,DW.X,  ALU.SLT,   N, MEM.X,        N,N,N,N,N,N,N,CSR.N,N,N,N,N),
    BLTU->      List(Y,N,N,Y,N,N,Y,Y,N,A2.RS2, A1.RS1, IMM.SB,DW.X,  ALU.SLTU,  N, MEM.X,        N,N,N,N,N,N,N,CSR.N,N,N,N,N),
    BGE->       List(Y,N,N,Y,N,N,Y,Y,N,A2.RS2, A1.RS1, IMM.SB,DW.X,  ALU.SGE,   N, MEM.X,        N,N,N,N,N,N,N,CSR.N,N,N,N,N),
    BGEU->      List(Y,N,N,Y,N,N,Y,Y,N,A2.RS2, A1.RS1, IMM.SB,DW.X,  ALU.SGEU,  N, MEM.X,        N,N,N,N,N,N,N,CSR.N,N,N,N,N),

    JAL->       List(Y,N,N,N,Y,N,N,N,N,A2.SIZE,A1.PC,  IMM.UJ,DW.XPR,ALU.ADD,   N, MEM.X,        N,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    JALR->      List(Y,N,N,N,N,Y,N,Y,N,A2.IMM, A1.RS1, IMM.I, DW.XPR,ALU.ADD,   N, MEM.X,        N,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    AUIPC->     List(Y,N,N,N,N,N,N,N,N,A2.IMM, A1.PC,  IMM.U, DW.XPR,ALU.ADD,   N, MEM.X,        N,N,N,N,N,N,Y,CSR.N,N,N,N,N),

    LB->        List(Y,N,N,N,N,N,N,Y,N,A2.IMM, A1.RS1, IMM.I, DW.XPR,ALU.ADD,   Y, MEM.XRD,      N,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    LH->        List(Y,N,N,N,N,N,N,Y,N,A2.IMM, A1.RS1, IMM.I, DW.XPR,ALU.ADD,   Y, MEM.XRD,      N,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    LW->        List(Y,N,N,N,N,N,N,Y,N,A2.IMM, A1.RS1, IMM.I, DW.XPR,ALU.ADD,   Y, MEM.XRD,      N,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    LBU->       List(Y,N,N,N,N,N,N,Y,N,A2.IMM, A1.RS1, IMM.I, DW.XPR,ALU.ADD,   Y, MEM.XRD,      N,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    LHU->       List(Y,N,N,N,N,N,N,Y,N,A2.IMM, A1.RS1, IMM.I, DW.XPR,ALU.ADD,   Y, MEM.XRD,      N,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    SB->        List(Y,N,N,N,N,N,Y,Y,N,A2.IMM, A1.RS1, IMM.S, DW.XPR,ALU.ADD,   Y, MEM.XWR,      N,N,N,N,N,N,N,CSR.N,N,N,N,N),
    SH->        List(Y,N,N,N,N,N,Y,Y,N,A2.IMM, A1.RS1, IMM.S, DW.XPR,ALU.ADD,   Y, MEM.XWR,      N,N,N,N,N,N,N,CSR.N,N,N,N,N),
    SW->        List(Y,N,N,N,N,N,Y,Y,N,A2.IMM, A1.RS1, IMM.S, DW.XPR,ALU.ADD,   Y, MEM.XWR,      N,N,N,N,N,N,N,CSR.N,N,N,N,N),

    LUI->       List(Y,N,N,N,N,N,N,N,N,A2.IMM, A1.ZERO,IMM.U, DW.XPR,ALU.ADD,   N, MEM.X,        N,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    ADDI->      List(Y,N,N,N,N,N,N,Y,N,A2.IMM, A1.RS1, IMM.I, DW.XPR,ALU.ADD,   N, MEM.X,        N,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    SLTI ->     List(Y,N,N,N,N,N,N,Y,N,A2.IMM, A1.RS1, IMM.I, DW.XPR,ALU.SLT,   N, MEM.X,        N,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    SLTIU->     List(Y,N,N,N,N,N,N,Y,N,A2.IMM, A1.RS1, IMM.I, DW.XPR,ALU.SLTU,  N, MEM.X,        N,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    ANDI->      List(Y,N,N,N,N,N,N,Y,N,A2.IMM, A1.RS1, IMM.I, DW.XPR,ALU.AND,   N, MEM.X,        N,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    ORI->       List(Y,N,N,N,N,N,N,Y,N,A2.IMM, A1.RS1, IMM.I, DW.XPR,ALU.OR,    N, MEM.X,        N,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    XORI->      List(Y,N,N,N,N,N,N,Y,N,A2.IMM, A1.RS1, IMM.I, DW.XPR,ALU.XOR,   N, MEM.X,        N,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    ADD->       List(Y,N,N,N,N,N,Y,Y,N,A2.RS2, A1.RS1, IMM.X, DW.XPR,ALU.ADD,   N, MEM.X,        N,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    SUB->       List(Y,N,N,N,N,N,Y,Y,N,A2.RS2, A1.RS1, IMM.X, DW.XPR,ALU.SUB,   N, MEM.X,        N,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    SLT->       List(Y,N,N,N,N,N,Y,Y,N,A2.RS2, A1.RS1, IMM.X, DW.XPR,ALU.SLT,   N, MEM.X,        N,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    SLTU->      List(Y,N,N,N,N,N,Y,Y,N,A2.RS2, A1.RS1, IMM.X, DW.XPR,ALU.SLTU,  N, MEM.X,        N,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    AND->       List(Y,N,N,N,N,N,Y,Y,N,A2.RS2, A1.RS1, IMM.X, DW.XPR,ALU.AND,   N, MEM.X,        N,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    OR->        List(Y,N,N,N,N,N,Y,Y,N,A2.RS2, A1.RS1, IMM.X, DW.XPR,ALU.OR,    N, MEM.X,        N,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    XOR->       List(Y,N,N,N,N,N,Y,Y,N,A2.RS2, A1.RS1, IMM.X, DW.XPR,ALU.XOR,   N, MEM.X,        N,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    SLL->       List(Y,N,N,N,N,N,Y,Y,N,A2.RS2, A1.RS1, IMM.X, DW.XPR,ALU.SL,    N, MEM.X,        N,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    SRL->       List(Y,N,N,N,N,N,Y,Y,N,A2.RS2, A1.RS1, IMM.X, DW.XPR,ALU.SR,    N, MEM.X,        N,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    SRA->       List(Y,N,N,N,N,N,Y,Y,N,A2.RS2, A1.RS1, IMM.X, DW.XPR,ALU.SRA,   N, MEM.X,        N,N,N,N,N,N,Y,CSR.N,N,N,N,N),

    FENCE->     List(Y,N,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        N,N,N,N,N,N,N,CSR.N,N,Y,N,N),

    ECALL->     List(Y,N,N,N,N,N,N,X,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        N,N,N,N,N,N,N,CSR.I,N,N,N,N),
    EBREAK->    List(Y,N,N,N,N,N,N,X,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        N,N,N,N,N,N,N,CSR.I,N,N,N,N),
    MRET->      List(Y,N,N,N,N,N,N,X,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        N,N,N,N,N,N,N,CSR.I,N,N,N,N),
    WFI->       List(Y,N,N,N,N,N,N,X,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        N,N,N,N,N,N,N,CSR.I,N,N,N,N),
    CEASE->     List(Y,N,N,N,N,N,N,X,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        N,N,N,N,N,N,N,CSR.I,N,N,N,N),
    CSRRW->     List(Y,N,N,N,N,N,N,Y,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   N, MEM.X,        N,N,N,N,N,N,Y,CSR.W,N,N,N,N),
    CSRRS->     List(Y,N,N,N,N,N,N,Y,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   N, MEM.X,        N,N,N,N,N,N,Y,CSR.S,N,N,N,N),
    CSRRC->     List(Y,N,N,N,N,N,N,Y,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   N, MEM.X,        N,N,N,N,N,N,Y,CSR.C,N,N,N,N),
    CSRRWI->    List(Y,N,N,N,N,N,N,N,N,A2.IMM, A1.ZERO,IMM.Z, DW.XPR,ALU.ADD,   N, MEM.X,        N,N,N,N,N,N,Y,CSR.W,N,N,N,N),
    CSRRSI->    List(Y,N,N,N,N,N,N,N,N,A2.IMM, A1.ZERO,IMM.Z, DW.XPR,ALU.ADD,   N, MEM.X,        N,N,N,N,N,N,Y,CSR.S,N,N,N,N),
    CSRRCI->    List(Y,N,N,N,N,N,N,N,N,A2.IMM, A1.ZERO,IMM.Z, DW.XPR,ALU.ADD,   N, MEM.X,        N,N,N,N,N,N,Y,CSR.C,N,N,N,N))
}

class FenceIDecode(flushDCache: Boolean)(implicit val p: Parameters) extends Operands
{
  private val (v, cmd) = if (flushDCache) (Y, MEM.FLUSH_ALL) else (N,  MEM.X)

  val table: Array[(BitPat, List[BitPat])] = Array(
    FENCE_I->   List(Y,N,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     v,cmd,        N,N,N,N,N,N,N,CSR.N,Y,Y,N,N))
}

class CFlushDecode(supportsFlushLine: Boolean)(implicit val p: Parameters) extends Operands
{
  private def zapRs1(x: BitPat) = if (supportsFlushLine) x else BitPat(x.value.U)

  val table: Array[(BitPat, List[BitPat])] = Array(
    zapRs1(CFLUSH_D_L1)->
                List(Y,N,N,N,N,N,N,Y,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   Y, MEM.FLUSH_ALL,N,N,N,N,N,N,N,CSR.I,N,N,N,N),
    zapRs1(CDISCARD_D_L1)->
                List(Y,N,N,N,N,N,N,Y,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   Y, MEM.FLUSH_ALL,N,N,N,N,N,N,N,CSR.I,N,N,N,N))
}

class SVMDecode(implicit val p: Parameters) extends Operands
{
  val table: Array[(BitPat, List[BitPat])] = Array(
    SFENCE_VMA->List(Y,N,N,N,N,N,Y,Y,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   Y, MEM.SFENCE,   N,N,N,N,N,N,N,CSR.I,N,N,N,N))
}

class SDecode(implicit val p: Parameters) extends Operands
{
  val table: Array[(BitPat, List[BitPat])] = Array(
    SRET->      List(Y,N,N,N,N,N,N,X,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        N,N,N,N,N,N,N,CSR.I,N,N,N,N))
}

class HypervisorDecode(implicit val p: Parameters) extends Operands
{
  val table: Array[(BitPat, List[BitPat])] = Array(

    HFENCE_VVMA->List(Y,N,N,N,N,N,Y,Y,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR, ALU.ADD, Y, MEM.HFENCEV,  N,N,N,N,N,N,N,CSR.I,N,N,N,N),
    HFENCE_GVMA->List(Y,N,N,N,N,N,Y,Y,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR, ALU.ADD, Y, MEM.HFENCEG,  N,N,N,N,N,N,N,CSR.I,N,N,N,N),

    HLV_B ->    List(Y,N,N,N,N,N,N,Y,N,A2.ZERO, A1.RS1, IMM.X, DW.XPR, ALU.ADD, Y, MEM.XRD,      N,N,N,N,N,N,Y,CSR.I,N,N,N,N),
    HLV_BU->    List(Y,N,N,N,N,N,N,Y,N,A2.ZERO, A1.RS1, IMM.X, DW.XPR, ALU.ADD, Y, MEM.XRD,      N,N,N,N,N,N,Y,CSR.I,N,N,N,N),
    HLV_H ->    List(Y,N,N,N,N,N,N,Y,N,A2.ZERO, A1.RS1, IMM.X, DW.XPR, ALU.ADD, Y, MEM.XRD,      N,N,N,N,N,N,Y,CSR.I,N,N,N,N),
    HLV_HU->    List(Y,N,N,N,N,N,N,Y,N,A2.ZERO, A1.RS1, IMM.X, DW.XPR, ALU.ADD, Y, MEM.XRD,      N,N,N,N,N,N,Y,CSR.I,N,N,N,N),
    HLVX_HU->   List(Y,N,N,N,N,N,N,Y,N,A2.ZERO, A1.RS1, IMM.X, DW.XPR, ALU.ADD, Y, MEM.HLVX,     N,N,N,N,N,N,Y,CSR.I,N,N,N,N),
    HLV_W->     List(Y,N,N,N,N,N,N,Y,N,A2.ZERO, A1.RS1, IMM.X, DW.XPR, ALU.ADD, Y, MEM.XRD,      N,N,N,N,N,N,Y,CSR.I,N,N,N,N),
    HLVX_WU->   List(Y,N,N,N,N,N,N,Y,N,A2.ZERO, A1.RS1, IMM.X, DW.XPR, ALU.ADD, Y, MEM.HLVX,     N,N,N,N,N,N,Y,CSR.I,N,N,N,N),

    HSV_B->     List(Y,N,N,N,N,N,Y,Y,N,A2.ZERO, A1.RS1, IMM.I, DW.XPR, ALU.ADD, Y, MEM.XWR,      N,N,N,N,N,N,N,CSR.I,N,N,N,N),
    HSV_H->     List(Y,N,N,N,N,N,Y,Y,N,A2.ZERO, A1.RS1, IMM.I, DW.XPR, ALU.ADD, Y, MEM.XWR,      N,N,N,N,N,N,N,CSR.I,N,N,N,N),
    HSV_W->     List(Y,N,N,N,N,N,Y,Y,N,A2.ZERO, A1.RS1, IMM.I, DW.XPR, ALU.ADD, Y, MEM.XWR,      N,N,N,N,N,N,N,CSR.I,N,N,N,N))
}

class DebugDecode(implicit val p: Parameters) extends Operands
{
  val table: Array[(BitPat, List[BitPat])] = Array(
    DRET->      List(Y,N,N,N,N,N,N,X,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        N,N,N,N,N,N,N,CSR.I,N,N,N,N))
}

class NMIDecode(implicit val p: Parameters) extends Operands
{
  val table: Array[(BitPat, List[BitPat])] = Array(
    MNRET->     List(Y,N,N,N,N,N,N,X,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        N,N,N,N,N,N,N,CSR.I,N,N,N,N))
}

class I32Decode(implicit val p: Parameters) extends Operands
{
  val table: Array[(BitPat, List[BitPat])] = Array(
    Instructions32.SLLI->
                List(Y,N,N,N,N,N,N,Y,N,A2.IMM, A1.RS1, IMM.I, DW.XPR,ALU.SL,    N, MEM.X,        N,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    Instructions32.SRLI->
                List(Y,N,N,N,N,N,N,Y,N,A2.IMM, A1.RS1, IMM.I, DW.XPR,ALU.SR,    N, MEM.X,        N,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    Instructions32.SRAI->
                List(Y,N,N,N,N,N,N,Y,N,A2.IMM, A1.RS1, IMM.I, DW.XPR,ALU.SRA,   N, MEM.X,        N,N,N,N,N,N,Y,CSR.N,N,N,N,N))
}

class I64Decode(implicit val p: Parameters) extends Operands
{
  val table: Array[(BitPat, List[BitPat])] = Array(
    LD->        List(Y,N,N,N,N,N,N,Y,N,A2.IMM, A1.RS1, IMM.I, DW.XPR,ALU.ADD,   Y, MEM.XRD,      N,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    LWU->       List(Y,N,N,N,N,N,N,Y,N,A2.IMM, A1.RS1, IMM.I, DW.XPR,ALU.ADD,   Y, MEM.XRD,      N,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    SD->        List(Y,N,N,N,N,N,Y,Y,N,A2.IMM, A1.RS1, IMM.S, DW.XPR,ALU.ADD,   Y, MEM.XWR,      N,N,N,N,N,N,N,CSR.N,N,N,N,N),

    SLLI->      List(Y,N,N,N,N,N,N,Y,N,A2.IMM, A1.RS1, IMM.I, DW.XPR,ALU.SL,    N, MEM.X,        N,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    SRLI->      List(Y,N,N,N,N,N,N,Y,N,A2.IMM, A1.RS1, IMM.I, DW.XPR,ALU.SR,    N, MEM.X,        N,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    SRAI->      List(Y,N,N,N,N,N,N,Y,N,A2.IMM, A1.RS1, IMM.I, DW.XPR,ALU.SRA,   N, MEM.X,        N,N,N,N,N,N,Y,CSR.N,N,N,N,N),

    ADDIW->     List(Y,N,N,N,N,N,N,Y,N,A2.IMM, A1.RS1, IMM.I, DW.N,ALU.ADD,    N, MEM.X,        N,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    SLLIW->     List(Y,N,N,N,N,N,N,Y,N,A2.IMM, A1.RS1, IMM.I, DW.N,ALU.SL,     N, MEM.X,        N,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    SRLIW->     List(Y,N,N,N,N,N,N,Y,N,A2.IMM, A1.RS1, IMM.I, DW.N,ALU.SR,     N, MEM.X,        N,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    SRAIW->     List(Y,N,N,N,N,N,N,Y,N,A2.IMM, A1.RS1, IMM.I, DW.N,ALU.SRA,    N, MEM.X,        N,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    ADDW->      List(Y,N,N,N,N,N,Y,Y,N,A2.RS2, A1.RS1, IMM.X, DW.N,ALU.ADD,    N, MEM.X,        N,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    SUBW->      List(Y,N,N,N,N,N,Y,Y,N,A2.RS2, A1.RS1, IMM.X, DW.N,ALU.SUB,    N, MEM.X,        N,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    SLLW->      List(Y,N,N,N,N,N,Y,Y,N,A2.RS2, A1.RS1, IMM.X, DW.N,ALU.SL,     N, MEM.X,        N,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    SRLW->      List(Y,N,N,N,N,N,Y,Y,N,A2.RS2, A1.RS1, IMM.X, DW.N,ALU.SR,     N, MEM.X,        N,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    SRAW->      List(Y,N,N,N,N,N,Y,Y,N,A2.RS2, A1.RS1, IMM.X, DW.N,ALU.SRA,    N, MEM.X,        N,N,N,N,N,N,Y,CSR.N,N,N,N,N))
}

class Hypervisor64Decode(implicit val p: Parameters) extends Operands
{
  val table: Array[(BitPat, List[BitPat])] = Array(
    HLV_D->     List(Y,N,N,N,N,N,N,Y,N,A2.ZERO, A1.RS1, IMM.X, DW.XPR, ALU.ADD, Y, MEM.XRD,      N,N,N,N,N,N,Y,CSR.I,N,N,N,N),
    HSV_D->     List(Y,N,N,N,N,N,Y,Y,N,A2.ZERO, A1.RS1, IMM.I, DW.XPR, ALU.ADD, Y, MEM.XWR,      N,N,N,N,N,N,N,CSR.I,N,N,N,N),
    HLV_WU->    List(Y,N,N,N,N,N,N,Y,N,A2.ZERO, A1.RS1, IMM.X, DW.XPR, ALU.ADD, Y, MEM.XRD,      N,N,N,N,N,N,Y,CSR.I,N,N,N,N))
}

class MDecode(pipelinedMul: Boolean)(implicit val p: Parameters) extends Operands
{
  val M = if (pipelinedMul) Y else N
  val D = if (pipelinedMul) N else Y
  val table: Array[(BitPat, List[BitPat])] = Array(
    MUL->       List(Y,N,N,N,N,N,Y,Y,N,A2.RS2, A1.RS1, IMM.X, DW.XPR,ALU.MUL,   N, MEM.X,        N,N,N,N,M,D,Y,CSR.N,N,N,N,N),
    MULH->      List(Y,N,N,N,N,N,Y,Y,N,A2.RS2, A1.RS1, IMM.X, DW.XPR,ALU.MULH,  N, MEM.X,        N,N,N,N,M,D,Y,CSR.N,N,N,N,N),
    MULHU->     List(Y,N,N,N,N,N,Y,Y,N,A2.RS2, A1.RS1, IMM.X, DW.XPR,ALU.MULHU, N, MEM.X,        N,N,N,N,M,D,Y,CSR.N,N,N,N,N),
    MULHSU->    List(Y,N,N,N,N,N,Y,Y,N,A2.RS2, A1.RS1, IMM.X, DW.XPR,ALU.MULHSU,N, MEM.X,        N,N,N,N,M,D,Y,CSR.N,N,N,N,N),

    DIV->       List(Y,N,N,N,N,N,Y,Y,N,A2.RS2, A1.RS1, IMM.X, DW.XPR,ALU.DIV,   N, MEM.X,        N,N,N,N,N,Y,Y,CSR.N,N,N,N,N),
    DIVU->      List(Y,N,N,N,N,N,Y,Y,N,A2.RS2, A1.RS1, IMM.X, DW.XPR,ALU.DIVU,  N, MEM.X,        N,N,N,N,N,Y,Y,CSR.N,N,N,N,N),
    REM->       List(Y,N,N,N,N,N,Y,Y,N,A2.RS2, A1.RS1, IMM.X, DW.XPR,ALU.REM,   N, MEM.X,        N,N,N,N,N,Y,Y,CSR.N,N,N,N,N),
    REMU->      List(Y,N,N,N,N,N,Y,Y,N,A2.RS2, A1.RS1, IMM.X, DW.XPR,ALU.REMU,  N, MEM.X,        N,N,N,N,N,Y,Y,CSR.N,N,N,N,N))
}

class M64Decode(pipelinedMul: Boolean)(implicit val p: Parameters) extends Operands
{
  val M = if (pipelinedMul) Y else N
  val D = if (pipelinedMul) N else Y
  val table: Array[(BitPat, List[BitPat])] = Array(
    MULW->      List(Y,N,N,N,N,N,Y,Y,N,A2.RS2, A1.RS1, IMM.X, DW.N, ALU.MUL,   N, MEM.X,        N,N,N,N,M,D,Y,CSR.N,N,N,N,N),

    DIVW->      List(Y,N,N,N,N,N,Y,Y,N,A2.RS2, A1.RS1, IMM.X, DW.N, ALU.DIV,   N, MEM.X,        N,N,N,N,N,Y,Y,CSR.N,N,N,N,N),
    DIVUW->     List(Y,N,N,N,N,N,Y,Y,N,A2.RS2, A1.RS1, IMM.X, DW.N, ALU.DIVU,  N, MEM.X,        N,N,N,N,N,Y,Y,CSR.N,N,N,N,N),
    REMW->      List(Y,N,N,N,N,N,Y,Y,N,A2.RS2, A1.RS1, IMM.X, DW.N, ALU.REM,   N, MEM.X,        N,N,N,N,N,Y,Y,CSR.N,N,N,N,N),
    REMUW->     List(Y,N,N,N,N,N,Y,Y,N,A2.RS2, A1.RS1, IMM.X, DW.N, ALU.REMU,  N, MEM.X,        N,N,N,N,N,Y,Y,CSR.N,N,N,N,N))
}

class ADecode(implicit val p: Parameters) extends Operands
{
  val table: Array[(BitPat, List[BitPat])] = Array(
    AMOADD_W->  List(Y,N,N,N,N,N,Y,Y,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   Y, MEM.XA.ADD,   N,N,N,N,N,N,Y,CSR.N,N,N,Y,N),
    AMOXOR_W->  List(Y,N,N,N,N,N,Y,Y,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   Y, MEM.XA.XOR,   N,N,N,N,N,N,Y,CSR.N,N,N,Y,N),
    AMOSWAP_W-> List(Y,N,N,N,N,N,Y,Y,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   Y, MEM.XA.SWAP,  N,N,N,N,N,N,Y,CSR.N,N,N,Y,N),
    AMOAND_W->  List(Y,N,N,N,N,N,Y,Y,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   Y, MEM.XA.AND,   N,N,N,N,N,N,Y,CSR.N,N,N,Y,N),
    AMOOR_W->   List(Y,N,N,N,N,N,Y,Y,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   Y, MEM.XA.OR,    N,N,N,N,N,N,Y,CSR.N,N,N,Y,N),
    AMOMIN_W->  List(Y,N,N,N,N,N,Y,Y,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   Y, MEM.XA.MIN,   N,N,N,N,N,N,Y,CSR.N,N,N,Y,N),
    AMOMINU_W-> List(Y,N,N,N,N,N,Y,Y,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   Y, MEM.XA.MINU,  N,N,N,N,N,N,Y,CSR.N,N,N,Y,N),
    AMOMAX_W->  List(Y,N,N,N,N,N,Y,Y,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   Y, MEM.XA.MAX,   N,N,N,N,N,N,Y,CSR.N,N,N,Y,N),
    AMOMAXU_W-> List(Y,N,N,N,N,N,Y,Y,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   Y, MEM.XA.MAXU,  N,N,N,N,N,N,Y,CSR.N,N,N,Y,N),

    LR_W->      List(Y,N,N,N,N,N,Y,Y,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   Y, MEM.XLR,      N,N,N,N,N,N,Y,CSR.N,N,N,Y,N),
    SC_W->      List(Y,N,N,N,N,N,Y,Y,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   Y, MEM.XSC,      N,N,N,N,N,N,Y,CSR.N,N,N,Y,N))
}

class A64Decode(implicit val p: Parameters) extends Operands
{
  val table: Array[(BitPat, List[BitPat])] = Array(
    AMOADD_D->  List(Y,N,N,N,N,N,Y,Y,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   Y, MEM.XA.ADD,   N,N,N,N,N,N,Y,CSR.N,N,N,Y,N),
    AMOSWAP_D-> List(Y,N,N,N,N,N,Y,Y,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   Y, MEM.XA.SWAP,  N,N,N,N,N,N,Y,CSR.N,N,N,Y,N),
    AMOXOR_D->  List(Y,N,N,N,N,N,Y,Y,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   Y, MEM.XA.XOR,   N,N,N,N,N,N,Y,CSR.N,N,N,Y,N),
    AMOAND_D->  List(Y,N,N,N,N,N,Y,Y,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   Y, MEM.XA.AND,   N,N,N,N,N,N,Y,CSR.N,N,N,Y,N),
    AMOOR_D->   List(Y,N,N,N,N,N,Y,Y,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   Y, MEM.XA.OR,    N,N,N,N,N,N,Y,CSR.N,N,N,Y,N),
    AMOMIN_D->  List(Y,N,N,N,N,N,Y,Y,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   Y, MEM.XA.MIN,   N,N,N,N,N,N,Y,CSR.N,N,N,Y,N),
    AMOMINU_D-> List(Y,N,N,N,N,N,Y,Y,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   Y, MEM.XA.MINU,  N,N,N,N,N,N,Y,CSR.N,N,N,Y,N),
    AMOMAX_D->  List(Y,N,N,N,N,N,Y,Y,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   Y, MEM.XA.MAX,   N,N,N,N,N,N,Y,CSR.N,N,N,Y,N),
    AMOMAXU_D-> List(Y,N,N,N,N,N,Y,Y,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   Y, MEM.XA.MAXU,  N,N,N,N,N,N,Y,CSR.N,N,N,Y,N),

    LR_D->      List(Y,N,N,N,N,N,Y,Y,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   Y, MEM.XLR,      N,N,N,N,N,N,Y,CSR.N,N,N,Y,N),
    SC_D->      List(Y,N,N,N,N,N,Y,Y,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   Y, MEM.XSC,      N,N,N,N,N,N,Y,CSR.N,N,N,Y,N))
}

class HDecode(implicit val p: Parameters) extends Operands
{
  val table: Array[(BitPat, List[BitPat])] = Array(
    FCVT_S_H->  List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,N,N,Y,N,N,N,CSR.N,N,N,N,N),
    FCVT_H_S->  List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,N,N,Y,N,N,N,CSR.N,N,N,N,N),
    FSGNJ_H->   List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,N,Y,N,N,N,CSR.N,N,N,N,N),
    FSGNJX_H->  List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,N,Y,N,N,N,CSR.N,N,N,N,N),
    FSGNJN_H->  List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,N,Y,N,N,N,CSR.N,N,N,N,N),
    FMIN_H->    List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,N,Y,N,N,N,CSR.N,N,N,N,N),
    FMAX_H->    List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,N,Y,N,N,N,CSR.N,N,N,N,N),
    FADD_H->    List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,N,Y,N,N,N,CSR.N,N,N,N,N),
    FSUB_H->    List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,N,Y,N,N,N,CSR.N,N,N,N,N),
    FMUL_H->    List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,N,Y,N,N,N,CSR.N,N,N,N,N),
    FMADD_H->   List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,Y,Y,N,N,N,CSR.N,N,N,N,N),
    FMSUB_H->   List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,Y,Y,N,N,N,CSR.N,N,N,N,N),
    FNMADD_H->  List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,Y,Y,N,N,N,CSR.N,N,N,N,N),
    FNMSUB_H->  List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,Y,Y,N,N,N,CSR.N,N,N,N,N),
    FCLASS_H->  List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    FMV_X_H->   List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    FCVT_W_H->  List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    FCVT_WU_H-> List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    FEQ_H->     List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,N,N,N,N,Y,CSR.N,N,N,N,N),
    FLT_H->     List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,N,N,N,N,Y,CSR.N,N,N,N,N),
    FLE_H->     List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,N,N,N,N,Y,CSR.N,N,N,N,N),
    FMV_H_X->   List(Y,Y,N,N,N,N,N,Y,N,A2.X,   A1.RS1, IMM.X, DW.X,  ALU.X,     N, MEM.X,        N,N,N,Y,N,N,N,CSR.N,N,N,N,N),
    FCVT_H_W->  List(Y,Y,N,N,N,N,N,Y,N,A2.X,   A1.RS1, IMM.X, DW.X,  ALU.X,     N, MEM.X,        N,N,N,Y,N,N,N,CSR.N,N,N,N,N),
    FCVT_H_WU-> List(Y,Y,N,N,N,N,N,Y,N,A2.X,   A1.RS1, IMM.X, DW.X,  ALU.X,     N, MEM.X,        N,N,N,Y,N,N,N,CSR.N,N,N,N,N),
    FLH->       List(Y,Y,N,N,N,N,N,Y,N,A2.IMM, A1.RS1, IMM.I, DW.XPR,ALU.ADD,   Y, MEM.XRD,      N,N,N,Y,N,N,N,CSR.N,N,N,N,N),
    FSH->       List(Y,Y,N,N,N,N,N,Y,N,A2.IMM, A1.RS1, IMM.S, DW.XPR,ALU.ADD,   Y, MEM.XWR,      N,Y,N,N,N,N,N,CSR.N,N,N,N,N),
    FDIV_H->    List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,N,Y,N,N,N,CSR.N,N,N,N,N),
    FSQRT_H->   List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,N,Y,N,N,N,CSR.N,N,N,N,N))
}

class FDecode(implicit val p: Parameters) extends Operands
{
  val table: Array[(BitPat, List[BitPat])] = Array(
    FSGNJ_S->   List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,N,Y,N,N,N,CSR.N,N,N,N,N),
    FSGNJX_S->  List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,N,Y,N,N,N,CSR.N,N,N,N,N),
    FSGNJN_S->  List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,N,Y,N,N,N,CSR.N,N,N,N,N),
    FMIN_S->    List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,N,Y,N,N,N,CSR.N,N,N,N,N),
    FMAX_S->    List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,N,Y,N,N,N,CSR.N,N,N,N,N),
    FADD_S->    List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,N,Y,N,N,N,CSR.N,N,N,N,N),
    FSUB_S->    List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,N,Y,N,N,N,CSR.N,N,N,N,N),
    FMUL_S->    List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,N,Y,N,N,N,CSR.N,N,N,N,N),
    FMADD_S->   List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,Y,Y,N,N,N,CSR.N,N,N,N,N),
    FMSUB_S->   List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,Y,Y,N,N,N,CSR.N,N,N,N,N),
    FNMADD_S->  List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,Y,Y,N,N,N,CSR.N,N,N,N,N),
    FNMSUB_S->  List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,Y,Y,N,N,N,CSR.N,N,N,N,N),
    FCLASS_S->  List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    FMV_X_W->   List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    FCVT_W_S->  List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    FCVT_WU_S-> List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    FEQ_S->     List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,N,N,N,N,Y,CSR.N,N,N,N,N),
    FLT_S->     List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,N,N,N,N,Y,CSR.N,N,N,N,N),
    FLE_S->     List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,N,N,N,N,Y,CSR.N,N,N,N,N),
    FMV_W_X->   List(Y,Y,N,N,N,N,N,Y,N,A2.X,   A1.RS1, IMM.X, DW.X,  ALU.X,     N, MEM.X,        N,N,N,Y,N,N,N,CSR.N,N,N,N,N),
    FCVT_S_W->  List(Y,Y,N,N,N,N,N,Y,N,A2.X,   A1.RS1, IMM.X, DW.X,  ALU.X,     N, MEM.X,        N,N,N,Y,N,N,N,CSR.N,N,N,N,N),
    FCVT_S_WU-> List(Y,Y,N,N,N,N,N,Y,N,A2.X,   A1.RS1, IMM.X, DW.X,  ALU.X,     N, MEM.X,        N,N,N,Y,N,N,N,CSR.N,N,N,N,N),
    FLW->       List(Y,Y,N,N,N,N,N,Y,N,A2.IMM, A1.RS1, IMM.I, DW.XPR,ALU.ADD,   Y, MEM.XRD,      N,N,N,Y,N,N,N,CSR.N,N,N,N,N),
    FSW->       List(Y,Y,N,N,N,N,N,Y,N,A2.IMM, A1.RS1, IMM.S, DW.XPR,ALU.ADD,   Y, MEM.XWR,      N,Y,N,N,N,N,N,CSR.N,N,N,N,N),
    FDIV_S->    List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,N,Y,N,N,N,CSR.N,N,N,N,N),
    FSQRT_S->   List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,N,Y,N,N,N,CSR.N,N,N,N,N))
}

class DDecode(implicit val p: Parameters) extends Operands
{
  val table: Array[(BitPat, List[BitPat])] = Array(
    FCVT_S_D->  List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,N,N,Y,N,N,N,CSR.N,N,N,N,Y),
    FCVT_D_S->  List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,N,N,Y,N,N,N,CSR.N,N,N,N,Y),
    FSGNJ_D->   List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,N,Y,N,N,N,CSR.N,N,N,N,Y),
    FSGNJX_D->  List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,N,Y,N,N,N,CSR.N,N,N,N,Y),
    FSGNJN_D->  List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,N,Y,N,N,N,CSR.N,N,N,N,Y),
    FMIN_D->    List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,N,Y,N,N,N,CSR.N,N,N,N,Y),
    FMAX_D->    List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,N,Y,N,N,N,CSR.N,N,N,N,Y),
    FADD_D->    List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,N,Y,N,N,N,CSR.N,N,N,N,Y),
    FSUB_D->    List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,N,Y,N,N,N,CSR.N,N,N,N,Y),
    FMUL_D->    List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,N,Y,N,N,N,CSR.N,N,N,N,Y),
    FMADD_D->   List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,Y,Y,N,N,N,CSR.N,N,N,N,Y),
    FMSUB_D->   List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,Y,Y,N,N,N,CSR.N,N,N,N,Y),
    FNMADD_D->  List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,Y,Y,N,N,N,CSR.N,N,N,N,Y),
    FNMSUB_D->  List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,Y,Y,N,N,N,CSR.N,N,N,N,Y),
    FCLASS_D->  List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,N,N,N,N,N,Y,CSR.N,N,N,N,Y),
    FCVT_W_D->  List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,N,N,N,N,N,Y,CSR.N,N,N,N,Y),
    FCVT_WU_D-> List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,N,N,N,N,N,Y,CSR.N,N,N,N,Y),
    FEQ_D->     List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,N,N,N,N,Y,CSR.N,N,N,N,Y),
    FLT_D->     List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,N,N,N,N,Y,CSR.N,N,N,N,Y),
    FLE_D->     List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,N,N,N,N,Y,CSR.N,N,N,N,Y),
    FCVT_D_W->  List(Y,Y,N,N,N,N,N,Y,N,A2.X,   A1.RS1, IMM.X, DW.X,  ALU.X,     N, MEM.X,        N,N,N,Y,N,N,N,CSR.N,N,N,N,Y),
    FCVT_D_WU-> List(Y,Y,N,N,N,N,N,Y,N,A2.X,   A1.RS1, IMM.X, DW.X,  ALU.X,     N, MEM.X,        N,N,N,Y,N,N,N,CSR.N,N,N,N,Y),
    FLD->       List(Y,Y,N,N,N,N,N,Y,N,A2.IMM, A1.RS1, IMM.I, DW.XPR,ALU.ADD,   Y, MEM.XRD,      N,N,N,Y,N,N,N,CSR.N,N,N,N,Y),
    FSD->       List(Y,Y,N,N,N,N,N,Y,N,A2.IMM, A1.RS1, IMM.S, DW.XPR,ALU.ADD,   Y, MEM.XWR,      N,Y,N,N,N,N,N,CSR.N,N,N,N,Y),
    FDIV_D->    List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,N,Y,N,N,N,CSR.N,N,N,N,Y),
    FSQRT_D->   List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,Y,N,Y,N,N,N,CSR.N,N,N,N,Y))
}

class HDDecode(implicit val p: Parameters) extends Operands
{
  val table: Array[(BitPat, List[BitPat])] = Array(
    FCVT_D_H->  List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,N,N,Y,N,N,N,CSR.N,N,N,N,Y),
    FCVT_H_D->  List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,N,N,Y,N,N,N,CSR.N,N,N,N,Y))
}

class H64Decode(implicit val p: Parameters) extends Operands
{
  val table: Array[(BitPat, List[BitPat])] = Array(
    FCVT_L_H->  List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    FCVT_LU_H-> List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    FCVT_H_L->  List(Y,Y,N,N,N,N,N,Y,N,A2.X,   A1.RS1, IMM.X, DW.X,  ALU.X,     N, MEM.X,        N,N,N,Y,N,N,N,CSR.N,N,N,N,N),
    FCVT_H_LU-> List(Y,Y,N,N,N,N,N,Y,N,A2.X,   A1.RS1, IMM.X, DW.X,  ALU.X,     N, MEM.X,        N,N,N,Y,N,N,N,CSR.N,N,N,N,N))
}

class F64Decode(implicit val p: Parameters) extends Operands
{
  val table: Array[(BitPat, List[BitPat])] = Array(
    FCVT_L_S->  List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    FCVT_LU_S-> List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    FCVT_S_L->  List(Y,Y,N,N,N,N,N,Y,N,A2.X,   A1.RS1, IMM.X, DW.X,  ALU.X,     N, MEM.X,        N,N,N,Y,N,N,N,CSR.N,N,N,N,N),
    FCVT_S_LU-> List(Y,Y,N,N,N,N,N,Y,N,A2.X,   A1.RS1, IMM.X, DW.X,  ALU.X,     N, MEM.X,        N,N,N,Y,N,N,N,CSR.N,N,N,N,N))
}

class D64Decode(implicit val p: Parameters) extends Operands
{
  val table: Array[(BitPat, List[BitPat])] = Array(
    FMV_X_D->   List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,N,N,N,N,N,Y,CSR.N,N,N,N,Y),
    FCVT_L_D->  List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,N,N,N,N,N,Y,CSR.N,N,N,N,Y),
    FCVT_LU_D-> List(Y,Y,N,N,N,N,N,N,N,A2.X,   A1.X,   IMM.X, DW.X,  ALU.X,     N, MEM.X,        Y,N,N,N,N,N,Y,CSR.N,N,N,N,Y),
    FMV_D_X->   List(Y,Y,N,N,N,N,N,Y,N,A2.X,   A1.RS1, IMM.X, DW.X,  ALU.X,     N, MEM.X,        N,N,N,Y,N,N,N,CSR.N,N,N,N,Y),
    FCVT_D_L->  List(Y,Y,N,N,N,N,N,Y,N,A2.X,   A1.RS1, IMM.X, DW.X,  ALU.X,     N, MEM.X,        N,N,N,Y,N,N,N,CSR.N,N,N,N,Y),
    FCVT_D_LU-> List(Y,Y,N,N,N,N,N,Y,N,A2.X,   A1.RS1, IMM.X, DW.X,  ALU.X,     N, MEM.X,        N,N,N,Y,N,N,N,CSR.N,N,N,N,Y))
}

class SCIEDecode(implicit val p: Parameters) extends Operands
{
  val table: Array[(BitPat, List[BitPat])] = Array(
    SCIE.opcode->       List(Y,N,N,N,N,N,Y,Y,Y,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.X,     N, MEM.X,        N,N,N,N,N,N,Y,CSR.N,N,N,N,N))
}

class RoCCDecode(implicit val p: Parameters) extends Operands
{
  val table: Array[(BitPat, List[BitPat])] = Array(
    CUSTOM0->           List(Y,N,Y,N,N,N,N,N,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   N, MEM.X,        N,N,N,N,N,N,N,CSR.N,N,N,N,N),
    CUSTOM0_RS1->       List(Y,N,Y,N,N,N,N,Y,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   N, MEM.X,        N,N,N,N,N,N,N,CSR.N,N,N,N,N),
    CUSTOM0_RS1_RS2->   List(Y,N,Y,N,N,N,Y,Y,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   N, MEM.X,        N,N,N,N,N,N,N,CSR.N,N,N,N,N),
    CUSTOM0_RD->        List(Y,N,Y,N,N,N,N,N,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   N, MEM.X,        N,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    CUSTOM0_RD_RS1->    List(Y,N,Y,N,N,N,N,Y,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   N, MEM.X,        N,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    CUSTOM0_RD_RS1_RS2->List(Y,N,Y,N,N,N,Y,Y,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   N, MEM.X,        N,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    CUSTOM1->           List(Y,N,Y,N,N,N,N,N,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   N, MEM.X,        N,N,N,N,N,N,N,CSR.N,N,N,N,N),
    CUSTOM1_RS1->       List(Y,N,Y,N,N,N,N,Y,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   N, MEM.X,        N,N,N,N,N,N,N,CSR.N,N,N,N,N),
    CUSTOM1_RS1_RS2->   List(Y,N,Y,N,N,N,Y,Y,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   N, MEM.X,        N,N,N,N,N,N,N,CSR.N,N,N,N,N),
    CUSTOM1_RD->        List(Y,N,Y,N,N,N,N,N,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   N, MEM.X,        N,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    CUSTOM1_RD_RS1->    List(Y,N,Y,N,N,N,N,Y,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   N, MEM.X,        N,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    CUSTOM1_RD_RS1_RS2->List(Y,N,Y,N,N,N,Y,Y,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   N, MEM.X,        N,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    CUSTOM2->           List(Y,N,Y,N,N,N,N,N,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   N, MEM.X,        N,N,N,N,N,N,N,CSR.N,N,N,N,N),
    CUSTOM2_RS1->       List(Y,N,Y,N,N,N,N,Y,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   N, MEM.X,        N,N,N,N,N,N,N,CSR.N,N,N,N,N),
    CUSTOM2_RS1_RS2->   List(Y,N,Y,N,N,N,Y,Y,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   N, MEM.X,        N,N,N,N,N,N,N,CSR.N,N,N,N,N),
    CUSTOM2_RD->        List(Y,N,Y,N,N,N,N,N,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   N, MEM.X,        N,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    CUSTOM2_RD_RS1->    List(Y,N,Y,N,N,N,N,Y,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   N, MEM.X,        N,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    CUSTOM2_RD_RS1_RS2->List(Y,N,Y,N,N,N,Y,Y,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   N, MEM.X,        N,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    CUSTOM3->           List(Y,N,Y,N,N,N,N,N,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   N, MEM.X,        N,N,N,N,N,N,N,CSR.N,N,N,N,N),
    CUSTOM3_RS1->       List(Y,N,Y,N,N,N,N,Y,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   N, MEM.X,        N,N,N,N,N,N,N,CSR.N,N,N,N,N),
    CUSTOM3_RS1_RS2->   List(Y,N,Y,N,N,N,Y,Y,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   N, MEM.X,        N,N,N,N,N,N,N,CSR.N,N,N,N,N),
    CUSTOM3_RD->        List(Y,N,Y,N,N,N,N,N,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   N, MEM.X,        N,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    CUSTOM3_RD_RS1->    List(Y,N,Y,N,N,N,N,Y,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   N, MEM.X,        N,N,N,N,N,N,Y,CSR.N,N,N,N,N),
    CUSTOM3_RD_RS1_RS2->List(Y,N,Y,N,N,N,Y,Y,N,A2.ZERO,A1.RS1, IMM.X, DW.XPR,ALU.ADD,   N, MEM.X,        N,N,N,N,N,N,Y,CSR.N,N,N,N,N))
}
