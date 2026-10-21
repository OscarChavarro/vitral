cmake_minimum_required(VERSION 2.4)
# C is enabled because FindOpenGL/FindX11 run C checks in CMake 2.4
project(vitral_api_tests C CXX)

# Legacy C++98 port: must build with old toolchains (e.g. CMake 2.4 and
# g++ 4.1 on Ubuntu 7.04), so only CMake 2.4 commands are used here.
# Requires the base, opengl1 and glut libraries (see scripts/compile.sh).
# _occtPolyhedralBoundedSolidBooleansTool (OpenCascade, C++11) is not part
# of this port.
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

# Mesa offscreen rendering, used by _OpenGL1PbufferExample when available
find_path(OSMESA_INCLUDE_DIR GL/osmesa.h)
find_library(OSMESA_LIBRARY OSMesa)
if(OSMESA_INCLUDE_DIR AND OSMESA_LIBRARY)
    set(OSMESA_FOUND ON)
    message(STATUS "OSMesa found: ${OSMESA_LIBRARY}")
else(OSMESA_INCLUDE_DIR AND OSMESA_LIBRARY)
    message(STATUS "OSMesa not found: _OpenGL1PbufferExample only with GLUT")
endif(OSMESA_INCLUDE_DIR AND OSMESA_LIBRARY)

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

set(VITRAL_API_TESTS_LIBRARIES
    vitral_opengl1 vitral_glut vitral_base
    ${GLUT_glut_LIBRARY} ${OPENGL_glu_LIBRARY} ${OPENGL_gl_LIBRARY} pthread)

add_subdirectory(_OpenGL1HelloWorld)
add_subdirectory(_OpenGL1PbufferExample)
