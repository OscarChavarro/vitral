cmake_minimum_required(VERSION 3.16)
project(AlgebraicExpressionExample CXX)

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "${CMAKE_CURRENT_LIST_DIR}/build")

set(BUILD_ALGEBRAIC_EXAMPLE TRUE)

if(NOT TARGET vitral_base)
  get_filename_component(VITRAL_BASE_DIR "${CMAKE_CURRENT_LIST_DIR}/../../../base" ABSOLUTE)
  if(EXISTS "${VITRAL_BASE_DIR}/CMakeLists.txt")
    add_subdirectory("${VITRAL_BASE_DIR}" "${CMAKE_CURRENT_BINARY_DIR}/vitral_base")
  else()
    message(WARNING "vitral_base not found at ${VITRAL_BASE_DIR}. AlgebraicExpressionExample will be skipped.")
    set(BUILD_ALGEBRAIC_EXAMPLE FALSE)
  endif()
endif()

if(BUILD_ALGEBRAIC_EXAMPLE)
  add_executable(AlgebraicExpressionExample
    src/AlgebraicExpressionExample.cpp
  )

  target_include_directories(AlgebraicExpressionExample PRIVATE
    "${CMAKE_CURRENT_LIST_DIR}/src"
    "${CMAKE_CURRENT_LIST_DIR}/../../../base/src"
    "${CMAKE_CURRENT_LIST_DIR}/../../../base/src/main"
  )

  target_link_libraries(AlgebraicExpressionExample PRIVATE vitral_base)

  if(APPLE)
    target_compile_definitions(AlgebraicExpressionExample PRIVATE GL_SILENCE_DEPRECATION)
    set(VITRAL_CODESIGN_IDENTITY "$ENV{VITRAL_CODESIGN_IDENTITY}")
    if(VITRAL_CODESIGN_IDENTITY STREQUAL "")
      set(VITRAL_CODESIGN_IDENTITY "-")
    endif()
    add_custom_command(
      TARGET AlgebraicExpressionExample
      POST_BUILD
      COMMAND /usr/bin/codesign --force --sign "${VITRAL_CODESIGN_IDENTITY}" --timestamp=none $<TARGET_FILE:AlgebraicExpressionExample>
      VERBATIM
      COMMENT "Code-signing AlgebraicExpressionExample (${VITRAL_CODESIGN_IDENTITY})")
  endif()
else()
  message(STATUS "AlgebraicExpressionExample skipped due to missing dependencies")
endif()
