# VGAExamples

Linux builds of the graphics programs of the Vitral Kernel (`../../x86_64_smp_kernel`). Inside the kernel they
use its svgalib, readline and libc compatibility libraries. Here they use the operating system's svgalib, GNU
readline and libc, so they run on a text console of a Linux host and look and behave the same as in the kernel.

| Program | Linux executable | Built by |
|---|---|---|
| `vga` | `vitral_vga` | this directory's `CMakeLists.txt` |
| `vitral_rasterizer` | `vitral_rasterizer` | this directory's `CMakeLists.txt` |
| `vitral_raytracer` | `vitral_raytracer` | this directory's `CMakeLists.txt` |
| `sceneEditor` | `sceneEditor` | `sceneEditor/CMakeLists.txt`, its own project (see [sceneEditor](#sceneeditor)) |

`vga/`, `vitralRasterizer/`, `vitralRaytracer/` and `sceneEditor/` are copies of the kernel's `src/main/programs/` sources and may
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

`sceneEditor/` is a separate CMake project. Its `SceneEditor.cpp` is a small main: it picks the graphics mode and hands
the screen to `FramebufferSceneEditor`, which is compiled from `../ApplicationCases/SceneEditorApplication` together
with the classes of its `x11_framebuffer` variant. The project also compiles the svgalib backend of the framebuffer
GUI (`cpp/svgalib`), `lib/vgaModes` and `lib/mouseCursor` from the kernel tree, and `cpp/base` through
`add_subdirectory` (with JPEG and PNG on). It links `-lvga -lvgagl` and readline. `SceneEditorProgram.*` is the
kernel's entry point and is not compiled here.

```sh
cd cpp/testsuite/VGAExamples/sceneEditor
cmake -S . -B build-cmake -DCMAKE_BUILD_TYPE=Release
cmake --build build-cmake -j"$(nproc)"      # build/sceneEditor and build/libvitral_base.so
```

`run.sh` does both steps and then runs `sudo build/sceneEditor "$@"`. The program changes to the folder of
`SceneEditorApplication` before it starts, so that the application finds `etc` at `../../../../etc`.

## Run

svgalib in `NoHelper` mode needs root (for `/dev/mem` and `iopl`) and a real virtual console. Run the programs on
the host's console (for example tty1). They do not work over SSH or inside X.

```sh
sudo ./vitral_vga [MODE]
sudo ./vitral_rasterizer [MODE]
sudo ./vitral_raytracer [SCENE.ray] [MODE]
sudo ./sceneEditor [english | spanish] [MODE]
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
