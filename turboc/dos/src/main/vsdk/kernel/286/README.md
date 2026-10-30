# Vitral 80286 protected-mode runtime

## Loader contract

Invoke `loader.exe NAME [arguments...]` from the directory containing
`NAME.BIN` and `NAME.MAP`. NAME contains 1-8 ASCII letters, digits, `_` or
`-`, without an extension or path. The loader reads both files from the
current directory and passes
the remaining command line to the payload as `int main(int argc, char** argv)`;
`argv[0]` is NAME as supplied, arguments may be double-quoted, and
`argv[argc]` is null.
The DOS command tail is limited to 126 bytes and the payload accepts at most
30 additional arguments. The payload must use the 286 runtime ABI, export
the loader state symbols in a detailed TLINK map, and link the same mode
switch object first as the loader. It must provide the separate 32 KiB stack.
The loader captures the current DOS directory on drive C: and passes it to
the runtime. Relative paths use that directory; absolute C: paths start at
the volume root. The protected FAT backend supports existing 8.3 directories
and does not mount other drives. The loader rejects a different current drive.
The loader initializes the protected runtime before calling `main`; after
`main` returns, the runtime runs exit handlers and waits for a key to reboot.

This backend runs the portable raytracer in 80286 protected mode under DOS.
It requires a 287-compatible FPU and exclusive access to extended memory;
boot without HIMEM, EMM386, DPMI or other extended-memory managers. The
loader reports detected XMS, DPMI and protected/virtual-8086 environments
and exits to DOS with an error before switching modes.

The DOS launcher loads the protected payload and its MAP file, initializes
memory and processor descriptors, and enters protected mode. The runtime
provides a heap, C and C++ support, VGA text and progress display, keyboard
input, and ATA PIO access to FAT12/FAT16 files. The raytracer
reads 8.3 scene files and writes PPM images. RGB storage can span multiple
64 KiB segments. On completion, the program waits for a key and reboots.

The packages are `boot`, `loader`, `drivers`, `fs`, `kernel`, `vitralLibC`,
`vitralLibC++` and `svgalib`. Define `TURBO_C_286_ENABLED` to select this
backend; the separate 8086 backend uses XMS and its own build option.
