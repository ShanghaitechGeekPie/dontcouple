
package dontcouple.ops

import chisel3._
import chisel3.util._

object SoManyMultiOp {
  def apply(
    op_group_size_max: Int,
    eles_in: List[SimpleOp],
    on_kick: () => Unit = () => (),
    on_done: () => Unit = () => ()
  ): MultiOp = {
    if(eles_in.size <= op_group_size_max) {
      new ManyMultiOp(eles_in, on_kick = on_kick, on_done = on_done)
    } else {
      SoManyMultiOp(
        op_group_size_max,
        eles_in.grouped(op_group_size_max).toList.map(
          (eles_subgrp: List[SimpleOp]) => {
            new ManyMultiOp(eles_subgrp, on_kick = on_kick, on_done = on_done)
          }
        ),
        on_kick = on_kick, on_done = on_done
      )
    }
  }
}
