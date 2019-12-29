#===========================================================================

JDKHOME:=`sh ./scripts/locate_jdk.sh`/..
# Values for PLATFORM: "linux" or "darwin"
PLATFORM = `sh ./scripts/get_platform.sh`
LIB_DIR=../../lib
CLASSES_DIR = ./classes
SRC_DIR=./src
TMP_DIR=./tmp
# -ansi
CCO=g++ -fPIC -Wall -pedantic -I./src_lib -I$(JDKHOME)/include -I$(JDKHOME)/include/$(PLATFORM) -I/opt/local/include
LIBTOOL=g++ -fPIC -Wall -pedantic -L/opt/local/lib
JC = javac -proc:none -classpath ../../../lib/vsdk.jar:../$(CLASSES_DIR) -d ../$(CLASSES_DIR)
JH = javah -classpath ../../../lib/vsdk.jar -d ../$(TMP_DIR)

DLL_EXTENSION=so
ifeq ($(PLATFORM), "darwin") 
    DLL_EXTENSION=dylib
endif

all: check ./bin/testImageReader $(LIB_DIR)/libNativeImageReader.$(DLL_EXTENSION) $(CLASSES_DIR)/testImageReader.class

check: ./classes ./bin ./tmp
	@if [ ! -x $(JDKHOME)/bin/java ]; then                             \
	echo "ERROR: JDK not found!";                                      \
	echo "Check you have Sun Microsystems' Java implementation and";   \
	echo "edit this Makefile to modify the JDKHOME variable, or";      \
	echo "invoke make like this:";                                     \
	echo "	make JDKHOME=/path/to/jdk PLATFORM=yourplatformname";      \
	echo "where platform is linux, solaris or other (Java specific)";  \
	exit 1;                                                            \
	fi
	@if [ ! -x $(JDKHOME)/include/$(PLATFORM) ]; then                  \
	echo "ERROR: PLATFORM not configured or not found!";               \
	echo "Edit this Makefile to modify the PLATFORM variable, or";     \
	echo "invoke make like this:";                                     \
	echo "	make JDKHOME=/path/to/jdk PLATFORM=yourplatformname";      \
	echo "where platform is linux, solaris or other (Java specific)";  \
	exit 1;                                                            \
	fi

./classes:
	@mkdir classes

./bin:
	@mkdir bin

./tmp:
	@mkdir tmp

$(CLASSES_DIR)/testImageReader.class: $(SRC_DIR)/testImageReader.java
	cd $(SRC_DIR);$(JC) testImageReader.java

# The Linux way
$(LIB_DIR)/libNativeImageReader.so: ./tmp/PngReader.o ./tmp/NativeImageReaderWrapper.o
	$(CCO) -shared -Wl,-soname,libNativeImageReader.so.1 -o $(LIB_DIR)/libNativeImageReader.so.1 ./tmp/PngReader.o ./tmp/NativeImageReaderWrapper.o -lpng
	@cd $(LIB_DIR);rm -f ./libNativeImageReader.so;ln -s ./libNativeImageReader.so.1 ./libNativeImageReader.so

# The MacOSX/Darwin way
$(LIB_DIR)/libNativeImageReader.dylib: ./tmp/PngReader.o ./tmp/NativeImageReaderWrapper.o
	$(CCO) -dynamiclib -o $(LIB_DIR)/libNativeImageReader.dylib ./tmp/PngReader.o ./tmp/NativeImageReaderWrapper.o -L/opt/local/lib -lpng
	@cd $(LIB_DIR);rm -f ./libNativeImageReader.so;ln -s ./libNativeImageReader.so.1 ./libNativeImageReader.so

./tmp/PngReader.o: ./src_lib/PngReader.C ./src_lib/NativeImageReader.h
	$(CCO) -o ./tmp/PngReader.o -c ./src_lib/PngReader.C

./tmp/testImageReader.o: ./src/testImageReader.C ./src_lib/NativeImageReader.h
	$(CCO) -o ./tmp/testImageReader.o -c ./src/testImageReader.C

bin/testImageReader: ./tmp/PngReader.o ./tmp/testImageReader.o
	$(LIBTOOL) -o bin/testImageReader ./tmp/PngReader.o ./tmp/testImageReader.o -lpng

./tmp/NativeImageReaderWrapper.o: $(SRC_DIR)/NativeImageReaderWrapper.C ./src_lib/NativeImageReader.h
	@cd $(SRC_DIR);$(JH) -d ../tmp -jni vsdk.toolkit.io.image.NativeImageReaderWrapper
	$(CCO) -o ./tmp/NativeImageReaderWrapper.o -c $(SRC_DIR)/NativeImageReaderWrapper.C

clean:
	@rm -rf $(CLASSES_DIR)/*
	@rm -rf $(TMP_DIR)/*
	@rm -rf $(LIB_DIR)/*.so ./classes ./bin ./tmp
	@rm -rf `find . -name "*~"` ./tmp/*.o bin/testImageReader

#===========================================================================
#= EOF                                                                     =
#===========================================================================
