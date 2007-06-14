mkdir classes
clear
cd src
javac -Xlint:unchecked -classpath .:../../../lib/vsdk.jar -d ../classes CameraExample.java
cd ..
