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
    BooleanCompositeOracleFuzz.cpp \
    BooleanOracleFuzz.cpp \
    BooleanVolumeIdentityFuzz.cpp \
    CompositeShapeOracle.cpp \
    CsgCycleTestOperand.cpp \
    CsgOpaqueTestExpression.cpp \
    CsgPolicyTestOperand.cpp \
    CsgPolicyTestTree.cpp \
    CsgSampleCorpusFixtures.cpp \
    CsgTestOperand.cpp \
    GeometryWorkspaceTestWorker.cpp \
    HeightFieldTestFactory.cpp \
    ImplicitSolidTestFactory.cpp \
    NearDegenerateBooleanFuzz.cpp \
    PolyhedralBoundedSolidTestFixtures.cpp \
    PolyhedralBoundedSolidVolumeOracle.cpp \
    PrismFixtures.cpp \
    RayCrossingTestCollector.cpp \
    SplitterOracleFuzz.cpp \
    TessellationMeshCheck.cpp \
    UncertifiedTestBody.cpp

TESTS = \
    boolean_composite_oracle_fuzz_gate_tests \
    boolean_oracle_fuzz_gate_tests \
    boolean_volume_identity_fuzz_gate_tests \
    bounding_volume_hierarchy_tests \
    camera_control_gizmo_2d_painter_tests \
    camera_control_gizmo_tests \
    certified_containment_tests \
    circular_array_list_tests \
    csg_evaluator_tests \
    csg_kurlander_bowl_full_model_tests \
    csg_placement_tests \
    csg_point_workspace_tests \
    euler_operator_sequence_fuzz_tests \
    heightfield_solid_tests \
    hidden_surface_renderer_tests \
    implicit_solid_tests \
    interior_overlap_predicate_tests \
    lamina_policy_tests \
    microfaceted_material_csv_tests \
    near_degenerate_boolean_fuzz_gate_tests \
    planar_face_tests \
    polyhedral_bounded_solid_euler_operators_tests \
    polyhedral_bounded_solid_face_geometry_tests \
    polyhedral_bounded_solid_splitter_degenerate_plane_tests \
    polyhedral_bounded_solid_structural_copy_tests \
    ray_affine_contract_tests \
    ray_crossing_tests \
    reader_mit_scene_commands_tests \
    reader_mit_triangles_tests \
    refraction_tests \
    rigid_motion_invariance_gate_tests \
    set_op_connect_no_loose_invariant_tests \
    set_op_connect_ring_invariant_tests \
    set_op_contract_tests \
    set_op_determinism_tests \
    set_op_finish_invariants_tests \
    set_op_reentrancy_tests \
    set_op_result_path_tests \
    set_op_whole_shell_tests \
    set_operand_aligner_tests \
    solid_boundary_tessellator_tests \
    splitter_general_plane_oracle_fuzz_tests \
    splitter_oracle_fuzz_tests \
    subdivided_bezier_patch_tests \
    triangle_acceleration_tests \
    triangle_mesh_contract_tests \
    tube_tests \
    validation_does_not_modify_tests

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

# The programs again, with the slow cases enabled (Java's @Tag("slow"),
# gradle :base:slowTest; see VITRAL_SLOW_TEST() in VitralTest.h)
slowcheck: all
	@failed=0; \
	for program in $(PROGRAMS); do \
	    echo "=== $$program"; \
	    if VITRAL_SLOW_TESTS=1 ./$$program; then :; else failed=1; echo "*** $$program FAILED"; fi; \
	done; \
	exit $$failed

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

.PHONY: all check slowcheck clean
