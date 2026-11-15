# Makefile of the test programs of the base library, for the legacy
# toolchains without CMake (e.g. GNU make 3.75 and gcc 2.7.2 on Debian 1.1).
# The cpp port runs its GoogleTest cases in one program; the turboc port has
# no GoogleTest, so each cpp test file is a program of its own (see
# VitralTest.h) that returns non-zero when a case fails.
#
#   make          builds the programs in build/
#   make check    builds and runs them (from this folder, so the relative
#                 paths to ../../../../etc of the tests hold)

VITRAL_DIR = ../../..
include $(VITRAL_DIR)/legacy.mk

LIBS = $(BASE_LINK) $(SYSTEM_LIBS)

CXXFLAGS = $(OPTIMIZATION) -Wall $(LEGACY_CXXFLAGS) $(EXTRA_CXXFLAGS) \
    $(BASE_INCLUDES) -I. $(DEFINES)

# Helpers shared by the test programs (as the test sources of the
# vitral_base_tests target of cpp/base/CMakeLists.txt)
HELPER_SOURCES = \
    CsgCycleTestOperand.cpp \
    CsgOpaqueTestExpression.cpp \
    CsgPolicyTestOperand.cpp \
    CsgPolicyTestTree.cpp \
    CsgTestOperand.cpp \
    GeometryWorkspaceTestWorker.cpp \
    HeightFieldTestFactory.cpp \
    ImplicitSolidTestFactory.cpp \
    RayCrossingTestCollector.cpp \
    TessellationMeshCheck.cpp \
    UncertifiedTestBody.cpp

TESTS = \
    bounding_volume_hierarchy_tests \
    certified_containment_tests \
    csg_evaluator_tests \
    csg_placement_tests \
    csg_point_workspace_tests \
    heightfield_solid_tests \
    implicit_solid_tests \
    microfaceted_material_csv_tests \
    ray_affine_contract_tests \
    ray_crossing_tests \
    reader_mit_scene_commands_tests \
    reader_mit_triangles_tests \
    refraction_tests \
    solid_boundary_tessellator_tests \
    subdivided_bezier_patch_tests \
    triangle_mesh_contract_tests

OTHER_PROGRAMS = \
    build/DitheringTest

HELPER_OBJECTS = $(patsubst %.cpp,buildmk/%.o,$(HELPER_SOURCES))
PROGRAMS = $(patsubst %,build/%,$(TESTS)) $(OTHER_PROGRAMS)

all: $(PROGRAMS)

build/%: buildmk/%.o $(HELPER_OBJECTS) $(BASE_LIBRARY)
	@mkdir -p build
	$(CXX) -o $@ $< $(HELPER_OBJECTS) $(LIBS) $(EXTRA_LIBS)

build/DitheringTest: buildmk/vsdk/toolkit/render/dithering/DitheringTest.o \
    $(BASE_LIBRARY)
	@mkdir -p build
	$(CXX) -o $@ $< $(LIBS) $(EXTRA_LIBS)

buildmk/%.o: %.cpp
	@mkdir -p $(dir $@)
	$(CXX) $(CXXFLAGS) -c $< -o $@

check: all
	@failed=0; \
	for program in $(PROGRAMS); do \
	    echo "=== $$program"; \
	    if ./$$program; then :; else failed=1; echo "*** $$program FAILED"; fi; \
	done; \
	exit $$failed

clean:
	rm -rf buildmk build

# The objects are kept (not removed as intermediate files of the programs)
.SECONDARY:

.PHONY: all check clean
