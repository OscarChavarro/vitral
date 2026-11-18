#!/bin/bash
# Drives the DOS VirtualBox VM of the test environment from the host, through
# its console (no network is needed in the guest). See doc/portability.md,
# "Example: DOS nodes", and "Validated DOS VM transfer procedure" in
# turboc/README.md.
#
#   dosVm.sh type "<command>" [seconds]   type a command line, press Enter, wait
#   dosVm.sh shot <file.png>              screenshot of the guest screen
#   dosVm.sh text                         OCR text of the guest screen
#   dosVm.sh wait [poll] [max]            wait until the screen stops changing
#                                         at a DOS prompt; prints its last lines
#   dosVm.sh insert <file.iso>            put an ISO in the CD drive (D:)
#   dosVm.sh full <staging dir>           replace C:\VITRAL\ETC and TURBOC with
#                                         the VITRAL.ISO of stageDosFull.sh
#   dosVm.sh patch <new stage> <old stage>  send only the files that changed
#   dosVm.sh get <host dir> <DOS dir> <file>...  copy guest files to the host
#                                         through a scratch floppy image (map
#                                         and log files of a build)
#   dosVm.sh ctrlc                        send Ctrl+C
#
# Environment: DOSVM (VM name, default DOS), DOSVM_WORK (scratch folder of the
# host, default a folder under ${TMPDIR:-/tmp}). Needs VBoxManage,
# ImageMagick (convert) and tesseract for the OCR, mkisofs for the ISOs,
# mtools for get.
#
# Rules: every file created in the guest stays inside C:\VITRAL, and only the
# etc/ and turboc/ payload of stageDosFull.sh is sent, never the repository.
set -eu
VM="${DOSVM:-DOS}"
WORK="${DOSVM_WORK:-${TMPDIR:-/tmp}/vitral-dosvm}"
mkdir -p "$WORK"

# COMMAND.COM loses keys when long strings are typed at once (Shift gets
# stuck): type in chunks of 12 characters.
type_line() {
    local text="$1"
    while [ -n "$text" ]; do
        VBoxManage controlvm "$VM" keyboardputstring "${text:0:12}"
        text="${text:12}"
        sleep 0.4
    done
    VBoxManage controlvm "$VM" keyboardputscancode 1c 9c
}

screen_text() {
    VBoxManage controlvm "$VM" screenshotpng "$WORK/screen.png"
    convert "$WORK/screen.png" -resize 300% -negate "$WORK/screen-ocr.png"
    tesseract "$WORK/screen-ocr.png" - 2>/dev/null | sed '/^[[:space:]]*$/d'
}

# Waits until two screenshots in a row give the same text and the last line
# is a prompt (ends with ">").
wait_prompt() {
    local poll="${1:-10}" max="${2:-3600}" waited=0 previous="" text last
    while [ "$waited" -lt "$max" ]; do
        sleep "$poll"
        waited=$((waited + poll))
        text="$(screen_text)"
        last="$(printf '%s\n' "$text" | tail -1)"
        if [ "$text" = "$previous" ] && [[ "$last" =~ \>[[:space:]_]*$ ]]; then
            printf '%s\n' "$text" | tail -12
            return 0
        fi
        previous="$text"
    done
    echo "dosVm.sh: no prompt after $max s" >&2
    printf '%s\n' "$text" | tail -12
    return 1
}

insert_iso() {
    VBoxManage storageattach "$VM" --storagectl IDE --port 1 --device 0 \
        --type dvddrive --medium "$1"
}

# ISO with PATCH.TGZ and DELOLD.BAT, which deletes the old copies first (djtar
# does not overwrite files: it stops at a prompt). COMMAND.COM truncates lines
# at 127 characters, so DELOLD changes to each folder and deletes bare names.
make_patch_iso() {
    local stage="$1" list="$2" dir="$WORK/patch"
    rm -rf "$dir"
    mkdir -p "$dir"
    tar --format=gnu -czf "$dir/PATCH.TGZ" -C "$stage" -T "$list"
    python3 - "$list" "$dir/DELOLD.BAT" <<'PY'
import sys
lines = ["@echo off", "rem Deletes the files replaced by PATCH.TGZ"]
for path in open(sys.argv[1]).read().split():
    folder, name = path.rsplit("/", 1)
    lines += ["cd \\VITRAL\\" + folder.replace("/", "\\"), "if exist " + name + " del " + name]
lines.append("cd \\VITRAL")
assert max(len(line) for line in lines) < 120
open(sys.argv[2], "wb").write(("\r\n".join(lines) + "\r\n").encode())
PY
    mkisofs -quiet -iso-level 1 -V PATCH -o "$WORK/PATCH.ISO" "$dir"
}

case "${1:-}" in
type)
    type_line "$2"
    sleep "${3:-2}"
    ;;
shot)
    VBoxManage controlvm "$VM" screenshotpng "$2"
    ;;
text)
    screen_text
    ;;
wait)
    wait_prompt "${2:-10}" "${3:-3600}"
    ;;
insert)
    insert_iso "$2"
    ;;
ctrlc)
    VBoxManage controlvm "$VM" keyboardputscancode 1d 2e ae 9d
    ;;
full)
    stage="$2"
    insert_iso "$stage/VITRAL.ISO"
    type_line "cls"; sleep 1
    type_line "cd C:\\VITRAL"; sleep 1
    type_line "deltree /y TURBOC"; wait_prompt 5 600 >/dev/null
    type_line "deltree /y ETC"; wait_prompt 5 600 >/dev/null
    type_line "C:\\NETDOS\\DJTAR -x D:\\VITRAL.TGZ > C:\\VITRAL\\DJTAR.LOG"; wait_prompt 10 1200 >/dev/null
    type_line "cls"; sleep 1
    type_line "dir /s /b /a-d ETC | find /c /v \"\""; sleep 8
    type_line "dir /s /b /a-d TURBOC | find /c /v \"\""; wait_prompt 5 120
    echo "expected: ETC $(grep -c '^ETC/' "$stage/FILES.LST"), TURBOC $(grep -c '^TURBOC/' "$stage/FILES.LST")"
    ;;
patch)
    new="$2"
    old="$3"
    list="$WORK/changed.lst"
    : > "$list"
    while read -r path; do
        cmp -s "$new/$path" "$old/$path" || echo "$path" >> "$list"
    done < "$new/FILES.LST"
    if [ ! -s "$list" ]; then
        echo "dosVm.sh: no changed files"
        exit 0
    fi
    cat "$list"
    make_patch_iso "$new" "$list"
    insert_iso "$WORK/PATCH.ISO"
    type_line "cls"; sleep 1
    type_line "cd C:\\VITRAL"; sleep 1
    type_line "D:\\DELOLD"; wait_prompt 3 300 >/dev/null
    type_line "C:\\NETDOS\\DJTAR -x D:\\PATCH.TGZ > C:\\VITRAL\\PATCH.LOG"; wait_prompt 3 600 >/dev/null
    type_line "find /c \"TURBOC\" C:\\VITRAL\\PATCH.LOG"; wait_prompt 3 60
    echo "expected: $(grep -c . "$list") entries"
    ;;
get)
    # The floppy image of the VM (A:) is replaced by an empty one while
    # the files are copied, and put back afterwards
    out="$2"
    folder="$3"
    shift 3
    image="$WORK/GET.IMG"
    floppy=$(VBoxManage showvminfo "$VM" --machinereadable | sed -n 's/^"Floppy-0-0"="\(.*\)"$/\1/p')
    rm -f "$image"
    mformat -C -f 1440 -i "$image" ::
    VBoxManage storageattach "$VM" --storagectl Floppy --port 0 --device 0 --medium "$image" --forceunmount
    type_line "cls"; sleep 1
    for name in "$@"; do
        type_line "copy $folder\\$name A:\\"; sleep 3
    done
    wait_prompt 3 300 >/dev/null
    VBoxManage storageattach "$VM" --storagectl Floppy --port 0 --device 0 --medium "${floppy:-emptydrive}" --forceunmount
    mkdir -p "$out"
    for name in "$@"; do
        mcopy -o -i "$image" "::$name" "$out/" && echo "$out/$name"
    done
    ;;
*)
    sed -n '2,27p' "$0"
    exit 2
    ;;
esac
