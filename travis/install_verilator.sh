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

echo Expected verilator $VERILATOR_VERSION

if [ -d "./verilator/bin" ] && [ "VERILATOR_VERSION" == "$(cat ./verilator/version)" ]
then
	echo Valid cache found, moving stuff to where it belongs
	sudo mkdir -vp /usr/local/share/pkgconfig/ /usr/local/share/verilator/ 
	sudo cp ./verilator/bin/* /usr/local/bin/ && sudo cp ./verilator/pkgconfig/* /usr/local/share/pkgconfig/ && sudo cp -r ./verilator/verilator/* /usr/local/share/verilator/
	exit $?
fi

echo No cache or wrong version, rebuilding from scratch

# Clean the cache first, in case version changed
rm verilator -rf

# Fetch
wget "https://www.veripool.org/ftp/verilator-$VERILATOR_VERSION.tgz"
echo Unpacking source tarball...
tar -zxf verilator-*.tgz
cd verilator-*
echo Installing...
# Install
if ./configure && make && sudo make install
then
	# Prepare the cache for save
	echo Built. Creating cache...
	mkdir -vp ../verilator/pkgconfig/ ../verilator/verilator/
	echo Moving bin...
	mv bin ../verilator/bin
	echo Moving pkgconfig...
	cp /usr/local/share/pkgconfig/* ../verilator/pkgconfig/
	echo Moving include...
	cp -r /usr/local/share/verilator/* ../verilator/verilator/
	echo Writing version number to cache...
	echo VERILATOR_VERSION > ../verilator/version
	exit 0
else
	# Something went wrong
	ERR_CODE=$?
	echo Build returned with error code $ERR_CODE. Aborting...
	exit $?
fi
