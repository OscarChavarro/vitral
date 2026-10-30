#!/usr/bin/env bash
# Boots the kernel through GRUB with Multiboot2 (instead of our own BIOS boot sector): builds an ISO
# with GRUB (BIOS, i386-pc) and kernel64.bin and starts it in QEMU as a CD-ROM.
# Usage: scripts/rungrub.sh [-n]   (-n: build the ISO only)
# Needs: brew install i686-elf-grub xorriso mtools     (or grub-mkrescue + xorriso on Linux)
# Overrides: CPU= (default qemu64), SMP= (qemu -smp, default host logical CPUs), QEMU=, ZOOM= (see common.sh).
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"
[[ "${1:-}" == "-n" ]] && NO_RUN=1

MKRESCUE="$(command -v grub-mkrescue || command -v i686-elf-grub-mkrescue || true)"
[[ -n "$MKRESCUE" ]] || { echo "grub-mkrescue not found (brew install i686-elf-grub xorriso mtools)" >&2; exit 1; }

BUILD="$ROOT/build"
cmake -S "$ROOT" -B "$BUILD" -DCMAKE_TOOLCHAIN_FILE="$ROOT/cmake/toolchain.cmake" >/dev/null
cmake --build "$BUILD" --parallel "$JOBS" >/dev/null

ISO_DIR="$BUILD/grub-iso"
rm -rf "$ISO_DIR"
mkdir -p "$ISO_DIR/boot/grub"
cp "$BUILD/kernel64.bin" "$ISO_DIR/boot/kernel64.bin"
cat > "$ISO_DIR/boot/grub/grub.cfg" <<'CFG'
set timeout=1
set default=0
set gfxmode=1024x768x32,auto
set gfxpayload=keep
menuentry "Vitral Kernel (Multiboot2)" {
    multiboot2 /boot/kernel64.bin
    boot
}
CFG
"$MKRESCUE" -o "$BUILD/vitral.iso" "$ISO_DIR" >/dev/null 2>&1
echo "ISO: $BUILD/vitral.iso"

[[ "${NO_RUN:-}" == "1" ]] && exit 0
QEMU_EXTRA=(-smp "${SMP:-$(host_logical_cpus)}")
launch_qemu "${QEMU:-qemu-system-x86_64}" "${CPU:-qemu64}" -cdrom "$BUILD/vitral.iso" -boot d
