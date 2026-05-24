#!/usr/bin/env bash
# Run from: java/testsuite/OfflineExamples/PolyhedralBoundedSolidExpotImportExample
# The program resolves output/ relative to CWD, so always execute from here.
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAVA_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

cd "$JAVA_ROOT" || exit 1

gradle --quiet \
    :testsuite:OfflineExamples:PolyhedralBoundedSolidExpotImportExample:runMain \
    -PrunMainClass=PolyhedralBoundedSolidExportImportExample \
    -PrunJvmArgs="-Duser.dir=$SCRIPT_DIR"
