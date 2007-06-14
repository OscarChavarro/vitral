clear
# -Xms800m -Xmx800m
time java -Xms800m -Xmx800m -classpath ./classes:../../lib/vitral.jar:../../lib/vitral_transition.jar RaytracerSimple
display salida.ppm
cd ..
