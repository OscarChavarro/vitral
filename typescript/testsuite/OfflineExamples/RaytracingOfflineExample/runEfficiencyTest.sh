#!/usr/bin/env bash
clear
# The Java script caps the JVM heap at -Xms300m -Xmx300m; the Node counterpart
# is --max-old-space-size, in megabytes.
rm -f output.ppm output.bmp

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$script_dir"
time node --max-old-space-size=300 "$script_dir/dist/RaytracerSimple.js" "$@" nosave
cd ..
