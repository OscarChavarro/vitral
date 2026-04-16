#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$SCRIPT_DIR"
while [ ! -f "$ROOT_DIR/gradlew" ] && [ "$ROOT_DIR" != "/" ]; do
  ROOT_DIR="$(dirname "$ROOT_DIR")"
done

if [ ! -f "$ROOT_DIR/gradlew" ]; then
  echo "gradlew not found desde $SCRIPT_DIR" >&2
  exit 1
fi

cd "$SCRIPT_DIR"


GRADLE_USER_HOME="${GRADLE_USER_HOME:-$ROOT_DIR/.gradle-home}"
export GRADLE_USER_HOME

TASK=":testsuite:ApplicationCases:SearchEngineFor3DModels:runMain"
MAIN_CLASS="BatchConsole"
RUN_JVM_ARGS="-Djava.library.path=../../../lib|-Xms100m|-Xmx1000m"
DEFAULT_ARGS=()
COMBINED_ARGS=("${DEFAULT_ARGS[@]}" "$@")
GRADLE_CMD=("$ROOT_DIR/gradlew" "$TASK" "-PrunMainClass=$MAIN_CLASS")

if [ -n "$RUN_JVM_ARGS" ]; then
  GRADLE_CMD+=("-PrunJvmArgs=$RUN_JVM_ARGS")
fi

if [ ${#COMBINED_ARGS[@]} -gt 0 ]; then
  ARG_STRING="$(printf '%q ' "${COMBINED_ARGS[@]}")"
  ARG_STRING="${ARG_STRING% }"
  GRADLE_CMD+=("--args=$ARG_STRING")
fi
"${GRADLE_CMD[@]}"

