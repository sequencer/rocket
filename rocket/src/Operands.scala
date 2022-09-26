package org.chipsalliance.rocket

import chisel3.util.BitPat

/** micro-ops for rocket. */
object Operands {
  def X: BitPat = BitPat.dontCare(1)

  def Y: BitPat = BitPat.Y()

  def N: BitPat = BitPat.N()
}

