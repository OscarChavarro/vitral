rm -f output.jpg
java -classpath ./classes:../../../lib/vsdk.jar PbufferExample
if [ -d ./output.jpg ]; then
    display output.jpg
fi

