cmake_minimum_required(VERSION 3.16)
project(ImageOfflineExample CXX)

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "${CMAKE_CURRENT_LIST_DIR}/build")

if(NOT TARGET vitral_base)
  get_filename_component(VITRAL_BASE_DIR "${CMAKE_CURRENT_LIST_DIR}/../../../base" ABSOLUTE)
  if(EXISTS "${VITRAL_BASE_DIR}/CMakeLists.txt")
    add_subdirectory("${VITRAL_BASE_DIR}" "${CMAKE_CURRENT_BINARY_DIR}/vitral_base")
  else()
    message(FATAL_ERROR "vitral_base not found at ${VITRAL_BASE_DIR}")
  endif()
endif()

add_executable(ImageOfflineExample
  src/ImageOfflineExample.cpp
)

target_include_directories(ImageOfflineExample PRIVATE
  "${CMAKE_CURRENT_LIST_DIR}/src"
  "${CMAKE_CURRENT_LIST_DIR}/../../../base/src"
  "${CMAKE_CURRENT_LIST_DIR}/../../../base/src/main"
)

target_link_libraries(ImageOfflineExample PRIVATE vitral_base)

if(APPLE)
  target_compile_definitions(ImageOfflineExample PRIVATE GL_SILENCE_DEPRECATION)
  set(VITRAL_CODESIGN_IDENTITY "$ENV{VITRAL_CODESIGN_IDENTITY}")
  if(VITRAL_CODESIGN_IDENTITY STREQUAL "")
    set(VITRAL_CODESIGN_IDENTITY "-")
  endif()
  add_custom_command(
    TARGET ImageOfflineExample
    POST_BUILD
    COMMAND /usr/bin/codesign --force --sign "${VITRAL_CODESIGN_IDENTITY}" --timestamp=none $<TARGET_FILE:ImageOfflineExample>
    VERBATIM
    COMMENT "Code-signing ImageOfflineExample (${VITRAL_CODESIGN_IDENTITY})")
endif()
