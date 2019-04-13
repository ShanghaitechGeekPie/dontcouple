// See README.md for license details.

package dontcouple

import java.io.File
import scala.collection.mutable.Queue

import chisel3._
import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

import dontcouple.thunks._

class mUnitTester(m: TDecoupledFilter[SInt, SInt]) extends PeekPokeTester(m) {
  println("Start test.")
  def fm(x: Int): Int = {
    (x * x - 35 + 1) * (x * x - 35 + 1) - 35
  }

  val queue_i = Queue[Int]()
  val queue_o = Queue[Int]()
  for(i <- 0 to 10000) {
    val x = rnd.nextInt(100000) - (100000 / 2)
    queue_i += x
    queue_o += fm(x)
  }
  
  poke(m.iport().bits, queue_i(0))
  poke(m.iport().valid, 1)
  poke(m.oport().ready, 1)
  
  while(queue_o.size > 0) {
    step(1)
    val is_r_ready = peek(m.iport().ready)
    if(is_r_ready == 1) {
      if(queue_i.size == 0) {
        poke(m.iport().valid, 0)
      } else {
        poke(m.iport().bits, queue_i.dequeue)
      }
    }
    val is_t_valid = peek(m.oport().valid)
    if(is_t_valid == 1) {
      expect(m.oport().bits, queue_o.dequeue)
    }
  }
  
  for(i <- 0 to 20) {
    step(1)
    val is_t_valid = peek(m.oport().valid)
    if(is_t_valid == 1) {
      println(peek(m.oport().bits).toString())
    }
    expect(m.oport().valid, 0)
  }
  println("intended extra cycle: 20")

}

/**
  * This is a trivial example of how to run this Specification
  * From within sbt use:
  * {{{
  * testOnly gcd.GCDTester
  * }}}
  * From a terminal shell use:
  * {{{
  * sbt 'testOnly gcd.GCDTester'
  * }}}
  */
class mTester extends ChiselFlatSpec with Dontcouple_DecoupledContext {
  val f1 = (SInt(32.W), SInt(32.W),
    (x: SInt) => {x * x - 35.S}
  )
  val f2 = (SInt(32.W), SInt(32.W),
    (x: SInt) => {x + 1.S}
  )
  val mgen = f1 <> f2 <> f1
  // Disable this until we fix isCommandAvailable to swallow stderr along with stdout
  private val backendNames = if(false && firrtl.FileUtils.isCommandAvailable(Seq("verilator", "--version"))) {
    Array("firrtl", "verilator")
  }
  else {
    Array("firrtl")
  }
  for ( backendName <- backendNames ) {
    "DecoupledFilter" should s"1) finish in finite time with finite inputs 2) on average 2 cycle per input 3) output correctly (with $backendName)" in {
      Driver(() => mgen(), backendName) {
        m => new mUnitTester(m)
      } should be (true)
    }
  }

  "using --backend-name verilator" should "be an alternative way to run using verilator" in {
    if(backendNames.contains("verilator")) {
      iotesters.Driver.execute(Array("--backend-name", "verilator"), () => mgen()) {
        m => new mUnitTester(m)
      } should be(true)
    }
  }

  "running with --is-verbose" should "show more about what's going on in your tester" in {
    iotesters.Driver.execute(Array("--is-verbose"), () => mgen()) {
      m => new mUnitTester(m)
    } should be(true)
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

      m => new mUnitTester(m)
    } should be(true)

    new File("test_run_dir/make_no_vcd/make_a_vcd.vcd").exists should be (false)

  }

}
