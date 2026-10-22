# Makefile of the OpenGL 1 examples (GLUT) for legacy toolchains without
# CMake (e.g. GNU make 3.75 and gcc 2.7.2 on Debian 1.1). It mirrors the
# CMakeLists.txt files of the examples: keep both in sync.
#
# Links against the static libraries of the port (base, opengl1 and glut,
# built by their own Makefiles), with the options of ../../legacy.mk. Each
# executable is written to the build/ folder of its example, so it can be
# run from there with run.sh.

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
    CameraExample/build/CameraExample \
    ImageExample/build/ImageExample \
    MeshExample/build/MeshExample \
    PolygonClippingExample/build/PolygonClippingExample

CAMERAEXAMPLE_OBJECTS = \
    buildmk/CameraExample/src/CameraExample.o

IMAGEEXAMPLE_OBJECTS = \
    buildmk/ImageExample/src/ImageExample.o

MESHEXAMPLE_OBJECTS = \
    buildmk/MeshExample/src/MeshExample.o \
    buildmk/MeshExample/src/animation/AnimationController.o \
    buildmk/MeshExample/src/gui/MeshKeyboardInteractionTechniques.o \
    buildmk/MeshExample/src/gui/MeshMouseInteractionTechniques.o \
    buildmk/MeshExample/src/gui/TangibleInterfaceInteractionTechniques.o \
    buildmk/MeshExample/src/model/MeshModel.o \
    buildmk/MeshExample/src/options/CommandLineOptions.o \
    buildmk/MeshExample/src/render/OpenGL1DebuggerRenderer.o

POLYGONCLIPPINGEXAMPLE_OBJECTS = \
    buildmk/PolygonClippingExample/src/PolygonClippingExample.o \
    buildmk/PolygonClippingExample/src/model/PolygonClippingFixtures.o \
    buildmk/PolygonClippingExample/src/options/CommandLineOptions.o \
    buildmk/PolygonClippingExample/src/render/PolygonClippingHudRenderer.o \
    buildmk/PolygonClippingExample/src/render/PolygonTriangularRenderer.o

all: $(PROGRAMS)

CameraExample/build/CameraExample: $(CAMERAEXAMPLE_OBJECTS) $(TOOLKIT_LIBRARIES)
	@mkdir -p $(dir $@)
	$(CXX) -o $@ $(CAMERAEXAMPLE_OBJECTS) $(LIBS) $(EXTRA_LIBS)

ImageExample/build/ImageExample: $(IMAGEEXAMPLE_OBJECTS) $(TOOLKIT_LIBRARIES)
	@mkdir -p $(dir $@)
	$(CXX) -o $@ $(IMAGEEXAMPLE_OBJECTS) $(LIBS) $(EXTRA_LIBS)

MeshExample/build/MeshExample: $(MESHEXAMPLE_OBJECTS) $(TOOLKIT_LIBRARIES)
	@mkdir -p $(dir $@)
	$(CXX) -o $@ $(MESHEXAMPLE_OBJECTS) $(LIBS) $(EXTRA_LIBS)

PolygonClippingExample/build/PolygonClippingExample: $(POLYGONCLIPPINGEXAMPLE_OBJECTS) $(TOOLKIT_LIBRARIES)
	@mkdir -p $(dir $@)
	$(CXX) -o $@ $(POLYGONCLIPPINGEXAMPLE_OBJECTS) $(LIBS) $(EXTRA_LIBS)

# Each example includes its sources from its src/ folder
buildmk/%.o: %.cpp
	@mkdir -p $(dir $@)
	$(CXX) $(CXXFLAGS) -I$(firstword $(subst /, ,$*))/src -c $< -o $@

clean:
	rm -rf buildmk $(PROGRAMS)

.PHONY: all clean
