
package dontcouple.thunks

import chisel3._
import chisel3.util._

trait Dontcouple_Context {
  implicit def lift[SRC_T <: Data, DST_T <: Data](tup: (SRC_T, DST_T, SRC_T => DST_T)) = {
    new FilterFunction(tup._1, tup._2, tup._3)
  }
}

case class FilterFunction[SRC_T <: Data, DST_T <: Data] (src: SRC_T, dst: DST_T, f: SRC_T => DST_T) {
  def get_src = src.cloneType
  def get_dst = dst.cloneType
  def compose[PRV_T <: Data](g: FilterFunction[PRV_T, SRC_T]) = {
    val _f = this.f
    val _g = g.f
    val composed_f: PRV_T => DST_T = (t: PRV_T) => (_f(_g(t)))
    new FilterFunction(g.get_src, get_dst, composed_f)
  }
  def concat[NXT_T <: Data](g: FilterFunction[DST_T, NXT_T]) = {
    new FilterCons(new FilterBrick(this), new FilterBrick(g))
  }
  def <>[NXT_T <: Data](g: FilterFunction[DST_T, NXT_T]) = {
    this concat g
  }
}

sealed abstract class TFilter[SRC_T <: Data, DST_T <: Data]{
  def src: () => SRC_T
  def dst: () => DST_T
  def <>[NXT_T <: Data](g: FilterFunction[DST_T, NXT_T]) = {
    new FilterCons(this, new FilterBrick(g))
  }
  def <>[NXT_T <: Data](g: TFilter[DST_T, NXT_T]) = {
    new FilterCons(this, g)
  }
}

case class FilterBrick[SRC_T <: Data, DST_T <: Data](
  f: FilterFunction[SRC_T, DST_T]
) extends TFilter[SRC_T, DST_T] {
  def src = () => f.get_src
  def dst = () => f.get_dst
}

case class FilterCons[SRC_T <: Data, MID_T <: Data, DST_T <: Data](
  l: TFilter[SRC_T, MID_T],
  r: TFilter[MID_T, DST_T]
) extends TFilter[SRC_T, DST_T] {
  type MID_T_ = MID_T
  def src = () => l.src()
  def dst = () => r.dst()
}
