#!/usr/bin/env bash
# Removes every temporary artifact of the CMake builds: build* directories (which also hold the
# images, the GRUB ISO and the build/sim16-* variants of run16.sh), IDE build directories and any
# CMake files left in the source tree by an in-source configure. Sources and bin/ are never touched; the root file system directory is kept empty.
# Usage: scripts/clean.sh [-n]   (-n: only list what would be removed)
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DRY_RUN=0
[[ "${1:-}" == "-n" ]] && DRY_RUN=1

cd "$ROOT"
targets=(out CMakeCache.txt CMakeFiles CMakeScripts cmake_install.cmake install_manifest.txt
         CTestTestfile.cmake Testing _deps compile_commands.json Makefile)
for dir in build*; do [[ -d "$dir" ]] && targets+=("$dir"); done
for dir in cmake-build-*; do [[ -e "$dir" ]] && targets+=("$dir"); done

for entry in loopKernelRootFileSystem/{*,.[!.]*,..?*}; do
    [[ -e "$entry" || -L "$entry" ]] && targets+=("$entry")
done

removed=0
for target in "${targets[@]}"; do
    [[ -e "$target" ]] || continue
    if [[ $DRY_RUN == 1 ]]; then
        echo "would remove $target"
    else
        rm -rf -- "$target"
        echo "removed $target"
    fi
    removed=$((removed + 1))
done
[[ $removed == 0 ]] && echo "nothing to clean"
exit 0
