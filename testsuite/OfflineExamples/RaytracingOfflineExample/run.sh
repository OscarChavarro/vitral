clear
# -Xms800m -Xmx800m
rm -f output.ppm
time java -Xms300m -Xmx300m -classpath ./classes:../../../lib/vsdk.jar RaytracerSimple $@
display output.bmp
cd ..
