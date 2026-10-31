# VGAExamples

Linux builds of the graphics programs of the Vitral Kernel (`../../x86_64_smp_kernel`). Inside the kernel they
use its svgalib, readline and libc compatibility libraries. Here they use the operating system's svgalib, GNU
readline and libc, so they run on a text console of a Linux host and look and behave the same as in the kernel.

| Program | Linux executable | Built by |
|---|---|---|
| `vga` | `vitral_vga` | this directory's `CMakeLists.txt` |
| `vitral_rasterizer` | `vitral_rasterizer` | this directory's `CMakeLists.txt` |
| `vitral_raytracer` | `vitral_raytracer` | this directory's `CMakeLists.txt` |
| `sceneEditor` | `sceneEditor` | not built here yet (see [sceneEditor](#sceneeditor)) |

`vga/`, `vitralRasterizer/` and `vitralRaytracer/` are copies of the kernel's `src/main/programs/` sources and may
differ from them. The shared helpers (`lib/dithering`, `lib/mouseCursor`, `lib/vgaModes`) are compiled from the
kernel tree.

## Host setup

The reference host is Ubuntu 20.04 (x86_64, GCC 9, CMake 3.16) in a VirtualBox VM. Do these steps once, as root.

1. **svgalib 1.9.25** from <https://github.com/kakkeshyor/svgalib>, in `/usr/local/src/svgalib`. Build it as a
   normal user and install it as root. Skip the `svgalib_helper` kernel module, which does not build on current kernels:

   ```sh
   make shared static INSTALLMODULE=          # as the owner of the source tree
   make install INSTALLMODULE=                # as root; installs into /usr/local and runs ldconfig
   ```

   With GCC 9 or newer, two fixes are needed:
   - `utils/gtf/gtfcalc.c`: rename its `static round()` to `gtf_round()`, because it clashes with glibc.
   - `threeDKit`: build it with `make -C threeDKit CC="cc -fgnu89-inline"`, because it relies on GNU89 `extern inline`.

   The `plane` and `wrap` demos of `threeDKit` do not link. They are not needed.

2. **readline**: `apt-get install libreadline-dev`.

3. **`/etc/vga/libvga.config`**: because there is no helper module, switch to `NoHelper` mode, which uses
   `iopl`/`ioperm` and `/dev/mem`. Then configure the mouse:

   ```
   # Helper /dev/svga
   NoHelper
   mouse IMPS2
   mdev /dev/input/mice
   ```

   Without this, programs stop with `svgalib: Cannot open /dev/svga ... Is svgalib_helper module loaded?`.

4. **A plain VGA text console.** No DRM or KMS driver may own the card, or svgalib cannot program it:
   - **Kernel command line**: `nomodeset`.
   - **GRUB**: `GRUB_TERMINAL=console` and `GRUB_GFXPAYLOAD_LINUX=text`.
   - **Module blacklist** (in `/etc/modprobe.d/`): `vmwgfx`, `vboxvideo`, `simpledrm`, `bochs` and `cirrus`.

   `dmesg | grep Console` must show only `colour VGA+ 80x25`, and `/proc/fb` must be empty.

5. **VirtualBox**: use a PS/2 mouse and turn mouse integration off. With the absolute USB tablet,
   `/dev/input/mice` receives no movement.

## Build

Configure the whole `cpp` tree with the kernel's `src/main` as the portable source directory:

```sh
cd cpp
cmake -S . -B build -DWITH_JPEG=ON \
      -DVITRAL_VGA_PORTABLE_SOURCE_DIR="$PWD/x86_64_smp_kernel/src/main"
cmake --build build -j"$(nproc)" --target vitral_vga vitral_rasterizer vitral_raytracer
```

If configuration prints `VGAExamples disabled: ...`, the message names what is missing: svgalib, readline or the
portable source directory. The option `VITRAL_BUILD_VGA_EXAMPLES=OFF` skips this directory.

### sceneEditor

`sceneEditor` (`x86_64_smp_kernel/src/main/programs/sceneEditor`) is not part of this directory's `CMakeLists.txt`.
It will move to `../ApplicationCases`. Until then, build it out of tree with a small CMake project that has these
pieces:

- **The base library**: `add_subdirectory(cpp/base)`, configured with `WITH_JPEG=ON`. `cpp/base` always writes
  `libvitral_base.so` into `cpp/base/build`, the same file the main build uses, so its options must match those of
  `cpp/scripts/compile.sh`.
- **The program**: the sources below, compiled into one executable:
  - `x86_64_smp_kernel/src/main/programs/sceneEditor/SceneEditor.cpp`
  - `x86_64_smp_kernel/src/main/lib/vgaModes/VgaModes.cpp`
  - `x86_64_smp_kernel/src/main/lib/mouseCursor/MouseCursor.cpp`
  - `svgalib/src/main/vsdk/toolkit/gui/framebuffer/svgalib/SvgalibFramebuffer.cpp`
  - `svgalib/src/main/vsdk/toolkit/gui/framebuffer/svgalib/SvgalibInput.cpp`
- **Base sources missing from `cpp/base/CMakeLists.txt`**, added to the executable as well:
  - `base/src/main/vsdk/toolkit/gui/framebuffer/*.cpp` (6 files)
  - `base/src/main/jackson/databind/JsonNode.cpp`
  - `base/src/main/jackson/databind/ObjectMapper.cpp`
- **Include directories**: `x86_64_smp_kernel/src/main` and `svgalib/src/main`. Do not add the kernel's
  `src/main/lib/svgalib` or `src/main/lib/readline/include`, which are the kernel's stand-ins for the system headers.
- **Compile definition**: `SCENE_EDITOR_GUI_DIR="<checkout>/etc/gui"`, the directory with `english.json` and
  `spanish.json`.
- **Libraries**: `vitral_base`, `libvga` and `libreadline`.

## Run

svgalib in `NoHelper` mode needs root (for `/dev/mem` and `iopl`) and a real virtual console. Run the programs on
the host's console (for example tty1). They do not work over SSH or inside X.

```sh
sudo ./vitral_vga [MODE]
sudo ./vitral_rasterizer [MODE]
sudo ./vitral_raytracer [SCENE.ray] [MODE]
sudo ./sceneEditor [english | spanish | FILE.json] [MODE]
```

- **`MODE`** is an svgalib mode such as `G640x480x16`. Without it, each program picks a default or offers a menu.
  The programs only show modes that the detected chipset supports.
- **`vitral_raytracer`** defaults to `etc/geometry/mitscenes/balls.ray`, set by the CMake variable
  `VITRAL_RAYTRACER_DEFAULT_SCENE`.
- **`sceneEditor`** shows the Scene Editor's menu bar. Use the mouse, or F10, the arrows, Enter and Escape.
  `r` and `R` step through the graphics modes, and Escape quits when no menu is open.

Every program restores text mode when it exits. If a crash leaves the console in graphics mode, run
`/usr/local/bin/textmode` or `restoretextmode`, or switch to another VT and back.

### Video hardware

The emulated VMware SVGA II adapter has no specific svgalib driver, and the VESA driver needs vm86 (LRMI), which
x86_64 lacks. On that adapter only the standard VGA modes are available, such as `G320x200x256`, `G640x480x16` and
`G640x480x2`. For 256-colour SVGA modes, use an adapter svgalib knows, such as Cirrus, or `chipset` in
`libvga.config`.

## Comparing with the kernel

To run the same program inside the kernel:

```sh
cd ../../x86_64_smp_kernel
scripts/run64smp.sh          # or run64smpSVGA.sh for Bochs VBE modes
```

Then start it from the kernel shell by its kernel name, with the same arguments: `vga`, `vitral_rasterizer`,
`vitral_raytracer` or `sceneEditor`. Inside the kernel, data files live under `/media/ramdisk/etc`, for example
`/media/ramdisk/etc/gui`. See `../../x86_64_smp_kernel/README.md`.
