############################################################################
############################################################################
##
##		Spherical Harmonic Transform Kit 2.5
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

CC = gcc

# define WALLCLOCK on the CFLAGS line if want to time
# walltime and not cpu time (cpu time is default); also
# define optimization flags suitable for your platform

# CFLAGS = -O3 -fp_reorder -DWALLCLOCK
CFLAGS = -O2 -ansi -Wall -I./src
LDFLAGS = -lm

# for naive
NAIVESRC = src/naive_synthesis.c src/weights.c src/csecond.c

NAIVEOBJ = tmp/naive_synthesis.o tmp/weights.o tmp/csecond.o


# for semi-naive
SEMISRC = src/OURperms.c src/OURmods.c src/newFCT.c src/weights.c \
	src/cospmls.c src/seminaive.c src/oddweights.c \
	src/csecond.c src/primitive.c

SEMIOBJ = tmp/OURperms.o tmp/OURmods.o tmp/newFCT.o tmp/weights.o \
	tmp/cospmls.o tmp/seminaive.o tmp/oddweights.o \
	tmp/csecond.o tmp/primitive.o

# seminaive spherical transform and convolution
FSTSEMISRC = $(SEMISRC) src/naive_synthesis.c \
	src/primitive_FST.c src/permroots.c src/indextables.c \
	src/FFTcode.c src/fft_grids.c

FSTSEMIOBJ = $(SEMIOBJ) tmp/naive_synthesis.o \
	tmp/primitive_FST.o tmp/permroots.o tmp/indextables.o tmp/FFTcode.o \
	tmp/fft_grids.o

# hybrid/seminaive spherical transform and convolution
FSTHYBRIDSRC =  $(FLTHYBRIDSRC) \
	  src/primitive_FST.c src/permroots.c src/indextables.c src/FFTcode.c \
	  src/fft_grids.c

FSTHYBRIDOBJ = $(FLTHYBRIDOBJ) \
	  tmp/primitive_FST.o tmp/permroots.o tmp/indextables.o tmp/FFTcode.o \
	  tmp/fft_grids.o

FSTHYBRIDXSRC =  $(FLTHYBRIDSRC) \
	  src/primitive_FST.c src/permroots.c src/indextables.c src/FFTcode.c \
	  src/fft_grids.c

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

all: bin/generateSphericalHarmonics

clean: 
	@rm -f tmp/*.o bin/generateSphericalHarmonics `find . -name "*~"`

bin/generateSphericalHarmonics: $(FSTSEMIOBJ) tmp/FST_semi_disk.o tmp/generateSphericalHarmonics.o
	$(CC) $(CFLAGS) $(LDFLAGS) -o bin/generateSphericalHarmonics $(FSTSEMIOBJ) tmp/FST_semi_disk.o tmp/generateSphericalHarmonics.o

tmp/FFTcode.o: src/FFTcode.c src/indextables.h src/permroots.h
	$(CC) $(CFLAGS) -o tmp/FFTcode.o -c src/FFTcode.c

tmp/FST_semi_disk.o: src/FST_semi_disk.c src/FFTcode.h src/config.h src/cospmls.h src/naive_synthesis.h src/primitive.h src/primitive_FST.h src/seminaive.h
	$(CC) $(CFLAGS) -o tmp/FST_semi_disk.o -c src/FST_semi_disk.c

tmp/OURmods.o: src/OURmods.c
	$(CC) $(CFLAGS) -o tmp/OURmods.o -c src/OURmods.c

tmp/OURperms.o: src/OURperms.c
	$(CC) $(CFLAGS) -o tmp/OURperms.o -c src/OURperms.c

tmp/cospmls.o: src/cospmls.c src/newFCT.h src/primitive.h
	$(CC) $(CFLAGS) -o tmp/cospmls.o -c src/cospmls.c

tmp/csecond.o: src/csecond.c
	$(CC) $(CFLAGS) -o tmp/csecond.o -c src/csecond.c

tmp/flt_hybrid.o: src/flt_hybrid.c src/csecond.h src/cospmls.h src/newFCT.h src/oddweights.h src/seminaive.h src/weights.h
	$(CC) $(CFLAGS) -o tmp/flt_hybrid.o -c src/flt_hybrid.c

tmp/fft_grids.o: src/fft_grids.c src/fft_grids.h
	$(CC) $(CFLAGS) -o tmp/fft_grids.o -c src/fft_grids.c

tmp/indextables.o: src/indextables.c
	$(CC) $(CFLAGS) -o tmp/indextables.o -c src/indextables.c

tmp/naive_synthesis.o: src/naive_synthesis.c src/csecond.h src/weights.h
	$(CC) $(CFLAGS) -o tmp/naive_synthesis.o -c src/naive_synthesis.c

tmp/newFCT.o: src/newFCT.c src/OURperms.h src/OURmods.h src/newFCT.h
	$(CC) $(CFLAGS) -o tmp/newFCT.o -c src/newFCT.c

tmp/oddweights.o: src/oddweights.c
	$(CC) $(CFLAGS) -o tmp/oddweights.o -c src/oddweights.c

tmp/permroots.o: src/permroots.c
	$(CC) $(CFLAGS) -o tmp/permroots.o -c src/permroots.c

tmp/precomp_flt_hybrid.o: src/precomp_flt_hybrid.c src/cospmls.h src/newFCT.h src/primitive.h
	$(CC) $(CFLAGS) -o tmp/precomp_flt_hybrid.o -c src/precomp_flt_hybrid.c

tmp/primitive.o: src/primitive.c
	$(CC) $(CFLAGS) -o tmp/primitive.o -c src/primitive.c

tmp/primitive_FST.o: src/primitive_FST.c
	$(CC) $(CFLAGS) -o tmp/primitive_FST.o -c src/primitive_FST.c

tmp/seminaive.o: src/seminaive.c src/cospmls.h src/csecond.h src/newFCT.h src/oddweights.h src/weights.h
	$(CC) $(CFLAGS) -o tmp/seminaive.o -c src/seminaive.c

tmp/weights.o: src/weights.c
	$(CC) $(CFLAGS) -o tmp/weights.o -c src/weights.c

tmp/generateSphericalHarmonics.o: src/generateSphericalHarmonics.c src/FST_semi_disk.h src/cospmls.h src/primitive_FST.h
	$(CC) $(CFLAGS) -o tmp/generateSphericalHarmonics.o -c src/generateSphericalHarmonics.c
