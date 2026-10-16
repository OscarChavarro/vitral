cmake_minimum_required(VERSION 3.10)
project(vitral_glfw CXX)

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
