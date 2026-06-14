#!/usr/bin/env sh
set -e
cd "$(dirname "$0")"

if [ ! -x build/tangibleInterfaceLabelsCreator ]; then
  echo "Binary not found. Run ../../../../scripts/compile.sh first." >&2
  exit 1
fi

exec build/tangibleInterfaceLabelsCreator "$@"
