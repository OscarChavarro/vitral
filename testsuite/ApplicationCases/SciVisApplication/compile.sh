#!/bin/sh

if [ ! -d ./classes ]; then
    mkdir ./classes
fi

cd src
javac -Xlint:unchecked -classpath .:../../../../lib/vsdk.jar:../../SceneEditorApplication/lib/vsdk_transition.jar -d ../classes SciVisApplication.java
cd ..
