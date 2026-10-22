# Makefile of vitral_glut (the GLUT window system binding of the toolkit) for
# legacy toolchains without CMake (e.g. GNU make 3.75 and gcc 2.7.2 on
# Debian 1.1). It mirrors CMakeLists.txt: keep both in sync.
#
# The library is built as a static archive in build/libvitral_glut.a, with
# the objects in buildmk/. The options are the ones of ../legacy.mk.

VITRAL_DIR = ..
include $(VITRAL_DIR)/legacy.mk

# As CMakeLists.txt with X11 found (it is, wherever the Makefiles are used)
CXXFLAGS = $(OPTIMIZATION) -Wall $(LEGACY_CXXFLAGS) $(EXTRA_CXXFLAGS) \
    -Isrc/main $(BASE_INCLUDES) $(GL_INCLUDES) $(X11_INCLUDES) \
    -DVITRAL_GLUT_WITH_X11 $(DEFINES)

SOURCES = \
    src/main/vsdk/toolkit/gui/GlutSystem.cpp

OBJECTS = $(patsubst %.cpp,buildmk/%.o,$(SOURCES))
LIBRARY = build/libvitral_glut.a

all: $(LIBRARY)

$(LIBRARY): $(OBJECTS)
	@mkdir -p build
	rm -f $@
	$(AR) rc $@ $(OBJECTS)
	$(RANLIB) $@

buildmk/%.o: %.cpp
	@mkdir -p $(dir $@)
	$(CXX) $(CXXFLAGS) -c $< -o $@

clean:
	rm -rf buildmk $(LIBRARY)

.PHONY: all clean
