cmake_minimum_required(VERSION 2.4)
# C is enabled because FindOpenGL/FindX11 run C checks in CMake 2.4
project(vitral_opengl1_turboc C CXX)

# Legacy C++98 port: must build with old toolchains (e.g. CMake 2.4 and
# g++ 4.1 on Ubuntu 7.04), so only CMake 2.4 commands are used here.
# Requires base/build/libvitral_base (build base first, see scripts/compile.sh)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++98 -Wall")

set(LIBRARY_OUTPUT_PATH "${CMAKE_CURRENT_SOURCE_DIR}/build")
set(VITRAL_BASE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../base")

find_package(OpenGL)
if(NOT OPENGL_FOUND)
    message(FATAL_ERROR "OpenGL library not found")
endif(NOT OPENGL_FOUND)

# OpenGL 1.2 fixed function pipeline: no shaders and no loader, the functions
# used are exported by the system OpenGL library (see OpenGL1Api.h)
set(VITRAL_OPENGL1_SOURCES
    src/main/vsdk/toolkit/render/opengl1/OpenGL1Loader.cpp
    src/main/vsdk/toolkit/render/opengl1/OpenGL1MatrixState.cpp
    src/main/vsdk/toolkit/render/opengl1/OpenGL1Renderer.cpp
    src/main/vsdk/toolkit/render/opengl1/OpenGL1ColoredPrimitiveRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/OpenGL1GeometryRenderer.cpp
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
    src/main/vsdk/toolkit/render/opengl1/OpenGL1ArrowRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/gizmo/OpenGL1InputGizmoRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/gizmo/OpenGL1RayGizmoRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/gizmo/OpenGL1TranslateGizmoRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/gizmo/OpenGL1RotateGizmoRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/gizmo/OpenGL1ScaleGizmoRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/OpenGL1Polygon2DRenderer.cpp
    src/main/vsdk/toolkit/render/opengl1/OpenGL1GluTessellator.cpp
    src/main/vsdk/toolkit/fixtures/OpenGL1SimpleCorridorSample.cpp
    src/main/vsdk/toolkit/render/opengl1/OpenGL1SphereRenderer.cpp
)

include_directories(src/main ${VITRAL_BASE_DIR}/src ${VITRAL_BASE_DIR}/src/main
    ${OPENGL_INCLUDE_DIR})
link_directories(${VITRAL_BASE_DIR}/build)

if(APPLE)
    add_definitions(-DGL_SILENCE_DEPRECATION)
endif(APPLE)

add_library(vitral_opengl1 SHARED ${VITRAL_OPENGL1_SOURCES})

target_link_libraries(vitral_opengl1 vitral_base
    ${OPENGL_glu_LIBRARY} ${OPENGL_gl_LIBRARY})
