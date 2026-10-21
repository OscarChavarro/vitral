#!/bin/sh
# Removes the intermediate and output files of the turboc builds
# (<module>/buildtc and <module>/build, and every
# testsuite/ApplicationCases/SceneEditorApplication/build*), and macOS
# metadata files ("._*") left when copying the tree to other machines.
set -e

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

for dir in $(find "$ROOT_DIR" -type d \( -name buildtc -o -name build \) \
        -prune | sort); do
    echo "Removing $dir"
    rm -rf "$dir"
done

# Every build folder of the scene editor (build, buildtc, and the
# build-cmake-<variant> ones of the cpp port launchers)
for dir in "$ROOT_DIR"/testsuite/ApplicationCases/SceneEditorApplication/build*; do
    if [ -d "$dir" ]; then
        echo "Removing $dir"
        rm -rf "$dir"
    fi
done

find "$ROOT_DIR" -name '._*' -type f -exec rm -f {} \;
