#!/usr/bin/env bash
set -eu
if [ "$#" -eq 0 ]; then set -- output2.png; fi
gradle -q run -PrunMainClass=PolygonTest --args="$*"
