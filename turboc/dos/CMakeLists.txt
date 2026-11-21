cmake_minimum_required(VERSION 2.4)
project(vitral_dos_turboc CXX)

# IEEE 754 floating point everywhere: no fast-math (no reassociation, no
# finite-math or reciprocal shortcuts) and no fused multiply-add contraction,
# so results do not depend on how the compiler rewrites an expression
if(CMAKE_C_COMPILER_ID MATCHES "Clang|GNU" OR CMAKE_CXX_COMPILER_ID MATCHES "Clang|GNU")
    add_compile_options(-fno-fast-math -ffp-contract=off)
endif()

# DOS-specific services for the Turbo C++ port.  This module deliberately
# stays outside base: Unix and modern ports retain their normal linear-memory
# implementations, while real-mode DOS programs can opt into XMS storage.
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++98 -Wall")

set(LIBRARY_OUTPUT_PATH "${CMAKE_CURRENT_SOURCE_DIR}/build")
set(VITRAL_BASE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../base")

set(VITRAL_DOS_SOURCES
    src/main/vsdk/toolkit/media/XmsMemory.cpp
    src/main/vsdk/toolkit/media/XmsRgbImage.cpp
)

include_directories(src/main ${VITRAL_BASE_DIR}/src ${VITRAL_BASE_DIR}/src/main)
link_directories(${VITRAL_BASE_DIR}/build)

add_library(vitral_dos SHARED ${VITRAL_DOS_SOURCES})
target_link_libraries(vitral_dos vitral_base)

if(NOT APPLE)
    set_target_properties(vitral_dos PROPERTIES LINK_FLAGS "-Wl,--no-undefined")
endif(NOT APPLE)
