#!/usr/bin/env sh
set -e
cd "$(dirname "$0")"

if [ ! -x build/tangibleInterfaceMarkersCreator ]; then
  echo "Binary not found. Run ../../../../scripts/compile.sh first." >&2
  exit 1
fi

exec build/tangibleInterfaceMarkersCreator "$@"
