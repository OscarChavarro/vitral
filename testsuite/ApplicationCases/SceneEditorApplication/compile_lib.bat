md classes
md lib
cd src_lib
javac -Xlint:unchecked -classpath .;..\..\..\..\lib\vsdk.jar -d ..\lib vsdk\toolkit\gui\*.java vsdk\toolkit\io\gui\*.java vsdk\toolkit\render\swing\*.java
cd ..
cd lib
jar cf vsdk_transition.jar vsdk
cd ..
pause
