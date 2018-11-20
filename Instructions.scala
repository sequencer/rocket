// See LICENSE.SiFive for license details.
// See LICENSE.Berkeley for license details.

package freechips.rocketchip.rocket

import Chisel._

/* Automatically generated by parse-opcodes */
object Instructions {
  def BEQ                = BitPat("b?????????????????000?????1100011")
  def BNE                = BitPat("b?????????????????001?????1100011")
  def BLT                = BitPat("b?????????????????100?????1100011")
  def BGE                = BitPat("b?????????????????101?????1100011")
  def BLTU               = BitPat("b?????????????????110?????1100011")
  def BGEU               = BitPat("b?????????????????111?????1100011")
  def JALR               = BitPat("b?????????????????000?????1100111")
  def JAL                = BitPat("b?????????????????????????1101111")
  def LUI                = BitPat("b?????????????????????????0110111")
  def AUIPC              = BitPat("b?????????????????????????0010111")
  def ADDI               = BitPat("b?????????????????000?????0010011")
  def SLLI               = BitPat("b000000???????????001?????0010011")
  def SLTI               = BitPat("b?????????????????010?????0010011")
  def SLTIU              = BitPat("b?????????????????011?????0010011")
  def XORI               = BitPat("b?????????????????100?????0010011")
  def SRLI               = BitPat("b000000???????????101?????0010011")
  def SRAI               = BitPat("b010000???????????101?????0010011")
  def ORI                = BitPat("b?????????????????110?????0010011")
  def ANDI               = BitPat("b?????????????????111?????0010011")
  def ADD                = BitPat("b0000000??????????000?????0110011")
  def SUB                = BitPat("b0100000??????????000?????0110011")
  def SLL                = BitPat("b0000000??????????001?????0110011")
  def SLT                = BitPat("b0000000??????????010?????0110011")
  def SLTU               = BitPat("b0000000??????????011?????0110011")
  def XOR                = BitPat("b0000000??????????100?????0110011")
  def SRL                = BitPat("b0000000??????????101?????0110011")
  def SRA                = BitPat("b0100000??????????101?????0110011")
  def OR                 = BitPat("b0000000??????????110?????0110011")
  def AND                = BitPat("b0000000??????????111?????0110011")
  def ADDIW              = BitPat("b?????????????????000?????0011011")
  def SLLIW              = BitPat("b0000000??????????001?????0011011")
  def SRLIW              = BitPat("b0000000??????????101?????0011011")
  def SRAIW              = BitPat("b0100000??????????101?????0011011")
  def ADDW               = BitPat("b0000000??????????000?????0111011")
  def SUBW               = BitPat("b0100000??????????000?????0111011")
  def SLLW               = BitPat("b0000000??????????001?????0111011")
  def SRLW               = BitPat("b0000000??????????101?????0111011")
  def SRAW               = BitPat("b0100000??????????101?????0111011")
  def LB                 = BitPat("b?????????????????000?????0000011")
  def LH                 = BitPat("b?????????????????001?????0000011")
  def LW                 = BitPat("b?????????????????010?????0000011")
  def LD                 = BitPat("b?????????????????011?????0000011")
  def LBU                = BitPat("b?????????????????100?????0000011")
  def LHU                = BitPat("b?????????????????101?????0000011")
  def LWU                = BitPat("b?????????????????110?????0000011")
  def SB                 = BitPat("b?????????????????000?????0100011")
  def SH                 = BitPat("b?????????????????001?????0100011")
  def SW                 = BitPat("b?????????????????010?????0100011")
  def SD                 = BitPat("b?????????????????011?????0100011")
  def FENCE              = BitPat("b?????????????????000?????0001111")
  def FENCE_I            = BitPat("b?????????????????001?????0001111")
  def MUL                = BitPat("b0000001??????????000?????0110011")
  def MULH               = BitPat("b0000001??????????001?????0110011")
  def MULHSU             = BitPat("b0000001??????????010?????0110011")
  def MULHU              = BitPat("b0000001??????????011?????0110011")
  def DIV                = BitPat("b0000001??????????100?????0110011")
  def DIVU               = BitPat("b0000001??????????101?????0110011")
  def REM                = BitPat("b0000001??????????110?????0110011")
  def REMU               = BitPat("b0000001??????????111?????0110011")
  def MULW               = BitPat("b0000001??????????000?????0111011")
  def DIVW               = BitPat("b0000001??????????100?????0111011")
  def DIVUW              = BitPat("b0000001??????????101?????0111011")
  def REMW               = BitPat("b0000001??????????110?????0111011")
  def REMUW              = BitPat("b0000001??????????111?????0111011")
  def AMOADD_W           = BitPat("b00000????????????010?????0101111")
  def AMOXOR_W           = BitPat("b00100????????????010?????0101111")
  def AMOOR_W            = BitPat("b01000????????????010?????0101111")
  def AMOAND_W           = BitPat("b01100????????????010?????0101111")
  def AMOMIN_W           = BitPat("b10000????????????010?????0101111")
  def AMOMAX_W           = BitPat("b10100????????????010?????0101111")
  def AMOMINU_W          = BitPat("b11000????????????010?????0101111")
  def AMOMAXU_W          = BitPat("b11100????????????010?????0101111")
  def AMOSWAP_W          = BitPat("b00001????????????010?????0101111")
  def LR_W               = BitPat("b00010??00000?????010?????0101111")
  def SC_W               = BitPat("b00011????????????010?????0101111")
  def AMOADD_D           = BitPat("b00000????????????011?????0101111")
  def AMOXOR_D           = BitPat("b00100????????????011?????0101111")
  def AMOOR_D            = BitPat("b01000????????????011?????0101111")
  def AMOAND_D           = BitPat("b01100????????????011?????0101111")
  def AMOMIN_D           = BitPat("b10000????????????011?????0101111")
  def AMOMAX_D           = BitPat("b10100????????????011?????0101111")
  def AMOMINU_D          = BitPat("b11000????????????011?????0101111")
  def AMOMAXU_D          = BitPat("b11100????????????011?????0101111")
  def AMOSWAP_D          = BitPat("b00001????????????011?????0101111")
  def LR_D               = BitPat("b00010??00000?????011?????0101111")
  def SC_D               = BitPat("b00011????????????011?????0101111")
  def ECALL              = BitPat("b00000000000000000000000001110011")
  def EBREAK             = BitPat("b00000000000100000000000001110011")
  def URET               = BitPat("b00000000001000000000000001110011")
  def SRET               = BitPat("b00010000001000000000000001110011")
  def MRET               = BitPat("b00110000001000000000000001110011")
  def DRET               = BitPat("b01111011001000000000000001110011")
  def SFENCE_VMA         = BitPat("b0001001??????????000000001110011")
  def WFI                = BitPat("b00010000010100000000000001110011")
  def CEASE              = BitPat("b00110000010100000000000001110011")
  def CSRRW              = BitPat("b?????????????????001?????1110011")
  def CSRRS              = BitPat("b?????????????????010?????1110011")
  def CSRRC              = BitPat("b?????????????????011?????1110011")
  def CSRRWI             = BitPat("b?????????????????101?????1110011")
  def CSRRSI             = BitPat("b?????????????????110?????1110011")
  def CSRRCI             = BitPat("b?????????????????111?????1110011")
  def FADD_S             = BitPat("b0000000??????????????????1010011")
  def FSUB_S             = BitPat("b0000100??????????????????1010011")
  def FMUL_S             = BitPat("b0001000??????????????????1010011")
  def FDIV_S             = BitPat("b0001100??????????????????1010011")
  def FSGNJ_S            = BitPat("b0010000??????????000?????1010011")
  def FSGNJN_S           = BitPat("b0010000??????????001?????1010011")
  def FSGNJX_S           = BitPat("b0010000??????????010?????1010011")
  def FMIN_S             = BitPat("b0010100??????????000?????1010011")
  def FMAX_S             = BitPat("b0010100??????????001?????1010011")
  def FSQRT_S            = BitPat("b010110000000?????????????1010011")
  def FADD_D             = BitPat("b0000001??????????????????1010011")
  def FSUB_D             = BitPat("b0000101??????????????????1010011")
  def FMUL_D             = BitPat("b0001001??????????????????1010011")
  def FDIV_D             = BitPat("b0001101??????????????????1010011")
  def FSGNJ_D            = BitPat("b0010001??????????000?????1010011")
  def FSGNJN_D           = BitPat("b0010001??????????001?????1010011")
  def FSGNJX_D           = BitPat("b0010001??????????010?????1010011")
  def FMIN_D             = BitPat("b0010101??????????000?????1010011")
  def FMAX_D             = BitPat("b0010101??????????001?????1010011")
  def FCVT_S_D           = BitPat("b010000000001?????????????1010011")
  def FCVT_D_S           = BitPat("b010000100000?????????????1010011")
  def FSQRT_D            = BitPat("b010110100000?????????????1010011")
  def FADD_Q             = BitPat("b0000011??????????????????1010011")
  def FSUB_Q             = BitPat("b0000111??????????????????1010011")
  def FMUL_Q             = BitPat("b0001011??????????????????1010011")
  def FDIV_Q             = BitPat("b0001111??????????????????1010011")
  def FSGNJ_Q            = BitPat("b0010011??????????000?????1010011")
  def FSGNJN_Q           = BitPat("b0010011??????????001?????1010011")
  def FSGNJX_Q           = BitPat("b0010011??????????010?????1010011")
  def FMIN_Q             = BitPat("b0010111??????????000?????1010011")
  def FMAX_Q             = BitPat("b0010111??????????001?????1010011")
  def FCVT_S_Q           = BitPat("b010000000011?????????????1010011")
  def FCVT_Q_S           = BitPat("b010001100000?????????????1010011")
  def FCVT_D_Q           = BitPat("b010000100011?????????????1010011")
  def FCVT_Q_D           = BitPat("b010001100001?????????????1010011")
  def FSQRT_Q            = BitPat("b010111100000?????????????1010011")
  def FLE_S              = BitPat("b1010000??????????000?????1010011")
  def FLT_S              = BitPat("b1010000??????????001?????1010011")
  def FEQ_S              = BitPat("b1010000??????????010?????1010011")
  def FLE_D              = BitPat("b1010001??????????000?????1010011")
  def FLT_D              = BitPat("b1010001??????????001?????1010011")
  def FEQ_D              = BitPat("b1010001??????????010?????1010011")
  def FLE_Q              = BitPat("b1010011??????????000?????1010011")
  def FLT_Q              = BitPat("b1010011??????????001?????1010011")
  def FEQ_Q              = BitPat("b1010011??????????010?????1010011")
  def FCVT_W_S           = BitPat("b110000000000?????????????1010011")
  def FCVT_WU_S          = BitPat("b110000000001?????????????1010011")
  def FCVT_L_S           = BitPat("b110000000010?????????????1010011")
  def FCVT_LU_S          = BitPat("b110000000011?????????????1010011")
  def FMV_X_W            = BitPat("b111000000000?????000?????1010011")
  def FCLASS_S           = BitPat("b111000000000?????001?????1010011")
  def FCVT_W_D           = BitPat("b110000100000?????????????1010011")
  def FCVT_WU_D          = BitPat("b110000100001?????????????1010011")
  def FCVT_L_D           = BitPat("b110000100010?????????????1010011")
  def FCVT_LU_D          = BitPat("b110000100011?????????????1010011")
  def FMV_X_D            = BitPat("b111000100000?????000?????1010011")
  def FCLASS_D           = BitPat("b111000100000?????001?????1010011")
  def FCVT_W_Q           = BitPat("b110001100000?????????????1010011")
  def FCVT_WU_Q          = BitPat("b110001100001?????????????1010011")
  def FCVT_L_Q           = BitPat("b110001100010?????????????1010011")
  def FCVT_LU_Q          = BitPat("b110001100011?????????????1010011")
  def FMV_X_Q            = BitPat("b111001100000?????000?????1010011")
  def FCLASS_Q           = BitPat("b111001100000?????001?????1010011")
  def FCVT_S_W           = BitPat("b110100000000?????????????1010011")
  def FCVT_S_WU          = BitPat("b110100000001?????????????1010011")
  def FCVT_S_L           = BitPat("b110100000010?????????????1010011")
  def FCVT_S_LU          = BitPat("b110100000011?????????????1010011")
  def FMV_W_X            = BitPat("b111100000000?????000?????1010011")
  def FCVT_D_W           = BitPat("b110100100000?????????????1010011")
  def FCVT_D_WU          = BitPat("b110100100001?????????????1010011")
  def FCVT_D_L           = BitPat("b110100100010?????????????1010011")
  def FCVT_D_LU          = BitPat("b110100100011?????????????1010011")
  def FMV_D_X            = BitPat("b111100100000?????000?????1010011")
  def FCVT_Q_W           = BitPat("b110101100000?????????????1010011")
  def FCVT_Q_WU          = BitPat("b110101100001?????????????1010011")
  def FCVT_Q_L           = BitPat("b110101100010?????????????1010011")
  def FCVT_Q_LU          = BitPat("b110101100011?????????????1010011")
  def FMV_Q_X            = BitPat("b111101100000?????000?????1010011")
  def FLW                = BitPat("b?????????????????010?????0000111")
  def FLD                = BitPat("b?????????????????011?????0000111")
  def FLQ                = BitPat("b?????????????????100?????0000111")
  def FSW                = BitPat("b?????????????????010?????0100111")
  def FSD                = BitPat("b?????????????????011?????0100111")
  def FSQ                = BitPat("b?????????????????100?????0100111")
  def FMADD_S            = BitPat("b?????00??????????????????1000011")
  def FMSUB_S            = BitPat("b?????00??????????????????1000111")
  def FNMSUB_S           = BitPat("b?????00??????????????????1001011")
  def FNMADD_S           = BitPat("b?????00??????????????????1001111")
  def FMADD_D            = BitPat("b?????01??????????????????1000011")
  def FMSUB_D            = BitPat("b?????01??????????????????1000111")
  def FNMSUB_D           = BitPat("b?????01??????????????????1001011")
  def FNMADD_D           = BitPat("b?????01??????????????????1001111")
  def FMADD_Q            = BitPat("b?????11??????????????????1000011")
  def FMSUB_Q            = BitPat("b?????11??????????????????1000111")
  def FNMSUB_Q           = BitPat("b?????11??????????????????1001011")
  def FNMADD_Q           = BitPat("b?????11??????????????????1001111")
  def C_ADDI4SPN         = BitPat("b????????????????000???????????00")
  def C_FLD              = BitPat("b????????????????001???????????00")
  def C_LW               = BitPat("b????????????????010???????????00")
  def C_FLW              = BitPat("b????????????????011???????????00")
  def C_FSD              = BitPat("b????????????????101???????????00")
  def C_SW               = BitPat("b????????????????110???????????00")
  def C_FSW              = BitPat("b????????????????111???????????00")
  def C_ADDI             = BitPat("b????????????????000???????????01")
  def C_JAL              = BitPat("b????????????????001???????????01")
  def C_LI               = BitPat("b????????????????010???????????01")
  def C_LUI              = BitPat("b????????????????011???????????01")
  def C_SRLI             = BitPat("b????????????????100?00????????01")
  def C_SRAI             = BitPat("b????????????????100?01????????01")
  def C_ANDI             = BitPat("b????????????????100?10????????01")
  def C_SUB              = BitPat("b????????????????100011???00???01")
  def C_XOR              = BitPat("b????????????????100011???01???01")
  def C_OR               = BitPat("b????????????????100011???10???01")
  def C_AND              = BitPat("b????????????????100011???11???01")
  def C_SUBW             = BitPat("b????????????????100111???00???01")
  def C_ADDW             = BitPat("b????????????????100111???01???01")
  def C_J                = BitPat("b????????????????101???????????01")
  def C_BEQZ             = BitPat("b????????????????110???????????01")
  def C_BNEZ             = BitPat("b????????????????111???????????01")
  def C_SLLI             = BitPat("b????????????????000???????????10")
  def C_FLDSP            = BitPat("b????????????????001???????????10")
  def C_LWSP             = BitPat("b????????????????010???????????10")
  def C_FLWSP            = BitPat("b????????????????011???????????10")
  def C_MV               = BitPat("b????????????????1000??????????10")
  def C_ADD              = BitPat("b????????????????1001??????????10")
  def C_FSDSP            = BitPat("b????????????????101???????????10")
  def C_SWSP             = BitPat("b????????????????110???????????10")
  def C_FSWSP            = BitPat("b????????????????111???????????10")
  def C_NOP              = BitPat("b????????????????0000000000000001")
  def C_ADDI16SP         = BitPat("b????????????????011?00010?????01")
  def C_JR               = BitPat("b????????????????1000?????0000010")
  def C_JALR             = BitPat("b????????????????1001?????0000010")
  def C_EBREAK           = BitPat("b????????????????1001000000000010")
  def C_LD               = BitPat("b????????????????011???????????00")
  def C_SD               = BitPat("b????????????????111???????????00")
  def C_ADDIW            = BitPat("b????????????????001???????????01")
  def C_LDSP             = BitPat("b????????????????011???????????10")
  def C_SDSP             = BitPat("b????????????????111???????????10")
  def C_SLLI_RV32        = BitPat("b????????????????0000??????????10")
  def C_SRLI_RV32        = BitPat("b????????????????100000????????01")
  def C_SRAI_RV32        = BitPat("b????????????????100001????????01")
  def CUSTOM0            = BitPat("b?????????????????000?????0001011")
  def CUSTOM0_RS1        = BitPat("b?????????????????010?????0001011")
  def CUSTOM0_RS1_RS2    = BitPat("b?????????????????011?????0001011")
  def CUSTOM0_RD         = BitPat("b?????????????????100?????0001011")
  def CUSTOM0_RD_RS1     = BitPat("b?????????????????110?????0001011")
  def CUSTOM0_RD_RS1_RS2 = BitPat("b?????????????????111?????0001011")
  def CUSTOM1            = BitPat("b?????????????????000?????0101011")
  def CUSTOM1_RS1        = BitPat("b?????????????????010?????0101011")
  def CUSTOM1_RS1_RS2    = BitPat("b?????????????????011?????0101011")
  def CUSTOM1_RD         = BitPat("b?????????????????100?????0101011")
  def CUSTOM1_RD_RS1     = BitPat("b?????????????????110?????0101011")
  def CUSTOM1_RD_RS1_RS2 = BitPat("b?????????????????111?????0101011")
  def CUSTOM2            = BitPat("b?????????????????000?????1011011")
  def CUSTOM2_RS1        = BitPat("b?????????????????010?????1011011")
  def CUSTOM2_RS1_RS2    = BitPat("b?????????????????011?????1011011")
  def CUSTOM2_RD         = BitPat("b?????????????????100?????1011011")
  def CUSTOM2_RD_RS1     = BitPat("b?????????????????110?????1011011")
  def CUSTOM2_RD_RS1_RS2 = BitPat("b?????????????????111?????1011011")
  def CUSTOM3            = BitPat("b?????????????????000?????1111011")
  def CUSTOM3_RS1        = BitPat("b?????????????????010?????1111011")
  def CUSTOM3_RS1_RS2    = BitPat("b?????????????????011?????1111011")
  def CUSTOM3_RD         = BitPat("b?????????????????100?????1111011")
  def CUSTOM3_RD_RS1     = BitPat("b?????????????????110?????1111011")
  def CUSTOM3_RD_RS1_RS2 = BitPat("b?????????????????111?????1111011")
  def SLLI_RV32          = BitPat("b0000000??????????001?????0010011")
  def SRLI_RV32          = BitPat("b0000000??????????101?????0010011")
  def SRAI_RV32          = BitPat("b0100000??????????101?????0010011")
  def FRFLAGS            = BitPat("b00000000000100000010?????1110011")
  def FSFLAGS            = BitPat("b000000000001?????001?????1110011")
  def FSFLAGSI           = BitPat("b000000000001?????101?????1110011")
  def FRRM               = BitPat("b00000000001000000010?????1110011")
  def FSRM               = BitPat("b000000000010?????001?????1110011")
  def FSRMI              = BitPat("b000000000010?????101?????1110011")
  def FSCSR              = BitPat("b000000000011?????001?????1110011")
  def FRCSR              = BitPat("b00000000001100000010?????1110011")
  def RDCYCLE            = BitPat("b11000000000000000010?????1110011")
  def RDTIME             = BitPat("b11000000000100000010?????1110011")
  def RDINSTRET          = BitPat("b11000000001000000010?????1110011")
  def RDCYCLEH           = BitPat("b11001000000000000010?????1110011")
  def RDTIMEH            = BitPat("b11001000000100000010?????1110011")
  def RDINSTRETH         = BitPat("b11001000001000000010?????1110011")
  def SCALL              = BitPat("b00000000000000000000000001110011")
  def SBREAK             = BitPat("b00000000000100000000000001110011")
  def FMV_X_S            = BitPat("b111000000000?????000?????1010011")
  def FMV_S_X            = BitPat("b111100000000?????000?????1010011")
  def FENCE_TSO          = BitPat("b100000110011?????000?????0001111")
}
object Causes {
  val misaligned_fetch = 0x0
  val fetch_access = 0x1
  val illegal_instruction = 0x2
  val breakpoint = 0x3
  val misaligned_load = 0x4
  val load_access = 0x5
  val misaligned_store = 0x6
  val store_access = 0x7
  val user_ecall = 0x8
  val supervisor_ecall = 0x9
  val hypervisor_ecall = 0xa
  val machine_ecall = 0xb
  val fetch_page_fault = 0xc
  val load_page_fault = 0xd
  val store_page_fault = 0xf
  val all = {
    val res = collection.mutable.ArrayBuffer[Int]()
    res += misaligned_fetch
    res += fetch_access
    res += illegal_instruction
    res += breakpoint
    res += misaligned_load
    res += load_access
    res += misaligned_store
    res += store_access
    res += user_ecall
    res += supervisor_ecall
    res += hypervisor_ecall
    res += machine_ecall
    res += fetch_page_fault
    res += load_page_fault
    res += store_page_fault
    res.toArray
  }
}
object CSRs {
  val fflags = 0x1
  val frm = 0x2
  val fcsr = 0x3
  val cycle = 0xc00
  val time = 0xc01
  val instret = 0xc02
  val hpmcounter3 = 0xc03
  val hpmcounter4 = 0xc04
  val hpmcounter5 = 0xc05
  val hpmcounter6 = 0xc06
  val hpmcounter7 = 0xc07
  val hpmcounter8 = 0xc08
  val hpmcounter9 = 0xc09
  val hpmcounter10 = 0xc0a
  val hpmcounter11 = 0xc0b
  val hpmcounter12 = 0xc0c
  val hpmcounter13 = 0xc0d
  val hpmcounter14 = 0xc0e
  val hpmcounter15 = 0xc0f
  val hpmcounter16 = 0xc10
  val hpmcounter17 = 0xc11
  val hpmcounter18 = 0xc12
  val hpmcounter19 = 0xc13
  val hpmcounter20 = 0xc14
  val hpmcounter21 = 0xc15
  val hpmcounter22 = 0xc16
  val hpmcounter23 = 0xc17
  val hpmcounter24 = 0xc18
  val hpmcounter25 = 0xc19
  val hpmcounter26 = 0xc1a
  val hpmcounter27 = 0xc1b
  val hpmcounter28 = 0xc1c
  val hpmcounter29 = 0xc1d
  val hpmcounter30 = 0xc1e
  val hpmcounter31 = 0xc1f
  val sstatus = 0x100
  val sie = 0x104
  val stvec = 0x105
  val scounteren = 0x106
  val sscratch = 0x140
  val sepc = 0x141
  val scause = 0x142
  val stval = 0x143
  val sbadaddr = stval // legacy name
  val sip = 0x144
  val satp = 0x180
  val sptbr = satp // legacy name
  val mstatus = 0x300
  val misa = 0x301
  val medeleg = 0x302
  val mideleg = 0x303
  val mie = 0x304
  val mtvec = 0x305
  val mcounteren = 0x306
  val mscratch = 0x340
  val mepc = 0x341
  val mcause = 0x342
  val mtval = 0x343
  val mbadaddr = mtval // legacy name
  val mip = 0x344
  val pmpcfg0 = 0x3a0
  val pmpcfg1 = 0x3a1
  val pmpcfg2 = 0x3a2
  val pmpcfg3 = 0x3a3
  val pmpaddr0 = 0x3b0
  val pmpaddr1 = 0x3b1
  val pmpaddr2 = 0x3b2
  val pmpaddr3 = 0x3b3
  val pmpaddr4 = 0x3b4
  val pmpaddr5 = 0x3b5
  val pmpaddr6 = 0x3b6
  val pmpaddr7 = 0x3b7
  val pmpaddr8 = 0x3b8
  val pmpaddr9 = 0x3b9
  val pmpaddr10 = 0x3ba
  val pmpaddr11 = 0x3bb
  val pmpaddr12 = 0x3bc
  val pmpaddr13 = 0x3bd
  val pmpaddr14 = 0x3be
  val pmpaddr15 = 0x3bf
  val tselect = 0x7a0
  val tdata1 = 0x7a1
  val tdata2 = 0x7a2
  val tdata3 = 0x7a3
  val dcsr = 0x7b0
  val dpc = 0x7b1
  val dscratch = 0x7b2
  val mcycle = 0xb00
  val minstret = 0xb02
  val mhpmcounter3 = 0xb03
  val mhpmcounter4 = 0xb04
  val mhpmcounter5 = 0xb05
  val mhpmcounter6 = 0xb06
  val mhpmcounter7 = 0xb07
  val mhpmcounter8 = 0xb08
  val mhpmcounter9 = 0xb09
  val mhpmcounter10 = 0xb0a
  val mhpmcounter11 = 0xb0b
  val mhpmcounter12 = 0xb0c
  val mhpmcounter13 = 0xb0d
  val mhpmcounter14 = 0xb0e
  val mhpmcounter15 = 0xb0f
  val mhpmcounter16 = 0xb10
  val mhpmcounter17 = 0xb11
  val mhpmcounter18 = 0xb12
  val mhpmcounter19 = 0xb13
  val mhpmcounter20 = 0xb14
  val mhpmcounter21 = 0xb15
  val mhpmcounter22 = 0xb16
  val mhpmcounter23 = 0xb17
  val mhpmcounter24 = 0xb18
  val mhpmcounter25 = 0xb19
  val mhpmcounter26 = 0xb1a
  val mhpmcounter27 = 0xb1b
  val mhpmcounter28 = 0xb1c
  val mhpmcounter29 = 0xb1d
  val mhpmcounter30 = 0xb1e
  val mhpmcounter31 = 0xb1f
  val mhpmevent3 = 0x323
  val mhpmevent4 = 0x324
  val mhpmevent5 = 0x325
  val mhpmevent6 = 0x326
  val mhpmevent7 = 0x327
  val mhpmevent8 = 0x328
  val mhpmevent9 = 0x329
  val mhpmevent10 = 0x32a
  val mhpmevent11 = 0x32b
  val mhpmevent12 = 0x32c
  val mhpmevent13 = 0x32d
  val mhpmevent14 = 0x32e
  val mhpmevent15 = 0x32f
  val mhpmevent16 = 0x330
  val mhpmevent17 = 0x331
  val mhpmevent18 = 0x332
  val mhpmevent19 = 0x333
  val mhpmevent20 = 0x334
  val mhpmevent21 = 0x335
  val mhpmevent22 = 0x336
  val mhpmevent23 = 0x337
  val mhpmevent24 = 0x338
  val mhpmevent25 = 0x339
  val mhpmevent26 = 0x33a
  val mhpmevent27 = 0x33b
  val mhpmevent28 = 0x33c
  val mhpmevent29 = 0x33d
  val mhpmevent30 = 0x33e
  val mhpmevent31 = 0x33f
  val mvendorid = 0xf11
  val marchid = 0xf12
  val mimpid = 0xf13
  val mhartid = 0xf14
  val cycleh = 0xc80
  val timeh = 0xc81
  val instreth = 0xc82
  val hpmcounter3h = 0xc83
  val hpmcounter4h = 0xc84
  val hpmcounter5h = 0xc85
  val hpmcounter6h = 0xc86
  val hpmcounter7h = 0xc87
  val hpmcounter8h = 0xc88
  val hpmcounter9h = 0xc89
  val hpmcounter10h = 0xc8a
  val hpmcounter11h = 0xc8b
  val hpmcounter12h = 0xc8c
  val hpmcounter13h = 0xc8d
  val hpmcounter14h = 0xc8e
  val hpmcounter15h = 0xc8f
  val hpmcounter16h = 0xc90
  val hpmcounter17h = 0xc91
  val hpmcounter18h = 0xc92
  val hpmcounter19h = 0xc93
  val hpmcounter20h = 0xc94
  val hpmcounter21h = 0xc95
  val hpmcounter22h = 0xc96
  val hpmcounter23h = 0xc97
  val hpmcounter24h = 0xc98
  val hpmcounter25h = 0xc99
  val hpmcounter26h = 0xc9a
  val hpmcounter27h = 0xc9b
  val hpmcounter28h = 0xc9c
  val hpmcounter29h = 0xc9d
  val hpmcounter30h = 0xc9e
  val hpmcounter31h = 0xc9f
  val mcycleh = 0xb80
  val minstreth = 0xb82
  val mhpmcounter3h = 0xb83
  val mhpmcounter4h = 0xb84
  val mhpmcounter5h = 0xb85
  val mhpmcounter6h = 0xb86
  val mhpmcounter7h = 0xb87
  val mhpmcounter8h = 0xb88
  val mhpmcounter9h = 0xb89
  val mhpmcounter10h = 0xb8a
  val mhpmcounter11h = 0xb8b
  val mhpmcounter12h = 0xb8c
  val mhpmcounter13h = 0xb8d
  val mhpmcounter14h = 0xb8e
  val mhpmcounter15h = 0xb8f
  val mhpmcounter16h = 0xb90
  val mhpmcounter17h = 0xb91
  val mhpmcounter18h = 0xb92
  val mhpmcounter19h = 0xb93
  val mhpmcounter20h = 0xb94
  val mhpmcounter21h = 0xb95
  val mhpmcounter22h = 0xb96
  val mhpmcounter23h = 0xb97
  val mhpmcounter24h = 0xb98
  val mhpmcounter25h = 0xb99
  val mhpmcounter26h = 0xb9a
  val mhpmcounter27h = 0xb9b
  val mhpmcounter28h = 0xb9c
  val mhpmcounter29h = 0xb9d
  val mhpmcounter30h = 0xb9e
  val mhpmcounter31h = 0xb9f
  val all = {
    val res = collection.mutable.ArrayBuffer[Int]()
    res += fflags
    res += frm
    res += fcsr
    res += cycle
    res += time
    res += instret
    res += hpmcounter3
    res += hpmcounter4
    res += hpmcounter5
    res += hpmcounter6
    res += hpmcounter7
    res += hpmcounter8
    res += hpmcounter9
    res += hpmcounter10
    res += hpmcounter11
    res += hpmcounter12
    res += hpmcounter13
    res += hpmcounter14
    res += hpmcounter15
    res += hpmcounter16
    res += hpmcounter17
    res += hpmcounter18
    res += hpmcounter19
    res += hpmcounter20
    res += hpmcounter21
    res += hpmcounter22
    res += hpmcounter23
    res += hpmcounter24
    res += hpmcounter25
    res += hpmcounter26
    res += hpmcounter27
    res += hpmcounter28
    res += hpmcounter29
    res += hpmcounter30
    res += hpmcounter31
    res += sstatus
    res += sie
    res += stvec
    res += scounteren
    res += sscratch
    res += sepc
    res += scause
    res += stval
    res += sip
    res += satp
    res += mstatus
    res += misa
    res += medeleg
    res += mideleg
    res += mie
    res += mtvec
    res += mcounteren
    res += mscratch
    res += mepc
    res += mcause
    res += mtval
    res += mip
    res += pmpcfg0
    res += pmpcfg1
    res += pmpcfg2
    res += pmpcfg3
    res += pmpaddr0
    res += pmpaddr1
    res += pmpaddr2
    res += pmpaddr3
    res += pmpaddr4
    res += pmpaddr5
    res += pmpaddr6
    res += pmpaddr7
    res += pmpaddr8
    res += pmpaddr9
    res += pmpaddr10
    res += pmpaddr11
    res += pmpaddr12
    res += pmpaddr13
    res += pmpaddr14
    res += pmpaddr15
    res += tselect
    res += tdata1
    res += tdata2
    res += tdata3
    res += dcsr
    res += dpc
    res += dscratch
    res += mcycle
    res += minstret
    res += mhpmcounter3
    res += mhpmcounter4
    res += mhpmcounter5
    res += mhpmcounter6
    res += mhpmcounter7
    res += mhpmcounter8
    res += mhpmcounter9
    res += mhpmcounter10
    res += mhpmcounter11
    res += mhpmcounter12
    res += mhpmcounter13
    res += mhpmcounter14
    res += mhpmcounter15
    res += mhpmcounter16
    res += mhpmcounter17
    res += mhpmcounter18
    res += mhpmcounter19
    res += mhpmcounter20
    res += mhpmcounter21
    res += mhpmcounter22
    res += mhpmcounter23
    res += mhpmcounter24
    res += mhpmcounter25
    res += mhpmcounter26
    res += mhpmcounter27
    res += mhpmcounter28
    res += mhpmcounter29
    res += mhpmcounter30
    res += mhpmcounter31
    res += mhpmevent3
    res += mhpmevent4
    res += mhpmevent5
    res += mhpmevent6
    res += mhpmevent7
    res += mhpmevent8
    res += mhpmevent9
    res += mhpmevent10
    res += mhpmevent11
    res += mhpmevent12
    res += mhpmevent13
    res += mhpmevent14
    res += mhpmevent15
    res += mhpmevent16
    res += mhpmevent17
    res += mhpmevent18
    res += mhpmevent19
    res += mhpmevent20
    res += mhpmevent21
    res += mhpmevent22
    res += mhpmevent23
    res += mhpmevent24
    res += mhpmevent25
    res += mhpmevent26
    res += mhpmevent27
    res += mhpmevent28
    res += mhpmevent29
    res += mhpmevent30
    res += mhpmevent31
    res += mvendorid
    res += marchid
    res += mimpid
    res += mhartid
    res.toArray
  }
  val all32 = {
    val res = collection.mutable.ArrayBuffer(all:_*)
    res += cycleh
    res += timeh
    res += instreth
    res += hpmcounter3h
    res += hpmcounter4h
    res += hpmcounter5h
    res += hpmcounter6h
    res += hpmcounter7h
    res += hpmcounter8h
    res += hpmcounter9h
    res += hpmcounter10h
    res += hpmcounter11h
    res += hpmcounter12h
    res += hpmcounter13h
    res += hpmcounter14h
    res += hpmcounter15h
    res += hpmcounter16h
    res += hpmcounter17h
    res += hpmcounter18h
    res += hpmcounter19h
    res += hpmcounter20h
    res += hpmcounter21h
    res += hpmcounter22h
    res += hpmcounter23h
    res += hpmcounter24h
    res += hpmcounter25h
    res += hpmcounter26h
    res += hpmcounter27h
    res += hpmcounter28h
    res += hpmcounter29h
    res += hpmcounter30h
    res += hpmcounter31h
    res += mcycleh
    res += minstreth
    res += mhpmcounter3h
    res += mhpmcounter4h
    res += mhpmcounter5h
    res += mhpmcounter6h
    res += mhpmcounter7h
    res += mhpmcounter8h
    res += mhpmcounter9h
    res += mhpmcounter10h
    res += mhpmcounter11h
    res += mhpmcounter12h
    res += mhpmcounter13h
    res += mhpmcounter14h
    res += mhpmcounter15h
    res += mhpmcounter16h
    res += mhpmcounter17h
    res += mhpmcounter18h
    res += mhpmcounter19h
    res += mhpmcounter20h
    res += mhpmcounter21h
    res += mhpmcounter22h
    res += mhpmcounter23h
    res += mhpmcounter24h
    res += mhpmcounter25h
    res += mhpmcounter26h
    res += mhpmcounter27h
    res += mhpmcounter28h
    res += mhpmcounter29h
    res += mhpmcounter30h
    res += mhpmcounter31h
    res.toArray
  }
}
