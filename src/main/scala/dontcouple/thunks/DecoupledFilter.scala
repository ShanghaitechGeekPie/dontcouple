
package dontcouple.thunks

import chisel3._
import chisel3.util._

import dontcouple.ops._

abstract class TDecoupledFilter[SRC_T <: Data, DST_T <: Data] extends Module{

  def src: () => SRC_T
  def dst: () => DST_T
  def iport: () => DecoupledIO[SRC_T]
  def oport: () => DecoupledIO[DST_T]
}

class DecoupledFilterBrick[SRC_T <: Data, DST_T <: Data](
  f: FilterFunction[SRC_T, DST_T]
) extends TDecoupledFilter[SRC_T, DST_T] {
  val io = IO(new Bundle {
    val i = Flipped(Decoupled(f.src))
    val o = Decoupled(f.dst)
  })
  def src = () => f.get_src
  def dst = () => f.get_dst
  def iport = () => io.i
  def oport = () => io.o

  val o_reg = RegInit(0.U.asTypeOf(f.dst))
  io.o.bits := o_reg

  val send_o: DecoupledTX[DST_T] = new DecoupledTX(io.o)

  val recv_i: DecoupledRX[SRC_T] = new DecoupledRX(io.i,
    on_recv = (x: SRC_T) => {
      o_reg := f.f(x)
    }
  )
  
  val main_loop = new SerialOp(recv_i :: send_o :: Nil, rewind = true, is_compact = true)
  main_loop()
}

class DecoupledFilterCons[SRC_T <: Data, MID_T <: Data, DST_T <: Data](
  gen_l: () => TDecoupledFilter[SRC_T, MID_T],
  gen_r: () => TDecoupledFilter[MID_T, DST_T]
) extends TDecoupledFilter[SRC_T, DST_T] {
  val blk_l = Module(gen_l())
  val blk_r = Module(gen_r())
  val io = IO(new Bundle {
    val i = Flipped(Decoupled(blk_l.src()))
    val o = Decoupled(blk_r.dst())
  })
  
  def src = () => blk_l.src()
  def dst = () => blk_r.dst()
  def iport = () => io.i
  def oport = () => io.o
  
  blk_l.iport() <> io.i
  blk_l.oport() <> blk_r.iport()
  blk_r.oport() <> io.o
}

object GEN_DECOUPLED_FILTER

class DecoupledFilterGen[SRC_T <: Data, DST_T <: Data](
  fil: TFilter[SRC_T, DST_T]
) {
  def apply(gen_sel: GEN_DECOUPLED_FILTER.type): () => TDecoupledFilter[SRC_T, DST_T] = {
    fil match {
      case brick: FilterBrick[SRC_T, DST_T] => {
        () => {new DecoupledFilterBrick[SRC_T, DST_T](brick.f)}
      }
      case cons: FilterCons[SRC_T, _, DST_T] => {
        cons match {
  	    case FilterCons(l: TFilter[SRC_T, cons.MID_T_], r: TFilter[cons.MID_T_, DST_T]) => 
  	      () => {
  	        new DecoupledFilterCons[SRC_T, cons.MID_T_, DST_T](
  	          () => {new DecoupledFilterGen(l)(gen_sel)()},
  	          () => {new DecoupledFilterGen(r)(gen_sel)()}
  	        )
  	      }
  	    }
      }
    }
  }
}

trait Dontcouple_DecoupledFilter_Context {
  implicit def lift[SRC_T <: Data, DST_T <: Data](fil: TFilter[SRC_T, DST_T]) = {
    new DecoupledFilterGen(fil)
  }
}
