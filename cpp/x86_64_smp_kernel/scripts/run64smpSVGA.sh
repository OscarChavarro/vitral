#!/usr/bin/env bash
# Like run64smp.sh, with one emulated core per online host logical CPU and the Bochs VBE display
# enabled so that the SVGA modes (800x600 .. 1920x1080 in 1, 4, 8, 16 and 24 bits per pixel) exist for the vga,
# vitral_rasterizer and vitral_raytracer programs. Usage: scripts/run64smpSVGA.sh [-n]   (-n: build only)
# QEMU's `-device VGA` is the Bochs VBE adapter (PCI 1234:1111): the VGA core plus the "dispi" registers and a
# linear frame buffer in BAR 0. vgamem_mb sets the video memory (1920x1080x24 needs 6 MB; QEMU's default is 16).
# More RAM than run64smp.sh: the kernel keeps a copy of each graphics console and the programs hold images of the
# size of the screen. Overrides: SMP, CPU, QEMU as in run64smp.sh; VGAMEM=<MB of video memory>, QEMU_RAM=<MB of RAM>.
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"
[[ "${1:-}" == "-n" ]] && NO_RUN=1
CORES="$(host_logical_cpus)"
SMP="${SMP:-$CORES,sockets=1,cores=$CORES,threads=1}"
echo "QEMU -smp $SMP, Bochs VBE display with ${VGAMEM:-32} MB of video memory"
QEMU_EXTRA=(-smp "$SMP")
QEMU_VGA=(-vga none -device "VGA,vgamem_mb=${VGAMEM:-32}")
QEMU_RAM="${QEMU_RAM:-512}"
run_qemu "${QEMU:-qemu-system-x86_64}" "${CPU:-Nehalem}" "$ROOT/build"
