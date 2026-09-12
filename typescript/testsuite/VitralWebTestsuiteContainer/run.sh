#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

if [[ ! -d node_modules ]]; then
    npm install
fi

# Angular/Vite caches optimized linked dependencies under .angular/cache.  The
# local @vitral/webgl package is a symlinked file dependency, so after adding
# new exports the dev server can keep serving a stale optimized bundle and fail
# before Angular bootstraps.
rm -rf "$SCRIPT_DIR/.angular/cache"

exec npm start -- \
    --host 0.0.0.0 \
    --allowed-hosts true \
    --headers "Access-Control-Allow-Origin=*" \
    --headers "Access-Control-Allow-Methods=GET, POST, PUT, PATCH, DELETE, OPTIONS" \
    --headers "Access-Control-Allow-Headers=*" \
    --headers "Referrer-Policy=unsafe-url" \
    "$@"
