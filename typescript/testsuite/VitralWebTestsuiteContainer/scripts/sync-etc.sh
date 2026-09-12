#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd -- "$SCRIPT_DIR/.." && pwd)"
ETC_SOURCE="$PROJECT_DIR/../../../etc"
ETC_PUBLIC="$PROJECT_DIR/public/etc"

if [[ ! -d "$ETC_SOURCE" ]]; then
    echo "Missing VITRAL etc directory: $ETC_SOURCE" >&2
    exit 1
fi

if [[ -L "$ETC_PUBLIC" ]]; then
    unlink "$ETC_PUBLIC"
fi

mkdir -p "$ETC_PUBLIC"
cp -a "$ETC_SOURCE"/. "$ETC_PUBLIC"/
