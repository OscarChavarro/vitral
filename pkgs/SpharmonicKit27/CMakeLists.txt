#===========================================================================
#= SpharmonicKit27: spherical harmonic transform library (SpharmonicKit    =
#= 2.7 from Dartmouth College), usable from C and, over JNI, from the      =
#= Vitral Java toolkit (vsdk.toolkit.processing.SpharmonicKitWrapper).     =
#=                                                                        =
#= This project replaces the previous hand written Makefile. The shared   =
#= library and the test programs are left in ./build.                     =
#=                                                                        =
#= See ./doc/LICENSE: commercial use of this package is prohibited.       =
#===========================================================================

cmake_minimum_required(VERSION 3.16)
project(SpharmonicKit27 LANGUAGES C CXX)

set(CMAKE_C_STANDARD 99)
set(CMAKE_C_STANDARD_REQUIRED ON)
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

if(NOT CMAKE_CONFIGURATION_TYPES AND NOT CMAKE_BUILD_TYPE)
  set(CMAKE_BUILD_TYPE Release CACHE STRING "Build type" FORCE)
endif()

set(CMAKE_LIBRARY_OUTPUT_DIRECTORY "${CMAKE_CURRENT_LIST_DIR}/build")
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "${CMAKE_CURRENT_LIST_DIR}/build")

find_package(JNI REQUIRED)

# WALLCLOCK: time wall clock instead of cpu time, as the original Makefile
add_compile_definitions(UNIX_ENABLED WALLCLOCK)

#- Seminaive spherical transform sources (FSTSEMISRC on the old Makefile) --

set(SPHARMONICKIT_SOURCES
    src_lib/OURperms.c
    src_lib/OURmods.c
    src_lib/newFCT.c
    src_lib/weights.c
    src_lib/cospmls.c
    src_lib/seminaive.c
    src_lib/oddweights.c
    src_lib/csecond.c
    src_lib/primitive.c
    src_lib/naive_synthesis.c
    src_lib/primitive_FST.c
    src_lib/permroots.c
    src_lib/indextables.c
    src_lib/FFTcode.c
    src_lib/fft_grids.c
    src_lib/FST_semi_disk.c)

add_library(spharmonickit_objects OBJECT ${SPHARMONICKIT_SOURCES})
target_include_directories(spharmonickit_objects PUBLIC src_lib)

#- Shared library, as loaded by vsdk.toolkit.processing.SpharmonicKitWrapper

add_library(spharmonickit SHARED
    src/SpharmonicKitWrapper.C
    $<TARGET_OBJECTS:spharmonickit_objects>)

target_include_directories(spharmonickit
    PUBLIC src_lib
    PRIVATE ${JNI_INCLUDE_DIRS})

target_link_libraries(spharmonickit PRIVATE m)

set_target_properties(spharmonickit PROPERTIES OUTPUT_NAME spharmonickit)

#- Stand alone C test programs ---------------------------------------------

add_executable(generateSphericalHarmonics
    src/generateSphericalHarmonics.c
    $<TARGET_OBJECTS:spharmonickit_objects>)

target_include_directories(generateSphericalHarmonics PRIVATE src_lib)
target_link_libraries(generateSphericalHarmonics PRIVATE m)

add_executable(convertFileToBynaryArray src/convertFileToBynaryArray.c)
target_include_directories(convertFileToBynaryArray PRIVATE src_lib)
target_link_libraries(convertFileToBynaryArray PRIVATE m)

#- Installation ------------------------------------------------------------

include(GNUInstallDirs)
install(TARGETS spharmonickit LIBRARY DESTINATION ${CMAKE_INSTALL_LIBDIR})
