#!/usr/bin/env bash
# x86_64 CPU with long mode (AMD64 / Intel 64): the bootloader builds the PAE tables and reports
# "Long mode (x86_64) available". Usage: scripts/run64.sh [-n]   (-n: build only)
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"
[[ "${1:-}" == "-n" ]] && NO_RUN=1
QEMU_EXTRA=(-smp "${SMP:-$(host_logical_cpus)}")
run_qemu "${QEMU:-qemu-system-x86_64}" "${CPU:-qemu64}" "$ROOT/build"
