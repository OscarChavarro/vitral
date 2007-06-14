mkdir classes &> /dev/null
cd src
javac -Xlint:unchecked -classpath .:../../../../lib/vsdk.jar -d ../classes PolyhedralBoundedSolidExample.java
cd ..
