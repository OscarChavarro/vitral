clear
cd src
javac -Xlint:unchecked -classpath .:../pkgs/jogl.jar:../pkgs/jogl-natives-win32.jar -d ../classes vitral/toolkits/common/*.java vitral/toolkits/environment/*.java vitral/toolkits/geometry/*.java vitral/toolkits/gui/adapters/*.java vitral/toolkits/gui/*.java vitral/toolkits/media/*.java vitral/toolkits/util/loaders/*.java vitral/toolkits/visual/jogl/*.java
cd ..
cd classes
jar cf ../lib/vitral.jar vitral
cd ..
