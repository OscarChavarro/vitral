#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "$0")" && pwd)"
cd "${script_dir}"

if [[ ! -x "./build/WireframeOfflineExample" ]]; then
  cmake -S . -B build
  cmake --build build --target WireframeOfflineExample
fi

if [[ $# -gt 0 ]]; then
  ./build/WireframeOfflineExample "$@"
else
  ./build/WireframeOfflineExample
fi
