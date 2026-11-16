#!/usr/bin/env bash
set -euo pipefail

# Runs the base unit tests, including the slow-tagged ones (slowTest task),
# and writes the merged JaCoCo coverage report to <java>/coverage/index.html.
# Extra arguments are passed to Gradle (i.e. -PslowTestHeap=16g).

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAVA_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "${JAVA_DIR}"

# Native JNI libraries (pkgs). Some tests depend on them: without
# libNativeImageReader, ImagePersistence returns a test pattern image for
# missing files instead of failing (i.e. ReaderMitSceneCommandsTest).
# NativeLibraryLoader finds them in pkgs/<project>/build, no install needed.
PKGS_DIR="$(cd "${JAVA_DIR}/../pkgs" && pwd)"
cmake -S "${PKGS_DIR}" -B "${PKGS_DIR}/build"
cmake --build "${PKGS_DIR}/build" -j

if [[ -x "./gradlew" ]]; then
  GRADLE_CMD=("./gradlew" "--no-daemon")
else
  GRADLE_CMD=("gradle")
fi

# --continue: the report is still generated when some test fails (the build
# then ends failed). The fast-only jacocoTestReport is skipped, the merged
# report replaces it.
"${GRADLE_CMD[@]}" --continue :base:test :base:slowTest \
  :base:jacocoAllTestsReport -x :base:jacocoTestReport "$@"

echo "Coverage report: ${JAVA_DIR}/coverage/index.html"
