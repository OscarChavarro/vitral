#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VARIANTS=(xaw_opengl1 xaw_opengl4 xm_opengl1 xm_opengl4)

for VARIANT in "${VARIANTS[@]}"; do
    BUILD_DIR="$ROOT_DIR/build-cmake-$VARIANT"
    cmake -S "$ROOT_DIR" -B "$BUILD_DIR" -DSCENE_EDITOR_VARIANT="$VARIANT"
    cmake --build "$BUILD_DIR" --config Release
done
