#!/usr/bin/env bash
set -euo pipefail

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
typescript_dir=$(CDPATH= cd -- "$script_dir/../../.." && pwd)

npm --prefix "$typescript_dir" run build
npm --prefix "$script_dir" run build

cd "$script_dir"

# Si se indica una escena/salida concreta (o --help), delegar en una sola ejecución.
# Flags de renderizado como --parallel o --nosave se reenvían a cada escena del lote.
single_run=false
for arg in "$@"; do
  case "${arg}" in
    --scene|-s|--output|-o|--help|-h) single_run=true ;;
    nosave) ;;
    -*) ;;
    *) single_run=true ;;
  esac
done

if [[ "${single_run}" == true ]]; then
  node "$script_dir/dist/RaytracerSimple.js" "$@"
else
  # The program runs with this directory as its working directory, and the
  # scene files reach their assets through ../../../../etc/... paths.
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
    node "$script_dir/dist/RaytracerSimple.js" "$@" \
      --scene "${scene_file}" --output "${output_file}"
  done
fi
