#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "$0")" && pwd)"
cd "${script_dir}"

if [[ ! -x "./build/RaytracingOfflineExample" ]]; then
  cmake -S . -B build
  cmake --build build --target RaytracingOfflineExample
fi

if [[ $# -gt 0 ]]; then
  ./build/RaytracingOfflineExample "$@"
else
  scene_dir="../../../../etc/geometry/mitscenes"
  output_dir="."
  shopt -s nullglob
  ray_files=( "${scene_dir}"/*.ray )
  shopt -u nullglob

  if [[ ${#ray_files[@]} -eq 0 ]]; then
    echo "No se encontraron escenas .ray en ${scene_dir}" >&2
    exit 1
  fi

  for scene_file in "${ray_files[@]}"; do
    base_name="$(basename "${scene_file}" .ray)"
    output_file="${output_dir}/${base_name}.ppm"

    echo "Rendering ${scene_file} -> ${output_file}"
    ./build/RaytracingOfflineExample --scene "${scene_file}" --output "${output_file}"
  done
fi
