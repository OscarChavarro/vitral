clear
cd src_lib
javac -Xlint:unchecked -classpath .:../lib/vitral.jar -d ../classes vitral_transition/framework/visual/RaytracerMIT.java vitral_transition/framework/Universe.java vitral_transition/toolkits/media/RGBPixel.java vitral_transition/toolkits/media/RGBImage.java vitral_transition/toolkits/environment/Light2.java
cd ..
cd classes
jar cf ../lib/vitral_transition.jar vitral_transition
cd ..
