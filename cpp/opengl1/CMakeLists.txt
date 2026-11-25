cmake_minimum_required(VERSION 3.10)
project(vitral_opengl1 CXX)

# IEEE 754 floating point everywhere: no fast-math (no reassociation, no
# finite-math or reciprocal shortcuts) and no fused multiply-add contraction,
# so results do not depend on how the compiler rewrites an expression
if(CMAKE_C_COMPILER_ID MATCHES "Clang|GNU" OR CMAKE_CXX_COMPILER_ID MATCHES "Clang|GNU")
    add_compile_options(-fno-fast-math -ffp-contract=off)
endif()

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(CMAKE_LIBRARY_OUTPUT_DIRECTORY "${CMAKE_CURRENT_LIST_DIR}/build")

find_package(OpenGL REQUIRED)

# OpenGL 1.2 fixed function pipeline: no shaders and no loader, the functions
# used are exported by the system OpenGL library (see OpenGL1Api.h)
add_library(vitral_opengl1 SHARED
    src/main/vsdk/toolkit/render/opengl1/OpenGL1Loader.cpp
    src/main/vsdk/toolkit/render/opengl1/OpenGL1MatrixState.cpp
    src/main/vsdk/toolkit/render/opengl1/OpenGL1Renderer.cpp
    src/main/vsdk/toolkit/render/opengl1/OpenGL1ColoredPrimitiveRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/OpenGL1GeometryRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/curve/OpenGL1ParametricCurveRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/surface/OpenGL1FunctionalExplicitSurfaceRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/surface/OpenGL1InfinitePlaneRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/surface/OpenGL1ParametricBiCubicPatchRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/surface/OpenGL1QuadMeshRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/surface/OpenGL1SubdividedBezierPatchRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/surface/OpenGL1TriangleMeshGroupRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/surface/OpenGL1TriangleMeshRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/surface/OpenGL1TriangleStripMeshRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/volume/OpenGL1BoxRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/volume/OpenGL1ConeRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/volume/OpenGL1HeightFieldSolidRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/volume/OpenGL1ImplicitSolidRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/volume/OpenGL1PlanarHalfSpaceRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/volume/OpenGL1QuadricSurfaceRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/volume/OpenGL1TorusRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/volume/OpenGL1TubeRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/volume/OpenGL1PrismRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/volume/OpenGL1VolumeRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/volume/_OpenGL1SolidBoundaryMeshBuilder.cpp
    src/main/vsdk/toolkit/render/opengl1/volume/OpenGL1VoxelVolumeRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/volume/polyhedralBoundedSolid/OpenGL1PolyhedralBoundedSolidRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/OpenGL1MeshBuilder.cpp
    src/main/vsdk/toolkit/render/opengl1/OpenGL1MeshRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/OpenGL1RendererConfigurationStateSelector.cpp
    src/main/vsdk/toolkit/render/opengl1/OpenGL1BackgroundRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/OpenGL1FrameBufferReader.cpp
    src/main/vsdk/toolkit/render/opengl1/OpenGL1MinMaxRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/OpenGL1SelectionCornersRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/OpenGL1ViewportWindow.cpp
    src/main/vsdk/toolkit/render/opengl1/OpenGL1ViewportSetRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/OpenGL1LineRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/OpenGL1CameraRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/OpenGL1MatrixRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/OpenGL1ImageRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/OpenGL1ColorDepthImageRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/OpenGL1RGBImageUncompressedRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/OpenGL1RGBAImageUncompressedRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/OpenGL1RGBAImageCompressedRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/OpenGL1LightRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/volume/OpenGL1ArrowRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/gizmo/OpenGL1InputGizmoRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/gizmo/OpenGL1RayGizmoRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/gizmo/OpenGL1TranslateGizmoRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/gizmo/OpenGL1RotateGizmoRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/gizmo/OpenGL1CameraControlGizmoRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/gizmo/OpenGL1ScaleGizmoRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/surface/polygon/OpenGL1Polygon2DRenderer.cpp
    src/main/vsdk/toolkit/fixtures/OpenGL1SimpleCorridorSample.cpp
    src/main/vsdk/toolkit/render/opengl1/volume/OpenGL1SphereRenderer.cpp
)

target_include_directories(vitral_opengl1 PUBLIC src/main)
target_include_directories(vitral_opengl1 PRIVATE ${CMAKE_CURRENT_LIST_DIR}/../base/src/main)
set_target_properties(vitral_opengl1 PROPERTIES POSITION_INDEPENDENT_CODE ON)
target_link_libraries(vitral_opengl1 PUBLIC OpenGL::GL OpenGL::GLU)
target_link_libraries(vitral_opengl1 PRIVATE vitral_base)

if(APPLE)
    target_compile_definitions(vitral_opengl1 PRIVATE GL_SILENCE_DEPRECATION)
endif()
