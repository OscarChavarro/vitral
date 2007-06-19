#!/bin/sh

if [ ! -d ./classes ]; then
    mkdir ./classes
fi

cd src
javac -d ../classes HelloWorldJOGL.java
cd ..
