#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEFAULT_FILE="$SCRIPT_DIR/../../../../etc/geometry/cow.obj"
MIN_MEMORY_MIB=3000

input_file=""
args=("$@")
for ((i = 0; i < ${#args[@]}; i++)); do
    if [ "${args[$i]}" = "-tangibleServer" ]; then
        i=$((i + 1))
        continue
    fi
    if [[ "${args[$i]}" != -* ]]; then
        input_file="${args[$i]}"
        break
    fi
done

EXTRA_ARGS=""
if [ "$#" -gt 0 ]; then
    JOINED=$(printf '%s|' "$@")
    JOINED="${JOINED%|}"
    EXTRA_ARGS="-PrunArgs=${JOINED}"
else
    input_file="$DEFAULT_FILE"
    EXTRA_ARGS="-PrunArgs=${DEFAULT_FILE}"
fi

stat_file="$input_file"
if [ ! -f "$stat_file" ] && [[ "$input_file" != /* ]]; then
    if [ -f "$SCRIPT_DIR/$input_file" ]; then
        stat_file="$SCRIPT_DIR/$input_file"
    fi
fi

MEMORY_MIB="$MIN_MEMORY_MIB"
if [ -f "$stat_file" ]; then
    file_bytes=$(stat -c '%s' "$stat_file")
    computed_mib=$(( (file_bytes * 2 + 1048575) / 1048576 ))
    if [ "$computed_mib" -gt "$MEMORY_MIB" ]; then
        MEMORY_MIB="$computed_mib"
    fi
fi

gradle --quiet :testsuite:Jogl4Examples:MeshExample:runMain \
  -PrunMainClass=MeshExample \
  -PrunJvmArgs="-Xms${MEMORY_MIB}m|-Xmx${MEMORY_MIB}m|--add-exports=java.desktop/sun.awt=ALL-UNNAMED|--add-opens=java.desktop/sun.awt=ALL-UNNAMED" \
  ${EXTRA_ARGS}
