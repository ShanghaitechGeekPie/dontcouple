
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

class CombFilterCons[SRC_T <: Data, MID_T <: Data, DST_T <: Data](
  gen_l: () => TCombFilter[SRC_T, MID_T],
  gen_r: () => TCombFilter[MID_T, DST_T]
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

object GEN_COMB_FILTER

class CombFilterGen[SRC_T <: Data, DST_T <: Data](
  fil: TFilter[SRC_T, DST_T]
) {
  def apply(gen_sel: GEN_COMB_FILTER.type): () => TCombFilter[SRC_T, DST_T] = {
    fil match {
      case brick: FilterBrick[SRC_T, DST_T] => {
        () => {new CombFilterBrick[SRC_T, DST_T](brick.f)}
      }
      case cons: FilterCons[SRC_T, _, DST_T] => {
        cons match {
  	    case FilterCons(l: TFilter[SRC_T, cons.MID_T_], r: TFilter[cons.MID_T_, DST_T]) => 
  	      () => {
  	        new CombFilterCons[SRC_T, cons.MID_T_, DST_T](
  	          () => {new CombFilterGen(l)(gen_sel)()},
  	          () => {new CombFilterGen(r)(gen_sel)()}
  	        )
  	      }
  	    }
      }
    }
  }
}

trait Dontcouple_CombFilter_Context {
  implicit def lift[SRC_T <: Data, DST_T <: Data](fil: TFilter[SRC_T, DST_T]) = {
    new CombFilterGen(fil)
  }
}
