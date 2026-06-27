#!/usr/bin/env bash

EXTRA_ARGS=""
if [ "$#" -gt 0 ]; then
    JOINED=$(printf '%s|' "$@")
    JOINED="${JOINED%|}"
    EXTRA_ARGS="-PrunArgs=${JOINED}"
fi

gradle --quiet :testsuite:Jogl4Examples:MeshExample:runMain \
  -PrunMainClass=MeshExample \
  -PrunJvmArgs='-Xms300m|-Xmx300m|--add-exports=java.desktop/sun.awt=ALL-UNNAMED|--add-opens=java.desktop/sun.awt=ALL-UNNAMED' \
  ${EXTRA_ARGS}
