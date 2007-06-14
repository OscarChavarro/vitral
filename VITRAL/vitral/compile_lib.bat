md classes
md lib
cls
cd src
javac -Xlint:unchecked -classpath . -d ../classes vsdk\toolkit\common\*.java vsdk\toolkit\environment\*.java vsdk\toolkit\environment\geometry\*.java vsdk\toolkit\gui\adapters\*.java vsdk\toolkit\gui\*.java vsdk\toolkit\media\*.java vsdk\toolkit\io\*.java vsdk\toolkit\io\geometry\*.java vsdk\toolkit\io\image\*.java vsdk\toolkit\render\*.java vsdk\toolkit\render\jogl\*.java vsdk\toolkit\render\awt\*.java
rem javadoc -classpath .;../pkgs/jogl.jar;../pkgs/jogl-natives-win32.jar -d ../doc/javadoc ????
cd ..
cd classes
jar cf ../lib/vsdk.jar vsdk
cd ..
