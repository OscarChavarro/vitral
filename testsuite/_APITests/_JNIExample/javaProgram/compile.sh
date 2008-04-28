#!/bin/sh

if [ ! -d ./classes ]; then
    mkdir ./classes
fi
if [ ! -d ./tmp ]; then
    mkdir ./tmp
fi

cd src
javac -Xlint:deprecation -Xlint:unchecked -classpath . -d ../classes MyNativeInterface.java program.java
cd ..
cd classes
javah -jni -d ../tmp MyNativeInterface
cd ..
