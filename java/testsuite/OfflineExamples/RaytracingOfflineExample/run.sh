#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../../.."

if [[ $# -gt 0 ]]; then
  gradle --quiet :testsuite:OfflineExamples:RaytracingOfflineExample:runMain \
    -PrunMainClass=RaytracerSimple \
    -PrunJvmArgs='-Xms300m|-Xmx300m' \
    --args="$*"
else
  gradle --quiet :testsuite:OfflineExamples:RaytracingOfflineExample:runMain \
    -PrunMainClass=RaytracerSimple \
    -PrunJvmArgs='-Xms300m|-Xmx300m'
fi
