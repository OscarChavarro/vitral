#!/bin/sh
# Builds the turboc port: the libraries in dependency order (base, opengl1,
# glut and the Xt layer: xt, with its Athena and Motif widget sets xaw and
# xm), and then the testsuite projects of the port, so each one can be run
# from its own folder with ./run.sh:
#   - testsuite/OpenGL1Examples (CameraExample, ImageExample, MeshExample,
#     PolygonClippingExample)
#   - testsuite/_APITests (_OpenGL1HelloWorld, _OpenGL1PbufferExample)
#   - testsuite/ApplicationCases/SceneEditorApplication (xm_opengl1 and
#     xaw_opengl1 variants)
# Only uses commands available in legacy toolchains (CMake 2.4, no
# `cmake -S/-B`, no `nproc`). Intermediate files go to <folder>/buildtc,
# libraries to <module>/build and executables to <project>/build.
#
# Usage: compile.sh [extra cmake arguments, e.g. -DWITH_JPEG=ON]
set -e

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
MODULES="base opengl1 glut xt xaw xm"
TESTSUITE_GROUPS="testsuite/OpenGL1Examples testsuite/_APITests"
SCENE_EDITOR_DIR="testsuite/ApplicationCases/SceneEditorApplication"
SCENE_EDITOR_VARIANTS="xm_opengl1 xaw_opengl1"
JOBS="$(getconf _NPROCESSORS_ONLN 2>/dev/null || echo 1)"

# Configures (once) and builds a CMake project
# Usage: build <label> <source folder> <build folder> [cmake arguments]
build() {
    label="$1"
    sourceDir="$2"
    buildDir="$3"
    shift 3
    echo "=== Building $label"
    mkdir -p "$buildDir"
    cd "$buildDir"
    cmake "$@" "$sourceDir"
    make -j"$JOBS"
}

for module in $MODULES; do
    build "$module" "$ROOT_DIR/$module" "$ROOT_DIR/$module/buildtc" "$@"
done

for group in $TESTSUITE_GROUPS; do
    build "$group" "$ROOT_DIR/$group" "$ROOT_DIR/$group/buildtc" "$@"
done

for variant in $SCENE_EDITOR_VARIANTS; do
    build "$SCENE_EDITOR_DIR ($variant)" "$ROOT_DIR/$SCENE_EDITOR_DIR" \
        "$ROOT_DIR/$SCENE_EDITOR_DIR/buildtc/$variant" \
        -DSCENE_EDITOR_VARIANT="$variant" "$@"
done

echo "=== Libraries:"
for module in $MODULES; do
    ls "$ROOT_DIR/$module/build/"libvitral_* 2>/dev/null || true
done
echo "=== Testsuite programs (run them with ./run.sh from their folders):"
for group in $TESTSUITE_GROUPS $SCENE_EDITOR_DIR; do
    find "$ROOT_DIR/$group" -path "*/build/*" -type f -perm -u+x 2>/dev/null | sort
done
