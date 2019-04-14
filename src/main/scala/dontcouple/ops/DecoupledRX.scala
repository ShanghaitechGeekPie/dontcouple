
package dontcouple.ops

import chisel3._
import chisel3.util._

class DecoupledRX[T <: Data](
  val m: DecoupledIO[T],
  val on_kick: () => Unit = () => (),
  val on_recv: T  => Unit = (t: T) => (),
  val on_done: () => Unit = () => ()
) extends SimpleOp {
  val sIdle :: sBusy :: Nil = Enum(2)
  val state = RegInit(sIdle)
  val s_ready = RegInit(false.B)
  m.ready := s_ready
  def reset() = {
    state := sIdle
    s_ready := false.B
  }
  def kick() = {
    on_kick()
    state   := sBusy
    s_ready := true.B
  }
  def done() = {
    on_done()
    reset()
  }
  def spin_till_done() = {
    when(state === sBusy) {
      when(m.valid) {
        on_recv(m.bits)
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
    new DecoupledRX(m, new_on_kick, on_recv, on_done)
  } 
  def copy_but_on_done(new_on_done: () => Unit) = {
    new DecoupledRX(m, on_kick, on_recv, new_on_done)
  }
}
