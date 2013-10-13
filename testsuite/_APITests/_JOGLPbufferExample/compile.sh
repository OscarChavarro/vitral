#!/bin/sh

if [ ! -d ./classes ]; then
    mkdir ./classes
fi

cd src
javac -proc:none -Xlint:deprecation -Xlint:unchecked -d ../classes -classpath ../../../../lib/vsdk.jar:. ./PbufferExample.java
cd ..
