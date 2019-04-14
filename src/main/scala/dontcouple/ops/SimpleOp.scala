
package dontcouple.ops

import chisel3._
import chisel3.util._

trait SimpleOp {
  val on_kick: () => Unit
  val on_done: () => Unit
  def kick(): Unit
  def copy_but_on_kick(new_on_kick: () => Unit): SimpleOp
  def copy_but_on_done(new_on_done: () => Unit): SimpleOp
}
