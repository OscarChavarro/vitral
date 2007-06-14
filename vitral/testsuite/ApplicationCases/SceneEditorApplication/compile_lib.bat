cd src_lib
javac -Xlint:unchecked -classpath .;..\..\..\..\lib\vsdk.jar -d ..\lib vsdk\transition\gui\*.java vsdk\transition\io\presentation\*.java vsdk\transition\render\swing\*.java
cd ..
cd lib
jar cf vsdk_transition.jar vsdk
cd ..
