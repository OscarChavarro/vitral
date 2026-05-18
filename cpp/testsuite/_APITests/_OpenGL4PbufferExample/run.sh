#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_DIR="$SCRIPT_DIR/build"

if [ ! -x "$BUILD_DIR/_OpenGL4PbufferExample" ]; then
  cmake -S "$SCRIPT_DIR" -B "$BUILD_DIR"
  cmake --build "$BUILD_DIR" --target _OpenGL4PbufferExample
fi

"$BUILD_DIR/_OpenGL4PbufferExample"
