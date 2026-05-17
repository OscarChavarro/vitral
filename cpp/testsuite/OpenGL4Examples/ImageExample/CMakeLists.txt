# NOTE: Compile from cpp/ directory using ./scripts/compile.sh or:
#   cd cpp && mkdir -p build && cd build && cmake .. && make
# Do NOT compile from within this build directory.

cmake_minimum_required(VERSION 3.16)
project(ImageExample)

set(CMAKE_CXX_STANDARD 11)

find_package(OpenGL REQUIRED)
find_package(GLEW REQUIRED)
find_package(glfw3 REQUIRED)

include_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}/src
    ${CMAKE_SOURCE_DIR}/../../../base/src
    ${CMAKE_SOURCE_DIR}/../../../base/src/main
    ${CMAKE_SOURCE_DIR}/../../../opengl4/src/main
    ${CMAKE_SOURCE_DIR}/../../../glfw/src/main
)

# Find pre-compiled libraries from the module builds
set(VITRAL_BASE_LIB_DIR "${CMAKE_SOURCE_DIR}/../../../base/build")
set(VITRAL_OPENGL4_LIB_DIR "${CMAKE_SOURCE_DIR}/../../../opengl4/build")
set(VITRAL_GLFW_LIB_DIR "${CMAKE_SOURCE_DIR}/../../../glfw/build")

add_executable(ImageExample
    src/ImageExample.cpp
)

# Check if pre-compiled libraries exist
if(EXISTS "${VITRAL_BASE_LIB_DIR}/libvitral_base.dylib")
    # Link against pre-compiled .dylib libraries (macOS)
    target_link_libraries(ImageExample
        ${VITRAL_BASE_LIB_DIR}/libvitral_base.dylib
        ${VITRAL_OPENGL4_LIB_DIR}/libvitral_opengl4.dylib
        ${VITRAL_GLFW_LIB_DIR}/libvitral_glfw.dylib
        OpenGL::GL
        GLEW::GLEW
        glfw
    )
elseif(EXISTS "${VITRAL_BASE_LIB_DIR}/libvitral_base.a")
    # Link against pre-compiled .a libraries (Linux/Unix)
    target_link_libraries(ImageExample
        ${VITRAL_BASE_LIB_DIR}/libvitral_base.a
        ${VITRAL_OPENGL4_LIB_DIR}/libvitral_opengl4.a
        ${VITRAL_GLFW_LIB_DIR}/libvitral_glfw.a
        OpenGL::GL
        GLEW::GLEW
        glfw
    )
else()
    # Link against targets (when compiled as part of main project)
    target_link_libraries(ImageExample
        vitral_base
        vitral_opengl4
        vitral_glfw
        OpenGL::GL
        GLEW::GLEW
        glfw
    )
endif()

set_target_properties(ImageExample PROPERTIES
    RUNTIME_OUTPUT_DIRECTORY "${CMAKE_CURRENT_LIST_DIR}/build"
)

# =======================================================
# IMPORTANT: Must be executed from cpp/ directory
# =======================================================
# The program must be run from the cpp/ directory:
#   cd /Users/jedilink/VITRAL/vitral/cpp
#   ./testsuite/OpenGL4Examples/ImageExample/build/ImageExample
#
# Relative paths in the code point to:
#   - etc/images/render.ppm
#   - etc/textures/earth.ppm
#   - etc/glslShaders/
# These paths are only correct when running from cpp/
# =======================================================
