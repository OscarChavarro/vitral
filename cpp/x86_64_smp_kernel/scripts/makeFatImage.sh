#!/usr/bin/env bash
# Builds the VFAT image of the kernel's root file system: a copy of a directory (loopKernelRootFileSystem) in a FAT
# volume that Linux can mount with `mount -o loop`. The image is embedded in the kernel and is what /dev/loop0 serves.
# Needs mtools (brew install mtools / apt install mtools): mformat and mcopy, no root and no mount needed.
# Usage: scripts/makeFatImage.sh [SOURCE_DIR [IMAGE [DIR:NAME...]]]   (default: loopKernelRootFileSystem  build/loop.img)
# Each DIR:NAME also copies the contents of DIR into the directory NAME of the image (httpd's front end goes to www).
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SOURCE="${1:-$ROOT/loopKernelRootFileSystem}"
IMAGE="${2:-$ROOT/build/loop.img}"
EXTRA=("${@:3}")
LABEL="${LOOP_LABEL:-KERNELROOT}"

for tool in mformat mcopy mmd; do
    command -v "$tool" >/dev/null || { echo "$tool not found: install mtools" >&2; exit 1; }
done
[[ -d "$SOURCE" ]] || { echo "$SOURCE is not a directory" >&2; exit 1; }

# Size: file contents rounded up to 4 KB each (worst case cluster) plus 2 KB per file or directory for the directory
# entries, plus 12% and 768 KB free so that programs can create files. Rounded up to a whole MB.
file_bytes=0; items=0
while IFS= read -r -d '' path; do
    size="$(stat -f%z "$path" 2>/dev/null || stat -c%s "$path")"
    file_bytes=$((file_bytes + (size + 4095) / 4096 * 4096 + 2048))
    items=$((items + 1))
done < <(find "$SOURCE" ${EXTRA[@]+"${EXTRA[@]%%:*}"} -mindepth 1 \( -type f -o -type d \) -print0)
total=$((file_bytes + file_bytes / 8 + 786432))
mb=$(((total + 1048575) / 1048576))

mkdir -p "$(dirname "$IMAGE")"
rm -f "$IMAGE"
dd if=/dev/zero of="$IMAGE" bs=1048576 count="$mb" 2>/dev/null
mformat -i "$IMAGE" -v "$LABEL" ::
# -s: recursive, -m: keep the modification times, -Q: stop on the first error, -n: overwrite without asking
(cd "$SOURCE" && mcopy -i "$IMAGE" -s -m -Q -n ./* ::)
for extra in ${EXTRA[@]+"${EXTRA[@]}"}; do
    dir="${extra%%:*}"; name="${extra#*:}"
    [[ -d "$dir" ]] || { echo "$dir is not a directory" >&2; exit 1; }
    mmd -i "$IMAGE" "::$name"
    (cd "$dir" && mcopy -i "$IMAGE" -s -m -Q -n ./* "::$name")
done
echo "FAT image: $IMAGE ($mb MB, $items files and directories from $SOURCE)"
