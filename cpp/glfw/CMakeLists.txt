cmake_minimum_required(VERSION 3.10)
project(vitral_glfw CXX)

# IEEE 754 floating point everywhere: no fast-math (no reassociation, no
# finite-math or reciprocal shortcuts) and no fused multiply-add contraction,
# so results do not depend on how the compiler rewrites an expression
if(CMAKE_C_COMPILER_ID MATCHES "Clang|GNU" OR CMAKE_CXX_COMPILER_ID MATCHES "Clang|GNU")
    add_compile_options(-fno-fast-math -ffp-contract=off)
endif()

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(CMAKE_LIBRARY_OUTPUT_DIRECTORY "${CMAKE_CURRENT_LIST_DIR}/build")

find_package(glfw3 REQUIRED)

add_library(vitral_glfw SHARED
    src/main/vsdk/toolkit/gui/GlfwSystem.cpp
)

target_include_directories(vitral_glfw PUBLIC src/main)
target_include_directories(vitral_glfw PRIVATE ${CMAKE_CURRENT_LIST_DIR}/../base/src/main)
target_link_libraries(vitral_glfw PUBLIC glfw)
target_link_libraries(vitral_glfw PRIVATE vitral_base)

# Text labels (GlfwSystem::calculateLabelImage) are rasterized with FreeType 2
find_package(Freetype)
if(FREETYPE_FOUND)
    target_link_libraries(vitral_glfw PRIVATE Freetype::Freetype)
    target_compile_definitions(vitral_glfw PRIVATE VITRAL_GLFW_WITH_FREETYPE)
else()
    message(WARNING "FreeType 2 library not found. GLFW text labels will be empty.")
endif()
