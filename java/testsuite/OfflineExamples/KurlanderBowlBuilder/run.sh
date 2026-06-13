#!/usr/bin/env bash
# Run from: java/testsuite/OfflineExamples/KurlanderBowlBuilder
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAVA_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

cd "$JAVA_ROOT" || exit 1

gradle --quiet \
    :testsuite:OfflineExamples:KurlanderBowlBuilder:runMain \
    -PrunMainClass=KurlanderBowlBuilder \
    -PrunJvmArgs="-Duser.dir=$SCRIPT_DIR"
