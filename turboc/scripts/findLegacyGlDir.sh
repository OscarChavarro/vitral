#!/bin/sh
# Prints the prefix of the OpenGL implementation for the legacy Makefiles
# (GL_DIR of legacy.mk), or nothing when it is in the system folders or
# missing: $GL_DIR if set, else the newest Mesa built from its sources in
# /usr/local/src (i.e. /usr/local/src/Mesa-3.0, with include/ and lib/).
if [ -n "$GL_DIR" ]; then
    echo "$GL_DIR"
    exit 0
fi
found=""
for dir in /usr/local/src/Mesa-*; do
    if [ -f "$dir/include/GL/gl.h" ] && [ -d "$dir/lib" ]; then
        found="$dir"
    fi
done
echo "$found"
