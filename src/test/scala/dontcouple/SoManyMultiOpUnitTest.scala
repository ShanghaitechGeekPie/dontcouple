
package dontcouple

import java.io.File
import scala.collection.mutable.Queue

import chisel3._
import chisel3.util._
import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

import dontcouple.ops._
import dontcouple.thunks._

class SoManyMultiOpM extends Module {
  val io = IO(new Bundle {
    val stateprobe = Output(Bool())
    val i1 = Flipped(Decoupled(UInt(16.W)))
    val i2 = Flipped(Decoupled(UInt(16.W)))
    val i3 = Flipped(Decoupled(UInt(16.W)))
    val o = Decoupled(UInt(20.W))
  })

  val sRecv :: sSend :: Nil = Enum(2)
  val state = RegInit(sRecv)
  io.stateprobe := state === sRecv
  val i1_reg = RegInit(0.U(16.W))
  val i2_reg = RegInit(0.U(16.W))
  val i3_reg = RegInit(0.U(16.W))
  val o_reg = RegInit(0.U(20.W))
  io.o.bits := o_reg

  val send_o: DecoupledTX[UInt] = new DecoupledTX(io.o,
    on_done = () => {
      state := sRecv
      recv_i.kick()
    }
  )

  val recv_i1: DecoupledRX[UInt] = new DecoupledRX(io.i1,
    on_recv = (x: UInt) => {
      i1_reg := x
    }
  )
  val recv_i2: DecoupledRX[UInt] = new DecoupledRX(io.i2,
    on_recv = (y: UInt) => {
      i2_reg := y
    }
  )
  val recv_i3: DecoupledRX[UInt] = new DecoupledRX(io.i3,
    on_recv = (y: UInt) => {
      i3_reg := y
    }
  )
  val recv_i = SoManyMultiOp(2, recv_i1 :: recv_i2 :: recv_i3 :: Nil,
    on_done = () => {
      o_reg := i1_reg + 2.U * i2_reg + 3.U * i3_reg
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

class SoManyMultiOpUnitTester(m: SoManyMultiOpM) extends PeekPokeTester(m) {

  def fm(x: Int, y: Int, z: Int): Int = {
    x + 2 * y + 3 * z
  }

  val queue_i1 = Queue[Int]()
  val queue_i2 = Queue[Int]()
  val queue_i3 = Queue[Int]()
  val queue_o = Queue[Int]()
  for(i <- 0 to 1000) {
    val x = rnd.nextInt(10000)
    val y = rnd.nextInt(10000)
    val z = rnd.nextInt(10000)
    queue_i1 += x
    queue_i2 += y
    queue_i3 += z
    queue_o += fm(x, y, z)
  }
  
  poke(m.io.i1.bits, queue_i1(0))
  poke(m.io.i1.valid, 1)
  poke(m.io.i2.bits, queue_i2(0))
  poke(m.io.i2.valid, 1)
  poke(m.io.i2.bits, queue_i3(0))
  poke(m.io.i3.valid, 1)
  poke(m.io.o.ready, 1)
  
  while(queue_o.size > 0) {
    step(1)
    val is_r1_ready = peek(m.io.i1.ready)
    if(is_r1_ready == 1) {
      if(queue_i1.size == 0) {
        poke(m.io.i1.valid, 0)
      } else {
        poke(m.io.i1.bits, queue_i1.dequeue)
      }
    }
    val is_r2_ready = peek(m.io.i2.ready)
    if(is_r2_ready == 1) {
      if(queue_i2.size == 0) {
        poke(m.io.i2.valid, 0)
      } else {
        poke(m.io.i2.bits, queue_i2.dequeue)
      }
    }
    val is_r3_ready = peek(m.io.i3.ready)
    if(is_r3_ready == 1) {
      if(queue_i3.size == 0) {
        poke(m.io.i3.valid, 0)
      } else {
        poke(m.io.i3.bits, queue_i3.dequeue)
      }
    }
    val is_t_valid = peek(m.io.o.valid)
    if(is_t_valid == 1) {
      expect(m.io.o.bits, queue_o.dequeue)
    }
  }
  poke(m.io.i1.valid, 0)
  poke(m.io.i2.valid, 0)
  poke(m.io.i3.valid, 0)
  for(i <- 0 to 20) {
    step(1)
    val is_t_valid = peek(m.io.o.valid)
    if(is_t_valid == 1) {
      println(peek(m.io.o.bits).toString())
    }
    expect(m.io.i1.valid, 0)
    expect(m.io.i2.valid, 0)
    expect(m.io.i3.valid, 0)
    expect(m.io.i1.ready, 1)
    expect(m.io.i2.ready, 1)
    expect(m.io.i3.ready, 1)
    expect(m.io.o.valid, 0)
    expect(m.io.stateprobe, 1)
  }
  println("intended extra cycle: 20")
}

class SoManyMultiOpTester extends ChiselFlatSpec with Dontcouple_Context with Dontcouple_CombFilter_Context {

  val mgen = () => new SoManyMultiOpM
  // Disable this until we fix isCommandAvailable to swallow stderr along with stdout
  private val backendNames = if(false && firrtl.FileUtils.isCommandAvailable(Seq("verilator", "--version"))) {
    Array("firrtl", "verilator")
  }
  else {
    Array("firrtl")
  }
  for ( backendName <- backendNames ) {
    "MultiOp" should s"1) done when every sub-op done 2) output correctly (with $backendName)" in {
      Driver(() => mgen(), backendName) {
        m => new SoManyMultiOpUnitTester(m)
      } should be (true)
    }
  }

  "using --backend-name verilator" should "be an alternative way to run using verilator" in {
    if(backendNames.contains("verilator")) {
      iotesters.Driver.execute(Array("--backend-name", "verilator"), () => mgen()) {
        m => new SoManyMultiOpUnitTester(m)
      } should be(true)
    }
  }

  /**
    * By default verilator backend produces vcd file, and firrtl and treadle backends do not.
    * Following examples show you how to turn on vcd for firrtl and treadle and how to turn it off for verilator
    */

  "running with --generate-vcd-output off" should "not create a vcd file from your test" in {
    iotesters.Driver.execute(
      Array("--generate-vcd-output", "off", "--target-dir", "test_run_dir/make_no_vcd", "--top-name", "make_no_vcd",
      "--backend-name", "verilator"),
      () => mgen()
    ) {

      m => new SoManyMultiOpUnitTester(m)
    } should be(true)

    new File("test_run_dir/make_no_vcd/make_a_vcd.vcd").exists should be (false)

  }

}
