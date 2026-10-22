# Makefile of the API tests (GLUT and OSMesa) for legacy toolchains without
# CMake (e.g. GNU make 3.75 and gcc 2.7.2 on Debian 1.1). It mirrors the
# CMakeLists.txt files of the tests: keep both in sync.
#
# Links against the static libraries of the port (base, opengl1 and glut,
# built by their own Makefiles), with the options of ../../legacy.mk. Each
# executable is written to the build/ folder of its test, so it can be run
# from there with run.sh. _OpenGL1PbufferExample renders offscreen with
# OSMesa when legacy.mk finds it (OSMESA_LIBS), else it is the GLUT variant.

VITRAL_DIR = ../..
include $(VITRAL_DIR)/legacy.mk

TOOLKIT_LIBRARIES = \
    $(VITRAL_DIR)/opengl1/build/libvitral_opengl1.a \
    $(VITRAL_DIR)/glut/build/libvitral_glut.a \
    $(BASE_LIBRARY)
LIBS = $(TOOLKIT_LIBRARIES) $(GLUT_LIBS) $(GL_LIBS) $(SYSTEM_LIBS)

CXXFLAGS = $(OPTIMIZATION) -Wall $(LEGACY_CXXFLAGS) $(EXTRA_CXXFLAGS) \
    $(BASE_INCLUDES) -I$(VITRAL_DIR)/opengl1/src/main \
    -I$(VITRAL_DIR)/glut/src/main $(GL_INCLUDES) $(X11_INCLUDES) $(DEFINES)

PROGRAMS = \
    _OpenGL1HelloWorld/build/_OpenGL1HelloWorld \
    _OpenGL1PbufferExample/build/_OpenGL1PbufferExampleGlut \
    _OpenGL1PbufferExample/build/_OpenGL1PbufferExample

PBUFFER_SOURCE = _OpenGL1PbufferExample/src/OpenGL1PbufferExample.cpp

all: $(PROGRAMS)

_OpenGL1HelloWorld/build/_OpenGL1HelloWorld: \
    buildmk/_OpenGL1HelloWorld/src/HelloWorldOpenGL1.o $(TOOLKIT_LIBRARIES)
	@mkdir -p $(dir $@)
	$(CXX) -o $@ $< $(LIBS) $(EXTRA_LIBS)

_OpenGL1PbufferExample/build/_OpenGL1PbufferExampleGlut: \
    buildmk/_OpenGL1PbufferExample/src/OpenGL1PbufferExample.o \
    $(TOOLKIT_LIBRARIES)
	@mkdir -p $(dir $@)
	$(CXX) -o $@ $< $(LIBS) $(EXTRA_LIBS)

ifneq ($(OSMESA_LIBS),)
_OpenGL1PbufferExample/build/_OpenGL1PbufferExample: \
    buildmk/_OpenGL1PbufferExample/osmesa/OpenGL1PbufferExample.o \
    $(BASE_LIBRARY)
	@mkdir -p $(dir $@)
	$(CXX) -o $@ $< $(BASE_LIBRARY) $(OSMESA_LIBS) $(SYSTEM_LIBS) \
	    $(EXTRA_LIBS)

buildmk/_OpenGL1PbufferExample/osmesa/OpenGL1PbufferExample.o: \
    $(PBUFFER_SOURCE)
	@mkdir -p $(dir $@)
	$(CXX) $(CXXFLAGS) -DVITRAL_WITH_OSMESA -c $< -o $@
else
_OpenGL1PbufferExample/build/_OpenGL1PbufferExample: \
    buildmk/_OpenGL1PbufferExample/src/OpenGL1PbufferExample.o \
    $(TOOLKIT_LIBRARIES)
	@mkdir -p $(dir $@)
	$(CXX) -o $@ $< $(LIBS) $(EXTRA_LIBS)
endif

buildmk/%.o: %.cpp
	@mkdir -p $(dir $@)
	$(CXX) $(CXXFLAGS) -c $< -o $@

clean:
	rm -rf buildmk $(PROGRAMS)

.PHONY: all clean
