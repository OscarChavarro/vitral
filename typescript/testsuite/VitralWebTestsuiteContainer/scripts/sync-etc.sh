#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd -- "$SCRIPT_DIR/.." && pwd)"
ETC_SOURCE="$PROJECT_DIR/../../../etc"
ETC_PUBLIC="$PROJECT_DIR/public/etc"

if [[ ! -d "$ETC_SOURCE" ]]; then
    echo "Missing VITRAL etc directory: $ETC_SOURCE" >&2
    exit 1
fi

if [[ -L "$ETC_PUBLIC" ]]; then
    unlink "$ETC_PUBLIC"
fi

mkdir -p "$ETC_PUBLIC"
cp -a "$ETC_SOURCE"/. "$ETC_PUBLIC"/

# A browser can not list a folder, so the languages of the GUI (the JSON
# files of etc/gui, as Java's GuiState.listLanguages finds them) are listed
# in a manifest the web applications fetch.
GUI_PUBLIC="$ETC_PUBLIC/gui"
if [[ -d "$GUI_PUBLIC" ]]; then
    {
        printf '['
        first=1
        for file in $(cd "$GUI_PUBLIC" && ls *.json 2>/dev/null | grep -v '^languages\.json$' | sort); do
            if [[ $first -eq 0 ]]; then printf ','; fi
            printf '"%s"' "${file%.json}"
            first=0
        done
        printf ']\n'
    } > "$GUI_PUBLIC/languages.json"
fi

# The tree of etc, for the file choosers of the web applications
node "$SCRIPT_DIR/etc-index.mjs" "$ETC_PUBLIC"
