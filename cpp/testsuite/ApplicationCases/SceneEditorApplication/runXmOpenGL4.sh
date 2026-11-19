#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VARIANT="xm_opengl4"
BUILD_DIR="$ROOT_DIR/build-cmake-$VARIANT"
# One build job per logical CPU: nproc (Linux, GNU coreutils), getconf
# (POSIX, also macOS) or sysctl (macOS, BSD); 1 when none of them answers
# with a positive number
JOBS=""
for COUNT_COMMAND in "nproc" "getconf _NPROCESSORS_ONLN" \
                     "sysctl -n hw.logicalcpu" "sysctl -n hw.ncpu"; do
    if command -v "${COUNT_COMMAND%% *}" >/dev/null 2>&1; then
        JOBS="$($COUNT_COMMAND 2>/dev/null || true)"
        if [[ "$JOBS" =~ ^[1-9][0-9]*$ ]]; then
            break
        fi
    fi
    JOBS=""
done
JOBS="${JOBS:-1}"

cmake -S "$ROOT_DIR" -B "$BUILD_DIR" -DSCENE_EDITOR_VARIANT="$VARIANT"
cmake --build "$BUILD_DIR" --config Release -j"$JOBS"

# Homebrew Mesa uses indirect GLX with XQuartz on macOS.
if [[ "$(uname)" == "Darwin" ]]; then
    export LIBGL_ALWAYS_INDIRECT="${LIBGL_ALWAYS_INDIRECT:-1}"
    source "$ROOT_DIR/prepareHomebrewX11Locale.sh"
    prepare_homebrew_x11_locale
fi
"$ROOT_DIR/build/$VARIANT/SceneEditorApplication" "$@"
