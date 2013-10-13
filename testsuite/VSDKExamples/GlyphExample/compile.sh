#!/bin/sh

if [ ! -d ./classes ]; then
    mkdir ./classes
fi

cd src
javac -proc:none -Xlint:deprecation -Xlint:unchecked -classpath .:../../../../lib/vsdk.jar -d ../classes GlyphExample.java
cd ..
