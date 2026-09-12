#!/usr/bin/env bash
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
typescript_dir=$(CDPATH= cd -- "$script_dir/../../.." && pwd)

npm --prefix "$typescript_dir" run build
npm --prefix "$script_dir" run build

exec node "$script_dir/dist/PolygonTest.js" "${@:-output2.png}"
