#!/bin/sh
# Removes the intermediate and output files of the turboc builds (every
# build* folder of the port: <module>/build, <module>/buildtc,
# <module>/buildmk, the build-cmake-<variant> ones of the scene editor
# launchers, ...), and macOS metadata files ("._*") left when copying the
# tree to other machines. map_to_83.py and map_from_83.py run it first, so no
# build folder is moved with the folders they rename.
set -e

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

for dir in $(find "$ROOT_DIR" -type d -name 'build*' -prune | sort); do
    echo "Removing $dir"
    rm -rf "$dir"
done

find "$ROOT_DIR" -name '._*' -type f -exec rm -f {} \;
