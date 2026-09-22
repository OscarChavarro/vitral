#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAVA_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
PKGS_DIR="$(cd "${JAVA_DIR}/../pkgs" 2>/dev/null && pwd || true)"

# Native (C / C++) packages used through JNI (see pkgs/CMakeLists.txt):
# vsdk.toolkit.common.NativeLibraryLoader looks for their shared libraries in
# pkgs/<package>/build, so they must be built there, not just configured, or
# the toolkit silently falls back to its slower pure Java equivalents (i.e.
# ImagePersistence uses the AWT based PNG reader instead of NativeImageReader).
# cleanAll.sh removes those libraries (`make clean`), so they must be built
# again after it; building is skipped, with a warning, if cmake is missing.
if [[ -n "${PKGS_DIR}" ]]; then
  if command -v cmake >/dev/null 2>&1; then
    cmake -S "${PKGS_DIR}" -B "${PKGS_DIR}/build"
    cmake --build "${PKGS_DIR}/build" -j
  else
    echo "compile.sh: cmake not found, skipping the native packages in ${PKGS_DIR}" \
         "(pure Java fallbacks will be used, i.e. the AWT based PNG reader)" >&2
  fi
fi

cd "${JAVA_DIR}"

if [[ -x "./gradlew" ]]; then
  GRADLE_CMD=("./gradlew" "--no-daemon" "-q")
else
  GRADLE_CMD=("gradle" "-q")
fi

"${GRADLE_CMD[@]}" build "$@"
