
package dontcouple.thunks

import chisel3._
import chisel3.util._

import dontcouple.ops._

case class FilterFunction[SRC_T <: Data, DST_T <: Data] (src: SRC_T, dst: DST_T, f: SRC_T => DST_T) {
  def get_src = src.cloneType
  def get_dst = dst.cloneType
}

