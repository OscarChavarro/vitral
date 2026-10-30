#!/usr/bin/env bash
# x86_64 multiprocessor: one emulated core per online logical CPU of this machine, so the
# kernel reports "SMP using N cores". Usage: scripts/run64smp.sh [-n]   (-n: build only)
# Overrides: SMP=<qemu -smp value> (e.g. SMP=4,sockets=1,cores=2,threads=2 for a Hyper-Threading
# topology), CPU=<qemu cpu model>, QEMU=<binary>.
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"
[[ "${1:-}" == "-n" ]] && NO_RUN=1
CORES="$(host_logical_cpus)"
SMP="${SMP:-$CORES,sockets=1,cores=$CORES,threads=1}"
echo "QEMU -smp $SMP"
QEMU_EXTRA=(-smp "$SMP")
run_qemu "${QEMU:-qemu-system-x86_64}" "${CPU:-Nehalem}" "$ROOT/build"
