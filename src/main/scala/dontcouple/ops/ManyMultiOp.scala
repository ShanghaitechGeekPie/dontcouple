
package dontcouple.ops

import chisel3._
import chisel3.util._

class ManyMultiOp(
  eles_in: List[SimpleOp],
  on_kick: () => Unit = () => (),
  on_done: () => Unit = () => ()
) extends MultiOp(eles_in, on_kick, on_done) {

  val all_eles_done = RegInit(false.B)

  override def reset() = {
    state := sIdle
    for(ele_done <- eles_done) {
      ele_done := false.B
    }
    all_eles_done := false.B
  }
  override def kick() = {
    on_kick()
    state := sBusy
    for(ele_done <- eles_done) {
      ele_done := false.B
    }
    all_eles_done := false.B
    for(ele <- eles) {
      ele.kick()
    }
  }
  override def spin_till_done() = {
    when(state === sBusy) {
      for((ele, ind) <- eles.zipWithIndex) {
        unless(eles_done(ind)) {
          ele()
        }
      }
      all_eles_done := eles_done.foldLeft(true.B)((z: Bool, x: Bool) => {z && x})
      when(all_eles_done) {
        done()
      }
    }
  }

  override def copy_but_on_kick(new_on_kick: () => Unit) = {
    new ManyMultiOp(eles_in, new_on_kick, on_done)
  } 
  override def copy_but_on_done(new_on_done: () => Unit) = {
    new ManyMultiOp(eles_in, on_kick, new_on_done)
  }
}
