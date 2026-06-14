cmake_minimum_required(VERSION 3.16)
project(tangibleInterfaceLabelsCreator LANGUAGES C CXX)

if(POLICY CMP0135)
  cmake_policy(SET CMP0135 NEW)
endif()

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

if(NOT CMAKE_BUILD_TYPE)
  set(CMAKE_BUILD_TYPE Release)
endif()

option(CAIRO_ENABLED "Enable Cairo PDF generation" ON)

if(NOT CAIRO_ENABLED)
  message(FATAL_ERROR "CAIRO_ENABLED is OFF. This project requires Cairo to generate PDF files.")
endif()

find_package(PkgConfig QUIET)
if(NOT PkgConfig_FOUND)
  message(FATAL_ERROR
    "pkg-config not found. Install it first.\n"
    "macOS (Homebrew): brew install pkg-config cairo\n"
    "Linux (apt): sudo apt-get update && sudo apt-get install -y pkg-config libcairo2-dev")
endif()

pkg_check_modules(CAIRO QUIET IMPORTED_TARGET cairo)
if(NOT CAIRO_FOUND)
  message(FATAL_ERROR
    "Cairo not found. Install it first.\n"
    "macOS (Homebrew): brew install cairo\n"
    "Linux (apt): sudo apt-get update && sudo apt-get install -y libcairo2-dev")
endif()

set(VITRAL_BUILD_TESTS OFF CACHE BOOL "Build Vitral tests" FORCE)
if(NOT TARGET vitral_base)
  add_subdirectory(../../../base base)
endif()

add_executable(tangibleInterfaceLabelsCreator
  src/main.cpp
  src/TangibleInterfaceLabelsCreator.cpp
  src/model/Label.cpp
  src/model/LabelsModel.cpp
  src/options/CommandLineOptions.cpp
  src/processing/IconGenerator.cpp
  src/processing/StyledCalligraphic2DBuffer.cpp
  src/render/CairoPdfCalligraphic2DBufferRenderer.cpp
  src/render/CairoPdfLabelRenderer.cpp
  src/render/CairoPdfPageRenderer.cpp
)

target_include_directories(tangibleInterfaceLabelsCreator PRIVATE
  ${CMAKE_CURRENT_SOURCE_DIR}/src
)

target_link_libraries(tangibleInterfaceLabelsCreator PRIVATE
  vitral_base
  PkgConfig::CAIRO
)

set_target_properties(tangibleInterfaceLabelsCreator PROPERTIES
  RUNTIME_OUTPUT_DIRECTORY "${CMAKE_CURRENT_LIST_DIR}/build"
)
