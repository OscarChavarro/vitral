#!/usr/bin/env sh
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
typescript_dir=$(CDPATH= cd -- "$script_dir/.." && pwd)

rm -rf "$typescript_dir/base/dist" \
  "$typescript_dir/base/tsconfig.tsbuildinfo" \
  "$typescript_dir/fs/dist" \
  "$typescript_dir/fs/tsconfig.tsbuildinfo" \
  "$typescript_dir/testsuite/_APITests/ConcurrencyWorkersForBrowsers/dist" \
  "$typescript_dir/testsuite/_APITests/ConcurrencyWorkersForBrowsers/tsconfig.tsbuildinfo" \
  "$typescript_dir/testsuite/_APITests/ConcurrencyWorkersForBrowsers/tsconfig.worker.tsbuildinfo" \
  "$typescript_dir/testsuite/_APITests/ConcurrencyFibers/dist" \
  "$typescript_dir/testsuite/_APITests/ConcurrencyFibers/tsconfig.tsbuildinfo" \
  "$typescript_dir/tsconfig.tsbuildinfo" \
  "$typescript_dir/coverage" \
  "$typescript_dir/artifacts" \
  "${TMPDIR:-/tmp}/vitral-npm-cache"
