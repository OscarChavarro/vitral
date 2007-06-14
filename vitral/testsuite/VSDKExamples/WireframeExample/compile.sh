mkdir classes &> /dev/null
cd src
javac -Xlint:unchecked -classpath .:../../../../lib/vsdk.jar -d ../classes WireframeExample.java
cd ..
