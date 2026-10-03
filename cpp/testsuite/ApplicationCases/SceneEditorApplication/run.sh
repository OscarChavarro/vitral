#!/usr/bin/env bash
# Runs the platform default variant; set SCENE_EDITOR_VARIANT to choose another
# one (xm_opengl4, xaw_opengl4, xm_opengl1, xaw_opengl1, gtk4_opengl4).
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ -z "${SCENE_EDITOR_VARIANT:-}" ]; then
    if [ "$(uname -s)" = "Darwin" ]; then
        # XQuartz exposes only legacy OpenGL through GLX (see README.md).
        SCENE_EDITOR_VARIANT="xaw_opengl1"
    else
        SCENE_EDITOR_VARIANT="xm_opengl4"
    fi
fi

case "$SCENE_EDITOR_VARIANT" in
    xm_opengl4)   LAUNCHER="runXmOpenGL4.sh" ;;
    xaw_opengl4)  LAUNCHER="runXawOpenGL4.sh" ;;
    xm_opengl1)   LAUNCHER="runXmOpenGL1.sh" ;;
    xaw_opengl1)  LAUNCHER="runXawOpenGL1.sh" ;;
    gtk4_opengl4) LAUNCHER="runGtk4OpenGL4.sh" ;;
    *)
        echo "Unknown SCENE_EDITOR_VARIANT: $SCENE_EDITOR_VARIANT" >&2
        exit 1
        ;;
esac
exec "$SCRIPT_DIR/$LAUNCHER" "$@"
