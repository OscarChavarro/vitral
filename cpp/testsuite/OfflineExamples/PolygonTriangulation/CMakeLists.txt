cmake_minimum_required(VERSION 3.16)
project(polygon_triangulation_offline LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "${CMAKE_CURRENT_LIST_DIR}/build")

if(NOT TARGET vitral_base)
    get_filename_component(VITRAL_BASE_DIR "${CMAKE_CURRENT_LIST_DIR}/../../../base" ABSOLUTE)
    if(EXISTS "${VITRAL_BASE_DIR}/CMakeLists.txt")
        set(VITRAL_BUILD_TESTS OFF CACHE BOOL "Build tests" FORCE)
        add_subdirectory("${VITRAL_BASE_DIR}" "${CMAKE_CURRENT_BINARY_DIR}/vitral_base")
    else()
        message(FATAL_ERROR "vitral_base not found at ${VITRAL_BASE_DIR}")
    endif()
endif()

add_executable(triangulatePolygon2D
    src/triangulatePolygon2D.cpp
)

target_include_directories(triangulatePolygon2D PRIVATE
    "${CMAKE_CURRENT_LIST_DIR}/../../../base/src"
    "${CMAKE_CURRENT_LIST_DIR}/../../../base/src/main"
)

target_link_libraries(triangulatePolygon2D PRIVATE vitral_base)

target_compile_options(triangulatePolygon2D PRIVATE
    -Wall
    -pedantic
    -Wextra
    -Werror
)
