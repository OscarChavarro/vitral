md classes
md lib
cls
cd src
javac -Xlint:unchecked -classpath .;..\lib\jogl.zip;..\lib\vsdk_frwk.jar -d ../classes vsdk\external\jogl\*.java vsdk\external\jogl\effects\*.java
cd ..
cd classes
jar cf ../lib/jogl_vsdk_frwk.jar vsdk
cd ..
pause
