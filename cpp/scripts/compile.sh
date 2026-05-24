#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_DIR="$ROOT_DIR/build"
JOBS="$(nproc 2>/dev/null || getconf _NPROCESSORS_ONLN 2>/dev/null || echo 1)"

CMAKE_ARGS=(-S "$ROOT_DIR" -B "$BUILD_DIR" -DWITH_JPEG=ON)

# Optional: pass OCCT_ROOT to enable the booleanSetOperator target.
# Set the environment variable before calling this script, e.g.:
#   OCCT_ROOT=/path/to/occt ./compile.sh
if [ -n "${OCCT_ROOT:-}" ]; then
  CMAKE_ARGS+=("-DOCCT_ROOT=${OCCT_ROOT}")
fi

cmake "${CMAKE_ARGS[@]}"
cmake --build "$BUILD_DIR" -j"$JOBS"
ctest --test-dir "$BUILD_DIR" --output-on-failure
