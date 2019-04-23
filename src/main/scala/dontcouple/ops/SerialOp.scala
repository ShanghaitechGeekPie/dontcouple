
package dontcouple.ops

import chisel3._
import chisel3.util._

class SerialOp(
  eles_in: List[SimpleOp],
  rewind: Boolean = false,
  is_compact: Boolean = false,
  val on_kick: () => Unit = () => (),
  val on_done: () => Unit = () => ()
) extends SimpleOp {
  require(eles_in.length > 0)
  val sIdle = 0.U
  val state = RegInit(0.U(log2Up(eles_in.length + 1).W))
  val eles: List[SimpleOp] = eles_in.zipWithIndex.map(
    (ele_with_ind: (SimpleOp, Int)) => {
      val (ele, ind) = ele_with_ind
      ele.copy_but_on_done(() => {
        ele.on_done()
        if(ind == (eles_in.length - 1)) {
          if(rewind) {
            done()
            kick()
          }
        } else {
          state := (ind + 2).U
          if(is_compact) {
            eles(ind + 1).kick()
          }
        }
      })
    }
  )
  def reset() = {
    state := sIdle
  }
  def kick() = {
    state := 1.U
    if(is_compact) {
      eles(0).kick()
    }
  }
  def done() = {
    reset()
    on_done()
  }
  def spin_till_done() = {
    when(state =/= sIdle) {
      for((ele, ind) <- eles.zipWithIndex) {
        when(state === ((ind + 1).U)) {
          ele()
        }
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
    new SerialOp(eles_in, rewind, is_compact, new_on_kick, on_done)
  } 
  def copy_but_on_done(new_on_done: () => Unit) = {
    new SerialOp(eles_in, rewind, is_compact, on_kick, new_on_done)
  }
}
