cmake_minimum_required(VERSION 3.10)
project(ShadersExample CXX)

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

add_executable(ShadersExample
    src/ShadersExample.cpp
    src/render/JogHudRenderer.cpp
    src/options/CommandLineOptions.cpp
    src/model/ShadersModel.cpp
    src/render/SoftwareRaycaster.cpp
    src/render/OpenGlOfflineSphereRenderer.cpp
    src/gui/Animation.cpp
    src/gui/ShadersKeyboardInteractionTechniques.cpp
    src/gui/ShadersMouseInteractionTechniques.cpp
    src/OfflineControl.cpp
)

target_include_directories(ShadersExample PRIVATE
    "${VITRAL_BASE_DIR}/src/main"
    "${VITRAL_GLFW_DIR}/src/main"
    "${VITRAL_OPENGL4_DIR}/src/main"
    ${GLEW_INCLUDE_DIRS}
)

target_link_libraries(ShadersExample PRIVATE
    glfw
    OpenGL::GL
    ${VITRAL_GLEW_LIB}
    vitral_base
    vitral_glfw
    vitral_opengl4
)

if(APPLE)
    target_compile_definitions(ShadersExample PRIVATE GL_SILENCE_DEPRECATION)

    option(VITRAL_SIGN_EXECUTABLES "Sign macOS executables after build" ON)
    set(VITRAL_CODESIGN_IDENTITY "-" CACHE STRING "codesign identity used for macOS executable signing")

    if(VITRAL_SIGN_EXECUTABLES)
        find_program(CODESIGN_EXECUTABLE codesign)
        if(CODESIGN_EXECUTABLE)
            add_custom_command(TARGET ShadersExample POST_BUILD
                COMMAND ${CODESIGN_EXECUTABLE}
                    --force
                    --sign "${VITRAL_CODESIGN_IDENTITY}"
                    --timestamp=none
                    "$<TARGET_FILE:ShadersExample>"
                COMMENT "Signing ShadersExample with identity '${VITRAL_CODESIGN_IDENTITY}'"
                VERBATIM)
        else()
            message(WARNING "codesign tool not found; ShadersExample will not be signed")
        endif()
    endif()
endif()
