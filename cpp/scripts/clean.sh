#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COUNT=0
tmp_list="$(mktemp)"
trap 'rm -f "$tmp_list"' EXIT

find "$ROOT_DIR" -type d -name build \
  -not -path '*/.git/*' \
  -not -path '*/node_modules/*' \
  -not -path '*/.idea/*' \
  -not -path '*/.gradle/*' | sort > "$tmp_list"

while IFS= read -r dir; do
  [ -n "$dir" ] || continue
  echo "Removing $dir"
  rm -rf "$dir"
  COUNT=$((COUNT + 1))
done < "$tmp_list"
