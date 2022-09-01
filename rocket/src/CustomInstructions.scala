// See LICENSE.SiFive for license details.
// See LICENSE.Berkeley for license details.

package org.chipsalliance.rocket

import chisel3.util.BitPat

object CustomInstructions {
  def MNRET              = BitPat("b01110000001000000000000001110011")
  def CEASE              = BitPat("b00110000010100000000000001110011")
  def CFLUSH_D_L1        = BitPat("b111111000000?????000000001110011")
}

object CustomCSRs {
  val mnscratch = 0x350
  val mnepc = 0x351
  val mncause = 0x352
  val mnstatus = 0x353
  val all = {
    val res = collection.mutable.ArrayBuffer[Int]()
    res += mnscratch
    res += mnepc
    res += mncause
    res += mnstatus
    res.toArray
  }
}
