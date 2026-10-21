#!/bin/sh
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
exec sh "$SCRIPT_DIR/../../../scripts/runTestsuiteProgram.sh" "$SCRIPT_DIR" \
  PolygonClippingExample ./build/PolygonClippingExample "$@"
