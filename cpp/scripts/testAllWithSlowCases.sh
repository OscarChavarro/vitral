#!/usr/bin/env bash
set -euo pipefail

# Runs the base unit tests, including the slow ones (the Java tests tagged
# @Tag("slow"), run there by the Gradle slowTest task): the default ctest
# run, then the vitral_base_slow_tests executable. The build directory keeps
# its VITRAL_SLOW_TESTS setting; configure it with -DVITRAL_SLOW_TESTS=ON to
# have ctest itself run the slow tests (label "slow": ctest -L slow).
# Extra arguments are passed to the slow test executable (i.e.
# --gtest_filter=...).

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_DIR="$ROOT_DIR/build"
JOBS="$(nproc 2>/dev/null || getconf _NPROCESSORS_ONLN 2>/dev/null || echo 1)"

cmake -S "$ROOT_DIR" -B "$BUILD_DIR" -DWITH_JPEG=ON
cmake --build "$BUILD_DIR" -j"$JOBS"

# Both test sets run even when the first one fails; the script then fails.
status=0
ctest --test-dir "$BUILD_DIR" -j"$JOBS" --output-on-failure || status=$?
"$BUILD_DIR/base/vitral_base_slow_tests" "$@" || status=$?
exit "$status"
