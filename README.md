dontcouple [![Build Status](https://travis-ci.org/ShanghaitechGeekPie/dontcouple.svg?branch=master)](https://travis-ci.org/ShanghaitechGeekPie/dontcouple)
=======================

## What it is?

A group of function-based and/or callback-styled abstraction for Chisel3.

## Progress

- Callback-styled decoupled read/write operations.
- Map one composition of one-input-one-output functions to one decoupled pipeline.
- Map one composition of one-input-one-output functions to one combinational block.

## Disadvantages?

- Bad names for the testbench files.
- Limited functions for the present.
- Increase output file size.

## Advantages?

- Zero cost in generated output.
- Low grammar noise.

## TODO

- Add support for AXI4 interface family.
- Add fold operation for stream-style input.
- Testbenches
- Concrete documentation
