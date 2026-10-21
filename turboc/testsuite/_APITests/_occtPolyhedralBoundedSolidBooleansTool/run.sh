#!/usr/bin/env bash
# Requires the cpp/build tree configured with OCCT_ROOT (see cpp/scripts/compile.sh).
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CPP_DIR="$(cd "$SCRIPT_DIR/../../.." && pwd)"
exec "$CPP_DIR/scripts/runTestsuiteProgram.sh" "$SCRIPT_DIR" \
  booleanSetOperator "${VITRAL_CPP_BUILD_DIR:-$CPP_DIR/build}/booleanSetOperator" "$@"
