mkdir classes &> /dev/null
cd src
javac -Xlint:unchecked -classpath .:../../../lib/vsdk.jar -d ../classes CameraExample.java
cd ..
