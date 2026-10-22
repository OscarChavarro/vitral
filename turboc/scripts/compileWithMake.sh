#!/bin/sh
# Builds the turboc port with GNU make only, for legacy systems without CMake
# (i.e. Debian 1.1 with gcc 2.7.2 and GNU make 3.75):
#   - the base library (base/build/libvitral_base.a) and the testsuite
#     programs that need no window system, testsuite/OfflineExamples
#     (AlgebraicExpressionExample, ImageOfflineExample, PolygonTriangulation,
#     Rasterizer2DExample, RaytracingOfflineExample, WireframeOfflineExample)
#   - when OpenGL is found (<GL/gl.h> in /usr/include, /usr/X11R6/include or
#     $GL_DIR/include, see findLegacyGlDir.sh), the opengl1, xt and xaw
#     libraries and the Athena variant of
#     testsuite/ApplicationCases/SceneEditorApplication
#   - when Motif is found as well (<Xm/Xm.h>, i.e. LessTif), the xm library
#     and the Motif variant of the scene editor
#   - when GLUT is found as well (<GL/glut.h>), the glut library and the
#     programs of testsuite/OpenGL1Examples (CameraExample, ImageExample,
#     MeshExample, PolygonClippingExample) and testsuite/_APITests
#     (_OpenGL1HelloWorld, _OpenGL1PbufferExample)
# Each executable goes to the build/ folder of its project, so it can be run
# from its folder with ./run.sh (./runXawOpenGL1.sh for the scene editor).
# The VGA examples need CMake: compile.sh builds them.
#
# The build is incremental, but the Makefiles do not track header
# dependencies: run "compileWithMake.sh clean" after changing headers.
#
# Usage: compileWithMake.sh [make arguments, e.g. WITH_POSIX_THREADS=1 or clean]
#   GL_DIR=<prefix> (environment) OpenGL outside the system folders, i.e.
#   GL_DIR=$HOME/mesa/usr for Mesa unpacked there; by default, the newest
#   Mesa built in /usr/local/src (i.e. /usr/local/src/Mesa-3.0)
set -e

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

if [ "$1" = "clean" ]; then
    make "$@"
    exit 0
fi

GL_DIR="$(sh "$ROOT_DIR/scripts/findLegacyGlDir.sh")"
export GL_DIR
TARGETS=all
if [ -n "$GL_DIR" ] || [ -f /usr/include/GL/gl.h ] || \
   [ -f /usr/X11R6/include/GL/gl.h ]; then
    TARGETS="$TARGETS scene-editor"
    if [ -f /usr/X11R6/include/Xm/Xm.h ] || [ -f /usr/include/Xm/Xm.h ]; then
        TARGETS="$TARGETS scene-editor-motif"
    fi
    if [ -f "$GL_DIR/include/GL/glut.h" ] || [ -f /usr/include/GL/glut.h ] || \
       [ -f /usr/X11R6/include/GL/glut.h ]; then
        TARGETS="$TARGETS glut-examples"
    fi
    if [ -n "$GL_DIR" ]; then
        echo "=== OpenGL from $GL_DIR"
    fi
fi
echo "=== Building with make: $TARGETS"
make $TARGETS "$@"

if [ -f base/build/libvitral_base.a ]; then
    echo "=== Libraries:"
    for library in base/build/libvitral_base.a opengl1/build/libvitral_opengl1.a \
                   xt/build/libvitral_xt.a xaw/build/libvitral_xaw.a \
                   xm/build/libvitral_xm.a glut/build/libvitral_glut.a; do
        if [ -f "$library" ]; then
            echo "$library"
        fi
    done
    echo "=== Testsuite programs (run them with ./run.sh from their folders):"
    for program in testsuite/OfflineExamples/*/build/* \
                   testsuite/OpenGL1Examples/*/build/* \
                   testsuite/_APITests/*/build/* \
                   testsuite/ApplicationCases/SceneEditorApplication/build/*/*; do
        if [ -f "$program" ]; then
            echo "$program"
        fi
    done
fi
