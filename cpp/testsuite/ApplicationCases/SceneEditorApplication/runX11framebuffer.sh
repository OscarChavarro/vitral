#!/usr/bin/env bash
# The scene editor on the framebuffer GUI of the toolkit over its Xlib backend:
# one X11 window drawn with Xlib primitives and core fonts, with CPU rendered
# viewports (no OpenGL, no Xt). Options: -geometry WxH+X+Y (by default the
# window fills the work area of the desktop, maximized), -fn FONT (by default
# 10x20, 9x15, 8x13 or fixed), -display DISPLAY, and the GUI LANGUAGE.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VARIANT="x11_framebuffer"
BUILD_DIR="$ROOT_DIR/build-cmake-$VARIANT"

cmake -S "$ROOT_DIR" -B "$BUILD_DIR" -DSCENE_EDITOR_VARIANT="$VARIANT"
cmake --build "$BUILD_DIR" --config Release
# The application finds etc/ relative to this folder
cd "$ROOT_DIR"
"$ROOT_DIR/build/$VARIANT/SceneEditorApplication" "$@"
