#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VARIANT="gtk4_opengl4"
BUILD_DIR="$ROOT_DIR/build-cmake-$VARIANT"

if ! pkg-config --exists 'gtk4 >= 4.22'; then
    cat >&2 <<'EOF'
GTK 4.22 development files were not found by pkg-config.

On this Mac with Homebrew, install them with:
  brew install gtk4 pkg-config

Then make sure pkg-config can see Homebrew packages, for example:
  export PKG_CONFIG_PATH="/opt/homebrew/lib/pkgconfig:/opt/homebrew/share/pkgconfig:${PKG_CONFIG_PATH:-}"

EOF
    exit 1
fi

cmake -S "$ROOT_DIR" -B "$BUILD_DIR" -DSCENE_EDITOR_VARIANT="$VARIANT"
cmake --build "$BUILD_DIR" --config Release
"$ROOT_DIR/build/$VARIANT/SceneEditorApplication" "$@"
