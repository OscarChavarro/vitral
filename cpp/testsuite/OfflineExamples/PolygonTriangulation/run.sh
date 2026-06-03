#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -ne 1 ]; then
  echo "Usage: $0 <polygon_file>" >&2
  exit 1
fi

DATA_FILE="$1"
if [[ "${DATA_FILE}" != /* ]]; then
  DATA_FILE="$(pwd)/${DATA_FILE}"
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}"
BIN="${SCRIPT_DIR}/build/triangulatePolygon2D"

if [[ ! -x "${BIN}" ]]; then
  cmake -S . -B build
  cmake --build build --target triangulatePolygon2D
fi

if [[ ! -f "${DATA_FILE}" ]]; then
  echo "Data file not found: ${DATA_FILE}" >&2
  exit 1
fi

"${BIN}" "${DATA_FILE}"
