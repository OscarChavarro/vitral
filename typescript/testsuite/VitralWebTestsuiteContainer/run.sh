#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

if [[ ! -d node_modules ]]; then
    npm install
fi

exec npm start -- \
    --host 0.0.0.0 \
    --allowed-hosts true \
    --headers "Access-Control-Allow-Origin=*" \
    --headers "Access-Control-Allow-Methods=GET, POST, PUT, PATCH, DELETE, OPTIONS" \
    --headers "Access-Control-Allow-Headers=*" \
    --headers "Referrer-Policy=unsafe-url" \
    "$@"
