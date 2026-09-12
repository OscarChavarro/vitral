#!/usr/bin/env bash
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
typescript_dir=$(CDPATH= cd -- "$script_dir/../../.." && pwd)

# The example consumes the `base` and `fs` Vitral modules as built packages,
# so their workspace outputs must exist before this project is compiled.
npm --prefix "$typescript_dir" run build
npm --prefix "$script_dir" run build

exec node "$script_dir/dist/AlgebraicExpressionExample.js" "$@"
