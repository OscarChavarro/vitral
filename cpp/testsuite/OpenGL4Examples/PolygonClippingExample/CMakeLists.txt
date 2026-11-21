cmake_minimum_required(VERSION 3.10)
project(PolygonClippingExample CXX)

# IEEE 754 floating point everywhere: no fast-math (no reassociation, no
# finite-math or reciprocal shortcuts) and no fused multiply-add contraction,
# so results do not depend on how the compiler rewrites an expression
if(CMAKE_C_COMPILER_ID MATCHES "Clang|GNU" OR CMAKE_CXX_COMPILER_ID MATCHES "Clang|GNU")
    add_compile_options(-fno-fast-math -ffp-contract=off)
endif()

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "${CMAKE_CURRENT_LIST_DIR}/build")

find_package(glfw3 REQUIRED)
find_package(OpenGL REQUIRED)

add_executable(PolygonClippingExample
    src/PolygonClippingExample.cpp
    src/model/PolygonClippingFixtures.cpp
    src/render/PolygonClippingHudRenderer.cpp
    src/render/PolygonTriangularRenderer.cpp
    src/options/CommandLineOptions.cpp
)

target_include_directories(PolygonClippingExample PRIVATE
    "${CMAKE_CURRENT_LIST_DIR}/src"
    "${CMAKE_CURRENT_LIST_DIR}/../../../base/src/main"
    "${CMAKE_CURRENT_LIST_DIR}/../../../glfw/src/main"
    "${CMAKE_CURRENT_LIST_DIR}/../../../opengl4/src/main"
    "${CMAKE_CURRENT_LIST_DIR}/../../../opengl4/src/glad/include")

target_link_libraries(PolygonClippingExample PRIVATE
    glfw OpenGL::GL vitral_base vitral_glfw vitral_opengl4)
