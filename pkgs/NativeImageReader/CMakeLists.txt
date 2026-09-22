#===========================================================================
#= NativeImageReader: libpng based image reader, usable from C++ and, over =
#= JNI, from the Vitral Java toolkit (vsdk.toolkit.io.image).              =
#=                                                                        =
#= This project replaces the previous hand written Makefile. The JNI      =
#= header is generated with `javac -h` (the `javah` tool it used was      =
#= removed in JDK 10), and the shared library is left in ./build, where   =
#= vsdk.toolkit.io.image.NativeImageReaderWrapper looks for it, so no     =
#= system wide installation nor -Djava.library.path is needed.            =
#===========================================================================

cmake_minimum_required(VERSION 3.16)
project(NativeImageReader LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

if(NOT CMAKE_CONFIGURATION_TYPES AND NOT CMAKE_BUILD_TYPE)
  set(CMAKE_BUILD_TYPE Release CACHE STRING "Build type" FORCE)
endif()

set(CMAKE_LIBRARY_OUTPUT_DIRECTORY "${CMAKE_CURRENT_LIST_DIR}/build")
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "${CMAKE_CURRENT_LIST_DIR}/build")

# On MacOSX, libpng usually comes from Homebrew or MacPorts, whose prefixes
# are not searched by default on Apple Silicon (the old Makefile had
# /opt/local hard coded for MacPorts).
if(APPLE)
  foreach(_prefix "/opt/homebrew" "/opt/local" "/usr/local")
    if(IS_DIRECTORY "${_prefix}")
      list(APPEND CMAKE_PREFIX_PATH "${_prefix}")
    endif()
  endforeach()
endif()

find_package(PNG REQUIRED)
find_package(JNI REQUIRED)
find_package(Java COMPONENTS Development REQUIRED)

#- JNI header generation ---------------------------------------------------
# The Java classes of the binding are compiled stand alone (they do not
# depend on the rest of the toolkit) only to obtain the JNI header.

set(VITRAL_JAVA_BASE_DIR "${CMAKE_CURRENT_LIST_DIR}/../../java/base/src/main")
set(VITRAL_JAVA_IMAGE_IO_DIR "${VITRAL_JAVA_BASE_DIR}/vsdk/toolkit/io/image")

set(NATIVE_IMAGE_READER_JAVA_SOURCES
    "${VITRAL_JAVA_IMAGE_IO_DIR}/NativeImageReaderWrapper.java"
    "${VITRAL_JAVA_IMAGE_IO_DIR}/_NativeImageReaderWrapperHeaderInfo.java"
    "${VITRAL_JAVA_BASE_DIR}/vsdk/toolkit/common/NativeLibraryLoader.java")

set(NATIVE_IMAGE_READER_GENERATED_DIR "${CMAKE_CURRENT_BINARY_DIR}/generated")
set(NATIVE_IMAGE_READER_GENERATED_HEADER
    "${NATIVE_IMAGE_READER_GENERATED_DIR}/vsdk_toolkit_io_image_NativeImageReaderWrapper.h")

add_custom_command(
    OUTPUT "${NATIVE_IMAGE_READER_GENERATED_HEADER}"
    COMMAND "${CMAKE_COMMAND}" -E make_directory "${NATIVE_IMAGE_READER_GENERATED_DIR}"
    COMMAND "${Java_JAVAC_EXECUTABLE}" -proc:none
            -d "${CMAKE_CURRENT_BINARY_DIR}/classes"
            -h "${NATIVE_IMAGE_READER_GENERATED_DIR}"
            ${NATIVE_IMAGE_READER_JAVA_SOURCES}
    DEPENDS ${NATIVE_IMAGE_READER_JAVA_SOURCES}
    COMMENT "Generating JNI header from the Java binding classes"
    VERBATIM)

add_custom_target(NativeImageReaderJniHeader
    DEPENDS "${NATIVE_IMAGE_READER_GENERATED_HEADER}")

#- Shared library ----------------------------------------------------------

add_library(NativeImageReader SHARED
    src_lib/PngReader.C
    src/NativeImageReaderWrapper.C
    "${NATIVE_IMAGE_READER_GENERATED_HEADER}")

add_dependencies(NativeImageReader NativeImageReaderJniHeader)

target_include_directories(NativeImageReader
    PUBLIC src_lib
    PRIVATE "${NATIVE_IMAGE_READER_GENERATED_DIR}" ${JNI_INCLUDE_DIRS})

target_link_libraries(NativeImageReader PRIVATE PNG::PNG)

# System.mapLibraryName expects libNativeImageReader.so (no version suffix)
set_target_properties(NativeImageReader PROPERTIES
    OUTPUT_NAME NativeImageReader)

#- Stand alone C++ test program --------------------------------------------

add_executable(testImageReader
    src/testImageReader.C
    src_lib/PngReader.C)

target_include_directories(testImageReader PRIVATE src_lib)
target_link_libraries(testImageReader PRIVATE PNG::PNG)

#- Installation ------------------------------------------------------------

include(GNUInstallDirs)
install(TARGETS NativeImageReader LIBRARY DESTINATION ${CMAKE_INSTALL_LIBDIR})
