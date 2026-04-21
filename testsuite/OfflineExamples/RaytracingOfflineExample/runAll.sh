#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "$0")" && pwd)"
cd "${script_dir}"

if [[ $# -gt 0 ]]; then
  gradle :testsuite:OfflineExamples:RaytracingOfflineExample:runMain \
    -PrunMainClass=RaytracerSimple \
    -PrunJvmArgs='-Xms300m|-Xmx300m' \
    --args="$*"
else
  # runMain (JavaExec) uses projectDir as working directory.
  # These scene files include assets using ../../../etc/... paths.
  scene_dir="../../../etc/geometry/mitscenes"
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
    output_file="${output_dir}/${base_name}.bmp"

    echo "Rendering ${scene_file} -> ${output_file}"
    gradle :testsuite:OfflineExamples:RaytracingOfflineExample:runMain \
      -PrunMainClass=RaytracerSimple \
      -PrunJvmArgs='-Xms300m|-Xmx300m' \
      --args="--scene ${scene_file} --output ${output_file}"
  done
fi
