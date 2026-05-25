#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "${ROOT_DIR}"

if [[ -x "./gradlew" ]]; then
  GRADLE_CMD=("./gradlew" "--no-daemon" "-q")
else
  GRADLE_CMD=("gradle" "-q")
fi

# Fast library-only build: compile/package base module, skipping tests.
"${GRADLE_CMD[@]}" :base:assemble -x test "$@"
