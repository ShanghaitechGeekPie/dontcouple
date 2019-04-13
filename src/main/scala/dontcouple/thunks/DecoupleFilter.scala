
package dontcouple.thunks

import chisel3._
import chisel3.util._

import dontcouple.ops._

abstract class TDecoupledFilter[SRC_T <: Data, DST_T <: Data] extends Module{
  this: Module =>
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
  val sRecv :: sSend :: Nil = Enum(2)
  val state = RegInit(sRecv)
  val o_reg = RegInit(0.U.asTypeOf(f.dst))
  io.o.bits := o_reg

  val send_o: DecoupledTX[DST_T] = new DecoupledTX(io.o,
    on_done = () => {
      state := sRecv
      recv_i.kick()
    }
  )

  val recv_i: DecoupledRX[SRC_T] = new DecoupledRX(io.i,
    on_recv = (x: SRC_T) => {
      o_reg := f.f(x)
    },
    on_done = () => {
      state := sSend
      send_o.kick()
    }
  )
  
  when (state === sRecv) {
    recv_i()
  }.elsewhen(state === sSend) {
    send_o()
  }
}

case class DecoupledFilterGen[SRC_T <: Data, DST_T <: Data](gen: () => TDecoupledFilter[SRC_T, DST_T]) {
  def apply() = {
    gen()
  }
  def <>[NXT_T <: Data](
    gen_r: DecoupledFilterGen[DST_T, NXT_T]
  ): DecoupledFilterGen[SRC_T, NXT_T] = {
    val gen_l = this
    new DecoupledFilterGen(() => {new DecoupledFilterCons(gen_l, gen_r)})
  }
}
object DecoupledFilterGen {
  implicit def lift[SRC_T <: Data, DST_T <: Data](tup: (SRC_T, DST_T, SRC_T => DST_T)): DecoupledFilterGen[SRC_T, DST_T] = {
    new DecoupledFilterGen(() => {new DecoupledFilterBrick(new FilterFunction(tup._1, tup._2, tup._3))})
  }
}

class DecoupledFilterCons[SRC_T <: Data, MID_T <: Data, DST_T <: Data](
  gen_l: DecoupledFilterGen[SRC_T, MID_T],
  gen_r: DecoupledFilterGen[MID_T, DST_T]
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

trait Dontcouple_DecoupledContext {
  implicit def lift[SRC_T <: Data, DST_T <: Data](tup: (SRC_T, DST_T, SRC_T => DST_T)): DecoupledFilterGen[SRC_T, DST_T] = {
    new DecoupledFilterGen(() => {new DecoupledFilterBrick(new FilterFunction(tup._1, tup._2, tup._3))})
  }
}

