cmake_minimum_required(VERSION 3.10)
project(vitral_opengl1 CXX)

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
    src/main/vsdk/toolkit/fixtures/OpenGL1SimpleCorridorSample.cpp
    src/main/vsdk/toolkit/render/opengl1/OpenGL1SphereRenderer.cpp
)

target_include_directories(vitral_opengl1 PUBLIC src/main)
target_include_directories(vitral_opengl1 PRIVATE ${CMAKE_CURRENT_LIST_DIR}/../base/src/main)
set_target_properties(vitral_opengl1 PROPERTIES POSITION_INDEPENDENT_CODE ON)
target_link_libraries(vitral_opengl1 PUBLIC OpenGL::GL OpenGL::GLU)
target_link_libraries(vitral_opengl1 PRIVATE vitral_base)

if(APPLE)
    target_compile_definitions(vitral_opengl1 PRIVATE GL_SILENCE_DEPRECATION)
endif()
