############################################################################
############################################################################
##
##              Spherical Harmonic Transform Kit 2.5
##
## Sean Moore, Dennis Healy, Dan Rockmore, Peter Kostelec
## smoore@bbn.com, {healy,rockmore,geelong}@cs.dartmouth.edu
##
## Contact: Peter Kostelec
##          geelong@cs.dartmouth.edu
##
##
## Copyright 1997, 1998 Sean Moore, Dennis Healy,
##                      Dan Rockmore, Peter Kostelec
##
##
##   This program is free software; you can redistribute it and/or modify
##   it under the terms of the GNU General Public License as published by
##   the Free Software Foundation; either version 2 of the License, or
##   (at your option) any later version.
##
##   This program is distributed in the hope that it will be useful,
##   but WITHOUT ANY WARRANTY; without even the implied warranty of
##   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
##   GNU General Public License for more details.
##
##   You should have received a copy of the GNU General Public License
##   along with this program; if not, write to the Free Software
##   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
##
##
## Commercial use is absolutely prohibited.
##
## See the accompanying LICENSE file for details.
##
############################################################################
############################################################################

#===========================================================================
JDKHOME:=`sh ./scripts/locate_jdk.sh`/..

PLATFORM=linux
SRC_DIR = ./src
LIB_DIR = ../../lib
CLASSES_DIR = ./classes
TMP_DIR = ./tmp
LIBTOOL = g++ -I$(JDKHOME)/include -I$(JDKHOME)/include/$(PLATFORM)
JC = javac -proc:none -classpath ../../../lib/vsdk.jar -d ../$(CLASSES_DIR)
JH = javah -classpath ../../../lib/vsdk.jar -d ../$(TMP_DIR)

#===========================================================================

CC = gcc -DUNIX_ENABLED
CCP = g++ -DUNIX_ENABLED

# define WALLCLOCK on the CFLAGS line if want to time
# walltime and not cpu time (cpu time is default); also
# define optimization flags suitable for your platform

# CFLAGS = -O3 -fp_reorder -DWALLCLOCK
CFLAGS = -O2 -ansi -Wall -fPIC -I./src_lib -I$(JDKHOME)/include -I$(JDKHOME)/include/$(PLATFORM)
LDFLAGS = -lm

# for naive
NAIVESRC = src_lib/naive_synthesis.c src_lib/weights.c src_lib/csecond.c

NAIVEOBJ = tmp/naive_synthesis.o tmp/weights.o tmp/csecond.o


# for semi-naive
SEMISRC = src_lib/OURperms.c src_lib/OURmods.c src_lib/newFCT.c src_lib/weights.c \
	src_lib/cospmls.c src_lib/seminaive.c src_lib/oddweights.c \
	src_lib/csecond.c src_lib/primitive.c

SEMIOBJ = tmp/OURperms.o tmp/OURmods.o tmp/newFCT.o tmp/weights.o \
	tmp/cospmls.o tmp/seminaive.o tmp/oddweights.o \
	tmp/csecond.o tmp/primitive.o

# seminaive spherical transform and convolution
FSTSEMISRC = $(SEMISRC) src_lib/naive_synthesis.c \
	src_lib/primitive_FST.c src_lib/permroots.c src_lib/indextables.c \
	src_lib/FFTcode.c src_lib/fft_grids.c

FSTSEMIOBJ = $(SEMIOBJ) tmp/naive_synthesis.o \
	tmp/primitive_FST.o tmp/permroots.o tmp/indextables.o tmp/FFTcode.o \
	tmp/fft_grids.o

# hybrid/seminaive spherical transform and convolution
FSTHYBRIDSRC =  $(FLTHYBRIDSRC) \
	  src_lib/primitive_FST.c src_lib/permroots.c src_lib/indextables.c src_lib/FFTcode.c \
	  src_lib/fft_grids.c

FSTHYBRIDOBJ = $(FLTHYBRIDOBJ) \
	  tmp/primitive_FST.o tmp/permroots.o tmp/indextables.o tmp/FFTcode.o \
	  tmp/fft_grids.o

FSTHYBRIDXSRC =  $(FLTHYBRIDSRC) \
	  src_lib/primitive_FST.c src_lib/permroots.c src_lib/indextables.c src_lib/FFTcode.c \
	  src_lib/fft_grids.c

FSTHYBRIDXOBJ = $(FLTHYBRIDOBJ) \
	  tmp/primitive_FST.o tmp/permroots.o tmp/indextables.o tmp/FFTcode.o \
	  tmp/fft_grids.o

###################################################################
##################################################################
######
######              things that can be made
######
##################################################################
##################################################################

# first some shortcuts

all: check $(CLASSES_DIR)/main.class $(LIB_DIR)/libspharmonickit.so bin/generateSphericalHarmonics bin/convertFileToBynaryArray

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

clean: 
	@rm -rf $(LIB_DIR)/*.so ./classes ./bin ./tmp
	@rm -rf $(CLASSES_DIR)/*
	@rm -rf $(TMP_DIR)/*
	@rm -f bin/generateSphericalHarmonics bin/convertFileToBynaryArray `find . -name "*~"`

#===========================================================================

$(TMP_DIR)/SpharmonicKitWrapper.o: $(SRC_DIR)/SpharmonicKitWrapper.C
	cd $(CLASSES_DIR);$(JH) -jni vsdk.toolkit.processing.SpharmonicKitWrapper
	cd $(CLASSES_DIR);$(JH) vsdk.toolkit.processing.SpharmonicKitWrapper
	$(CCP) $(CFLAGS) -I$(TMP_DIR) -o $(TMP_DIR)/SpharmonicKitWrapper.o -c $(SRC_DIR)/SpharmonicKitWrapper.C

$(LIB_DIR)/libspharmonickit.so: $(TMP_DIR)/SpharmonicKitWrapper.o $(FSTSEMIOBJ) tmp/FST_semi_disk.o
	$(LIBTOOL) $(CFLAGS) $(LDFLAGS) -shared -Wl,-soname,libspharmonickit.so -o $(LIB_DIR)/libspharmonickit.so $(TMP_DIR)/SpharmonicKitWrapper.o $(FSTSEMIOBJ) tmp/FST_semi_disk.o

$(CLASSES_DIR)/main.class: $(SRC_DIR)/main.java
	cd $(SRC_DIR);$(JC) -classpath ../../../lib/vsdk.jar main.java

#===========================================================================

bin/generateSphericalHarmonics: $(FSTSEMIOBJ) tmp/FST_semi_disk.o tmp/generateSphericalHarmonics.o
	$(LIBTOOL) $(CFLAGS) $(LDFLAGS) -o bin/generateSphericalHarmonics $(FSTSEMIOBJ) tmp/FST_semi_disk.o tmp/generateSphericalHarmonics.o

bin/convertFileToBynaryArray: tmp/convertFileToBynaryArray.o
	$(CC) $(CFLAGS) $(LDFLAGS) -o bin/convertFileToBynaryArray tmp/convertFileToBynaryArray.o

tmp/FFTcode.o: src_lib/FFTcode.c src_lib/indextables.h src_lib/permroots.h
	$(CC) $(CFLAGS) -o tmp/FFTcode.o -c src_lib/FFTcode.c

tmp/FST_semi_disk.o: src_lib/FST_semi_disk.c src_lib/FFTcode.h src_lib/config.h src_lib/cospmls.h src_lib/naive_synthesis.h src_lib/primitive.h src_lib/primitive_FST.h src_lib/seminaive.h
	$(CC) $(CFLAGS) -o tmp/FST_semi_disk.o -c src_lib/FST_semi_disk.c

tmp/OURmods.o: src_lib/OURmods.c
	$(CC) $(CFLAGS) -o tmp/OURmods.o -c src_lib/OURmods.c

tmp/OURperms.o: src_lib/OURperms.c
	$(CC) $(CFLAGS) -o tmp/OURperms.o -c src_lib/OURperms.c

tmp/cospmls.o: src_lib/cospmls.c src_lib/newFCT.h src_lib/primitive.h
	$(CC) $(CFLAGS) -o tmp/cospmls.o -c src_lib/cospmls.c

tmp/csecond.o: src_lib/csecond.c
	$(CC) $(CFLAGS) -o tmp/csecond.o -c src_lib/csecond.c

tmp/flt_hybrid.o: src_lib/flt_hybrid.c src_lib/csecond.h src_lib/cospmls.h src_lib/newFCT.h src_lib/oddweights.h src_lib/seminaive.h src_lib/weights.h
	$(CC) $(CFLAGS) -o tmp/flt_hybrid.o -c src_lib/flt_hybrid.c

tmp/fft_grids.o: src_lib/fft_grids.c src_lib/fft_grids.h
	$(CC) $(CFLAGS) -o tmp/fft_grids.o -c src_lib/fft_grids.c

tmp/indextables.o: src_lib/indextables.c
	$(CC) $(CFLAGS) -o tmp/indextables.o -c src_lib/indextables.c

tmp/naive_synthesis.o: src_lib/naive_synthesis.c src_lib/csecond.h src_lib/weights.h
	$(CC) $(CFLAGS) -o tmp/naive_synthesis.o -c src_lib/naive_synthesis.c

tmp/newFCT.o: src_lib/newFCT.c src_lib/OURperms.h src_lib/OURmods.h src_lib/newFCT.h
	$(CC) $(CFLAGS) -o tmp/newFCT.o -c src_lib/newFCT.c

tmp/oddweights.o: src_lib/oddweights.c
	$(CC) $(CFLAGS) -o tmp/oddweights.o -c src_lib/oddweights.c

tmp/permroots.o: src_lib/permroots.c
	$(CC) $(CFLAGS) -o tmp/permroots.o -c src_lib/permroots.c

tmp/precomp_flt_hybrid.o: src_lib/precomp_flt_hybrid.c src_lib/cospmls.h src_lib/newFCT.h src_lib/primitive.h
	$(CC) $(CFLAGS) -o tmp/precomp_flt_hybrid.o -c src_lib/precomp_flt_hybrid.c

tmp/primitive.o: src_lib/primitive.c
	$(CC) $(CFLAGS) -o tmp/primitive.o -c src_lib/primitive.c

tmp/primitive_FST.o: src_lib/primitive_FST.c
	$(CC) $(CFLAGS) -o tmp/primitive_FST.o -c src_lib/primitive_FST.c

tmp/seminaive.o: src_lib/seminaive.c src_lib/cospmls.h src_lib/csecond.h src_lib/newFCT.h src_lib/oddweights.h src_lib/weights.h
	$(CC) $(CFLAGS) -o tmp/seminaive.o -c src_lib/seminaive.c

tmp/weights.o: src_lib/weights.c
	$(CC) $(CFLAGS) -o tmp/weights.o -c src_lib/weights.c

tmp/generateSphericalHarmonics.o: src/generateSphericalHarmonics.c src_lib/FST_semi_disk.h src_lib/cospmls.h src_lib/primitive_FST.h
	$(CCP) $(CFLAGS) -o tmp/generateSphericalHarmonics.o -c src/generateSphericalHarmonics.c

tmp/convertFileToBynaryArray.o: src/convertFileToBynaryArray.c
	$(CC) $(CFLAGS) -o tmp/convertFileToBynaryArray.o -c src/convertFileToBynaryArray.c
