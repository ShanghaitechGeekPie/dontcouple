#!/bin/bash
# Copyright (c) 2019, llk89. Licensed to public under BSD 3-Clause.
# Expecting caching /usr/local/share/verilator and /verilator.
if [ -d "./verilator/bin" ]
then
	# has cache, move stuff to where it belongs
	sudo mv ./verilator/bin/* /usr/local/bin/ && sudo mv ./verilator/pkgconfig/* /usr/local/share/pkgconfig/ && sudo mv ./verilator/verilator /usr/local/share/verilator
	exit $?
fi

# No cache, build from scratch
# As suggested by chisel3, we use 4.006 here
wget https://www.veripool.org/ftp/verilator-4.006.tgz
tar -zxvf verilator-4.006.tgz
cd verilator-4.006
# install
if ./configure && make && sudo make install
then
	# prepare the cache for save
	mkdir -p /verilator/pkgconfig/
	mv bin ../verilator
	cp /usr/local/share/pkgconfig/verilator* ../verilator/pkgconfig/
	cp /usr/local/share/verilator ../verilator/verilator/
	exit 0
else
	# something went wrong
	exit $?
fi
