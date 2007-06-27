#!/bin/sh

if [ ! -d ./classes ]; then
    mkdir ./classes
fi

if [ ! -d ./lib ]; then
    mkdir ./lib
fi

cd src_lib
javac -Xlint:deprecation -Xlint:unchecked -classpath .:../../../../lib/vsdk.jar -d ../lib vsdk/transition/gui/*.java vsdk/transition/io/presentation/*.java vsdk/transition/render/swing/*.java
cd ..
cd lib
jar cf vsdk_transition.jar vsdk
cd ..
