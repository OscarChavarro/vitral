#!/usr/bin/env bash
# Run from: java/testsuite/OfflineExamples/PolyhedralBoundedSolidExpotImportExample
# Usage: ./run.sh [model]
#   model: moonMotif | bowl   (omit to print usage)
# The program resolves output/ relative to CWD, so always execute from here.
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAVA_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

JVM_ARGS="-Duser.dir=$SCRIPT_DIR"
if [ -n "${1:-}" ]; then
    JVM_ARGS="$JVM_ARGS|-Dmodel=$1"
fi

cd "$JAVA_ROOT" || exit 1

gradle --quiet \
    :testsuite:OfflineExamples:PolyhedralBoundedSolidExpotImportExample:runMain \
    -PrunMainClass=PolyhedralBoundedSolidExportImportExample \
    -PrunJvmArgs="$JVM_ARGS"
