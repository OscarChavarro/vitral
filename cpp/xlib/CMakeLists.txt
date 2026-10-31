cmake_minimum_required(VERSION 3.10)
project(vitral_xlib CXX)

# Xlib backend of the framebuffer GUI of the base library
# (vsdk/toolkit/gui/framebuffer): one top-level window drawn with Xlib
# primitives and core fonts, and its keyboard and pointer input. It depends
# only on libX11, as the svgalib backend depends only on svgalib: no Xt, no
# widget set, no OpenGL.

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(CMAKE_LIBRARY_OUTPUT_DIRECTORY "${CMAKE_CURRENT_LIST_DIR}/build")

find_package(X11 REQUIRED)

add_library(vitral_xlib SHARED
    src/main/vsdk/toolkit/gui/framebuffer/xlib/XlibFramebuffer.cpp
    src/main/vsdk/toolkit/gui/framebuffer/xlib/XlibInput.cpp
)

target_include_directories(vitral_xlib PUBLIC src/main ${X11_INCLUDE_DIR})
target_include_directories(vitral_xlib PRIVATE ${CMAKE_CURRENT_LIST_DIR}/../base/src/main)
target_link_libraries(vitral_xlib PUBLIC ${X11_LIBRARIES})
target_link_libraries(vitral_xlib PRIVATE vitral_base)
if(NOT APPLE)
    target_link_options(vitral_xlib PRIVATE "-Wl,--no-undefined")
endif()
