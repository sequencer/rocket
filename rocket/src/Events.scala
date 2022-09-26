// See LICENSE.Berkeley for license details.
// See LICENSE.SiFive for license details.

package org.chipsalliance.rocket

import chisel3._
import chisel3.util.log2Ceil

import scala.language.postfixOps

class EventSet(val gate: (UInt, UInt) => Bool, val events: Seq[(String, () => Bool)]) {
  def size: Int = events.size
  val hits: Vec[Bool] = Wire(Vec(size, Bool()))
  def check(mask: UInt): Bool = {
    hits := events.map(_._2())
    gate(mask, hits.asUInt)
  }
  def dump(): Unit = {
    for (((name, _), i) <- events.zipWithIndex)
      when (check(1.U << i: UInt)) { printf(s"Event $name\n") }
  }
  def withCovers: Unit = {
    events.zipWithIndex.foreach {
      case ((name, func), i) => cover(gate(1.U << i: UInt, func() << i), name)
    }
  }
}

class EventSets(val eventSets: Seq[EventSet]) {
  def maskEventSelector(eventSel: UInt): UInt = {
    // allow full associativity between counters and event sets (for now?)
    val setMask = (BigInt(1) << eventSetIdBits) - 1
    val maskMask = ((BigInt(1) << eventSets.map(_.size).max) - 1) << maxEventSetIdBits
    eventSel & (setMask | maskMask).U
  }

  private def decode(counter: UInt): (UInt, UInt) = {
    require(eventSets.size <= (1 << maxEventSetIdBits))
    require(eventSetIdBits > 0)
    (counter(eventSetIdBits-1, 0), counter >> maxEventSetIdBits: UInt)
  }

  def evaluate(eventSel: UInt): Bool = {
    val (set: UInt, mask: UInt) = decode(eventSel)
    val sets: Vec[Bool] = VecInit(for (e <- eventSets) yield {
      require(e.hits.getWidth <= mask.getWidth, s"too many events ${e.hits.getWidth} wider than mask ${mask.getWidth}")
      e check mask
    })
    sets(set)
  }

  def cover(): Unit = eventSets.foreach { _ withCovers }

  private def eventSetIdBits: Int = log2Ceil(eventSets.size)
  private def maxEventSetIdBits: Int = 8

  require(eventSetIdBits <= maxEventSetIdBits)
}
