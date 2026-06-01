#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}"
BIN="${SCRIPT_DIR}/build/triangulatePolygon2D"
DATA_FILE="${SCRIPT_DIR}/example1.polygon"

if [[ ! -x "${BIN}" ]]; then
  cmake -S . -B build
  cmake --build build --target triangulatePolygon2D
fi

if [[ ! -f "${DATA_FILE}" ]]; then
  echo "Data file not found: ${DATA_FILE}" >&2
  exit 1
fi

OUTPUT="$(${BIN} "${DATA_FILE}")"
TRIANGLE_COUNT="$(printf '%s\n' "${OUTPUT}" | grep -c '^triangle #[0-9]\+: ' || true)"

printf '%s\n' "${OUTPUT}"
