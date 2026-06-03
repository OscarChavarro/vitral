#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="${SCRIPT_DIR}/../../.."

RUN_ARGS=""
for arg in "$@"; do
  if [ -n "${RUN_ARGS}" ]; then
    RUN_ARGS="${RUN_ARGS}|${arg}"
  else
    RUN_ARGS="${arg}"
  fi
done

"${ROOT_DIR}/gradlew" --quiet :testsuite:Jogl4Examples:PolygonClippingExample:runMain \
  -PrunMainClass=PolygonClippingExample \
  -PrunJvmArgs='--add-exports=java.desktop/sun.awt=ALL-UNNAMED|--add-opens=java.desktop/sun.awt=ALL-UNNAMED' \
  -PrunArgs="${RUN_ARGS}"
