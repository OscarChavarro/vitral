#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COUNT=0
DRY_RUN=0
if [[ $# -gt 0 ]]; then
  if [[ $# -eq 1 && "$1" == "-n" ]]; then
    DRY_RUN=1
  else
    echo "Usage: $0 [-n]" >&2
    exit 1
  fi
fi
tmp_list="$(mktemp)"
trap 'rm -f "$tmp_list"' EXIT

find "$ROOT_DIR" -type d \( -name .git -o -name node_modules -o -name .idea -o -name .gradle \) -prune -o \
  -type d \( -name build -o -name 'build-*' -o -name 'cmake-build-*' \
    -o -path "$ROOT_DIR/x86_64_smp_kernel/build[0-9]*" \) -prune -print | sort > "$tmp_list"

while IFS= read -r dir; do
  [ -n "$dir" ] || continue
  if [[ "$DRY_RUN" == 1 ]]; then
    echo "Would remove $dir"
  else
    echo "Removing $dir"
    rm -rf "$dir"
  fi
  COUNT=$((COUNT + 1))
done < "$tmp_list"
