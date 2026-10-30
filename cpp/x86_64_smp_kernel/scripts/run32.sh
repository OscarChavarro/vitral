#!/usr/bin/env bash
# The kernel, libraries and programs built for i386 (32-bit protected mode, no paging), with one CPU per host logical CPU.
# CPU=pentium SMP=1 selects a Pentium (QEMU's Pentium model has no APIC).
# Usage: scripts/run32.sh [-n]   (-n: build only)
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"
[[ "${1:-}" == "-n" ]] && NO_RUN=1
QEMU_EXTRA=(-smp "${SMP:-$(host_logical_cpus)}")
BM_KERNEL=kernel32
run_qemu "${QEMU:-qemu-system-i386}" "${CPU:-qemu32}" "$ROOT/build32" -DBM_ARCH=i386
