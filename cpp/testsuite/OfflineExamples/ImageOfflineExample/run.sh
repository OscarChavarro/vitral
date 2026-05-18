#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "$0")" && pwd)"
cd "${script_dir}"

if [[ ! -x "./build/ImageOfflineExample" ]]; then
  cmake -S . -B build
  cmake --build build --target ImageOfflineExample
fi

if [[ $# -gt 0 ]]; then
  ./build/ImageOfflineExample "$@"
else
  ./build/ImageOfflineExample
fi
