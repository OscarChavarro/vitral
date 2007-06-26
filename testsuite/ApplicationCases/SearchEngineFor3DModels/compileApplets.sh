# Create empty working directories if they does not exist
if [ ! -d ./classes ]; then
    mkdir ./classes
fi

cd srcApplets
javac -Xlint:deprecation -Xlint:unchecked -classpath . -d ../classes *.java
cp index.html ../classes
cd ..
