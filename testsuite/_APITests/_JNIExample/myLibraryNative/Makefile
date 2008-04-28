#===========================================================================

JDKHOME:=`sh ./scripts/locate_jdk.sh`/..
PLATFORM=linux
CCO = gcc -std=c99 -m32 -I$(JDKHOME)/include -I$(JDKHOME)/include/$(PLATFORM) -O2 -fPIC -Wall -pedantic

all: ./lib/libmyLibraryNative.so

./lib/libmyLibraryNative.so: ./tmp/mylibrary.o
	$(CCO) -shared -Wl,-soname,libmyLibraryNative.so.1 -o ./lib/libmyLibraryNative.so.1 ./tmp/mylibrary.o
	@cd lib;ln -s ./libmyLibraryNative.so.1 ./libmyLibraryNative.so

./tmp/mylibrary.o: ./src/mylibrary.h ./src/mylibrary.c
	$(CCO) -I../javaProgram/tmp -I./src -o ./tmp/mylibrary.o -c ./src/mylibrary.c

clean:
	@rm -f tmp/* lib/* `find . -name "*~"`

#===========================================================================
#= EOF                                                                     =
#===========================================================================
