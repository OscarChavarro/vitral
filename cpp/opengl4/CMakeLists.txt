cmake_minimum_required(VERSION 3.10)
# C, for the glad loader
project(vitral_opengl4 C CXX)

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

add_library(vitral_opengl4 SHARED
    src/glad/src/gl.c
    src/main/vsdk/toolkit/render/opengl4/OpenGL4Loader.cpp
    src/main/vsdk/toolkit/render/opengl4/OpenGL4Renderer.cpp
    src/main/vsdk/toolkit/render/opengl4/OpenGL4ColoredPrimitiveRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/OpenGL4GeometryRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/curve/OpenGL4ParametricCurveRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/surface/OpenGL4FunctionalExplicitSurfaceRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/surface/OpenGL4InfinitePlaneRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/surface/OpenGL4ParametricBiCubicPatchRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/surface/OpenGL4QuadMeshRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/surface/OpenGL4SubdividedBezierPatchRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/surface/OpenGL4TriangleMeshGroupRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/surface/OpenGL4TriangleMeshRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/surface/OpenGL4TriangleStripMeshRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/volume/OpenGL4BoxRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/volume/OpenGL4ConeRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/volume/OpenGL4HeightFieldSolidRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/volume/OpenGL4ImplicitSolidRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/volume/OpenGL4PlanarHalfSpaceRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/volume/OpenGL4QuadricSurfaceRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/volume/OpenGL4TorusRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/volume/OpenGL4TubeRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/volume/OpenGL4PrismRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/volume/OpenGL4VolumeRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/volume/_OpenGL4SolidBoundaryMeshBuilder.cpp
    src/main/vsdk/toolkit/render/opengl4/volume/OpenGL4VoxelVolumeRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/OpenGL4SimpleMaterialRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/OpenGL4ShaderProgramUtil.cpp
    src/main/vsdk/toolkit/render/opengl4/volume/polyhedralBoundedSolid/OpenGL4PolyhedralBoundedSolidRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/volume/polyhedralBoundedSolid/OpenGL4PolyhedralBoundedSolidDebugRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/volume/polyhedralBoundedSolid/OpenGL4PolyhedralBoundedSolidDebugHUDRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/OpenGL4LabelCanvas.cpp
    src/main/vsdk/toolkit/render/opengl4/OpenGL4MeshBuilder.cpp
    src/main/vsdk/toolkit/render/opengl4/OpenGL4MeshRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/OpenGL4RendererConfigurationShaderSelector.cpp
    src/main/vsdk/toolkit/render/opengl4/OpenGL4BackgroundRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/OpenGL4FrameBufferReader.cpp
    src/main/vsdk/toolkit/render/opengl4/OpenGL4MinMaxRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/OpenGL4SelectionCornersRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/OpenGL4ViewportWindow.cpp
    src/main/vsdk/toolkit/render/opengl4/OpenGL4ViewportSetRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/OpenGL4LineRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/OpenGL4CameraRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/OpenGL4MatrixRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/OpenGL4ImageRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/OpenGL4SolidTextureRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/OpenGL4ColorDepthImageRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/OpenGL4RGBImageUncompressedRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/OpenGL4RGBAImageUncompressedRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/OpenGL4RGBAImageCompressedRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/OpenGL4LightRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/volume/OpenGL4ArrowRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/gizmo/OpenGL4InputGizmoRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/gizmo/OpenGL4RayGizmoRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/gizmo/OpenGL4TranslateGizmoRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/gizmo/OpenGL4RotateGizmoRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/gizmo/OpenGL4CameraControlGizmoRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/gizmo/OpenGL4ScaleGizmoRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/surface/polygon/OpenGL4Polygon2DRenderer.cpp
    src/main/vsdk/toolkit/fixtures/OpenGL4SimpleCorridorSample.cpp
    src/main/vsdk/toolkit/render/opengl4/volume/OpenGL4SphereRenderer.cpp
)

target_include_directories(vitral_opengl4 PUBLIC src/main)
target_include_directories(vitral_opengl4 PRIVATE ${CMAKE_CURRENT_LIST_DIR}/../base/src/main)
# The OpenGL functions are loaded by glad (see src/glad/README.md), once the
# application has a context (see OpenGL4Loader)
target_include_directories(vitral_opengl4 SYSTEM PUBLIC src/glad/include)
set_target_properties(vitral_opengl4 PROPERTIES POSITION_INDEPENDENT_CODE ON)
target_link_libraries(vitral_opengl4 PUBLIC OpenGL::GL OpenGL::GLU)
target_link_libraries(vitral_opengl4 PRIVATE vitral_base)

if(APPLE)
    target_compile_definitions(vitral_opengl4 PRIVATE GL_SILENCE_DEPRECATION)
endif()
