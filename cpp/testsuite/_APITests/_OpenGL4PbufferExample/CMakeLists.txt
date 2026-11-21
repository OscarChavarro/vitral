cmake_minimum_required(VERSION 3.10)
project(_OpenGL4PbufferExample CXX)

# IEEE 754 floating point everywhere: no fast-math (no reassociation, no
# finite-math or reciprocal shortcuts) and no fused multiply-add contraction,
# so results do not depend on how the compiler rewrites an expression
if(CMAKE_C_COMPILER_ID MATCHES "Clang|GNU" OR CMAKE_CXX_COMPILER_ID MATCHES "Clang|GNU")
    add_compile_options(-fno-fast-math -ffp-contract=off)
endif()

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
