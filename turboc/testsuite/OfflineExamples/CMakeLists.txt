cmake_minimum_required(VERSION 2.4)
project(vitral_offline_examples CXX)

# Legacy C++98 port: must build with old toolchains (e.g. CMake 2.4 and
# g++ 4.1 on Ubuntu 7.04), so only CMake 2.4 commands are used here.
# Offline (no window) programs: only require the base library (see
# scripts/compile.sh).
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++98 -Wall")

set(VITRAL_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../..")

if(APPLE)
    add_definitions(-DGL_SILENCE_DEPRECATION)
endif(APPLE)

include_directories(${VITRAL_DIR}/base/src ${VITRAL_DIR}/base/src/main)
link_directories(${VITRAL_DIR}/base/build)

set(VITRAL_OFFLINE_EXAMPLES_LIBRARIES vitral_base pthread)

add_subdirectory(AlgebraicExpressionExample)
add_subdirectory(ImageOfflineExample)
add_subdirectory(PolygonTriangulation)
add_subdirectory(Rasterizer2DExample)
add_subdirectory(RaytracingOfflineExample)
add_subdirectory(WireframeOfflineExample)
