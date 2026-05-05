#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../../.."

gradle :testsuite:Jogl4Examples:ShadersExample:runMain \
  -PrunMainClass=ShadersExample \
  --args="--offline /tmp/raytrace.png --method software --rotation 35 --light-rotation 20"

gradle :testsuite:Jogl4Examples:ShadersExample:runMain \
  -PrunMainClass=ShadersExample \
  --args="--offline /tmp/opengl.png --method opengl --rotation 35 --light-rotation 20"
