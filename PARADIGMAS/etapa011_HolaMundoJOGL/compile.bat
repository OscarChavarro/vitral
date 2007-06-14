cls
cd src
javac -classpath .;../pkgs/jogl.jar;../pkgs/jogl-natives-win32.jar -d ../classes HelloWorldJOGL.java
javadoc -classpath .;../pkgs/jogl.jar;../pkgs/jogl-natives-win32.jar -d ../doc HelloWorldJOGL.java
cd ..
