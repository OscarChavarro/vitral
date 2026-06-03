#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -ne 1 ]; then
  echo "Usage: $0 <polygon_file>" >&2
  exit 1
fi

gradle --quiet :testsuite:OfflineExamples:PolygonTriangulation:runMain \
  -PrunMainClass=PolygonTriangulation \
  -PrunArgs="$1"
