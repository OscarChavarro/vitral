#!/bin/sh
set -e

if [ "$#" -ne 1 ]; then
  echo "Usage: $0 <polygon_file>" >&2
  exit 1
fi

DATA_FILE="$1"
case "$DATA_FILE" in
  /*) ;;
  *) DATA_FILE="$(pwd)/$DATA_FILE" ;;
esac
if [ ! -f "$DATA_FILE" ]; then
  echo "Data file not found: $DATA_FILE" >&2
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
exec sh "$SCRIPT_DIR/../../../scripts/runTestsuiteProgram.sh" "$SCRIPT_DIR" \
  triangulatePolygon2D ./build/triangulatePolygon2D "$DATA_FILE"
