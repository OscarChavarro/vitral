#!/usr/bin/env bash
# Builds the svgalib Scene Editor and runs it. svgalib in NoHelper mode needs root and a real virtual console (tty1,
# ...): it does not work over SSH or inside X. Arguments: [LANGUAGE] [MODE], see `sceneEditor -h`.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_DIR="$ROOT_DIR/build-cmake"

cmake -S "$ROOT_DIR" -B "$BUILD_DIR" -DCMAKE_BUILD_TYPE=Release
cmake --build "$BUILD_DIR" -j"$(nproc)"
if [ "$(id -u)" -eq 0 ]; then
    "$ROOT_DIR/build/sceneEditor" "$@"
else
    sudo "$ROOT_DIR/build/sceneEditor" "$@"
fi
