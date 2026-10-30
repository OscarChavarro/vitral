#!/usr/bin/env bash
# Shows the first bytes of the FAT image that is embedded in the kernel (build/loop.img) in hexadecimal, in the same
# format as the "Filesystems" screen of top (F2): compare the two to check that /dev/loop0 serves the image.
# Builds the image first if it does not exist. Usage: scripts/viewFatFilesystemBytes.sh [BYTES [IMAGE]]   (default: 128)
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BYTES="${1:-128}"
IMAGE="${2:-$ROOT/build/loop.img}"

[[ -f "$IMAGE" ]] || "$ROOT/scripts/makeFatImage.sh" "$ROOT/loopKernelRootFileSystem" "$IMAGE"
echo "$IMAGE: $(stat -f%z "$IMAGE" 2>/dev/null || stat -c%s "$IMAGE") bytes; first $BYTES bytes:"
# hexdump -C: offset, 16 bytes in hex (extra space after the 8th) and the text between bars. -v: no "*" for repeats.
hexdump -v -C -n "$BYTES" "$IMAGE" | grep -v '^[0-9a-f]\{8\}$'
