#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "$0")" && pwd)"
cd "${script_dir}"

if [[ ! -x "./build/RaytracingOfflineExample" ]]; then
  cmake -S . -B build
  cmake --build build --target RaytracingOfflineExample
fi

if [[ $# -gt 0 ]]; then
  ./build/RaytracingOfflineExample "$@"
else
  ./build/RaytracingOfflineExample
fi
