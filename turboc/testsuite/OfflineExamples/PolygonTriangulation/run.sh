#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -ne 1 ]; then
  echo "Usage: $0 <polygon_file>" >&2
  exit 1
fi

# The program runs from its own directory: resolve the file from the caller's.
DATA_FILE="$1"
if [[ "${DATA_FILE}" != /* ]]; then
  DATA_FILE="$(pwd)/${DATA_FILE}"
fi
if [[ ! -f "${DATA_FILE}" ]]; then
  echo "Data file not found: ${DATA_FILE}" >&2
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec "$SCRIPT_DIR/../../../scripts/runTestsuiteProgram.sh" "$SCRIPT_DIR" \
  triangulatePolygon2D ./build/triangulatePolygon2D "${DATA_FILE}"
