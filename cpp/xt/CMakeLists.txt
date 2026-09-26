cmake_minimum_required(VERSION 3.10)
project(vitral_xt CXX)

# Xt/Xaw layer of the toolkit, the C++ counterpart of the Java `awt` module:
# conversion of the X events to vitral ones (XtSystem), presentation of
# images with Xlib (render/xlib), Xaw widgets built from the vitral GUI
# definition (render/xaw) and the Xaw generic editor of entities.

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(CMAKE_LIBRARY_OUTPUT_DIRECTORY "${CMAKE_CURRENT_LIST_DIR}/build")

find_package(X11 REQUIRED)
if(NOT X11_Xt_LIB OR NOT X11_Xaw_LIB)
    message(FATAL_ERROR "vitral_xt requires libXt and libXaw from the X11 development packages.")
endif()

add_library(vitral_xt SHARED
    src/main/vsdk/toolkit/gui/XtEventQueue.cpp
    src/main/vsdk/toolkit/gui/XtPanelWidgets.cpp
    src/main/vsdk/toolkit/gui/XtSystem.cpp
    src/main/vsdk/toolkit/gui/editor/XtGenericEditor.cpp
    src/main/vsdk/toolkit/render/xaw/XawGuiRenderer.cpp
    src/main/vsdk/toolkit/render/xlib/XlibRGBAImageUncompressedRenderer.cpp
    src/main/vsdk/toolkit/render/xlib/XlibRGBImageUncompressedRenderer.cpp
)

target_include_directories(vitral_xt PUBLIC src/main ${X11_INCLUDE_DIR})
target_include_directories(vitral_xt PRIVATE ${CMAKE_CURRENT_LIST_DIR}/../base/src/main)
target_link_libraries(vitral_xt PUBLIC ${X11_Xt_LIB} ${X11_Xaw_LIB} ${X11_LIBRARIES})
target_link_libraries(vitral_xt PRIVATE vitral_base)
