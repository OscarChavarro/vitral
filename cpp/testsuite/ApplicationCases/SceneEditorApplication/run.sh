#!/usr/bin/env bash
# Builds and runs the editor. The widget set of its Xt GUI is chosen when
# building: VITRAL_XT_WIDGET_SET=xm (Motif, the default) or xaw (Athena),
# each one in its own build folder, i.e.:
#   ./run.sh -s
#   VITRAL_XT_WIDGET_SET=xaw ./run.sh -s
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
WIDGET_SET="${VITRAL_XT_WIDGET_SET:-xm}"
BUILD_DIR="$ROOT_DIR/build-cmake-$WIDGET_SET"
mkdir -p "$BUILD_DIR"
cd "$BUILD_DIR"
cmake -DVITRAL_XT_WIDGET_SET="$WIDGET_SET" ..
cmake --build . --config Release
cd "$ROOT_DIR"
"./build/$WIDGET_SET/SceneEditorApplication" "$@"
