#!/bin/sh
# Builds and runs the Motif + OpenGL 1.2 variant
exec sh "$(cd "$(dirname "$0")" && pwd)/runVariant.sh" xm_opengl1 "$@"
