#===========================================================================

CCO = gcc -std=c99 -m32 -O2 -fPIC -Wall -pedantic -I../myLibraryNative/src -L../myLibraryNative/lib

all: nativeProgram

nativeProgram: src/nativeProgram.c
	$(CCO) -o nativeProgram src/nativeProgram.c -lmyLibraryNative
clean:
	@rm -f tmp/* lib/* `find . -name "*~"`

#===========================================================================
#= EOF                                                                     =
#===========================================================================
