cmake_minimum_required(VERSION 2.4)
# C is enabled because FindX11 runs C checks in CMake 2.4
project(vitral_xm_turboc C CXX)

# Motif widget set of the Xt layer: implementations of XtWidgetSet,
# XtPanelWidgets and XtGuiRenderer (vitral_xt) with the Motif widgets.
# It never uses Athena: an application links either vitral_xm or vitral_xaw.

# Legacy C++98 port: must build with old toolchains (e.g. CMake 2.4 and
# g++ 4.1 on Ubuntu 7.04), so only CMake 2.4 commands are used here.
# Requires base/build/libvitral_base and xt/build/libvitral_xt (see scripts/compile.sh).
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
    message(FATAL_ERROR "vitral_xm requires libXt from the X11 development packages (i.e. libxt-dev).")
endif(NOT XT_LIBRARY)

find_package(Motif)
if(NOT MOTIF_FOUND)
    message(FATAL_ERROR "vitral_xm requires the Motif development package (i.e. libmotif-dev or lesstif2-dev).")
endif(NOT MOTIF_FOUND)

set(VITRAL_XM_SOURCES
    src/main/vsdk/toolkit/gui/XmPanelWidgets.cpp
    src/main/vsdk/toolkit/gui/XmWidgetSet.cpp
    src/main/vsdk/toolkit/gui/XmWidgetSupport.cpp
    src/main/vsdk/toolkit/render/xm/XmGuiRenderer.cpp
)

include_directories(src/main ${VITRAL_XT_DIR}/src/main
    ${VITRAL_BASE_DIR}/src ${VITRAL_BASE_DIR}/src/main
    ${MOTIF_INCLUDE_DIR} ${X11_INCLUDE_DIR})
link_directories(${VITRAL_BASE_DIR}/build ${VITRAL_XT_DIR}/build)

add_library(vitral_xm SHARED ${VITRAL_XM_SOURCES})

# Motif before Xt: its VendorShell replaces the one of Xt
target_link_libraries(vitral_xm ${MOTIF_LIBRARIES} vitral_xt vitral_base
    ${XT_LIBRARY} ${X11_LIBRARIES})

# Any symbol missing from the libraries given is a link error
if(NOT APPLE)
    set_target_properties(vitral_xm PROPERTIES LINK_FLAGS "-Wl,--no-undefined")
endif(NOT APPLE)
