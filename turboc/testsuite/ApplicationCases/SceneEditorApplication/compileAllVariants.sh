#!/bin/sh
# Configures and builds every variant of the application without running it
APP_DIR="$(cd "$(dirname "$0")" && pwd)"
FAILED=""

for VARIANT in xaw_opengl1 xm_opengl1; do
    echo "==> Building $VARIANT"
    if ! sh "$APP_DIR/runVariant.sh" --build-only "$VARIANT"; then
        FAILED="$FAILED $VARIANT"
    fi
done

if [ -n "$FAILED" ]; then
    echo
    echo "The following SceneEditorApplication variants could not be built:$FAILED" >&2
    exit 1
fi
exit 0
