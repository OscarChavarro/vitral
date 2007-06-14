#!/bin/sh

if [ ! -d ./classes ]; then
    mkdir ./classes
fi

cd src
javac -d ../classes -classpath ../../../../lib/vsdk.jar:. ./PbufferExample.java
cd ..
