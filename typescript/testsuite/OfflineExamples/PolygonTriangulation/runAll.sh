#!/usr/bin/env bash

set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
polygon_dir="${script_dir}/../../../../etc/polygons"
status=0

if [ ! -d "${polygon_dir}" ]; then
    echo "Polygon directory not found: ${polygon_dir}" >&2
    exit 1
fi

shopt -s nullglob
for polygon_file in "${polygon_dir}"/example*.polygon; do
    echo "Running ${polygon_file}"
    if ! "${script_dir}/run.sh" "${polygon_file}"; then
        echo "Failed: ${polygon_file}" >&2
        status=1
    fi
done
shopt -u nullglob

exit "${status}"
