#!/usr/bin/env bash
# Builds and runs the editor. Its variant is chosen when building, with the
# widget set of its Xt GUI (xm: Motif, xaw: Athena) and the OpenGL version of
# its drawing area (opengl4: 4.1 core, opengl1: 1.2 fixed function):
# SCENE_EDITOR_VARIANT=xm_opengl4 (the default), xaw_opengl4, xm_opengl1 or
# xaw_opengl1, each one in its own build folder, i.e.:
#   ./run.sh -s
#   SCENE_EDITOR_VARIANT=xaw_opengl1 ./run.sh -s
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
VARIANT="${SCENE_EDITOR_VARIANT:-xm_opengl4}"
BUILD_DIR="$ROOT_DIR/build-cmake-$VARIANT"
mkdir -p "$BUILD_DIR"
cd "$BUILD_DIR"
cmake -DSCENE_EDITOR_VARIANT="$VARIANT" ..
cmake --build . --config Release
cd "$ROOT_DIR"
# On macOS the Motif variants use the Mesa of Homebrew: its direct software
# rendering fails over the MIT-SHM of XQuartz (BadShmSeg), so it renders
# through the indirect GLX of XQuartz (needs, once, and restarting XQuartz:
# defaults write org.xquartz.X11 enable_iglx -bool true)
if [[ "$(uname)" == "Darwin" && "$VARIANT" == xm_* ]]; then
    export LIBGL_ALWAYS_INDIRECT="${LIBGL_ALWAYS_INDIRECT:-1}"
fi
"./build/$VARIANT/SceneEditorApplication" "$@"
