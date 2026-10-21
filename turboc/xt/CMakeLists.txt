cmake_minimum_required(VERSION 2.4)
# C is enabled because FindX11 runs C checks in CMake 2.4
project(vitral_xt_turboc C CXX)

# Xt layer of the toolkit, the C++ counterpart of the Java `awt` module:
# conversion of the X events to vitral ones (XtSystem), presentation of
# images with Xlib (render/xlib) and the interfaces of the widget sets over
# the Xt Intrinsics (XtWidgetSet, XtPanelWidgets, XtGuiRenderer), with the
# Xt generic editor of entities built over them.
#
# It depends only on libX11 and libXt: the widget sets are the sibling
# modules vitral_xaw (Athena) and vitral_xm (Motif). An application links
# exactly one of them.

# Legacy C++98 port: must build with old toolchains (e.g. CMake 2.4 and
# g++ 4.1 on Ubuntu 7.04), so only CMake 2.4 commands are used here.
# Requires base/build/libvitral_base (see scripts/compile.sh).
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++98 -Wall")

# The `String` type of Xt is renamed `XtString` around its headers (see
# XtIntrinsics.h): resource names as string literals, so their macros do
# not expand to casts to `String` where the vitral class is meant
add_definitions(-DXTSTRINGDEFINES -DXMSTRINGDEFINES)

set(LIBRARY_OUTPUT_PATH "${CMAKE_CURRENT_SOURCE_DIR}/build")
set(VITRAL_BASE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../base")
set(VITRAL_XT_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../xt")

find_package(X11)
if(NOT X11_FOUND)
    message(FATAL_ERROR "X11 development files not found")
endif(NOT X11_FOUND)
# CMake 2.4 FindX11 does not look for the X toolkit libraries
find_library(XT_LIBRARY Xt ${X11_LIB_SEARCH_PATH} /usr/X11R6/lib /usr/lib)
if(NOT XT_LIBRARY)
    message(FATAL_ERROR "vitral_xt requires libXt from the X11 development packages (i.e. libxt-dev).")
endif(NOT XT_LIBRARY)

set(VITRAL_XT_SOURCES
    src/main/vsdk/toolkit/gui/XtEventQueue.cpp
    src/main/vsdk/toolkit/gui/XtSystem.cpp
    src/main/vsdk/toolkit/gui/XtWidgetSupport.cpp
    src/main/vsdk/toolkit/gui/editor/XtGenericEditor.cpp
    src/main/vsdk/toolkit/render/xt/XtDialogHost.cpp
    src/main/vsdk/toolkit/render/xt/XtTreeView.cpp
    src/main/vsdk/toolkit/render/xlib/XlibRGBAImageUncompressedRenderer.cpp
    src/main/vsdk/toolkit/render/xlib/XlibRGBImageUncompressedRenderer.cpp
)

include_directories(src/main ${VITRAL_BASE_DIR}/src ${VITRAL_BASE_DIR}/src/main
    ${X11_INCLUDE_DIR})
link_directories(${VITRAL_BASE_DIR}/build)

add_library(vitral_xt SHARED ${VITRAL_XT_SOURCES})

target_link_libraries(vitral_xt vitral_base ${XT_LIBRARY} ${X11_LIBRARIES} pthread)

# Any symbol missing from the libraries given is a link error
if(NOT APPLE)
    set_target_properties(vitral_xt PROPERTIES LINK_FLAGS "-Wl,--no-undefined")
endif(NOT APPLE)
