#!/bin/sh
# Usage: run.sh [--glut]
#   default: Mesa offscreen rendering (OSMesa), no display needed (the GLUT
#            variant when OSMesa was not found)
#   --glut:  back buffer of a GLUT window read with glReadPixels (needs a
#            display)
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
TARGET=_OpenGL1PbufferExample
if [ "${1:-}" = "--glut" ]; then
    TARGET=_OpenGL1PbufferExampleGlut
    shift
fi
exec sh "$SCRIPT_DIR/../../../scripts/runTestsuiteProgram.sh" "$SCRIPT_DIR" \
  "$TARGET" "./build/$TARGET" "$@"
