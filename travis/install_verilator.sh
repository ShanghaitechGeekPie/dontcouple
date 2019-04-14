#!/bin/bash
# Copyright (c) 2019, llk89. Licensed to public under BSD 3-Clause.
# Expecting caching /verilator.
# Accept a single argument to specify the verilator version.
if [ -z "$1" ]
then
	# As suggested by chisel3, we use 4.006 here
	VERILATOR_VERSION=4.006
else
	VERILATOR_VERSION=$1
fi

if [ -d "./verilator/bin" ] && [ "VERILATOR_VERSION" == "$(cat ./verilator/version)" ]
then
	# Valid cache, move stuff to where it belongs
	sudo mv ./verilator/bin/* /usr/local/bin/ && sudo mv ./verilator/pkgconfig/* /usr/local/share/pkgconfig/ && sudo mv ./verilator/verilator /usr/local/share/verilator
	exit $?
fi

# No cache or wrong version, build from scratch

# Clean the cache first, in case version changed
rm verilator -rf

# Fetch
wget "https://www.veripool.org/ftp/verilator-$VERILATOR_VERSION.tgz"
tar -zxf verilator-*.tgz
cd verilator-*

# Install
if ./configure && make && sudo make install
then
	# Prepare the cache for save
	mkdir -p ../verilator/pkgconfig/
	mkdir -p ../verilator/verilator/
	mv bin ../verilator
	cp /usr/local/share/pkgconfig/* ../verilator/pkgconfig/
	cp /usr/local/share/verilator/* ../verilator/verilator/*
	echo VERILATOR_VERSION > ../verilator/version
	exit 0
else
	# Something went wrong
	exit $?
fi
