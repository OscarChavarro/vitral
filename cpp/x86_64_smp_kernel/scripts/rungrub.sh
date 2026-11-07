#!/usr/bin/env bash
# Boots the kernel through GRUB with Multiboot2 (instead of our own BIOS boot sector): builds an ISO
# with GRUB (BIOS, i386-pc) and kernel64.bin (or kernel32.bin with ARCH=i386) and starts it in QEMU as a CD-ROM.
# Usage: scripts/rungrub.sh [-n]   (-n: build the ISO only)
# ARCH=i386 boots the i386 kernel in VGA text mode (gfxpayload=text), as on machines without long mode.
# Needs: brew install i686-elf-grub xorriso mtools     (or grub-mkrescue + xorriso on Linux)
# Overrides: CPU= (default qemu64), SMP= (qemu -smp, default host logical CPUs), QEMU=, ZOOM= (see common.sh).
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"
[[ "${1:-}" == "-n" ]] && NO_RUN=1

MKRESCUE="$(command -v grub-mkrescue || command -v i686-elf-grub-mkrescue || true)"
[[ -n "$MKRESCUE" ]] || { echo "grub-mkrescue not found (brew install i686-elf-grub xorriso mtools)" >&2; exit 1; }

if [[ "${ARCH:-x86_64}" == "i386" ]]; then
    BUILD="$ROOT/build32" KERNEL=kernel32 PAYLOAD=text QEMU_DEFAULT=qemu-system-i386 CPU_DEFAULT=qemu32
    ARCH_OPTIONS=(-DBM_ARCH=i386)
else
    BUILD="$ROOT/build" KERNEL=kernel64 PAYLOAD=keep QEMU_DEFAULT=qemu-system-x86_64 CPU_DEFAULT=qemu64
    ARCH_OPTIONS=()
fi
cmake -S "$ROOT" -B "$BUILD" -DCMAKE_TOOLCHAIN_FILE="$ROOT/cmake/toolchain.cmake" ${ARCH_OPTIONS[@]+"${ARCH_OPTIONS[@]}"} \
    >/dev/null
cmake --build "$BUILD" --parallel "$JOBS" >/dev/null

ISO_DIR="$BUILD/grub-iso"
rm -rf "$ISO_DIR"
mkdir -p "$ISO_DIR/boot/grub"
cp "$BUILD/$KERNEL.bin" "$ISO_DIR/boot/$KERNEL.bin"
cat > "$ISO_DIR/boot/grub/grub.cfg" <<CFG
set timeout=1
set default=0
menuentry "Vitral Kernel (Multiboot2)" {
    set gfxmode=1024x768x32,auto
    set gfxpayload=$PAYLOAD
    multiboot2 /boot/$KERNEL.bin
    boot
}
CFG
"$MKRESCUE" -o "$BUILD/vitral.iso" "$ISO_DIR" >/dev/null 2>&1
echo "ISO: $BUILD/vitral.iso"

[[ "${NO_RUN:-}" == "1" ]] && exit 0
QEMU_EXTRA=(-smp "${SMP:-$(host_logical_cpus)}")
QEMU_SERIAL_LOG="${QEMU_SERIAL_LOG:-$BUILD/com1.log}"
launch_qemu "${QEMU:-$QEMU_DEFAULT}" "${CPU:-$CPU_DEFAULT}" -cdrom "$BUILD/vitral.iso" -boot d
