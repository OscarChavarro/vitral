cmake_minimum_required(VERSION 3.10)
project(vitral_opengl4 CXX)

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(CMAKE_LIBRARY_OUTPUT_DIRECTORY "${CMAKE_CURRENT_LIST_DIR}/build")

find_package(OpenGL REQUIRED)
find_package(GLEW REQUIRED)

add_library(vitral_opengl4 SHARED
    src/main/vsdk/toolkit/render/opengl4/OpenGL4MatrixRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/OpenGL4ImageRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/OpenGL4RGBImageUncompressedRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/OpenGL4RGBAImageUncompressedRenderer.cpp
    src/main/vsdk/toolkit/render/opengl4/OpenGL4RGBAImageCompressedRenderer.cpp
    src/main/vsdk/toolkit/fixtures/OpenGL4SimpleCorridorSample.cpp
)

target_include_directories(vitral_opengl4 PUBLIC src/main)
target_include_directories(vitral_opengl4 PRIVATE ${CMAKE_CURRENT_LIST_DIR}/../base/src/main)
target_link_libraries(vitral_opengl4 PUBLIC OpenGL::GL GLEW::GLEW)
target_link_libraries(vitral_opengl4 PRIVATE vitral_base)

if(APPLE)
    target_compile_definitions(vitral_opengl4 PRIVATE GL_SILENCE_DEPRECATION)
endif()
