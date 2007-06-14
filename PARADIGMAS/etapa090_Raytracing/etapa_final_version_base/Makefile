all: libs
	@cd testsuite/framework/visual;make

libs: ./lib/vitral.jar

./lib/vitral.jar:
	cd src_lib;javac -Xlint:unchecked -classpath . -d ../tmp `find ./vitral -name "*.java"`
	cd tmp;jar cf ../lib/vitral.jar vitral

docs:
# Por alguna extranna razon javadoc no esta generando la documentacion para
# todas las clases...
#	@rm -rf doc/html_javadoc
#	cd src_lib;javadoc -d ../doc/html_javadoc `find . -name "*.java"`
	@rm -rf doc/html_doxygen
	doxygen ./doc/_srcdoc/doxygen/doxyfile.config

stat:
	@lineas `find . -name "*.java"`

clean:
	@rm -rf doc/html_doxygen
	@rm -rf doc/html_javadoc
	@rm -rf `find . -name "*.class"`
	@rm -rf tmp/*
	@rm -rf `find . -name "#*#"`
	@rm -rf `find . -name "*~"`
	@rm -rf salida.ppm
	@rm -rf lib/*.jar
