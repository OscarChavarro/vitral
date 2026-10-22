# Makefile of the offline examples for legacy toolchains without CMake
# (e.g. GNU make 3.75 and gcc 2.7.2 on Debian 1.1). It mirrors the
# CMakeLists.txt files of the examples: keep both in sync.
#
# Links against the static base library (../../base/build/libvitral_base.a,
# built by `make` in ../../base), with the options of ../../legacy.mk. Each
# executable is written to the build/ folder of its example, so it can be
# run from there with run.sh.

VITRAL_DIR = ../..
include $(VITRAL_DIR)/legacy.mk

LIBS = $(BASE_LIBRARY) $(SYSTEM_LIBS)

CXXFLAGS = $(OPTIMIZATION) -Wall $(LEGACY_CXXFLAGS) $(EXTRA_CXXFLAGS) \
    $(BASE_INCLUDES) $(DEFINES)

PROGRAMS = \
    AlgebraicExpressionExample/build/AlgebraicExpressionExample \
    ImageOfflineExample/build/ImageOfflineExample \
    PolygonTriangulation/build/triangulatePolygon2D \
    Rasterizer2DExample/build/Rasterizer2DExample \
    RaytracingOfflineExample/build/RaytracingOfflineExample \
    WireframeOfflineExample/build/WireframeOfflineExample

RAYTRACING_OBJECTS = \
    buildmk/RaytracingOfflineExample/src/RaytracerSimple.o \
    buildmk/RaytracingOfflineExample/src/CommandOptionsProcessor.o \
    buildmk/RaytracingOfflineExample/src/ImageExporter.o

all: $(PROGRAMS)

AlgebraicExpressionExample/build/AlgebraicExpressionExample: \
    buildmk/AlgebraicExpressionExample/src/AlgebraicExpressionExample.o \
    $(BASE_LIBRARY)
	@mkdir -p $(dir $@)
	$(CXX) -o $@ $< $(LIBS) $(EXTRA_LIBS)

ImageOfflineExample/build/ImageOfflineExample: \
    buildmk/ImageOfflineExample/src/ImageOfflineExample.o $(BASE_LIBRARY)
	@mkdir -p $(dir $@)
	$(CXX) -o $@ $< $(LIBS) $(EXTRA_LIBS)

PolygonTriangulation/build/triangulatePolygon2D: \
    buildmk/PolygonTriangulation/src/triangulatePolygon2D.o $(BASE_LIBRARY)
	@mkdir -p $(dir $@)
	$(CXX) -o $@ $< $(LIBS) $(EXTRA_LIBS)

Rasterizer2DExample/build/Rasterizer2DExample: \
    buildmk/Rasterizer2DExample/src/Rasterizer2DExample.o $(BASE_LIBRARY)
	@mkdir -p $(dir $@)
	$(CXX) -o $@ $< $(LIBS) $(EXTRA_LIBS)

RaytracingOfflineExample/build/RaytracingOfflineExample: \
    $(RAYTRACING_OBJECTS) $(BASE_LIBRARY)
	@mkdir -p $(dir $@)
	$(CXX) -o $@ $(RAYTRACING_OBJECTS) $(LIBS) $(EXTRA_LIBS)

WireframeOfflineExample/build/WireframeOfflineExample: \
    buildmk/WireframeOfflineExample/src/WireframeOfflineExample.o \
    $(BASE_LIBRARY)
	@mkdir -p $(dir $@)
	$(CXX) -o $@ $< $(LIBS) $(EXTRA_LIBS)

buildmk/%.o: %.cpp
	@mkdir -p $(dir $@)
	$(CXX) $(CXXFLAGS) -I$(dir $<) -c $< -o $@

clean:
	rm -rf buildmk $(PROGRAMS)

.PHONY: all clean
