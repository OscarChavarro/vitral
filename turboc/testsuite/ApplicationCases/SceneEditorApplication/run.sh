#!/bin/sh
# Runs the default variant (Motif); set SCENE_EDITOR_VARIANT to choose the
# other one: xm_opengl1 or xaw_opengl1. The turboc port has no OpenGL 4.1
# nor GTK variants.
exec sh "$(cd "$(dirname "$0")" && pwd)/runVariant.sh" \
    "${SCENE_EDITOR_VARIANT:-xm_opengl1}" "$@"
