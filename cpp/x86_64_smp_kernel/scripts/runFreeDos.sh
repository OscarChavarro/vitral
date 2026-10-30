#!/usr/bin/env bash
# Launches an emulated i386 machine with FreeDOS (LiveCD) using QEMU.
# Usage: scripts/runFreeDos.sh [-f floppy_image_or_extra_disk]
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ISO="$ROOT/bin/FD14LIVE.iso"
QEMU="${QEMU:-qemu-system-i386}"

if [[ ! -f "$ISO" ]]; then
    echo "$ISO not found." >&2
    echo "Download the FreeDOS 1.4 LiveCD from https://www.freedos.org/download/, unzip it and put FD14LIVE.iso in bin/ (it is not kept in git)." >&2
    exit 1
fi

# Prefer SDL; if this QEMU does not have it (brew on macOS), fall back to cocoa.
if "$QEMU" -display help 2>&1 | grep -qx sdl; then
    DISPLAY_OPT="sdl"
else
    DISPLAY_OPT="cocoa"
fi

EXTRA=()
if [[ "${1:-}" == "-f" && -n "${2:-}" ]]; then
    EXTRA+=(-drive "file=$2,format=raw,if=ide,index=0")
fi

exec "$QEMU" \
    -machine pc \
    -cpu pentium3 \
    -m 64 \
    -vga std \
    -display "$DISPLAY_OPT" \
    -cdrom "$ISO" \
    -boot d \
    -audiodev none,id=snd0 \
    ${EXTRA[@]+"${EXTRA[@]}"}
