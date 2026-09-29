#!/usr/bin/env bash
set -uo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VARIANTS=(xaw_opengl1 xaw_opengl4 xm_opengl1 xm_opengl4 gtk4_opengl4)
FAILED=()

for VARIANT in "${VARIANTS[@]}"; do
    BUILD_DIR="$ROOT_DIR/build-cmake-$VARIANT"
    echo "==> Building $VARIANT"
    if ! cmake -S "$ROOT_DIR" -B "$BUILD_DIR" -DSCENE_EDITOR_VARIANT="$VARIANT"; then
        FAILED+=("$VARIANT (configure)")
        continue
    fi
    if ! cmake --build "$BUILD_DIR" --config Release; then
        FAILED+=("$VARIANT (build)")
        continue
    fi
done

if (( ${#FAILED[@]} > 0 )); then
    echo
    echo "The following SceneEditorApplication variants could not be built:" >&2
    for ITEM in "${FAILED[@]}"; do
        echo "  - $ITEM" >&2
    done
fi

exit 0
