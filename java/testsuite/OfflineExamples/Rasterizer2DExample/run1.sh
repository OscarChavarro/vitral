#!/usr/bin/env bash
set -eu
if [ "$#" -eq 0 ]; then set -- output1.png; fi
gradle -q run -PrunMainClass=LineTest --args="$*"
