#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAVA_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
PKGS_DIR="$(cd "$JAVA_DIR/../pkgs" 2>/dev/null && pwd || true)"

cd "$JAVA_DIR" || exit 1

warn() {
    echo "clean.sh: $*" >&2
}

rm -rf build

# A failing Gradle (no JDK, no network to download the wrapper distribution,
# broken build script, ...) must not stop the rest of the cleanup, which does
# not depend on it.
if [[ -x ./gradlew ]]; then
    ./gradlew --no-daemon clean || warn "gradle clean failed, continuing"
elif command -v gradle >/dev/null 2>&1; then
    gradle --no-daemon clean || warn "gradle clean failed, continuing"
else
    warn "neither ./gradlew nor gradle found, skipping gradle clean"
fi

clean_native_package() {
    local package_name="$1"
    local package_dir="$PKGS_DIR/$package_name"
    local build_dir="$PKGS_DIR/build/$package_name"

    local make_dir=""

    if [[ -f "$package_dir/Makefile" ]]; then
        make_dir="$package_dir"
    elif [[ -f "$build_dir/Makefile" ]]; then
        make_dir="$build_dir"
    else
        return 0
    fi

    if ! command -v make >/dev/null 2>&1; then
        warn "make not found, skipping native package $package_name"
        return 0
    fi

    # The CMake generated Makefiles hold absolute paths, so they fail if the
    # tree was moved or pkgs/build is stale; that must not abort the cleanup.
    make -C "$make_dir" clean || warn "make clean failed for $package_name, continuing"
}

if [[ -n "$PKGS_DIR" ]]; then
    clean_native_package SpharmonicKit27
    clean_native_package LempelZivWelch
    clean_native_package NativeImageReader
fi

# Directories are pruned so find never descends into (or reports as missing)
# a tree that rm already deleted, which would make find exit with an error.
find . -type d \( -name "target" -o -name "lib" -o -name "classes" \) -prune -exec rm -rf {} +
find . \( -name "*~" -o -name "*.war" -o -name "*.class" -o -name "output.jpg" -o -name "output.ppm" -o -name "output.png" -o -name "output.bmp" \) -exec rm -rf {} +
rm -rf ./doc/html_doxygen ./doc/html_javadoc ./doc/_doxygen/warnings.log
rm -rf testsuite/ApplicationCases/SearchEngineFor3DModels/tmp
if [[ -n "$PKGS_DIR" ]]; then
    rm -rf "$PKGS_DIR/SpharmonicKit27/_ide/VisualStudioDotNet2005/SpharmonicKit/Release" \
           "$PKGS_DIR/SpharmonicKit27/_ide/VisualStudioDotNet2005/SpharmonicKit/Debug"
fi
METADATA_BIN=./testsuite/ApplicationCases/SearchEngineFor3DModels/etc/metadata.bin
if [[ -f "$METADATA_BIN" ]]; then
    # Only ask when there is someone to answer; never block a batch run.
    if [[ -t 0 ]]; then
        rm -i "$METADATA_BIN" || true
    else
        warn "not interactive, keeping $METADATA_BIN"
    fi
fi
rm -rf testsuite/Jogl4Examples/PolyhedralBoundedSolidExample/outputA* \
       testsuite/Jogl4Examples/PolyhedralBoundedSolidExample/outputB* \
       testsuite/Jogl4Examples/PolyhedralBoundedSolidExample/outputR*
rm -rf testsuite/Tools/SpriteFontGenerator/output/*
rm -rf testsuite/OfflineExamples/PolyhedralBoundedSolidExpotImportExample/output/

rm -rf .gradle-home/
# JaCoCo HTML report (base/build.gradle jacocoTestReport, jacocoAllTestsReport)
rm -rf coverage/
