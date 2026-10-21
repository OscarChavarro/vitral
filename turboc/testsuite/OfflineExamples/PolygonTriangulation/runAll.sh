#!/bin/sh

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
POLYGON_DIR="$SCRIPT_DIR/../../../../etc/polygons"
status=0

if [ ! -d "$POLYGON_DIR" ]; then
    echo "Polygon directory not found: $POLYGON_DIR" >&2
    exit 1
fi

for polygon_file in "$POLYGON_DIR"/example*.polygon; do
    [ -f "$polygon_file" ] || continue
    echo "Running $polygon_file"
    if ! sh "$SCRIPT_DIR/run.sh" "$polygon_file"; then
        echo "Failed: $polygon_file" >&2
        status=1
    fi
done

exit "$status"
