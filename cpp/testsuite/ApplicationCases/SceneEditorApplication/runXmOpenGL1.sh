#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VARIANT="xm_opengl1"
BUILD_DIR="$ROOT_DIR/build-cmake-$VARIANT"

cmake -S "$ROOT_DIR" -B "$BUILD_DIR" -DSCENE_EDITOR_VARIANT="$VARIANT"
cmake --build "$BUILD_DIR" --config Release

# Homebrew Mesa uses indirect GLX with XQuartz on macOS.
if [[ "$(uname)" == "Darwin" ]]; then
    export LIBGL_ALWAYS_INDIRECT="${LIBGL_ALWAYS_INDIRECT:-1}"
fi
"$ROOT_DIR/build/$VARIANT/SceneEditorApplication" "$@"
