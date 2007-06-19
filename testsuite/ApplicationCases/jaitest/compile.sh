#!/bin/sh

if [ ! -d ./classes ]; then
    mkdir ./classes
fi

cd src
javac -Xlint:unchecked -classpath .:../../../../lib/vsdk.jar -d ../classes jaitest.java
cd ..
