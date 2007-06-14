LIBS=../../../lib/vitral.jar

all: RaytracerSimple.class

RaytracerSimple.class: RaytracerSimple.java
	javac -classpath $(LIBS):. RaytracerSimple.java

run:
	@rm -f salida.ppm
	@java -classpath $(LIBS):. RaytracerSimple $*

clean:
	@rm -rf `find . -name "*~"` salida.ppm
