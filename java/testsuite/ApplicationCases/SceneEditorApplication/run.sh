#!/usr/bin/env bash
set -euo pipefail

run_args=()
if [ "$#" -gt 0 ]; then
    joined_args=$(printf '%s|' "$@")
    run_args+=("-PrunArgs=${joined_args%|}")
fi

gradle --quiet :testsuite:ApplicationCases:SceneEditorApplication:runMain \
    -PrunMainClass=application.SceneEditorApplication \
    -PrunJvmArgs='-Djava.library.path=../../../lib|-Xms300m|-Xmx300m' \
    "${run_args[@]}"
