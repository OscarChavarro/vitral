cmake_minimum_required(VERSION 3.10)
project(vitral_xm CXX)

# Motif widget set of the Xt layer: implementations of XtWidgetSet,
# XtPanelWidgets and XtGuiRenderer (vitral_xt) with the Motif widgets.
# It never uses Athena: an application links either vitral_xm or vitral_xaw.

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(CMAKE_LIBRARY_OUTPUT_DIRECTORY "${CMAKE_CURRENT_LIST_DIR}/build")

find_package(Motif)
if(NOT MOTIF_FOUND)
    message(FATAL_ERROR "vitral_xm requires the Motif development package (i.e. libmotif-dev).")
endif()

add_library(vitral_xm SHARED
    src/main/vsdk/toolkit/gui/XmPanelWidgets.cpp
    src/main/vsdk/toolkit/gui/XmWidgetSet.cpp
    src/main/vsdk/toolkit/gui/XmWidgetSupport.cpp
    src/main/vsdk/toolkit/render/xm/XmGuiRenderer.cpp
)

target_include_directories(vitral_xm PUBLIC src/main ${MOTIF_INCLUDE_DIR})
target_include_directories(vitral_xm PRIVATE ${CMAKE_CURRENT_LIST_DIR}/../base/src/main)
# Motif before Xt: its VendorShell replaces the one of Xt
target_link_libraries(vitral_xm PUBLIC ${MOTIF_LIBRARIES} vitral_xt)
target_link_libraries(vitral_xm PRIVATE vitral_base)
if(NOT APPLE)
    target_link_options(vitral_xm PRIVATE "-Wl,--no-undefined")
endif()
