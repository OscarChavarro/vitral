#!/bin/sh
# Builds and runs the Athena (Xaw) + OpenGL 1.2 variant
exec sh "$(cd "$(dirname "$0")" && pwd)/runVariant.sh" xaw_opengl1 "$@"
