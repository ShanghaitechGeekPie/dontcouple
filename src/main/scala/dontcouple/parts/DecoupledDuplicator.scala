
package dontcouple.parts

import chisel3._
import chisel3.util._

import dontcouple.ops._

class DecoupledDuplicator[T <: Data](private val gen: T, n: Int = 2) extends Module {
  val io = IO(new Bundle {
    val i = Flipped(Decoupled(gen.cloneType))
    val o = Vec(n, Decoupled(gen.cloneType))
  })
  val s = RegInit(DontCare.asTypeOf(gen.cloneType))
  val recv = new DecoupledRX(io.i, on_recv = (t: T) => {
    s := t
  })
  for(ind <- 0 to (n - 1)) {
    io.o(ind).bits := s
  }
  val send = new MultiOp(io.o.map((o_port: DecoupledIO[T]) => {
    new DecoupledTX(o_port)
  }).toList)
  val mainloop = new SerialOp(recv :: send :: Nil, rewind = true, is_compact = true)
  mainloop()
}

