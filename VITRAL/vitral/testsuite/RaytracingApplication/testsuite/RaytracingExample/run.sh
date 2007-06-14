clear
# -Xms800m -Xmx800m
time java -Xms800m -Xmx800m -classpath ./classes:../../../../lib/vsdk.jar:../../lib/vitral_transition.jar RaytracerSimple
display output.ppm
cd ..
