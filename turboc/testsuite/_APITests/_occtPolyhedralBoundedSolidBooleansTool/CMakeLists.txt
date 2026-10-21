# Optional target: requires OpenCASCADE (OCCT).
# When OCCT is not found the target is silently skipped and the rest of
# the build continues without errors.
#
# To build this target, clone and install OCCT from:
#   https://github.com/Open-Cascade-SAS/OCCT
# then re-run cmake passing the install root, e.g.:
#   cmake -DOCCT_ROOT=/path/to/occt ...
# or export OCCT_ROOT before calling compile.sh.

cmake_minimum_required(VERSION 3.16)
project(booleanSetOperator LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# ── OCCT discovery ────────────────────────────────────────────────────────────
# Search order (first match wins):
#   1. cmake -DOCCT_ROOT=<path>  or  $OCCT_ROOT env var
#   2. Standard system prefixes: /usr/local  /usr  /opt/homebrew  /opt/local
#
# We resolve the cmake config directory from each candidate prefix and check
# for the config file before calling find_package, so we never execute a
# broken or incomplete OpenCASCADE config from an unrelated build tree.

# -- Step 1: build the ordered list of candidate prefixes --------------------
set(_occt_prefix_candidates)

if(DEFINED OCCT_ROOT)
  list(APPEND _occt_prefix_candidates "${OCCT_ROOT}")
elseif(DEFINED ENV{OCCT_ROOT})
  list(APPEND _occt_prefix_candidates "$ENV{OCCT_ROOT}")
else()
  # No explicit hint — try the well-known source-build path first, then system.
  list(APPEND _occt_prefix_candidates
    "/usr/local/src/occt/OCCT-7_9_3.live"
    "/usr/local"
    "/usr"
    "/opt/homebrew"
    "/opt/local")
endif()

# -- Step 2: locate the first valid OpenCASCADEConfig.cmake ------------------
set(_occt_cmake_dir "")
foreach(_prefix IN LISTS _occt_prefix_candidates)
  set(_candidate "${_prefix}/lib/cmake/opencascade")
  if(EXISTS "${_candidate}/OpenCASCADEConfig.cmake"
     OR EXISTS "${_candidate}/opencascade-config.cmake")
    set(_occt_cmake_dir "${_candidate}")
    break()
  endif()
endforeach()

# -- Step 3: invoke find_package only against the validated path -------------
if(_occt_cmake_dir)
  find_package(OpenCASCADE QUIET
    PATHS "${_occt_cmake_dir}"
    NO_DEFAULT_PATH)
else()
  set(OpenCASCADE_FOUND FALSE)
endif()

if(NOT OpenCASCADE_FOUND)
  message(STATUS "")
  message(STATUS "  [booleanSetOperator] OpenCASCADE (OCCT) was not found.")
  message(STATUS "  Searched prefixes:")
  foreach(_p IN LISTS _occt_prefix_candidates)
    message(STATUS "    ${_p}/lib/cmake/opencascade")
  endforeach()
  message(STATUS "  This target will be skipped. The rest of the build is unaffected.")
  message(STATUS "  To enable this target, clone and install OCCT from:")
  message(STATUS "    https://github.com/Open-Cascade-SAS/OCCT")
  message(STATUS "  Then re-configure passing the install root, e.g.:")
  message(STATUS "    cmake -DOCCT_ROOT=/path/to/occt ...")
  message(STATUS "  Or export OCCT_ROOT before running compile.sh.")
  message(STATUS "")
  return()
endif()

# Expose the resolved prefix so RPATH and other settings can reference it.
get_filename_component(_occt_resolved_prefix
  "${_occt_cmake_dir}/../../.." ABSOLUTE)

# ── Target ────────────────────────────────────────────────────────────────────
add_executable(booleanSetOperator
  src/main/cpp/main.cpp
)

target_include_directories(booleanSetOperator PRIVATE ${OpenCASCADE_INCLUDE_DIR})
target_link_libraries(booleanSetOperator PRIVATE ${OpenCASCADE_LIBRARIES} vitral_base)

# Place the binary in the top-level build directory so it is easy to find
# and cleaned automatically by clean.sh.
set_target_properties(booleanSetOperator PROPERTIES
  RUNTIME_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}"
  BUILD_RPATH              "${_occt_resolved_prefix}/lib"
  INSTALL_RPATH            "${_occt_resolved_prefix}/lib"
)

# Force RPATH (not RUNPATH) so indirect OCCT shared-library deps resolve.
# GNU ld only — macOS uses @rpath natively and does not support this flag.
if(UNIX AND NOT APPLE)
  target_link_options(booleanSetOperator PRIVATE "LINKER:--disable-new-dtags")
endif()
