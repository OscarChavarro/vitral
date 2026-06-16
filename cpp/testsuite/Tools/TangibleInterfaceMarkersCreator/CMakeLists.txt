cmake_minimum_required(VERSION 3.16)
project(tangibleInterfaceMarkersCreator LANGUAGES C CXX)

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
option(WITH_APRILTAG "Use AprilTag library" ON)

set(BUILD_MARKERS_CREATOR TRUE)

if(NOT CAIRO_ENABLED)
  message(WARNING "CAIRO_ENABLED is OFF. TangibleInterfaceMarkersCreator will be skipped.")
  set(BUILD_MARKERS_CREATOR FALSE)
endif()

if(BUILD_MARKERS_CREATOR)
  find_package(PkgConfig QUIET)
  if(NOT PkgConfig_FOUND)
    message(WARNING
      "pkg-config not found. TangibleInterfaceMarkersCreator will be skipped.\n"
      "  To enable, install with:\n"
      "  macOS (Homebrew): brew install pkg-config cairo\n"
      "  Linux (apt): sudo apt-get update && sudo apt-get install -y pkg-config libcairo2-dev")
    set(BUILD_MARKERS_CREATOR FALSE)
  endif()
endif()

if(BUILD_MARKERS_CREATOR)
  pkg_check_modules(CAIRO QUIET IMPORTED_TARGET cairo)
  if(NOT CAIRO_FOUND)
    message(WARNING
      "Cairo not found. TangibleInterfaceMarkersCreator will be skipped.\n"
      "  To enable, install with:\n"
      "  macOS (Homebrew): brew install cairo\n"
      "  Linux (apt): sudo apt-get update && sudo apt-get install -y libcairo2-dev")
    set(BUILD_MARKERS_CREATOR FALSE)
  endif()
endif()

if(BUILD_MARKERS_CREATOR AND WITH_APRILTAG)
  find_package(apriltag QUIET)
  if(NOT apriltag_FOUND)
    message(WARNING
      "apriltag library not found. TangibleInterfaceMarkersCreator will be skipped.\n"
      "  To enable, install with:\n"
      "  macOS (Homebrew): brew install apriltag\n"
      "  Linux (apt): sudo apt-get update && sudo apt-get install -y libapriltag-dev")
    set(BUILD_MARKERS_CREATOR FALSE)
  else()
    if(TARGET apriltag::apriltag)
      set(APRILTAG_TARGET apriltag::apriltag)
    elseif(TARGET apriltag)
      set(APRILTAG_TARGET apriltag)
    else()
      message(WARNING "apriltag package found but targets not defined. Trying fallback...")
      find_library(APRILTAG_LIB apriltag)
      if(APRILTAG_LIB)
        set(APRILTAG_TARGET ${APRILTAG_LIB})
      else()
        message(WARNING "Could not find apriltag library. TangibleInterfaceMarkersCreator will be skipped.")
        set(BUILD_MARKERS_CREATOR FALSE)
      endif()
    endif()
  endif()
endif()

set(VITRAL_BUILD_TESTS OFF CACHE BOOL "Build Vitral tests" FORCE)
if(NOT TARGET vitral_base)
  add_subdirectory(../../../base base)
endif()

if(BUILD_MARKERS_CREATOR)
  add_executable(tangibleInterfaceMarkersCreator
  src/main.cpp
  src/TangibleInterfaceMarkersCreator.cpp
  src/model/Marker.cpp
  src/model/Grid.cpp
  src/model/MarkersModel.cpp
  src/options/CommandLineOptions.cpp
  src/processing/MarkerGenerator.cpp
  src/processing/ConstructivePolygonGeometryProcessor.cpp
  src/render/CairoPdfMarkerRenderer.cpp
  src/render/CairoPdfPageRenderer.cpp
)

target_include_directories(tangibleInterfaceMarkersCreator PRIVATE
  ${CMAKE_CURRENT_SOURCE_DIR}/src
  ${apriltag_SOURCE_DIR}
)

target_link_libraries(tangibleInterfaceMarkersCreator PRIVATE
  vitral_base
  PkgConfig::CAIRO
  ${APRILTAG_TARGET}
)

set_target_properties(tangibleInterfaceMarkersCreator PROPERTIES
  RUNTIME_OUTPUT_DIRECTORY "${CMAKE_CURRENT_LIST_DIR}/build"
)
else()
  message(STATUS "TangibleInterfaceMarkersCreator skipped due to missing dependencies")
endif()
