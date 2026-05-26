cmake_minimum_required(VERSION 3.10)
project(_OpenGL4HelloWorld CXX)

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_package(OpenGL REQUIRED)
find_package(GLEW REQUIRED)
find_package(glfw3 REQUIRED)
find_library(VITRAL_GLEW_LIB NAMES GLEW glew REQUIRED)

set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "${CMAKE_CURRENT_LIST_DIR}/build")

add_executable(_OpenGL4HelloWorld
    src/HelloWorldOpenGL4.cpp
)

target_link_libraries(_OpenGL4HelloWorld
    OpenGL::GL
    ${VITRAL_GLEW_LIB}
    glfw
    vitral_base
)
target_include_directories(_OpenGL4HelloWorld SYSTEM PRIVATE ${GLEW_INCLUDE_DIRS})

if(APPLE)
    target_compile_definitions(_OpenGL4HelloWorld PRIVATE GL_SILENCE_DEPRECATION)
endif()
