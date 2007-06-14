clear
cd src
javac -Xlint:unchecked -classpath . -d ../classes vsdk/toolkit/common/*.java vsdk/toolkit/environment/*.java vsdk/toolkit/environment/geometry/*.java vsdk/toolkit/environment/geometry/polyhedralBoundedSolidNodes/*.java vsdk/toolkit/gui/*.java vsdk/toolkit/media/*.java vsdk/toolkit/io/geometry/*.java vsdk/toolkit/io/image/*.java vsdk/toolkit/render/*.java vsdk/toolkit/render/awt/*.java
#vsdk/toolkit/render/jogl/*.java
cd ..
cd classes
jar cf ../lib/vsdk.jar vsdk
cd ..
