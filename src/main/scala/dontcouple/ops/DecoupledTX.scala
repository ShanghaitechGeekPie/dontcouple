
package dontcouple.ops

import chisel3._
import chisel3.util._

class DecoupledTX[T <: Data](
  val s: DecoupledIO[T],
  val on_kick: () => Unit = () => (),
  val on_done: () => Unit = () => ()
) extends SimpleOp {
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
  def copy_but_on_kick(new_on_kick: () => Unit) = {
    new DecoupledTX(s, new_on_kick, on_done)
  } 
  def copy_but_on_done(new_on_done: () => Unit) = {
    new DecoupledTX(s, on_kick, new_on_done)
  }
}
