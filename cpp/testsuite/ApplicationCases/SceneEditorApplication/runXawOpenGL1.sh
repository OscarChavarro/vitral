#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VARIANT="xaw_opengl1"
BUILD_DIR="$ROOT_DIR/build-cmake-$VARIANT"

cmake -S "$ROOT_DIR" -B "$BUILD_DIR" -DSCENE_EDITOR_VARIANT="$VARIANT"
cmake --build "$BUILD_DIR" --config Release
"$ROOT_DIR/build/$VARIANT/SceneEditorApplication" "$@"
