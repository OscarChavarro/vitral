cmake_minimum_required(VERSION 3.10)
project(CameraExample CXX)

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "${CMAKE_CURRENT_LIST_DIR}/build")

find_package(glfw3 REQUIRED)
find_package(OpenGL REQUIRED)
find_package(GLEW REQUIRED)

# Define base directory for vitral libraries
# CameraExample is at: cpp/testsuite/OpenGL4Examples/CameraExample
# We need to go up 3 levels to reach cpp/
set(VITRAL_BASE_DIR "${CMAKE_CURRENT_LIST_DIR}/../../../base")
set(VITRAL_GLFW_DIR "${CMAKE_CURRENT_LIST_DIR}/../../../glfw")
set(VITRAL_OPENGL4_DIR "${CMAKE_CURRENT_LIST_DIR}/../../../opengl4")

# Check if this is being invoked as a subproject (targets already exist)
if(NOT TARGET vitral_base)
    # Standalone mode: need to build or find the libraries
    if(EXISTS "${VITRAL_BASE_DIR}/CMakeLists.txt")
        add_subdirectory("${VITRAL_BASE_DIR}" "${CMAKE_CURRENT_BINARY_DIR}/vitral_base")
    else()
        message(FATAL_ERROR "vitral_base not found at ${VITRAL_BASE_DIR}")
    endif()
endif()

if(NOT TARGET vitral_glfw)
    if(EXISTS "${VITRAL_GLFW_DIR}/CMakeLists.txt")
        add_subdirectory("${VITRAL_GLFW_DIR}" "${CMAKE_CURRENT_BINARY_DIR}/vitral_glfw")
    else()
        message(FATAL_ERROR "vitral_glfw not found at ${VITRAL_GLFW_DIR}")
    endif()
endif()

if(NOT TARGET vitral_opengl4)
    if(EXISTS "${VITRAL_OPENGL4_DIR}/CMakeLists.txt")
        add_subdirectory("${VITRAL_OPENGL4_DIR}" "${CMAKE_CURRENT_BINARY_DIR}/vitral_opengl4")
    else()
        message(FATAL_ERROR "vitral_opengl4 not found at ${VITRAL_OPENGL4_DIR}")
    endif()
endif()

add_executable(CameraExample src/CameraExample.cpp)

target_include_directories(CameraExample PRIVATE
    "${VITRAL_BASE_DIR}/src/main"
    "${VITRAL_GLFW_DIR}/src/main"
    "${VITRAL_OPENGL4_DIR}/src/main"
)

target_link_libraries(CameraExample PRIVATE
    glfw
    OpenGL::GL
    GLEW::GLEW
    vitral_base
    vitral_glfw
    vitral_opengl4
)

if(APPLE)
    target_compile_definitions(CameraExample PRIVATE GL_SILENCE_DEPRECATION)
endif()
