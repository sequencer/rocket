package org.chipsalliance.rocket

import chisel3._
class NMI(val w: Int) extends Bundle {
  val rnmi = Bool()
  val rnmi_interrupt_vector = UInt(w.W)
  val rnmi_exception_vector = UInt(w.W)
}

class CoreInterrupts(usingSupervisor: Boolean, usingNMI: Boolean, nLocalInterrupts: Int, resetVectorLen: Int, beuAddr: Option[BigInt]) extends Bundle {
  val debug = Bool()
  val mtip = Bool()
  val msip = Bool()
  val meip = Bool()
  val seip = if (usingSupervisor) Some(Bool()) else None
  val lip = Vec(nLocalInterrupts, Bool())
  val nmi = if (usingNMI) Some(new NMI(resetVectorLen)) else None
  val buserror = beuAddr.map(_ => Bool())
}
