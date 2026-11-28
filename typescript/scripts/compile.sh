#!/usr/bin/env sh
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
typescript_dir=$(CDPATH= cd -- "$script_dir/.." && pwd)

cd "$typescript_dir"
npm run build
npm run pack
npm run pack:fs
npm run --workspace @vitral/webgl pack

archive_for() {
  version=$(node -p 'require(process.argv[1]).version' "$typescript_dir/$1/package.json")
  archive="$typescript_dir/artifacts/vitral-$1-$version.tgz"
  if [ ! -f "$archive" ]; then
    echo "Expected package was not produced: $archive" >&2
    return 1
  fi
  printf '%s\n' "$archive"
}

base_archive=$(archive_for base)
fs_archive=$(archive_for fs)
webgl_archive=$(archive_for webgl)
consumer_dir=$(mktemp -d "${TMPDIR:-/tmp}/vitral-consumer.XXXXXX")
trap 'rm -rf "$consumer_dir"' EXIT HUP INT TERM
cd "$consumer_dir"
npm init --yes >/dev/null
npm_config_cache="${TMPDIR:-/tmp}/vitral-npm-cache" npm install --ignore-scripts --no-audit --no-fund "$base_archive" "$fs_archive" "$webgl_archive" >/dev/null
# Compile and run against the tarballs, independently of workspace resolution.
cat > index.mts <<'EOF'
import { ByteBuffer, ConvexWindowClipper, Matrix4x4f, WeilerAthertonPolygonClipper } from "@vitral/base";
import { ReaderPly } from "@vitral/base/vsdk/toolkit/io/geometry/ReaderPly";
import { File, NodeWorkerExecutor } from "@vitral/fs";
import { WebReaderPly } from "@vitral/webgl";

void [
  ByteBuffer.allocate(4), new Matrix4x4f(), ConvexWindowClipper, WeilerAthertonPolygonClipper,
  ReaderPly, new File("."), NodeWorkerExecutor, WebReaderPly,
];
EOF
"$typescript_dir/node_modules/.bin/tsc" --target ES2022 --module NodeNext --moduleResolution NodeNext --strict index.mts
node index.mjs
