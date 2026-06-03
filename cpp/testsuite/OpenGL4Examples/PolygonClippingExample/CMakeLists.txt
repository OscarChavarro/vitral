cmake_minimum_required(VERSION 3.10)
project(PolygonClippingExample CXX)

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "${CMAKE_CURRENT_LIST_DIR}/build")

find_package(glfw3 REQUIRED)
find_package(OpenGL REQUIRED)
find_package(GLEW REQUIRED)
find_library(VITRAL_GLEW_LIB NAMES GLEW glew REQUIRED)

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
    ${GLEW_INCLUDE_DIRS})

target_link_libraries(PolygonClippingExample PRIVATE
    glfw OpenGL::GL ${VITRAL_GLEW_LIB} vitral_base vitral_glfw vitral_opengl4)
