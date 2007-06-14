md classes
md lib
cls
cd src_lib
javac -Xlint:unchecked -classpath .;../lib/vitral.jar -d ../classes vitral_transition/framework/visual/RaytracerMIT.java vitral_transition/framework/Universe.java vitral_transition/toolkits/media/RGBImage.java
rem javadoc -classpath .;../pkgs/jogl.jar;../pkgs/jogl-natives-win32.jar -d ../doc/javadoc ????
cd ..
cd classes
jar cf ../lib/vitral_transition.jar vitral_transition
cd ..
