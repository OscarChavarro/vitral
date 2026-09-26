cmake_minimum_required(VERSION 3.16)
project(vitral_cpp LANGUAGES CXX)

if(POLICY CMP0072)
  cmake_policy(SET CMP0072 NEW)
endif()

if(UNIX AND NOT APPLE)
  set(OpenGL_GL_PREFERENCE GLVND)
endif()

if(NOT CMAKE_CONFIGURATION_TYPES AND NOT CMAKE_BUILD_TYPE)
  set(CMAKE_BUILD_TYPE Release CACHE STRING "Build type" FORCE)
endif()

if(CMAKE_CXX_COMPILER_ID MATCHES "Clang|GNU")
  add_compile_options(
    "$<$<CONFIG:Release>:-O3>"
    "$<$<CONFIG:RelWithDebInfo>:-O3>"
    "$<$<CONFIG:MinSizeRel>:-O2>")
endif()

if(APPLE)
  # Sanitize stale CLT SDK paths that may leak from environment/toolchain cache.
  set(_STALE_FRAMEWORK_DIR "/Library/Developer/CommandLineTools/SDKs/MacOSX14.sdk/System/Library/Frameworks")
  foreach(_var
      CMAKE_EXE_LINKER_FLAGS
      CMAKE_SHARED_LINKER_FLAGS
      CMAKE_MODULE_LINKER_FLAGS
      CMAKE_EXE_LINKER_FLAGS_DEBUG
      CMAKE_EXE_LINKER_FLAGS_RELEASE
      CMAKE_SHARED_LINKER_FLAGS_DEBUG
      CMAKE_SHARED_LINKER_FLAGS_RELEASE
      CMAKE_MODULE_LINKER_FLAGS_DEBUG
      CMAKE_MODULE_LINKER_FLAGS_RELEASE)
    if(DEFINED ${_var})
      string(REPLACE "-F${_STALE_FRAMEWORK_DIR}" "" ${_var} "${${_var}}")
      string(REPLACE "${_STALE_FRAMEWORK_DIR}" "" ${_var} "${${_var}}")
    endif()
  endforeach()

  # Prefer the active SDK path from xcrun when available.
  execute_process(
    COMMAND xcrun --sdk macosx --show-sdk-path
    OUTPUT_VARIABLE _ACTIVE_MACOS_SDK
    OUTPUT_STRIP_TRAILING_WHITESPACE
    ERROR_QUIET)
  if(_ACTIVE_MACOS_SDK AND EXISTS "${_ACTIVE_MACOS_SDK}")
    set(CMAKE_OSX_SYSROOT "${_ACTIVE_MACOS_SDK}" CACHE PATH "macOS SDK root" FORCE)
  endif()
endif()

include(CTest)
enable_testing()

# Naming rules of attributes and accessors, shared by the three ports.
find_package(Python3 COMPONENTS Interpreter)
if(Python3_Interpreter_FOUND)
  add_test(NAME attribute_names
    COMMAND ${Python3_EXECUTABLE} ${CMAKE_SOURCE_DIR}/../scripts/checkAttributeNames.py --all
    WORKING_DIRECTORY ${CMAKE_SOURCE_DIR}/..)
  # The Xt layer uses neither Athena nor Motif, and they never use each other
  add_test(NAME xt_layers
    COMMAND ${Python3_EXECUTABLE} ${CMAKE_SOURCE_DIR}/../scripts/checkXtLayers.py
    WORKING_DIRECTORY ${CMAKE_SOURCE_DIR}/..)
endif()

# Widget set of the Xt GUIs, chosen when building: an executable links
# either Motif or Athena, never both (they must not share a process)
set(VITRAL_XT_WIDGET_SET "xm" CACHE STRING
    "Widget set of the Xt GUIs: xm (Motif) or xaw (Athena)")
set_property(CACHE VITRAL_XT_WIDGET_SET PROPERTY STRINGS xm xaw)
if(NOT VITRAL_XT_WIDGET_SET STREQUAL "xm" AND
   NOT VITRAL_XT_WIDGET_SET STREQUAL "xaw")
  message(FATAL_ERROR
    "VITRAL_XT_WIDGET_SET must be xm (Motif) or xaw (Athena), not "
    "'${VITRAL_XT_WIDGET_SET}'")
endif()

# Make java/lang helpers visible from every subproject target.
include_directories(${CMAKE_SOURCE_DIR}/base/src/main)

# Root controller for all C++ subprojects under cpp/
add_subdirectory(base)
add_subdirectory(glut)
add_subdirectory(glfw)
add_subdirectory(opengl4)
# Xt is the GUI of the X Window System: not available on macOS (XQuartz
# lacks the OpenGL 4 GLX the applications using it need) nor Windows. Only
# the chosen widget set over it is built.
if(UNIX AND NOT APPLE)
  add_subdirectory(xt)
  add_subdirectory(${VITRAL_XT_WIDGET_SET})
endif()
add_subdirectory(testsuite)
