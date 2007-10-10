#= Create empty working directories if they does not exist =================
if [ ! -d ./classes ]; then
    mkdir ./classes
fi

#= Compile sources =========================================================
javac -Xlint:deprecation -Xlint:unchecked -sourcepath ./src -d ./classes ./src/WiiRemoteSampleApplication.java

