#!/usr/bin/env bash
# Runs the x86_64 kernel on a Pentium (no PAE or long mode): it stops with an error in 32-bit protected mode.
# Usage: scripts/run32failon64.sh [-n]   (-n: build only)
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"
[[ "${1:-}" == "-n" ]] && NO_RUN=1
run_qemu "${QEMU:-qemu-system-i386}" "${CPU:-pentium}" "$ROOT/build32failon64"
