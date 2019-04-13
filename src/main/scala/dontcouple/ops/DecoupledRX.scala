
package dontcouple.ops

import chisel3._
import chisel3.util._

class DecoupledRX[T <: Data](
  m: DecoupledIO[T],
  on_kick: () => Unit = () => (),
  on_recv: T  => Unit = (t: T) => (),
  on_done: () => Unit = () => ()
) {
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
}
