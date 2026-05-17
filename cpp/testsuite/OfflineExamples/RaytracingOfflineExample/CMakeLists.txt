cmake_minimum_required(VERSION 3.16)
project(RaytracingOfflineExample CXX)

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "${CMAKE_CURRENT_LIST_DIR}/build")

# When building standalone from this folder's build directory,
# bring vitral_base as subproject.
if(NOT TARGET vitral_base)
  get_filename_component(VITRAL_BASE_DIR "${CMAKE_CURRENT_LIST_DIR}/../../../base" ABSOLUTE)
  if(EXISTS "${VITRAL_BASE_DIR}/CMakeLists.txt")
    add_subdirectory("${VITRAL_BASE_DIR}" "${CMAKE_CURRENT_BINARY_DIR}/vitral_base")
  else()
    message(FATAL_ERROR "vitral_base not found at ${VITRAL_BASE_DIR}")
  endif()
endif()

add_executable(RaytracingOfflineExample
  src/RaytracerSimple.cpp
  src/CommandOptionsProcessor.cpp
  src/ImageExporter.cpp
  src/RaytracerSerialExecutor.cpp
  src/RaytracerParallelExecutor.cpp
)

target_include_directories(RaytracingOfflineExample PRIVATE
  "${CMAKE_CURRENT_LIST_DIR}/src"
  "${CMAKE_CURRENT_LIST_DIR}/../../../base/src"
  "${CMAKE_CURRENT_LIST_DIR}/../../../base/src/main"
)

target_link_libraries(RaytracingOfflineExample PRIVATE vitral_base)

if(APPLE)
  target_compile_definitions(RaytracingOfflineExample PRIVATE GL_SILENCE_DEPRECATION)

  # Sign the executable on macOS so profiling/instrumentation tools can run it.
  # Use an explicit identity if provided; otherwise use ad-hoc signing.
  set(VITRAL_CODESIGN_IDENTITY "$ENV{VITRAL_CODESIGN_IDENTITY}")
  if(VITRAL_CODESIGN_IDENTITY STREQUAL "")
    set(VITRAL_CODESIGN_IDENTITY "-")
  endif()

  add_custom_command(
    TARGET RaytracingOfflineExample
    POST_BUILD
    COMMAND /usr/bin/codesign --force --sign "${VITRAL_CODESIGN_IDENTITY}" --timestamp=none $<TARGET_FILE:RaytracingOfflineExample>
    VERBATIM
    COMMENT "Code-signing RaytracingOfflineExample (${VITRAL_CODESIGN_IDENTITY})")
endif()
