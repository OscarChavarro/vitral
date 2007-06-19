#!/bin/sh

if [ ! -d ./classes ]; then
    mkdir ./classes
fi

cd src
javac -Xlint:deprecation -d ../classes HelloWorldJAI.java
cd ..
