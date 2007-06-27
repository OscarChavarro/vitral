#!/bin/sh

if [ ! -d ./classes ]; then
    mkdir ./classes
fi

cd src
javac -Xlint:deprecation -Xlint:unchecked -d ../classes -classpath ../../../../lib/vsdk.jar:. ./PbufferExample.java
cd ..
