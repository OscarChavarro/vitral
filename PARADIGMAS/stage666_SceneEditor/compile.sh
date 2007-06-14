#!/bin/sh
clear
cd src
javac -classpath . -d ../classes SceneEditor.java
#javadoc -classpath . -d ../doc SceneEditor.java
cd ..
