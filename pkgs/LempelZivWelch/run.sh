#!/bin/sh
#===========================================================================
#= Builds the LZW library and runs its Java test program over a file       =
#= compressed with the stand alone `compress` program.                     =
#===========================================================================
set -e

cmake -S . -B build/cmake
cmake --build build/cmake -j

rm -f main.java main.java.Z
cp src/main.java ./main.java
./build/compress main.java

mkdir -p build/classes
javac -proc:none \
    -classpath ../../java/base/build/classes/java/main \
    -d build/classes \
    src/main.java

java -classpath build/classes:../../java/base/build/classes/java/main main
