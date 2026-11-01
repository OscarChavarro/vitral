# BGI framebuffer backend (turboc port)

The backend of the framebuffer GUI (`base/src/main/vsdk/toolkit/gui/framebuffer`) for real mode DOS with Borland Turbo C++ 3.0, as `cpp/svgalib` is for svgalib. It draws with the Borland Graphics Interface (`graphics.h`, `GRAPHICS.LIB` and the `.BGI` driver files) and reads the keyboard through the BIOS and the mouse through the Microsoft mouse driver interface (INT 33h).

- `BgiModes`: the graphics modes, as `VgaModes` of the cpp minimal kernel. A fixed table lists the modes of the drivers of Turbo C++ (`CGA.BGI` for the 2 colour CGA and MCGA modes, `EGAVGA.BGI` for the 16 colour VGA modes) and of the SuperVGA drivers of Jordan Hargraphix (<https://github.com/jharg93/SvgaBGI>: `SVGA16.BGI`, `SVGA256.BGI`, `SVGA64K.BGI` and `SVGATC.BGI`, which reach the VESA modes up to 1280x1024 with 16, 256, 65536 and 16M colours). `probe()` sets each one and keeps those that work with their size; `enter()` uses `setgraphmode()` when the driver is already loaded and `initgraph()` otherwise, and when a mode cannot be set it sets the previous one again. The drivers are loaded from `C:\TURBOC\BGI`, or from the folder in the `BGIPATH` environment variable.
- Memory: real mode leaves the program less than 640 KB, and the BGI kernel takes its blocks through `_graphgetmem` and `_graphfreemem`, which `BgiModes` replaces. A failed `initgraph()` (a mode the SvgaBGI driver does not have, error -10) keeps its 4 KB graphics buffer, and `closegraph()` does not release it either; `BgiModes` releases those buffers after a failure and ignores a later release of them by the kernel. Without it every mode probed and rejected leaked 4 KB, and the editor ran out of memory and hung after some mode changes. `probe()` leaves graphics before setting each mode, so each one is tried with a fresh `initgraph()` (VirtualBox accepts 640x350x256 with `setgraphmode()` but draws garbage on it).
- `BgiFramebuffer`: the `Framebuffer` on the current mode, as `SvgalibFramebuffer`: 1, 4, 8, 16 and 24 bits per pixel, palette entries allocated on first use and programmed in the VGA DAC (with the attribute controller made the identity in 16 colour modes), the 8x16 font of the VGA BIOS (code page 437, Latin-1 letters mapped to it, bold in modes 640 pixels wide or more), lines and bars of the BGI and software clipping. The 64K and TrueColor drivers take colors through `setrgbpalette()` with the numbers 1024 (lines), 1025 (fills) and 1026 (pixels), and their `getpixel()` leaves the whole value in DX (EDX for TrueColor, read with a 386 instruction).
- `BgiInput`: the `FramebufferInput`: enhanced BIOS keyboard services (INT 16h, functions 10h and 11h: arrows, editing keys, F1 to F12, Control and Alt plus letters) and the mouse driver (reset, ranges fitted to the mode, position and buttons). The processor waits with `HLT` between events.
- `BgiPointer`: the mouse pointer (the arrow of the kernel's `MouseCursor`), drawn with native pixels and hidden while the framebuffer draws, as the mouse driver's own pointer only knows the standard VGA modes.

## SuperVGA drivers (download, for agents and emulators)

Turbo C++ 3.0 only brings `CGA.BGI` and `EGAVGA.BGI` (in `C:\TURBOC\BGI`), which stop at 640x480 with 16 colours. The SuperVGA modes of `BgiModes` need the four SvgaBGI drivers of Jordan Hargraphix, which are not in this repository: download them from <https://github.com/jharg93/SvgaBGI> (MIT license). They were tested at commit `a082d7e3987d9634a00b50c61d69dddcd721422b`. Without them the program still runs, and `probe()` only keeps the CGA and EGA/VGA modes.

On the host (Linux), download the files as raw bytes and check them before copying them to the emulated machine:

```sh
mkdir -p svgabgi && cd svgabgi
R=https://raw.githubusercontent.com/jharg93/SvgaBGI/a082d7e3987d9634a00b50c61d69dddcd721422b
for f in SVGA16 SVGA256 SVGA64K SVGATC; do curl -fsSLo $f.BGI $R/$f.BGI; done
sha256sum -c <<'SUMS'
f8f415156bd409a58532b53ae199439156ebf2866a899fb134dc96387b997c67  SVGA16.BGI
dcf0ea9acee9c74267045ffcd83441d5336d92b26c1cdf2000d49bb0b0dc0d7f  SVGA256.BGI
111d1bdd53a9bef4b8e38ed7c9f940ac0f0ad8f5eb14c3dd40da9e0a93d63310  SVGA64K.BGI
397591770ea0811638b41ba05a8215d1b2a1d9dcc1316761ae6ad63f4d8d01c7  SVGATC.BGI
SUMS
```

- Take the `.BGI` files from the root of the repository (real mode drivers). The `BGI30/*.BGX` files are for 16-bit protected mode (Turbo Pascal 7, Borland PowerPack) and do not work with Turbo C++ 3.0. `SVGA32K.BGI`, `SVGAS3.BGI` and the `TWK*.BGI` drivers are not used by `BgiModes`.
- Download them as binary files (the raw URL, or a `git clone`), never through an HTML page or a tool that turns them into text: a driver with changed bytes makes `initgraph()` fail with error -4 (invalid driver file) or hang the machine.
- Copy them with uppercase 8.3 names to `C:\TURBOC\BGI`, next to `EGAVGA.BGI`, or to another folder given in the `BGIPATH` environment variable (`SET BGIPATH=C:\SVGABGI`).
- To bring them into the emulated machine: in VirtualBox or QEMU, make an ISO 9660 image (`mkisofs -iso-level 1 -o SVGABGI.ISO svgabgi`), attach it as the CD drive and `COPY D:\*.BGI C:\TURBOC\BGI` (the CD driver, `MSCDEX`, must be loaded). In DOSBox, `MOUNT` a host folder that holds the files is enough. A floppy image works too, as the four files take about 36 KB.
- The emulated video card needs a VESA BIOS (VBE): VirtualBox (VBoxVGA or VBoxSVGA adapter), QEMU (`-vga std`), DOSBox (`machine=svga_s3`, the default) and Bochs (`vga: extension=vbe`) have one. `SVGA16.BGI` and `SVGA256.BGI` use the VBE when they detect it.
- `SVGA64K.BGI` and `SVGATC.BGI` are distributed built with `USEVESA EQU 0`, so they only know chipset-specific modes, and in VirtualBox they give no 16 or 24 bit mode (error -10, mode not supported; `probe()` drops those modes). To get them through the VBE, set `USEVESA EQU 1` in `SOURCE/SVGA64K.ASM` and `SOURCE/SVGATC.ASM` and rebuild with `MAKEBGI.BAT` of the same repository (TASM, `TLINK /3`, `EXE2BIN` and its `BGIHDR`).

## Build (DOS, Turbo C++ 3.0)

After mapping the tree to 8.3 names (`scripts/map_to_83.py`), from `C:\VITRAL\TURBOC\BGI`:

```dos
BGI
BGILINK
```

`BGI.BAT` compiles the four sources in real mode (`-ml`, floating point emulation, `TURBO_C_8086_REAL_MODE_ENABLED`) into `BUILDTC`, and `BGILINK.BAT` archives them as `BUILD\BGI.LIB`. The programs link it with `C:\TURBOC\LIB\GRAPHICS.LIB` and with the real mode base libraries of `BASE\BUILD86` (`BASE86.BAT`, `BASELK86.BAT`).
