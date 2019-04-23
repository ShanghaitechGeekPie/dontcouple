
package dontcouple

import java.io.File
import scala.collection.mutable.Queue

import chisel3._
import chisel3.util._
import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

import dontcouple.ops._
import dontcouple.thunks._

class RepeatOpM extends Module {
  val io = IO(new Bundle {
    val i = Flipped(Decoupled(UInt(3.W)))
    val o = Decoupled(UInt(1.W))
    val cnt_ref = Output(UInt(3.W))
    val recved = Output(Bool())
  })
  val cnt_ref = RegInit(0.U)
  io.cnt_ref := cnt_ref
  val t = RegInit(0.U(3.W))
  val recved = RegInit(false.B)
  io.recved := recved
  
  val recv_t: DecoupledRX[UInt] = new DecoupledRX(io.i,
    on_recv = (x: UInt) => {
      t := x
      cnt_ref := x
      recved := true.B
    }
  )
  io.o.bits := 0.U
  
  val send_o: DecoupledTX[UInt] = new DecoupledTX(io.o)
  val repeat_send = new RepeatOp(send_o, t, is_compact = true)
  val main_loop = new SerialOp(recv_t :: repeat_send :: Nil, rewind = true, is_compact = false)
  main_loop()

}

class RepeatOpUnitTester(m: RepeatOpM) extends PeekPokeTester(m) {

  var n = 7

  poke(m.io.i.bits, 0)
  poke(m.io.i.valid, 0)
  poke(m.io.o.ready, 0)
  step(50)
  poke(m.io.i.bits, n)
  poke(m.io.i.valid, 1)
  poke(m.io.o.ready, 1)
  step(1)
  poke(m.io.i.valid, 0)
  while(peek(m.io.o.valid) == 0) {
    expect(m.io.i.ready, 0)
    expect(m.io.recved, 1)
    expect(m.io.i.ready, 0)
    expect(m.io.cnt_ref, n)
    step(1)
  }
  for(i <- 1 to n) {
    expect(m.io.recved, 1)
    expect(m.io.cnt_ref, n)
    expect(m.io.o.valid, 1)
    step(1)
  }
  for(i <- 1 to 20) {
    expect(m.io.o.valid, 0)
    step(1)
  }
  
  n = 3

  poke(m.io.i.bits, 0)
  poke(m.io.i.valid, 0)
  poke(m.io.o.ready, 0)
  step(50)
  poke(m.io.i.bits, n)
  poke(m.io.i.valid, 1)
  poke(m.io.o.ready, 1)
  step(1)
  poke(m.io.i.valid, 0)
  while(peek(m.io.o.valid) == 0) {
    expect(m.io.i.ready, 0)
    expect(m.io.recved, 1)
    expect(m.io.i.ready, 0)
    expect(m.io.cnt_ref, n)
    step(1)
  }
  for(i <- 1 to n) {
    expect(m.io.recved, 1)
    expect(m.io.cnt_ref, n)
    expect(m.io.o.valid, 1)
    step(1)
  }
  for(i <- 1 to 20) {
    expect(m.io.o.valid, 0)
    step(1)
  }

}

class RepeatOpTester extends ChiselFlatSpec with Dontcouple_Context with Dontcouple_CombFilter_Context {

  val mgen = () => new RepeatOpM()
  // Disable this until we fix isCommandAvailable to swallow stderr along with stdout
  private val backendNames = if(false && firrtl.FileUtils.isCommandAvailable(Seq("verilator", "--version"))) {
    Array("firrtl", "verilator")
  }
  else {
    Array("firrtl")
  }
  for ( backendName <- backendNames ) {
    "RepeatOp" should s"1) do nothing when not called 2) repeat for correct times 3) do nothing until next call (with $backendName)" in {
      Driver(() => mgen(), backendName) {
        m => new RepeatOpUnitTester(m)
      } should be (true)
    }
  }

  "using --backend-name verilator" should "be an alternative way to run using verilator" in {
    if(backendNames.contains("verilator")) {
      iotesters.Driver.execute(Array("--backend-name", "verilator"), () => mgen()) {
        m => new RepeatOpUnitTester(m)
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

      m => new RepeatOpUnitTester(m)
    } should be(true)

    new File("test_run_dir/make_no_vcd/make_a_vcd.vcd").exists should be (false)

  }

}
