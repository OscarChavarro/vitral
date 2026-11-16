cmake_minimum_required(VERSION 3.10)
project(PolyhedralBoundedSolidExample CXX)

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "${CMAKE_CURRENT_LIST_DIR}/build")

find_package(glfw3 REQUIRED)
find_package(OpenGL REQUIRED)

set(VITRAL_BASE_DIR "${CMAKE_CURRENT_LIST_DIR}/../../../base")
set(VITRAL_GLFW_DIR "${CMAKE_CURRENT_LIST_DIR}/../../../glfw")
set(VITRAL_OPENGL4_DIR "${CMAKE_CURRENT_LIST_DIR}/../../../opengl4")

if(NOT TARGET vitral_base)
    set(VITRAL_BUILD_TESTS OFF CACHE BOOL "Build Vitral tests" FORCE)
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

add_executable(PolyhedralBoundedSolidExample
    src/PolyhedralBoundedSolidExample.cpp
    src/InteractiveDebugger.cpp
    src/PolyhedralBoundedSolidModelingTools.cpp
    src/animation/SolidAnimationController.cpp
    src/gui/CameraFaceFocusInteraction.cpp
    src/gui/DebuggerKeyboardInteractionTechniques.cpp
    src/gui/DebuggerMouseInteractionTechniques.cpp
    src/models/AppelDisplayMode.cpp
    src/models/CsgOperationNames.cpp
    src/models/CsgSampleNames.cpp
    src/models/DebuggerModel.cpp
    src/models/GeneralModelsBuilder.cpp
    src/models/SolidModelNames.cpp
    src/options/CommandLineOptions.cpp
    src/render/Jogl4DebuggerHudRenderer.cpp
    src/render/Jogl4DebuggerRenderer.cpp
    src/render/Jogl4HeadlessRenderer.cpp
    src/render/Jogl4HudOperandsRenderer.cpp
)

target_include_directories(PolyhedralBoundedSolidExample PRIVATE
    "${CMAKE_CURRENT_LIST_DIR}/src"
    "${VITRAL_BASE_DIR}/src/main"
    "${VITRAL_GLFW_DIR}/src/main"
    "${VITRAL_OPENGL4_DIR}/src/main"
    "${CMAKE_CURRENT_LIST_DIR}/../../../opengl4/src/glad/include"
)

target_link_libraries(PolyhedralBoundedSolidExample PRIVATE
    glfw
    OpenGL::GL
    vitral_base
    vitral_glfw
    vitral_opengl4
)

if(APPLE)
    target_compile_definitions(PolyhedralBoundedSolidExample PRIVATE GL_SILENCE_DEPRECATION)
endif()
