
package dontcouple.ops

import chisel3._
import chisel3.util._

class DecoupledTX[T <: Data](
  s: DecoupledIO[T],
  on_kick: () => Unit = () => (),
  on_done: () => Unit = () => ()
) {
  val sIdle :: sBusy :: Nil = Enum(2)
  val state = RegInit(sIdle)
  val m_valid = RegInit(false.B)
  s.valid := m_valid
  def reset() = {
    state := sIdle
    m_valid := false.B
  }
  def kick() = {
    on_kick()
    state   := sBusy
    m_valid := true.B
  }
  def done() = {
    on_done()
    reset()
  }
  def spin_till_done() = {
    when(state === sBusy) {
      when(s.ready) {
        done()
      }
    }
  }
  def apply() = {
    when(state === sIdle) {
      kick()
    }.otherwise {
      spin_till_done()
    }
  }
}
