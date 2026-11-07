# Vitral Kernel

Vitral Kernel is a small, preemptive SMP kernel for x86, written primarily in C++. The same kernel and programs run in x86_64 long mode or i386 protected mode. It runs parallel workloads, including Vitral's ray tracer, to compare their operating-system overhead with conventional operating systems.

All processes run in ring 0 in one address space. Programs are compiled into the image. System calls that can block use `int 0x80` with a C++ ABI, because only its frame lets the scheduler switch processes; the others (console, process ids, time) and the video functions are plain calls. There is no process isolation, executable loader, `fork`, users, permissions or signal delivery; a program fault can bring down the kernel. The x86_64 build uses an identity-mapped address space, with the frame buffer marked write-combining when the CPU has PAT; i386 runs without paging, so its frame buffer stays uncached.

## Build and run

The build requires CMake, LLVM (`clang`, `clang++`, `ld.lld`, `llvm-objcopy`), mtools and the Vitral base library at `../base`. QEMU is needed to run the images. GRUB and xorriso are also required for the GRUB image. On macOS:

```sh
brew install cmake qemu llvm lld mtools
brew install i686-elf-grub xorriso  # GRUB image only
```

```sh
scripts/run64.sh          # x86_64, one CPU per online host logical CPU
scripts/run64smp.sh       # x86_64 SMP, one CPU per online host logical CPU
scripts/run64smpSVGA.sh   # one CPU per online host logical CPU, Bochs VBE and more graphics memory
scripts/run32.sh          # i386, one CPU per online host logical CPU
scripts/rungrub.sh        # x86_64 via GRUB and Multiboot2 (ARCH=i386: the i386 kernel, in VGA text mode)
scripts/run32failon64.sh  # verify rejection of a CPU without long mode
scripts/run16.sh          # simulate rejection of a 286; pass 8086 for an 8086
```

These run scripts accept `-n` to build without starting QEMU. `CPU`, `QEMU`, `SMP` (where supported), `QEMU_RAM` and `JOBS` override their defaults. The BIOS scripts produce a disk image; `rungrub.sh` produces `build/vitral.iso`.

The scripts default to a bridged e1000 network connection, which may require `sudo`. Set `QEMU_BRIDGE_IF` to choose the host interface, `QEMU_NET=user` for QEMU NAT, or `QEMU_NET=none` to omit the NIC. On Linux, macvtap requires a wired interface and does not allow the host itself to reach the guest; a bridge requires QEMU bridge-helper configuration.

Useful CMake cache options include `BM_ARCH` (`x86_64` or `i386`), `BUILD_TESTS` (off by default), `VITRAL_BASE_DIR`, `VITRAL_SVGALIB_DIR`, `LOOP_ROOT_DIR`, `SVGALIB_KERNEL_MODES`, `VGA_PALETTE_DIR`, `SCENE_EDITOR_APPLICATION_SOURCE_DIR`, `SCENE_EDITOR_APPLICATION_DIR` and `VITRAL_RAYTRACER_DEFAULT_SCENE`. Configure with the supplied toolchain, for example:

```sh
cmake -S . -B build -DCMAKE_TOOLCHAIN_FILE=cmake/toolchain.cmake -DBUILD_TESTS=ON
cmake --build build --parallel
```

## Kernel and programs

The BIOS bootloader loads the kernel after checking the CPU and collecting the memory map. Both images can also boot through Multiboot2: the x86_64 one uses a GRUB framebuffer when available, and the i386 one asks GRUB for the EGA text console, so it starts in VGA text mode. For GRUB, `kernel32.bin` is loaded with `multiboot2` and `set gfxpayload=text` inside the `menuentry`. The kernel provides memory allocation, per-CPU round-robin scheduling, affinity, threads and futexes. CPU load is measured per core from cumulative busy and idle TSC cycles.

The VFS provides an in-memory root and `/dev`. An embedded FAT image is available at `/dev/loop0` and mounted at `/media/ramdisk`; its contents come from `loopKernelRootFileSystem/`, Vitral's `etc` data and the HTTP frontend. Drivers include VGA, Bochs VBE, PS/2 keyboard and mouse, USB (UHCI host controllers, EHCI controllers routed to their UHCI companions, hubs, and HID keyboards and mice in boot protocol, with hot plugging), COM1, RTC and network devices. The kernel log goes to the screen and to COM1 at 115200 baud, 8N1; COM1 is also `/dev/ttyS0`. The network stack provides Ethernet, ARP, IPv4, ICMP and server-side TCP.

At startup, `init` starts `serialShell` on COM1 when it exists: a command line for a terminal at the other end of a null modem cable, whose `mode` command lists the video modes and switches the screen to one (graphics modes show a test pattern on console 11), plus `console`, `ps`, `dmesg`, `uptime` and `reboot`. It then configures `eth0` as `192.168.1.199/24` when present, waits for its link, starts `httpd` if a network card is available, and runs `top`. Adjust the address in `src/main/os/kernel/InitScripts.cpp` for your network. `httpd` serves its frontend on port 80 and publishes system, CPU and log data over WebSockets. The shell runs registered programs; its `ls` lists program names, and `ps`, `exec` and `exit` are built in.

Included programs are `top`, `shell`, `vga`, `sceneEditor`, `vitral_rasterizer`, `vitral_raytracer`, `ifconfig`, `httpd`, `date`, `uptime`, `dmesg` and `reboot` (restarts the machine through the ACPI reset register, the keyboard controller, port 0xCF9 or a triple fault). The graphics programs use shared svgalib-compatible mode selection and can be built against the corresponding Linux APIs. The compatibility libraries provide the subset of libc, pthreads, svgalib and readline needed by the workloads, plus the C++ runtime support (`new`/`delete`, `std::exception`, `typeinfo`, `__cxa_*`, C library headers); there is no STL, containers and utilities come from the Vitral `java::` layer.

`sceneEditor [LANGUAGE] [MODE]` runs Vitral's Scene Editor (`FramebufferSceneEditor`, the application of `cpp/testsuite/ApplicationCases/SceneEditorApplication`) full screen on the framebuffer GUI of the base library and its svgalib backend. It is the same application as the `x11_framebuffer` variant of `runX11framebuffer.sh`: menus, tool bar, side panel, dialogs and viewports with hidden-line rendering and the parallel CPU raytracer, as well as importing and exporting scenes and geometries. `LANGUAGE` is a GUI definition of `etc/gui` (`english` by default, or `spanish`), and `MODE` is a graphics mode with 4, 8, 16 or 24 bits per pixel. Without `MODE`, it uses the first available mode of a preference list that starts with `G1024x768x64K` and puts direct colour ahead of palette modes, since the raytraced views need many colours. Use File → Quit to leave.

The application sources are compiled from their folder in the repository, and only the `main` of `src/main/programs/sceneEditor` is specific to the kernel. The application reads `../../../../etc` as in the repository, so it runs from `SCENE_EDITOR_APPLICATION_DIR`. That folder is `/media/ramdisk/cpp/testsuite/ApplicationCases/SceneEditorApplication` in the root image by default, so the relative path reaches `/media/ramdisk/etc`. C++ exceptions are not unwound: a `throw` ends the program, for example when a file cannot be read.

## Source layout

- `src/main/os/`: boot code, kernel, drivers, file systems, network stack and system calls.
- `src/main/lib/`: kernel utilities, compatibility libraries and shared graphics helpers.
- `src/main/programs/`: built-in programs and the HTTP frontend.
- `tests/`: optional test programs, included with `BUILD_TESTS=ON`.
- `scripts/`: image creation, QEMU launch and build utilities.

See [AGENTS.md](AGENTS.md) for architecture details and coding conventions.
