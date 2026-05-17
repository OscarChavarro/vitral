#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_DIR="$SCRIPT_DIR/build"

if [ ! -f "$BUILD_DIR/_OpenGL4HelloWorld" ]; then
    echo "Executable not found. Building..."
    bash "$SCRIPT_DIR/build.sh"
fi

echo "Running _OpenGL4HelloWorld..."
"$BUILD_DIR/_OpenGL4HelloWorld"
