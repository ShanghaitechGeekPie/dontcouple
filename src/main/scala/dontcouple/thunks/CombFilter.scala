
package dontcouple.thunks

import chisel3._
import chisel3.util._

import dontcouple.ops._

abstract class TCombFilter[SRC_T <: Data, DST_T <: Data] extends Module{
  this: Module =>
  def src: () => SRC_T
  def dst: () => DST_T
  def iport: () => SRC_T
  def oport: () => DST_T
}

class CombFilterBrick[SRC_T <: Data, DST_T <: Data](
  f: FilterFunction[SRC_T, DST_T]
) extends TCombFilter[SRC_T, DST_T] {
  val io = IO(new Bundle {
    val i = Input(f.src)
    val o = Output(f.dst)
  })
  def src = () => f.get_src
  def dst = () => f.get_dst
  def iport = () => io.i
  def oport = () => io.o
  io.o := f.f(io.i)
}

case class CombFilterGen[SRC_T <: Data, DST_T <: Data](gen: () => TCombFilter[SRC_T, DST_T]) {
  def apply() = {
    gen()
  }
  def <>[NXT_T <: Data](
    gen_r: CombFilterGen[DST_T, NXT_T]
  ): CombFilterGen[SRC_T, NXT_T] = {
    val gen_l = this
    new CombFilterGen(() => {new CombFilterCons(gen_l, gen_r)})
  }
}
object CombFilterGen {
  implicit def lift[SRC_T <: Data, DST_T <: Data](tup: (SRC_T, DST_T, SRC_T => DST_T)): CombFilterGen[SRC_T, DST_T] = {
    new CombFilterGen(() => {new CombFilterBrick(new FilterFunction(tup._1, tup._2, tup._3))})
  }
}

class CombFilterCons[SRC_T <: Data, MID_T <: Data, DST_T <: Data](
  gen_l: CombFilterGen[SRC_T, MID_T],
  gen_r: CombFilterGen[MID_T, DST_T]
) extends TCombFilter[SRC_T, DST_T] {
  val blk_l = Module(gen_l())
  val blk_r = Module(gen_r())
  val io = IO(new Bundle {
    val i = Input(blk_l.src())
    val o = Output(blk_r.dst())
  })
  
  def src = () => blk_l.src()
  def dst = () => blk_r.dst()
  def iport = () => io.i
  def oport = () => io.o
  
  blk_l.iport() <> io.i
  blk_l.oport() <> blk_r.iport()
  blk_r.oport() <> io.o
}

trait Dontcouple_CombContext {
  implicit def lift[SRC_T <: Data, DST_T <: Data](tup: (SRC_T, DST_T, SRC_T => DST_T)): CombFilterGen[SRC_T, DST_T] = {
    new CombFilterGen(() => {new CombFilterBrick(new FilterFunction(tup._1, tup._2, tup._3))})
  }
}

