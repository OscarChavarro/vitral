rm -f output.jpg
java -classpath ./classes:../../../lib/vsdk.jar PbufferExample
if [ -f ./output.jpg ]; then
    display output.jpg
fi

