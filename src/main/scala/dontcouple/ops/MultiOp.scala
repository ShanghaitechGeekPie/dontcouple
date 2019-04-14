
package dontcouple.ops

import chisel3._
import chisel3.util._

class MultiOp(
  eles_in: List[SimpleOp],
  val on_kick: () => Unit = () => (),
  val on_done: () => Unit = () => ()
) extends SimpleOp {

  val sIdle :: sBusy :: Nil = Enum(2)
  val state = RegInit(sIdle)
  val eles_done = RegInit(VecInit(Seq.fill(eles_in.length){false.B}))
  val eles: List[SimpleOp] = eles_in.zipWithIndex.map(
    (ele_with_ind: (SimpleOp, Int)) => {
      val (ele, ind) = ele_with_ind
      ele.copy_but_on_done(() => {
        ele.on_done()
        eles_done(ind) := true.B
      })
    }
  )
  def reset() = {
    state := sIdle
    for(ele_done <- eles_done) {
      ele_done := false.B
    }
  }
  def kick() = {
    on_kick()
    state := sBusy
    for(ele <- eles) {
      ele.kick()
    }
  }
  def done() = {
    on_done()
    reset()
  }
  def spin_till_done() = {
    when(state === sBusy) {
      for(ele <- eles) {
        ele()
      }
      val is_all_done = eles_done.foldLeft(true.B)((z: Bool, x: Bool) => {z && x})
      when(is_all_done) {
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
    new MultiOp(eles_in, new_on_kick, on_done)
  } 
  def copy_but_on_done(new_on_done: () => Unit) = {
    new MultiOp(eles_in, on_kick, new_on_done)
  }
}
