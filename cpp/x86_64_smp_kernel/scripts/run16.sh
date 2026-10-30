#!/usr/bin/env bash
# CPU without protected mode 32-bit (8086/80186 or 80286): stops in real mode with a "CPU ERROR"
# message, before the switch to protected mode.
#
# QEMU cannot emulate those CPUs (its oldest model is the 486), so this builds a variant of the
# image (build/sim16-*, -DBM_SIMULATE_CPU=...) whose real mode check is forced to report the old
# CPU. The detection code (entry16.S) is the same one used on real hardware.
# Usage: scripts/run16.sh [-n] [8086|286]   (default 286; -n: build only)
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"
[[ "${1:-}" == "-n" ]] && { NO_RUN=1; shift; }
CPU_KIND="${1:-286}"
case "$CPU_KIND" in 8086|286) ;; *) echo "usage: $0 [-n] [8086|286]" >&2; exit 1 ;; esac
run_qemu "${QEMU:-qemu-system-i386}" "${CPU:-486}" "$ROOT/build/sim16-$CPU_KIND" \
    -DBM_SIMULATE_CPU="$CPU_KIND"
