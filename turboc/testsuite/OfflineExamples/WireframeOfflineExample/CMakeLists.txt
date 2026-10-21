cmake_minimum_required(VERSION 3.16)
project(WireframeOfflineExample CXX)

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "${CMAKE_CURRENT_LIST_DIR}/build")

set(BUILD_WIREFRAME_EXAMPLE TRUE)

if(NOT TARGET vitral_base)
  get_filename_component(VITRAL_BASE_DIR "${CMAKE_CURRENT_LIST_DIR}/../../../base" ABSOLUTE)
  if(EXISTS "${VITRAL_BASE_DIR}/CMakeLists.txt")
    set(WITH_JPEG ON CACHE BOOL "" FORCE)
    add_subdirectory("${VITRAL_BASE_DIR}" "${CMAKE_CURRENT_BINARY_DIR}/vitral_base")
  else()
    message(WARNING "vitral_base not found at ${VITRAL_BASE_DIR}. WireframeOfflineExample will be skipped.")
    set(BUILD_WIREFRAME_EXAMPLE FALSE)
  endif()
endif()

if(BUILD_WIREFRAME_EXAMPLE)
  add_executable(WireframeOfflineExample
    src/WireframeOfflineExample.cpp
  )

  target_include_directories(WireframeOfflineExample PRIVATE
    "${CMAKE_CURRENT_LIST_DIR}/src"
    "${CMAKE_CURRENT_LIST_DIR}/../../../base/src"
    "${CMAKE_CURRENT_LIST_DIR}/../../../base/src/main"
  )

  target_link_libraries(WireframeOfflineExample PRIVATE vitral_base)

  if(APPLE)
    target_compile_definitions(WireframeOfflineExample PRIVATE GL_SILENCE_DEPRECATION)
    set(VITRAL_CODESIGN_IDENTITY "$ENV{VITRAL_CODESIGN_IDENTITY}")
    if(VITRAL_CODESIGN_IDENTITY STREQUAL "")
      set(VITRAL_CODESIGN_IDENTITY "-")
    endif()
    add_custom_command(
      TARGET WireframeOfflineExample
      POST_BUILD
      COMMAND /usr/bin/codesign --force --sign "${VITRAL_CODESIGN_IDENTITY}" --timestamp=none $<TARGET_FILE:WireframeOfflineExample>
      VERBATIM
      COMMENT "Code-signing WireframeOfflineExample (${VITRAL_CODESIGN_IDENTITY})")
  endif()
else()
  message(STATUS "WireframeOfflineExample skipped due to missing dependencies")
endif()
