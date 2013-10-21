#!/bin/sh

if [ ! -d ./classes ]; then
    mkdir ./classes
fi

if [ ! -d ./lib ]; then
    mkdir ./lib
fi

cd src_lib
javac -proc:none -Xlint:deprecation -Xlint:unchecked -classpath .:../../../../lib/vsdk.jar -d ../lib vsdk/toolkit/gui/*.java vsdk/toolkit/io/gui/*.java vsdk/toolkit/render/swing/*.java
cd ..
cd lib
jar cf vsdk_transition.jar vsdk
cd ..
