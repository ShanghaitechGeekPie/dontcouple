
package dontcouple.ops

import chisel3._
import chisel3.util._

class NoOp(
  val on_kick: () => Unit = () => (),
  val on_done: () => Unit = () => ()
) extends SimpleOp {
  def kick() = {
    on_kick()
  }
  def done() = {
    on_done
  }
  def apply() = {
    kick()
    done()
  }
  def copy_but_on_kick(new_on_kick: () => Unit) = {
    new NoOp(new_on_kick, on_done)
  }
  def copy_but_on_done(new_on_done: () => Unit) = {
    new NoOp(on_kick, new_on_done)
  }
}
