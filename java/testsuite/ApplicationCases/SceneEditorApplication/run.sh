#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -gt 0 ]; then
    joined_args=$(printf '%s|' "$@")
    gradle --quiet :testsuite:ApplicationCases:SceneEditorApplication:runMain \
        -PrunMainClass=application.AwtJogl4SceneEditorApplication \
        -PrunJvmArgs='-Djava.library.path=../../../lib|-Xms300m|-Xmx300m' \
        "-PrunArgs=${joined_args%|}"
else
    gradle --quiet :testsuite:ApplicationCases:SceneEditorApplication:runMain \
        -PrunMainClass=application.AwtJogl4SceneEditorApplication \
        -PrunJvmArgs='-Djava.library.path=../../../lib|-Xms300m|-Xmx300m'
fi
