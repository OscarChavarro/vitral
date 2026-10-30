# Vitral 80286 protected-mode runtime

This backend runs the portable raytracer in 80286 protected mode under DOS.
It requires a 287-compatible FPU and exclusive access to extended memory;
boot without HIMEM, EMM386, DPMI or other extended-memory managers.

The DOS launcher loads the protected payload and its MAP file, initializes
memory and processor descriptors, and enters protected mode. The runtime
provides a heap, C and C++ support, VGA text and progress display, keyboard
input, and ATA PIO access to FAT12/FAT16 root-directory files. The raytracer
reads 8.3 scene files and writes PPM images. RGB storage can span multiple
64 KiB segments. On completion, the program waits for a key and reboots.

The packages are `boot`, `drivers`, `fs`, `kernel`, `vitralLibC`,
`vitralLibC++` and `svgalib`. Define `TURBO_C_286_ENABLED` to select this
backend; the separate 8086 backend uses XMS and its own build option.
