#!/usr/bin/env bash

# Homebrew libX11 1.8.13 tries to load locale modules as *.so.2 from its
# installation directory, although Homebrew installs them as *.2.so. XLC
# honors XLOCALEDIR, but XOM uses the compiled-in module path, so an overlay
# is insufficient for XCreateFontSet.
prepare_homebrew_x11_locale() {
    local module_dir
    module_dir="$(brew --prefix libx11)/lib/X11/locale/common"
    if [[ ! -d "$module_dir" ]]; then
        echo "Homebrew libX11 locale modules are missing from $module_dir" >&2
        return 1
    fi

    local item module expected
    for item in "$module_dir"/*.2.so; do
        [[ -f "$item" ]] || continue
        module="${item##*/}"
        expected="$module_dir/${module%.2.so}.so.2"
        if [[ ! -e "$expected" ]]; then
            ln -sfn "$module" "$expected" || return 1
        fi
    done
}
