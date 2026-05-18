#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "$0")" && pwd)"
cd "${script_dir}"

if [[ ! -x "./build/AlgebraicExpressionExample" ]]; then
  cmake -S . -B build
  cmake --build build --target AlgebraicExpressionExample
fi

if [[ $# -gt 0 ]]; then
  ./build/AlgebraicExpressionExample "$@"
else
  ./build/AlgebraicExpressionExample
fi
