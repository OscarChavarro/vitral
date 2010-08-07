md classes
md lib
cls
cd src
cd vsdk
cd toolkit
cd gui
ren J2meSystem.java J2meSystem.donotcompile
cd ..
cd ..
cd ..
javac -Xlint:unchecked -classpath . -d ..\classes vsdk\toolkit\common\*.java vsdk\toolkit\common\linealAlgebra\*.java vsdk\toolkit\environment\*.java vsdk\toolkit\environment\geometry\*.java vsdk\toolkit\environment\geometry\polyhedralBoundedSolidNodes\*.java vsdk\toolkit\gui\*.java vsdk\toolkit\media\*.java vsdk\toolkit\io\*.java vsdk\toolkit\io\metadata\*.java vsdk\toolkit\io\geometry\*.java vsdk\toolkit\io\image\*.java vsdk\toolkit\render\*.java vsdk\toolkit\render\jogl\*.java vsdk\toolkit\render\awt\*.java vsdk\toolkit\processing\*.java vsdk\framework\*.java vsdk\framework\shapeMatching\*.java
cd vsdk
cd toolkit
cd gui
ren J2meSystem.donotcompile J2meSystem.java
cd ..
cd ..
cd ..

rem javadoc -classpath .;..\pkgs\jogl.jar -d ..\doc\javadoc ????
cd ..
cd classes
jar cf ..\lib\vsdk.jar vsdk
cd ..
pause