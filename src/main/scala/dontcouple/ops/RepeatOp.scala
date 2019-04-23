
package dontcouple.ops

import chisel3._
import chisel3.util._

class RepeatOp(
  op_in: SimpleOp,
  cnt_ref: UInt,
  is_compact: Boolean = false,
  val on_kick: () => Unit = () => (),
  val on_done: () => Unit = () => ()
) extends SimpleOp {
  val sIdle :: sBusy :: Nil = Enum(2)
  val state = RegInit(sIdle)
  val cnt = RegInit(0.U(cnt_ref.getWidth.W))
  def on_inc() = {
    val wrap = cnt === (cnt_ref - 1.U)
    cnt := cnt + 1.U
    if(!isPow2(cnt_ref.getWidth)) {
      when(wrap) {
        cnt := 0.U
      }
    }
    when(wrap) {
      done()
    }.otherwise {
      if(is_compact) {
        op.kick()
      }
    }
  }
  val op: SimpleOp = op_in.copy_but_on_done(
    () => {
      op_in.on_done()
      on_inc()
    }
  )
  def reset() = {
    state := sIdle
    cnt := 0.U
  }
  def kick() = {
    state := sBusy
    on_kick()
    when(cnt_ref === 0.U) {
      done()
    }.otherwise {
      op.kick()
    }
  }
  def done() = {
    reset()
    on_done()
  }
  def spin_till_done() = {
    when(state === sBusy) {
      op()
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
    new RepeatOp(op, cnt_ref, is_compact, new_on_kick, on_done)
  } 
  def copy_but_on_done(new_on_done: () => Unit) = {
    new RepeatOp(op, cnt_ref, is_compact, on_kick, new_on_done)
  }
}
