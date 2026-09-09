#!/usr/bin/env sh
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
typescript_dir=$(CDPATH= cd -- "$script_dir/.." && pwd)

cd "$typescript_dir"
npm run build
npm run pack

# Package validation is deliberately independent of the workspace resolution:
# a consumer sees only the tarball and the declared public exports.
npm_config_cache="${TMPDIR:-/tmp}/vitral-npm-cache" npm --workspace @vitral/base pack --dry-run
archive=$(find "$typescript_dir/artifacts" -maxdepth 1 -type f -name 'vitral-base-*.tgz' -print -quit)
if [ -z "$archive" ]; then
  echo "Expected @vitral/base tarball was not produced" >&2
  exit 1
fi
consumer_dir=$(mktemp -d "${TMPDIR:-/tmp}/vitral-consumer.XXXXXX")
trap 'rm -rf "$consumer_dir"' EXIT HUP INT TERM
cd "$consumer_dir"
npm init --yes >/dev/null
npm_config_cache="${TMPDIR:-/tmp}/vitral-npm-cache" npm install --ignore-scripts "$archive" >/dev/null
printf '%s\n' 'import { ByteBuffer } from "@vitral/base";' 'ByteBuffer.allocate(4);' > index.ts
"$typescript_dir/node_modules/.bin/tsc" --target ES2022 --module NodeNext --moduleResolution NodeNext --noEmit index.ts
