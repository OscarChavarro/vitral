md classes
cls
cd src
javac -Xlint:unchecked -classpath .;..\lib\vsdk_frwk.jar;..\lib\jogl_vsdk_frwk.jar;..\lib\jogl.zip -d ../classes test\vsdk\toolkit\environment\geometry\mesh\TestMesh.java
cd ..
pause