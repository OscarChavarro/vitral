#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "$0")" && pwd)"
cd "${script_dir}"

if [[ ! -x "./build/Rasterizer2DExample" ]]; then
  cmake -S . -B build
  cmake --build build --target Rasterizer2DExample
fi

./build/Rasterizer2DExample line output1.png
