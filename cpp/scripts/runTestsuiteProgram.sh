#!/usr/bin/env bash
# Builds (incrementally) one testsuite target in the shared cpp/build tree and
# runs it from the caller's program directory, forwarding the arguments.
# This is the C++ counterpart of the Java testsuite `gradle ... runMain` call
# used by java/testsuite/*/*/run.sh.
#
# Usage: runTestsuiteProgram.sh <programDir> <cmakeTarget> <executable> [args...]
#   programDir  directory used as working directory (usually the run.sh dir)
#   cmakeTarget target name in cpp/CMakeLists.txt
#   executable  path of the produced binary, relative to programDir
set -euo pipefail

if [ "$#" -lt 3 ]; then
    echo "Usage: $0 <programDir> <cmakeTarget> <executable> [args...]" >&2
    exit 2
fi

PROGRAM_DIR="$1"
TARGET="$2"
EXECUTABLE="$3"
shift 3

CPP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_DIR="${VITRAL_CPP_BUILD_DIR:-$CPP_DIR/build}"
JOBS="$(nproc 2>/dev/null || getconf _NPROCESSORS_ONLN 2>/dev/null || echo 1)"

# Quiet like `gradle --quiet`: the build log is shown only when it fails.
quiet() {
    local log
    if ! log="$("$@" 2>&1)"; then
        echo "$log" >&2
        exit 1
    fi
}

if [ ! -f "$BUILD_DIR/CMakeCache.txt" ]; then
    quiet cmake -S "$CPP_DIR" -B "$BUILD_DIR" -DWITH_JPEG=ON
fi
quiet cmake --build "$BUILD_DIR" --target "$TARGET" -j"$JOBS"

cd "$PROGRAM_DIR"
exec "$EXECUTABLE" "$@"
