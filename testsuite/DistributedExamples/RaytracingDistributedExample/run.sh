clear
# -Xms800m -Xmx800m
rm -f output.ppm output.bmp
time java -Xms300m -Xmx300m -classpath ./classes:../../../lib/vsdk.jar RaytracerDistributed $@
display output.bmp
cd ..
