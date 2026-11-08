#!/usr/bin/env bash
# Populate the generated etc tree with the assets used by the framebuffer editor and its sample scenes.
set -euo pipefail

root="${1:?missing loop root directory}"
etc="${2:?missing Vitral etc directory}"
application="${3:?missing SceneEditor application directory}"
mkdir -p "$root"
staging="$(mktemp -d "$root/.etc.XXXXXXXX")"
trap 'rm -rf -- "$staging"' EXIT

copy_asset() {
    local asset="$1"
    mkdir -p "$staging/$(dirname "$asset")"
    cp -- "$etc/$asset" "$staging/$asset"
}

for asset in gui/english.json gui/spanish.json gui/dialogs.json palettes/Cranes.gpl \
    geometry/cow.obj geometry/cow.mtl textures/miniearth.png bumpmaps/earth.bw; do
    copy_asset "$asset"
done
for asset in "$etc"/geometry/mitscenes/*.ray; do
    copy_asset "geometry/mitscenes/${asset##*/}"
done
for asset in "$etc"/cubemaps/dorise1/entorno{0,1,2,3,4,5}.jpg; do
    copy_asset "cubemaps/dorise1/${asset##*/}"
done
for asset in f_quit.gif f_new.gif f_load.gif f_save.gif marker72.png; do
    copy_asset "icons/$asset"
done
for id in 1010 1020 1022 1024 1073 1060; do
    copy_asset "icons/3dsmax7Icons/3dsmaxIcon${id}.png"
    copy_asset "icons/3dsmax7Icons/3dsmaxIcon${id}Mask.png"
done

if [[ -d "$root/etc" ]] && diff -qr "$staging" "$root/etc" >/dev/null; then
    rm -rf -- "$staging"
else
    rm -rf -- "$root/etc"
    mv -- "$staging" "$root/etc"
fi
trap - EXIT
mkdir -p "$root/$application"
