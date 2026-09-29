cmake_minimum_required(VERSION 3.10)
project(vitral_gtk4 CXX)

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(CMAKE_LIBRARY_OUTPUT_DIRECTORY "${CMAKE_CURRENT_LIST_DIR}/build")

find_package(PkgConfig REQUIRED)
pkg_check_modules(GTK4 QUIET IMPORTED_TARGET gtk4>=4.22)
if(NOT GTK4_FOUND)
    message(FATAL_ERROR
        "vitral_gtk4 requires GTK 4.22 development files. On macOS with "
        "Homebrew, install them with `brew install gtk4 pkg-config` and "
        "ensure pkg-config can see /opt/homebrew/lib/pkgconfig and "
        "/opt/homebrew/share/pkgconfig.")
endif()

add_library(vitral_gtk4 SHARED
    src/main/vsdk/toolkit/gui/editor/Gtk4GenericEditor.cpp
    src/main/vsdk/toolkit/gui/Gtk4LabelImageProvider.cpp
    src/main/vsdk/toolkit/gui/Gtk4System.cpp
)

target_include_directories(vitral_gtk4 PUBLIC src/main)
target_include_directories(vitral_gtk4 PRIVATE
    ${CMAKE_CURRENT_LIST_DIR}/../base/src/main
    ${CMAKE_CURRENT_LIST_DIR}/../opengl4/src/main
)
target_link_libraries(vitral_gtk4 PUBLIC PkgConfig::GTK4)
target_link_libraries(vitral_gtk4 PRIVATE vitral_base vitral_opengl4)
