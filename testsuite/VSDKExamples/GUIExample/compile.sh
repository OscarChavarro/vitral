#!/bin/sh

if [ ! -d ./classes ]; then
    mkdir ./classes
fi

cd src
javac -Xlint:deprecation -Xlint:unchecked -classpath .:../../../../lib/vsdk.jar:../../../ApplicationCases/SceneEditorApplication/lib/vsdk_transition.jar -d ../classes application/GUIExample.java
cd ..
