#===========================================================================
#= LempelZivWelch: LZW compression library, usable from C and, over JNI,   =
#= from the Vitral Java toolkit (vsdk.toolkit.processing.LzwWrapper).      =
#=                                                                        =
#= This project replaces the previous hand written Makefile. The shared   =
#= library and the test programs are left in ./build.                     =
#===========================================================================

cmake_minimum_required(VERSION 3.16)
project(LempelZivWelch LANGUAGES C)

set(CMAKE_C_STANDARD 99)
set(CMAKE_C_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

if(NOT CMAKE_CONFIGURATION_TYPES AND NOT CMAKE_BUILD_TYPE)
  set(CMAKE_BUILD_TYPE Release CACHE STRING "Build type" FORCE)
endif()

set(CMAKE_LIBRARY_OUTPUT_DIRECTORY "${CMAKE_CURRENT_LIST_DIR}/build")
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "${CMAKE_CURRENT_LIST_DIR}/build")

find_package(JNI REQUIRED)

#- Shared library, as loaded by vsdk.toolkit.processing.LzwWrapper ---------

add_library(LZW SHARED
    src_lib/compress.c
    src/LzwWrapper.c)

target_include_directories(LZW
    PUBLIC src_lib
    PRIVATE ${JNI_INCLUDE_DIRS})

set_target_properties(LZW PROPERTIES OUTPUT_NAME LZW)

#- Stand alone C test program ----------------------------------------------
# The program compresses or uncompresses depending on the name it is called
# with, as the original BSD `compress` command does.

add_executable(compress
    src/test.c
    src_lib/compress.c)

target_include_directories(compress PRIVATE src_lib)
target_compile_definitions(compress PRIVATE UNIX_ENABLED)

add_custom_command(TARGET compress POST_BUILD
    COMMAND "${CMAKE_COMMAND}" -E copy
            "$<TARGET_FILE:compress>"
            "${CMAKE_RUNTIME_OUTPUT_DIRECTORY}/uncompress"
    COMMENT "Creating the uncompress program")

#- Installation ------------------------------------------------------------

include(GNUInstallDirs)
install(TARGETS LZW LIBRARY DESTINATION ${CMAKE_INSTALL_LIBDIR})
