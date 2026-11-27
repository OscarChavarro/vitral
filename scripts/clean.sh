#!/usr/bin/env bash
# Cleans the build outputs of every port by running, one by one, the
# scripts/clean.sh of each one. A failing port does not stop the others; the
# exit status is non zero if any of them failed.
set -uo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PORTS=(cpp turboc java typescript)
FAILED=()

for port in "${PORTS[@]}"; do
    port_dir="$ROOT_DIR/$port"
    script="./scripts/clean.sh"

    if [[ ! -f "$port_dir/$script" ]]; then
        echo "clean.sh: $port has no $script, skipping" >&2
        FAILED+=("$port")
        continue
    fi

    echo "=== Cleaning $port ($script)"
    if ! (cd "$port_dir" && bash "$script"); then
        echo "clean.sh: cleaning $port failed" >&2
        FAILED+=("$port")
    fi
done

if [[ ${#FAILED[@]} -gt 0 ]]; then
    echo "clean.sh: failed ports: ${FAILED[*]}" >&2
    exit 1
fi
echo "=== All ports cleaned"
