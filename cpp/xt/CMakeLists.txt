cmake_minimum_required(VERSION 3.10)
project(vitral_xt CXX)

# Xt layer of the toolkit, the C++ counterpart of the Java `awt` module:
# conversion of the X events to vitral ones (XtSystem), presentation of
# images with Xlib (render/xlib) and the interfaces of the widget sets over
# the Xt Intrinsics (XtWidgetSet, XtPanelWidgets, XtGuiRenderer), with the
# Xt generic editor of entities built over them.
#
# It depends only on libX11 and libXt: the widget sets are the sibling
# modules vitral_xaw (Athena) and vitral_xm (Motif). An application links
# exactly one of them.

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(CMAKE_LIBRARY_OUTPUT_DIRECTORY "${CMAKE_CURRENT_LIST_DIR}/build")

find_package(X11 REQUIRED)
if(NOT X11_Xt_LIB)
    message(FATAL_ERROR "vitral_xt requires libXt from the X11 development packages.")
endif()

add_library(vitral_xt SHARED
    src/main/vsdk/toolkit/gui/XtEventQueue.cpp
    src/main/vsdk/toolkit/gui/XtSystem.cpp
    src/main/vsdk/toolkit/gui/XtWidgetSupport.cpp
    src/main/vsdk/toolkit/gui/editor/XtGenericEditor.cpp
    src/main/vsdk/toolkit/render/xlib/XlibRGBAImageUncompressedRenderer.cpp
    src/main/vsdk/toolkit/render/xlib/XlibRGBImageUncompressedRenderer.cpp
)

target_include_directories(vitral_xt PUBLIC src/main ${X11_INCLUDE_DIR})
target_include_directories(vitral_xt PRIVATE ${CMAKE_CURRENT_LIST_DIR}/../base/src/main)
target_link_libraries(vitral_xt PUBLIC ${X11_Xt_LIB} ${X11_LIBRARIES})
target_link_libraries(vitral_xt PRIVATE vitral_base)
# Any symbol of a widget set used from this layer is a link error
if(NOT APPLE)
    target_link_options(vitral_xt PRIVATE "-Wl,--no-undefined")
endif()
