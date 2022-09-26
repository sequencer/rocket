// See LICENSE.SiFive for license details.
// See LICENSE.Berkeley for license details.

package org.chipsalliance.rocket

import chisel3.util._

/* make EXTENSIONS="rv32*" inst.chisel */
/* rename Instructions to Instructions32 */
/* remove Causes and CSRs */

/* Automatically generated by parse_opcodes */
object Instructions32 {
  def AES32DSI           = BitPat("b??10101??????????000?????0110011")
  def AES32DSMI          = BitPat("b??10111??????????000?????0110011")
  def AES32ESI           = BitPat("b??10001??????????000?????0110011")
  def AES32ESMI          = BitPat("b??10011??????????000?????0110011")
  def BCLRI              = BitPat("b0100100??????????001?????0010011")
  def BEXTI              = BitPat("b0100100??????????101?????0010011")
  def BINVI              = BitPat("b0110100??????????001?????0010011")
  def BSETI              = BitPat("b0010100??????????001?????0010011")
  def C_FLW              = BitPat("b????????????????011???????????00")
  def C_FLWSP            = BitPat("b????????????????011???????????10")
  def C_FSW              = BitPat("b????????????????111???????????00")
  def C_FSWSP            = BitPat("b????????????????111???????????10")
  def C_JAL              = BitPat("b????????????????001???????????01")
  def C_SLLI             = BitPat("b????????????????0000??????????10")
  def C_SRAI             = BitPat("b????????????????100001????????01")
  def C_SRLI             = BitPat("b????????????????100000????????01")
  def REV8               = BitPat("b011010011000?????101?????0010011")
  def RORI               = BitPat("b0110000??????????101?????0010011")
  def SHA512SIG0H        = BitPat("b0101110??????????000?????0110011")
  def SHA512SIG0L        = BitPat("b0101010??????????000?????0110011")
  def SHA512SIG1H        = BitPat("b0101111??????????000?????0110011")
  def SHA512SIG1L        = BitPat("b0101011??????????000?????0110011")
  def SHA512SUM0R        = BitPat("b0101000??????????000?????0110011")
  def SHA512SUM1R        = BitPat("b0101001??????????000?????0110011")
  def SLLI               = BitPat("b0000000??????????001?????0010011")
  def SRAI               = BitPat("b0100000??????????101?????0010011")
  def SRLI               = BitPat("b0000000??????????101?????0010011")
  def UNZIP              = BitPat("b000010001111?????101?????0010011")
  def ZEXT_H             = BitPat("b000010000000?????100?????0110011")
  def ZIP                = BitPat("b000010001111?????001?????0010011")
}
