cmake_minimum_required(VERSION 2.4)
# C is enabled because FindOpenGL/FindX11 run C checks in CMake 2.4
project(vitral_opengl1_examples C CXX)

# Legacy C++98 port: must build with old toolchains (e.g. CMake 2.4 and
# g++ 4.1 on Ubuntu 7.04), so only CMake 2.4 commands are used here.
# Requires the base, opengl1 and glut libraries (see scripts/compile.sh).
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++98 -Wall")

set(VITRAL_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../..")

find_package(OpenGL)
if(NOT OPENGL_FOUND)
    message(FATAL_ERROR "OpenGL library not found")
endif(NOT OPENGL_FOUND)

find_package(GLUT)
if(NOT GLUT_FOUND)
    message(FATAL_ERROR "GLUT library not found")
endif(NOT GLUT_FOUND)

if(APPLE)
    add_definitions(-DGL_SILENCE_DEPRECATION)
endif(APPLE)

include_directories(
    ${VITRAL_DIR}/base/src ${VITRAL_DIR}/base/src/main
    ${VITRAL_DIR}/opengl1/src/main
    ${VITRAL_DIR}/glut/src/main
    ${OPENGL_INCLUDE_DIR} ${GLUT_INCLUDE_DIR})
link_directories(
    ${VITRAL_DIR}/base/build ${VITRAL_DIR}/opengl1/build ${VITRAL_DIR}/glut/build)

set(VITRAL_OPENGL1_EXAMPLES_LIBRARIES
    vitral_opengl1 vitral_glut vitral_base
    ${GLUT_glut_LIBRARY} ${OPENGL_glu_LIBRARY} ${OPENGL_gl_LIBRARY} pthread)

add_subdirectory(CameraExample)
add_subdirectory(ImageExample)
add_subdirectory(MeshExample)
add_subdirectory(PolygonClippingExample)
