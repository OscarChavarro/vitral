cmake_minimum_required(VERSION 3.10)
project(MeshExample CXX)

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "${CMAKE_CURRENT_LIST_DIR}/build")

find_package(glfw3 REQUIRED)
find_package(OpenGL REQUIRED)
find_package(GLEW REQUIRED)
find_library(VITRAL_GLEW_LIB NAMES GLEW glew REQUIRED)

set(VITRAL_BASE_DIR "${CMAKE_CURRENT_LIST_DIR}/../../../base")
set(VITRAL_GLFW_DIR "${CMAKE_CURRENT_LIST_DIR}/../../../glfw")
set(VITRAL_OPENGL4_DIR "${CMAKE_CURRENT_LIST_DIR}/../../../opengl4")

if(NOT TARGET vitral_base)
    set(WITH_JPEG ON CACHE BOOL "Enable JPEG image format support" FORCE)
    set(WITH_PNG ON CACHE BOOL "Enable PNG image format support" FORCE)
    add_subdirectory("${VITRAL_BASE_DIR}" "${CMAKE_CURRENT_BINARY_DIR}/vitral_base")
endif()
if(NOT TARGET vitral_glfw)
    add_subdirectory("${VITRAL_GLFW_DIR}" "${CMAKE_CURRENT_BINARY_DIR}/vitral_glfw")
endif()
if(NOT TARGET vitral_opengl4)
    add_subdirectory("${VITRAL_OPENGL4_DIR}" "${CMAKE_CURRENT_BINARY_DIR}/vitral_opengl4")
endif()

add_executable(MeshExample
    src/MeshExample.cpp
)

target_include_directories(MeshExample PRIVATE
    "${VITRAL_BASE_DIR}/src/main"
    "${VITRAL_GLFW_DIR}/src/main"
    "${VITRAL_OPENGL4_DIR}/src/main"
    ${GLEW_INCLUDE_DIRS}
)

target_link_libraries(MeshExample PRIVATE
    glfw
    OpenGL::GL
    ${VITRAL_GLEW_LIB}
    vitral_base
    vitral_glfw
    vitral_opengl4
)

if(APPLE)
    target_compile_definitions(MeshExample PRIVATE GL_SILENCE_DEPRECATION)
endif()
