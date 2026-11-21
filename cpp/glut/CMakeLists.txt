cmake_minimum_required(VERSION 3.10)
project(vitral_glut CXX)

# IEEE 754 floating point everywhere: no fast-math (no reassociation, no
# finite-math or reciprocal shortcuts) and no fused multiply-add contraction,
# so results do not depend on how the compiler rewrites an expression
if(CMAKE_C_COMPILER_ID MATCHES "Clang|GNU" OR CMAKE_CXX_COMPILER_ID MATCHES "Clang|GNU")
    add_compile_options(-fno-fast-math -ffp-contract=off)
endif()

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(CMAKE_LIBRARY_OUTPUT_DIRECTORY "${CMAKE_CURRENT_LIST_DIR}/build")

find_package(OpenGL REQUIRED)
find_package(GLUT REQUIRED)

add_library(vitral_glut SHARED
    src/main/vsdk/toolkit/gui/GlutSystem.cpp
)

target_include_directories(vitral_glut PUBLIC src/main)
target_include_directories(vitral_glut PRIVATE ${CMAKE_CURRENT_LIST_DIR}/../base/src/main)
target_link_libraries(vitral_glut PUBLIC OpenGL::GL GLUT::GLUT)
target_link_libraries(vitral_glut PRIVATE vitral_base)
