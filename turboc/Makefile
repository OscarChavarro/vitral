# Build of the turboc port with GNU make only, for legacy systems without
# CMake (e.g. Debian 1.1 with gcc 2.7.2): the base library and the offline
# examples, which need no window system. See base/Makefile and
# testsuite/OfflineExamples/Makefile for the options, i.e.
#   make WITH_POSIX_THREADS=1
# The CMake build (scripts/compile.sh) remains the one for the full port.

all:
	cd base && $(MAKE)
	cd testsuite/OfflineExamples && $(MAKE)

clean:
	cd base && $(MAKE) clean
	cd testsuite/OfflineExamples && $(MAKE) clean

.PHONY: all clean
