#===========================================================================

JDKHOME:=`sh ./scripts/locate_jdk.sh`/..
# Values for PLATFORM: "linux" or "darwin"
PLATFORM=`uname -a | awk '{print $1}' | tr '[:upper:]' '[:lower:]'`
LIB_DIR=../../lib
CLASSES_DIR = ./classes
SRC_DIR=./src
TMP_DIR=./tmp
#CCO=gcc -O4 -ansi -Wall -pedantic -I./src_lib
CCO=gcc -std=c99 -m32 -fPIC -Wall -pedantic -I./src_lib -I$(JDKHOME)/include -I$(JDKHOME)/include/$(PLATFORM)
JC = javac -proc:none -classpath ../../../lib/vsdk.jar:../$(CLASSES_DIR) -d ../$(CLASSES_DIR)
JH = javah -classpath ../../../lib/vsdk.jar -d ../$(TMP_DIR)

DLL_EXTENSION="so"
ifeq ($(PLATFORM), "darwin") 
    DLL_EXTENSION="dylib"
endif

all: check ./bin/compress ./bin/uncompress $(LIB_DIR)/libLZW.$(DLL_EXTENSION) $(CLASSES_DIR)/main.class

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

$(CLASSES_DIR)/main.class: $(SRC_DIR)/main.java
	cd $(SRC_DIR);$(JC) main.java

# The Linux Way
$(LIB_DIR)/libLZW.so: ./tmp/compress.o ./tmp/LzwWrapper.o
	$(CCO) -shared -Wl,-soname,libLZW.so.1 -o $(LIB_DIR)/libLZW.so.1 ./tmp/compress.o ./tmp/LzwWrapper.o
	@cd $(LIB_DIR);rm -f ./libLZW.so;ln -s ./libLZW.so.1 ./libLZW.so

# The MacOSX/Darwin Way
$(LIB_DIR)/libLZW.dylib: ./tmp/compress.o ./tmp/LzwWrapper.o
	$(CCO) -dynamiclib -o $(LIB_DIR)/libLZW.dylib ./tmp/compress.o ./tmp/LzwWrapper.o
	@cd $(LIB_DIR);rm -f ./libLZW.so;ln -s ./libLZW.so.1 ./libLZW.so

./bin/compress: ./tmp/compress.o ./tmp/test.o
	$(CCO) -o ./bin/compress ./tmp/compress.o ./tmp/test.o

./bin/uncompress: ./tmp/compress.o ./tmp/test.o
	$(CCO) -o ./bin/uncompress ./tmp/compress.o ./tmp/test.o

./tmp/compress.o: ./src_lib/compress.h ./src_lib/compress.c
	$(CCO) -o ./tmp/compress.o -c ./src_lib/compress.c

./tmp/test.o: $(SRC_DIR)/test.c ./src_lib/compress.h ./src_lib/compress.c
	$(CCO) -DUNIX_ENABLED -o ./tmp/test.o -c $(SRC_DIR)/test.c

./tmp/LzwWrapper.o: $(SRC_DIR)/LzwWrapper.c ./src_lib/compress.h ./src_lib/compress.c
	@cd $(SRC_DIR);$(JH) -d ../tmp -jni vsdk.toolkit.processing.LzwWrapper
	$(CCO) -o ./tmp/LzwWrapper.o -c $(SRC_DIR)/LzwWrapper.c

clean:
	@rm -rf $(CLASSES_DIR)/*
	@rm -rf $(TMP_DIR)/*
	@rm -rf $(LIB_DIR)/*.so ./classes ./bin ./tmp
	@rm -f bin/compress bin/uncompress `find . -name "*~"`
	@rm -f main.java main.java.Z

#===========================================================================
#= EOF                                                                     =
#===========================================================================
