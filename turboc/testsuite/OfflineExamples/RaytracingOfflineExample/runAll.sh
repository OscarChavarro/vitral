#!/bin/sh
# Renders every MIT scene (.ray) to a .ppm image on this folder, forwarding
# extra options (e.g. --resolution scene) to the raytracer
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SCENE_DIR="../../../../etc/geometry/mitscenes"
OUTPUT_DIR="."
cd "$SCRIPT_DIR"

found=0
for scene_file in "$SCENE_DIR"/*.ray; do
    [ -f "$scene_file" ] || continue
    found=1
    base_name="$(basename "$scene_file" .ray)"
    output_file="$OUTPUT_DIR/$base_name.ppm"

    echo "Rendering $scene_file -> $output_file"
    sh ./run.sh --scene "$scene_file" --output "$output_file" "$@"
done

if [ "$found" -eq 0 ]; then
    echo "No se encontraron escenas .ray en $SCENE_DIR" >&2
    exit 1
fi
