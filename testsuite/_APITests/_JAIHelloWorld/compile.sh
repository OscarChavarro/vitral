#!/bin/sh

if [ ! -d ./classes ]; then
    mkdir ./classes
fi

cd src
javac -Xlint:deprecation -Xlint:unchecked -d ../classes HelloWorldJAI.java
cd ..
