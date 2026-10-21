#!/bin/sh
# Builds (incrementally) a variant of the application and runs it from this
# folder (its resources are read from ../../../../etc), forwarding the
# arguments. The libraries of the turboc port are built first if missing.
# Only uses commands available in legacy toolchains (CMake 2.4).
#
# Usage: runVariant.sh <variant> [args...]   (xm_opengl1 or xaw_opengl1)
#        runVariant.sh --build-only <variant>
set -e

BUILD_ONLY=0
if [ "${1:-}" = "--build-only" ]; then
    BUILD_ONLY=1
    shift
fi
if [ "$#" -lt 1 ]; then
    echo "Usage: $0 [--build-only] <xm_opengl1|xaw_opengl1> [args...]" >&2
    exit 2
fi
VARIANT="$1"
shift

APP_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$APP_DIR/../../.." && pwd)"
BUILD_DIR="$APP_DIR/buildtc/$VARIANT"
JOBS="$(getconf _NPROCESSORS_ONLN 2>/dev/null || echo 1)"

case "$VARIANT" in
    xm_opengl1)  WIDGET_SET=xm ;;
    xaw_opengl1) WIDGET_SET=xaw ;;
    *)
        echo "Unknown variant: $VARIANT (xm_opengl1 or xaw_opengl1)" >&2
        exit 2
        ;;
esac

for library in base/build/libvitral_base.so opengl1/build/libvitral_opengl1.so \
               xt/build/libvitral_xt.so $WIDGET_SET/build/libvitral_$WIDGET_SET.so; do
    if [ ! -f "$ROOT_DIR/$library" ]; then
        sh "$ROOT_DIR/scripts/compile.sh"
        break
    fi
done

mkdir -p "$BUILD_DIR"
cd "$BUILD_DIR"
if [ ! -f CMakeCache.txt ]; then
    cmake -DSCENE_EDITOR_VARIANT="$VARIANT" "$APP_DIR"
fi
make -j"$JOBS"

if [ "$BUILD_ONLY" = 1 ]; then
    exit 0
fi

LD_LIBRARY_PATH="$ROOT_DIR/base/build:$ROOT_DIR/opengl1/build:$ROOT_DIR/xt/build:$ROOT_DIR/$WIDGET_SET/build${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
export LD_LIBRARY_PATH
cd "$APP_DIR"
exec "$APP_DIR/build/$VARIANT/SceneEditorApplication" "$@"
