make
rm -f main.java main.java.Z
cp src/main.java ./main.java
./bin/compress main.java
java -Djava.library.path="../../lib" -classpath ./classes:../../lib/vsdk.jar main
