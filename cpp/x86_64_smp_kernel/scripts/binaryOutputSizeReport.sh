#!/usr/bin/env bash
# Reports the size of the deliverables (boot.img for QEMU/BIOS, kernel64.bin for GRUB) and where their bytes go,
# from the linker map of kernel64.elf.
# Usage: scripts/binaryOutputSizeReport.sh [-b BUILD_DIR] [-t TOP]
#   -b  build directory (default: build); it must already contain kernel64.map (run scripts/run64.sh -n)
#   -t  how many objects / symbols to list in the rankings (default: 15)
# If base_check.elf was built (cmake --build build --target base_check.elf), it also shows how much of the
# Vitral base library the kernel image leaves out thanks to section garbage collection.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD="$ROOT/build"
TOP=15
while getopts "b:t:" opt; do
    case "$opt" in
        b) BUILD="$OPTARG" ;;
        t) TOP="$OPTARG" ;;
        *) echo "usage: $0 [-b BUILD_DIR] [-t TOP]" >&2; exit 1 ;;
    esac
done
[[ -f "$BUILD/kernel64.map" ]] || { echo "$BUILD/kernel64.map not found: build first (scripts/run64.sh -n)" >&2; exit 1; }

size_of() { stat -f%z "$1" 2>/dev/null || stat -c%s "$1"; }

echo "== Deliverables =="
describe() {  # file, purpose
    if [[ -f "$BUILD/$1" ]]; then printf "%-14s %9d bytes  %s\n" "$1" "$(size_of "$BUILD/$1")" "$2"; fi
}
describe boot.img "disk image for QEMU or a BIOS machine (our own boot sector + stage 2 + kernel)"
describe kernel64.bin "the kernel alone, for GRUB: multiboot2 /boot/kernel64.bin"
describe vitral.iso "bootable GRUB CD with the kernel inside (only after scripts/rungrub.sh)"
echo

python3 - "$BUILD/kernel64.map" "$TOP" "$ROOT" <<'PY'
import re, sys, collections

map_path, top, root = sys.argv[1], int(sys.argv[2]), sys.argv[3]
line_re = re.compile(r'^\s*([0-9a-f]+)\s+([0-9a-f]+)\s+([0-9a-f]+)\s+(\d+)\s(\s*)(.*)$')

NOT_IN_IMAGE = ('.bss', '.boottables', '.symtab', '.strtab', '.shstrtab')
sections = collections.OrderedDict()
objects = collections.Counter()
groups = collections.defaultdict(lambda: collections.Counter())
symbols = []
current = None
current_object = None

def group_of(path):
    p = path.split(':(')[0]
    if 'vitral/cpp/base/' in p:
        rest = p.split('vitral/cpp/base/src/main/')[-1]
        parts = rest.split('/')
        return 'base (Vitral)/' + '/'.join(parts[:2] if parts[0] != 'vsdk' else parts[:3])
    if 'vitral/cpp/testsuite/' in p:   # sources of Vitral compiled unchanged (the raytracer executors)
        return 'Vitral testsuite/' + p.split('vitral/cpp/testsuite/')[-1].split('/')[1]
    m = re.search(r'CMakeFiles/([^/]+)\.dir/(.*)$', p)
    if not m:
        return 'other'
    target, src = m.groups()
    if src.startswith('src/main/lib/vitralLibC'):
        return 'lib/vitralLibC'
    if src.startswith('src/main/lib/vitralStdC++'):
        return 'lib/vitralStdC++'
    parts = src.split('/')
    if parts[:2] == ['src', 'main']:
        parts.pop(1)
    if parts[:2] == ['src', 'lib']:
        return 'lib/' + parts[2]
    if parts[:2] == ['src', 'programs']:
        return 'programs/' + parts[2]
    if parts[:3] == ['src', 'os', 'drivers']:
        return 'os/drivers/' + parts[3]
    if parts[:2] == ['src', 'os']:
        return '/'.join(parts[1:3])
    return '/'.join(parts[:2])

for raw in open(map_path):
    m = line_re.match(raw)
    if not m:
        continue
    vma, lma, size, align, indent, text = m.groups()
    size = int(size, 16)
    depth = len(indent)
    if depth == 0 and not text.startswith('CMakeFiles') and not text.startswith('/') and '=' not in text and text.startswith('.'):
        current = text.split()[0]
        sections[current] = size
        continue
    if depth >= 7 and ':(' in text:
        current_object = text
        if current and size and current not in NOT_IN_IMAGE:
            name = text.split(':(')[0].split('.dir/')[-1] if '.dir/' in text else text
            # CMake mirrors the absolute path of a source outside this repository under the .dir: show it from vitral/cpp
            objects[(group_of(text), re.sub(r'^.*?(vitral/cpp/)', r'\1', name))] += size
        if current and size:
            groups[group_of(text)][current] += size
    elif depth >= 15 and current_object and size:
        symbols.append((size, text, group_of(current_object)))

print("== Output sections (linker script) ==")
file_bytes = 0
for name, size in sections.items():
    in_file = name not in NOT_IN_IMAGE
    if in_file:
        file_bytes += size
    print(f"{name:<12} {size:>10} bytes  {'in the image' if in_file else ('ELF symbol tables only' if name in ('.symtab', '.strtab', '.shstrtab') else 'memory only (not in kernel64.bin)')}")
print(f"{'total':<12} {file_bytes:>10} bytes  in the image (plus alignment padding)")
print()

print("== By part (bytes in the image / bytes of .bss) ==")
rows = []
for g, c in groups.items():
    if g == 'other' and not any(k not in NOT_IN_IMAGE for k in c):
        continue
    bss = c.get('.bss', 0) + c.get('.boottables', 0)
    img = sum(v for k, v in c.items() if k not in NOT_IN_IMAGE)
    rows.append((img, bss, g))
rows.sort(reverse=True)
total_img = sum(r[0] for r in rows) or 1
for img, bss, g in rows:
    print(f"{g:<48} {img:>9} {100.0 * img / total_img:5.1f}%   bss {bss:>9}")
print()

print(f"== {top} largest object files (image bytes) ==")
by_object = collections.Counter()
for (g, obj), size in objects.items():
    by_object[obj] += size
for obj, size in by_object.most_common(top):
    print(f"{size:>9}  {obj}")
print()

print(f"== {top} largest symbols ==")
for size, name, g in sorted(symbols, reverse=True)[:top]:
    print(f"{size:>9}  {name[:90]}  [{g}]")
PY

if [[ -f "$BUILD/base_check.map" ]]; then
    echo
    echo "== Vitral base library: everything linked vs. what the kernel image keeps =="
    python3 - "$BUILD/base_check.map" "$BUILD/kernel64.map" <<'PY'
import re, sys

def base_bytes(path):
    total = 0
    line_re = re.compile(r'^\s*([0-9a-f]+)\s+([0-9a-f]+)\s+([0-9a-f]+)\s+(\d+)\s(\s*)(.*)$')
    for raw in open(path):
        m = line_re.match(raw)
        if m and len(m.group(5)) >= 7 and ':(' in m.group(6) and 'vitral/cpp/base/' in m.group(6) \
                and not m.group(6).endswith('(.bss)'):
            total += int(m.group(3), 16)
    return total

everything, kept = base_bytes(sys.argv[1]), base_bytes(sys.argv[2])
print(f"all base objects linked (no gc): {everything:>9} bytes")
print(f"kept in kernel64.elf:            {kept:>9} bytes")
PY
fi
