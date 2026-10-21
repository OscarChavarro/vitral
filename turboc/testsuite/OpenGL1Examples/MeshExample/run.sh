#!/bin/sh
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
if [ "$#" -eq 0 ]; then
    set -- ../../../../etc/geometry/cow.obj
fi
exec sh "$SCRIPT_DIR/../../../scripts/runTestsuiteProgram.sh" "$SCRIPT_DIR" \
  MeshExample ./build/MeshExample "$@"
