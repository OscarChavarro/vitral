cmake_minimum_required(VERSION 2.4)
# C is enabled because FindOpenGL/FindX11 run C checks in CMake 2.4
project(vitral_glut_turboc C CXX)

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

find_package(GLUT)
if(NOT GLUT_FOUND)
    message(FATAL_ERROR "GLUT library not found")
endif(NOT GLUT_FOUND)

# Labels are rasterized with the X11 core fonts when X11 is available
if(NOT APPLE AND NOT WIN32)
    find_package(X11)
endif(NOT APPLE AND NOT WIN32)
if(X11_FOUND)
    add_definitions(-DVITRAL_GLUT_WITH_X11)
    include_directories(${X11_INCLUDE_DIR})
endif(X11_FOUND)

include_directories(src/main ${VITRAL_BASE_DIR}/src ${VITRAL_BASE_DIR}/src/main
    ${OPENGL_INCLUDE_DIR} ${GLUT_INCLUDE_DIR})
link_directories(${VITRAL_BASE_DIR}/build)

add_library(vitral_glut SHARED
    src/main/vsdk/toolkit/gui/GlutSystem.cpp
)

target_link_libraries(vitral_glut vitral_base
    ${GLUT_glut_LIBRARY} ${OPENGL_gl_LIBRARY})

if(X11_FOUND)
    target_link_libraries(vitral_glut ${X11_X11_LIB})
endif(X11_FOUND)
