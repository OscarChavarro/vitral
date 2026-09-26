cmake_minimum_required(VERSION 3.10)
project(_OpenGL4PbufferExample CXX)

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_package(OpenGL REQUIRED)
find_package(glfw3 REQUIRED)

set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "${CMAKE_CURRENT_LIST_DIR}/build")

add_executable(_OpenGL4PbufferExample
    src/OpenGL4PbufferExample.cpp
)

target_link_libraries(_OpenGL4PbufferExample
    OpenGL::GL
    glfw
    vitral_base
    vitral_opengl4
)

if(APPLE)
    target_compile_definitions(_OpenGL4PbufferExample PRIVATE GL_SILENCE_DEPRECATION)
endif()
