cmake_minimum_required(VERSION 3.10)
project(vitral_xaw CXX)

# Athena (Xaw) widget set of the Xt layer: implementations of XtWidgetSet,
# XtPanelWidgets and XtGuiRenderer (vitral_xt) with the Athena widgets.
# It never uses Motif: an application links either vitral_xaw or vitral_xm.

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(CMAKE_LIBRARY_OUTPUT_DIRECTORY "${CMAKE_CURRENT_LIST_DIR}/build")

find_package(X11 REQUIRED)
if(NOT X11_Xaw_LIB OR NOT X11_Xmu_LIB)
    message(FATAL_ERROR "vitral_xaw requires libXaw and libXmu from the X11 development packages (i.e. libxaw7-dev).")
endif()

add_library(vitral_xaw SHARED
    src/main/vsdk/toolkit/gui/XawPanelWidgets.cpp
    src/main/vsdk/toolkit/gui/XawWidgetSet.cpp
    src/main/vsdk/toolkit/render/xaw/XawGuiRenderer.cpp
)

target_include_directories(vitral_xaw PUBLIC src/main)
target_include_directories(vitral_xaw PRIVATE ${CMAKE_CURRENT_LIST_DIR}/../base/src/main)
target_link_libraries(vitral_xaw PUBLIC ${X11_Xaw_LIB} ${X11_Xmu_LIB} vitral_xt)
target_link_libraries(vitral_xaw PRIVATE vitral_base)
if(NOT APPLE)
    target_link_options(vitral_xaw PRIVATE "-Wl,--no-undefined")
endif()
