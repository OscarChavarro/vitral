cmake_minimum_required(VERSION 3.10)
project(CameraExample CXX)

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "${CMAKE_CURRENT_LIST_DIR}/build")

set(BUILD_CAMERA_EXAMPLE TRUE)

find_package(glfw3 QUIET)
if(NOT glfw3_FOUND)
  message(WARNING "glfw3 not found. CameraExample will be skipped.")
  set(BUILD_CAMERA_EXAMPLE FALSE)
endif()

find_package(OpenGL QUIET)
if(NOT OpenGL_FOUND)
  message(WARNING "OpenGL not found. CameraExample will be skipped.")
  set(BUILD_CAMERA_EXAMPLE FALSE)
endif()

find_package(GLEW QUIET)
if(NOT GLEW_FOUND)
  message(WARNING "GLEW not found. CameraExample will be skipped.")
  set(BUILD_CAMERA_EXAMPLE FALSE)
endif()

find_library(VITRAL_GLEW_LIB NAMES GLEW glew)
if(NOT VITRAL_GLEW_LIB)
  message(WARNING "GLEW library not found. CameraExample will be skipped.")
  set(BUILD_CAMERA_EXAMPLE FALSE)
endif()

# Define base directory for vitral libraries
# CameraExample is at: cpp/testsuite/OpenGL4Examples/CameraExample
# We need to go up 3 levels to reach cpp/
set(VITRAL_BASE_DIR "${CMAKE_CURRENT_LIST_DIR}/../../../base")
set(VITRAL_GLFW_DIR "${CMAKE_CURRENT_LIST_DIR}/../../../glfw")
set(VITRAL_OPENGL4_DIR "${CMAKE_CURRENT_LIST_DIR}/../../../opengl4")

# Check if this is being invoked as a subproject (targets already exist)
if(BUILD_CAMERA_EXAMPLE AND NOT TARGET vitral_base)
    # Standalone mode: need to build or find the libraries
    if(EXISTS "${VITRAL_BASE_DIR}/CMakeLists.txt")
        add_subdirectory("${VITRAL_BASE_DIR}" "${CMAKE_CURRENT_BINARY_DIR}/vitral_base")
    else()
        message(WARNING "vitral_base not found at ${VITRAL_BASE_DIR}. CameraExample will be skipped.")
        set(BUILD_CAMERA_EXAMPLE FALSE)
    endif()
endif()

if(BUILD_CAMERA_EXAMPLE AND NOT TARGET vitral_glfw)
    if(EXISTS "${VITRAL_GLFW_DIR}/CMakeLists.txt")
        add_subdirectory("${VITRAL_GLFW_DIR}" "${CMAKE_CURRENT_BINARY_DIR}/vitral_glfw")
    else()
        message(WARNING "vitral_glfw not found at ${VITRAL_GLFW_DIR}. CameraExample will be skipped.")
        set(BUILD_CAMERA_EXAMPLE FALSE)
    endif()
endif()

if(BUILD_CAMERA_EXAMPLE AND NOT TARGET vitral_opengl4)
    if(EXISTS "${VITRAL_OPENGL4_DIR}/CMakeLists.txt")
        add_subdirectory("${VITRAL_OPENGL4_DIR}" "${CMAKE_CURRENT_BINARY_DIR}/vitral_opengl4")
    else()
        message(WARNING "vitral_opengl4 not found at ${VITRAL_OPENGL4_DIR}. CameraExample will be skipped.")
        set(BUILD_CAMERA_EXAMPLE FALSE)
    endif()
endif()

if(BUILD_CAMERA_EXAMPLE)
  add_executable(CameraExample src/CameraExample.cpp)

  target_include_directories(CameraExample PRIVATE
      "${VITRAL_BASE_DIR}/src/main"
      "${VITRAL_GLFW_DIR}/src/main"
      "${VITRAL_OPENGL4_DIR}/src/main"
      ${GLEW_INCLUDE_DIRS}
  )

  target_link_libraries(CameraExample PRIVATE
      glfw
      OpenGL::GL
      ${VITRAL_GLEW_LIB}
      vitral_base
      vitral_glfw
      vitral_opengl4
  )

  if(APPLE)
      target_compile_definitions(CameraExample PRIVATE GL_SILENCE_DEPRECATION)
  endif()
else()
  message(STATUS "CameraExample skipped due to missing dependencies")
endif()
