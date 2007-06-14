mkdir classes &> /dev/null
cd src
javac -Xlint:unchecked -classpath .:../../../../lib/vsdk.jar -d ../classes GlyphExample.java
cd ..
