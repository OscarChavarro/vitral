cmake_minimum_required(VERSION 3.10)
project(vitral_glut CXX)

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(CMAKE_LIBRARY_OUTPUT_DIRECTORY "${CMAKE_CURRENT_LIST_DIR}/build")

find_package(OpenGL REQUIRED)
find_package(GLUT REQUIRED)

# Dependency: vitral_base
set(VITRAL_BASE_DIR "${CMAKE_CURRENT_LIST_DIR}/../base")
set(VITRAL_BASE_INCLUDE "${VITRAL_BASE_DIR}/src/main")
set(VITRAL_BASE_LIB "${VITRAL_BASE_DIR}/build/libvitral_base.dylib")

add_library(vitral_glut SHARED
    src/main/vsdk/toolkit/gui/GlutSystem.cpp
)

target_include_directories(vitral_glut PUBLIC src/main)
target_include_directories(vitral_glut PRIVATE ${VITRAL_BASE_INCLUDE})
target_link_libraries(vitral_glut PUBLIC OpenGL::GL GLUT::GLUT)
target_link_libraries(vitral_glut PRIVATE ${VITRAL_BASE_LIB})
