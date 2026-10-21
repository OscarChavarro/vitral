#!/bin/sh
# Builds (incrementally) one testsuite program of the turboc port and runs it
# from the caller's program directory, forwarding the arguments. The program
# group (e.g. testsuite/OpenGL1Examples) is the parent of the program
# directory; it is built in <group>/buildtc, with CMake 2.4 compatible calls.
# The libraries (base, opengl1, glut) are built first if missing.
#
# Usage: runTestsuiteProgram.sh <programDir> <cmakeTarget> <executable> [args...]
#   programDir  directory used as working directory (usually the run.sh dir)
#   cmakeTarget target name in the CMakeLists.txt of the program group
#   executable  path of the produced binary, relative to programDir
set -e

if [ "$#" -lt 3 ]; then
    echo "Usage: $0 <programDir> <cmakeTarget> <executable> [args...]" >&2
    exit 2
fi

PROGRAM_DIR="$1"
TARGET="$2"
EXECUTABLE="$3"
shift 3

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
GROUP_DIR="$(cd "$PROGRAM_DIR/.." && pwd)"
BUILD_DIR="$GROUP_DIR/buildtc"
JOBS="$(getconf _NPROCESSORS_ONLN 2>/dev/null || echo 1)"
LOG="${TMPDIR:-/tmp}/runTestsuiteProgram.$$.log"

# Quiet: the build log is shown only when it fails
quiet() {
    if ! "$@" > "$LOG" 2>&1; then
        cat "$LOG" >&2
        rm -f "$LOG"
        exit 1
    fi
    rm -f "$LOG"
}

for library in base/build/libvitral_base.so opengl1/build/libvitral_opengl1.so \
               glut/build/libvitral_glut.so; do
    if [ ! -f "$ROOT_DIR/$library" ]; then
        quiet sh "$ROOT_DIR/scripts/compile.sh"
        break
    fi
done

mkdir -p "$BUILD_DIR"
cd "$BUILD_DIR"
if [ ! -f CMakeCache.txt ]; then
    quiet cmake ..
fi
quiet make -j"$JOBS" "$TARGET"

LD_LIBRARY_PATH="$ROOT_DIR/base/build:$ROOT_DIR/opengl1/build:$ROOT_DIR/glut/build${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
export LD_LIBRARY_PATH

cd "$PROGRAM_DIR"
exec "$EXECUTABLE" "$@"
