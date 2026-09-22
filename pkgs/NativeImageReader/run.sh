#!/bin/sh
#===========================================================================
#= Builds the NativeImageReader library and runs its Java test program.    =
#===========================================================================
set -e

cmake -S . -B build/cmake
cmake --build build/cmake -j

mkdir -p build/classes
javac -proc:none \
    -classpath ../../java/base/build/classes/java/main \
    -d build/classes \
    src/testImageReader.java

java -classpath build/classes:../../java/base/build/classes/java/main \
    testImageReader
