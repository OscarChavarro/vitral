#!/usr/bin/env bash
set -eu
if [ "$#" -eq 0 ]; then set -- output3.png; fi
gradle -q run -PrunMainClass=SmoothPolygonTest --args="$*"
