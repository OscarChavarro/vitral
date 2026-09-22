#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAVA_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
PKGS_DIR="$(cd "$JAVA_DIR/../pkgs" 2>/dev/null && pwd || true)"

cd "$JAVA_DIR" || exit 1

rm -rf build
./gradlew --no-daemon clean

clean_native_package() {
    local package_name="$1"
    local package_dir="$PKGS_DIR/$package_name"
    local build_dir="$PKGS_DIR/build/$package_name"

    if [[ -f "$package_dir/Makefile" ]]; then
        make -C "$package_dir" clean
    elif [[ -f "$build_dir/Makefile" ]]; then
        make -C "$build_dir" clean
    fi
}

if [[ -n "$PKGS_DIR" ]]; then
    clean_native_package SpharmonicKit27
    clean_native_package LempelZivWelch
    clean_native_package NativeImageReader
fi

find . \( -name "*~" -o -name "*.war" -o -name "*.class" -o -name "output.jpg" -o -name "output.ppm" -o -name "output.png" -o -name "output.bmp" -o -name "target" \) -exec rm -rf {} +
rm -rf ./doc/html_doxygen ./doc/html_javadoc ./doc/_doxygen/warnings.log
find . -type d \( -name "lib" -o -name "classes" \) -exec rm -rf {} +
rm -rf testsuite/ApplicationCases/SearchEngineFor3DModels/tmp
if [[ -n "$PKGS_DIR" ]]; then
    rm -rf "$PKGS_DIR/SpharmonicKit27/_ide/VisualStudioDotNet2005/SpharmonicKit/Release" \
           "$PKGS_DIR/SpharmonicKit27/_ide/VisualStudioDotNet2005/SpharmonicKit/Debug"
fi
if [ -f ./testsuite/ApplicationCases/SearchEngineFor3DModels/etc/metadata.bin ]; then
    rm -i ./testsuite/ApplicationCases/SearchEngineFor3DModels/etc/metadata.bin
fi
rm -rf testsuite/Jogl4Examples/PolyhedralBoundedSolidExample/outputA* \
       testsuite/Jogl4Examples/PolyhedralBoundedSolidExample/outputB* \
       testsuite/Jogl4Examples/PolyhedralBoundedSolidExample/outputR*
rm -rf testsuite/Tools/SpriteFontGenerator/output/*
rm -rf testsuite/OfflineExamples/PolyhedralBoundedSolidExpotImportExample/output/

rm -rf .gradle-home/
