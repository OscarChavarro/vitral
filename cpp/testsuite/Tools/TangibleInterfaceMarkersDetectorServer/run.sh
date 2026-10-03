#!/usr/bin/env bash
# Standalone build: this tool needs OpenCV and AprilTag, which the shared
# cpp/build tree does not require.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_DIR="$SCRIPT_DIR/build-release"
cd "$SCRIPT_DIR"

if [ ! -f "$BUILD_DIR/CMakeCache.txt" ]; then
    cmake -S "$SCRIPT_DIR" -B "$BUILD_DIR" -DCMAKE_BUILD_TYPE=Release > /dev/null
fi
cmake --build "$BUILD_DIR" --target tangibleInterfaceServer > /dev/null

if [ "$#" -eq 0 ]; then
    set -- -cam 3 -preview
fi
exec ./build/tangibleInterfaceServer "$@"
