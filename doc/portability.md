# Portability and distributed multi-platform testing

Vitral aims to be portable across very different platforms: CPU
architectures (from the 16-bit Intel 8086 to 64-bit x86_64, MIPS, Alpha,
SPARC and ARM), GPUs (from no GPU at all, through fixed-function OpenGL 1.x,
to OpenGL 4.x class hardware), operating systems (from MS-DOS and a
minimalistic Vitral kernel to classic Unix workstations, Linux and macOS)
and development stacks (from Borland Turbo C++ 3.0 and gcc 2.7.2 to modern
clang, Java and TypeScript toolchains). The same architecture (a pure core
model plus technology-specific rendering adapters) is kept in every port,
and each port keeps its sources free of platform forks: platform
differences are confined to a few, well-identified compatibility points.

Portability is only real when it is tested on the real platforms. For that
reason Vitral uses a distributed test environment: a set of physical
machines and virtual machines, each running one of the supported systems,
to which a port is copied, compiled and tested.

## Supported systems

| Status | Platform | CPU | Operating system | Ports | Toolchain | Graphics / GPU |
|---|---|---|---|---|---|---|
| ✅ | DOS, real mode | Intel 8086/8088 (16-bit) | MS-DOS | `turboc` (`TURBO_C_8086_REAL_MODE_ENABLED`) | Borland Turbo C++ 3.0 | none (VGA/BGI where available); 640 KB conventional memory, XMS as backing store |
| ⚠️ | DOS, 286 protected mode | Intel 80286 + 287 FPU | MS-DOS booting the minimal Vitral 286 kernel | `turboc` (`TURBO_C_286_ENABLED`) | Borland Turbo C++ 3.0 | none; 16 MB address space. The libraries build, but the raytracer image (about 1.27 MB) does not fit in the 1 MB of a TLINK image (see [Example: DOS nodes](#example-dos-nodes)) |
| ✅ | Vitral minimalistic kernel, BIOS boot (`run64.sh`) | x86_64 | `cpp/x86_64_smp_kernel` | `cpp` | clang/LLVM, CMake | VGA / Bochs VBE |
| ✅ | Vitral minimalistic kernel, SMP (`run64smp.sh`) | x86_64 SMP (Nehalem) | `cpp/x86_64_smp_kernel` | `cpp` | clang/LLVM, CMake | VGA / Bochs VBE |
| ✅ | Vitral minimalistic kernel, SMP + SVGA (`run64smpSVGA.sh`) | x86_64 SMP (Nehalem) | `cpp/x86_64_smp_kernel` | `cpp` | clang/LLVM, CMake | Bochs VBE, up to 1920x1080x24 |
| ✅ | Vitral minimalistic kernel, i386 (`run32.sh`) | i386 and later, SMP | `cpp/x86_64_smp_kernel` | `cpp` | clang/LLVM, CMake | VGA / Bochs VBE |
| ✅ | Vitral minimalistic kernel, GRUB 2 Multiboot2 i386 (`kernel32.bin`) | i386 and later, SMP | `cpp/x86_64_smp_kernel` | `cpp` | clang/LLVM, CMake | VGA text console, Bochs VBE |
| ⚠️ | Vitral minimalistic kernel, GRUB 2 Multiboot2 x86_64 (`kernel64.bin`) | x86_64 SMP | `cpp/x86_64_smp_kernel` | `cpp` | clang/LLVM, CMake | GRUB framebuffer, with the BIOS 8x16 font taken in real mode at boot: text verified in QEMU (`-vga vmware`) and on `boobies` (GTX 1060, 640x480 framebuffer; no mode changes yet, a Pascal mode setting driver is in progress). Network (`tg3`), USB and the PL2303 serial line verified on `boobies`. With QEMU's standard VGA (Bochs VBE driver) the screen stays black |
| ➖ | Vitral minimalistic kernel, 16-bit CPUs (`run16.sh`) | 8086/8088/80186, 80286 | `cpp/x86_64_smp_kernel` | none | — | Not implemented: the boot loader detects the CPU and prints a `CPU ERROR`; use the DOS 8086 port |
| ✅ | Linux i386 (legacy) | i386 and later | Debian 1.1 "Buzz" up to Ubuntu 7.04 and similar distributions (verified on Debian 1.1, see [Example: Debian 1.1 node](#example-debian-11-node)) | `turboc`, `cpp` (where the compiler allows it) | gcc 2.7.2.1 to gcc 4.1.2, Makefiles / CMake 2.4 | X11R6, Xt, Athena, Motif/LessTif, Mesa 2.2 to 6.5, GLUT, SVGALIB |
|  | OSF/1 | MIPS | OSF/1 | `turboc` | vendor C++ compiler / gcc | X11, Motif |
|  | Tru64 Unix | DEC Alpha (64-bit) | Tru64 Unix (Digital Unix) | `turboc`, `cpp` | Compaq C++ / gcc | X11, Motif, OpenGL |
|  | IRIX | MIPS (SGI) | IRIX | `turboc`, `cpp` | MIPSpro / gcc | X11, Motif, OpenGL (SGI hardware) |
|  | Solaris | SPARC | Solaris | `turboc`, `cpp` | Sun Studio / gcc | X11, Motif, OpenGL |
| ✅ | Linux x86_64 (modern) | x86_64 | Ubuntu 16.04 up to Ubuntu 26.04 (`cpp` verified on Ubuntu 22.04 and Ubuntu 26.04, see [Example: Ubuntu 22.04 node](#example-ubuntu-2204-node) and [Example: boobies node](#example-boobies-node-ubuntu-2604)) | `cpp`, `java`, `typescript`, `turboc` | gcc / clang, CMake, Java 17+, Node.js | X11, GLFW, GLUT, GTK4, OpenGL 4.x, JOGL, WebGL |
|  | macOS | Apple M1 (arm64) | macOS | `cpp`, `java`, `typescript` | Apple clang, CMake, Java 17+, Node.js | GLFW, OpenGL 4.1, JOGL, WebGL |

✅ verified on the test environment (see [Testing the minimalistic kernel](#testing-the-minimalistic-kernel)),
⚠️ runs with known problems, ➖ not implemented by design; rows without a mark are not verified yet.

The details of each port, its language subset and its build are in the
README of the port (`turboc/README.md`, `cpp/README.md`,
`cpp/x86_64_smp_kernel/README.md`, `typescript/`, `java/`).

## Test nodes: `secrets.json` and `secrets-example.json`

The machines of the test environment are described in the
`etc/portMachines/secrets.json` file. It holds real data (host names, IP addresses,
users and passwords), so it is listed in `.gitignore` and **must never be
committed**, copied into another file of the repository, pasted in a
commit message or issue, or sent to any service other than the test node
it belongs to.

`secrets.json` lets AI agents (and people) know, for each node:

- what platform it is, so the right port and build procedure are chosen;
- how to send the project to it (for example `ftp`, `scp`, `tar | ssh`, an
  ISO image or a shared folder of a virtual machine);
- how to open a remote session on it (for example `ssh`, `telnet`, VNC, or
  the VirtualBox console);
- which credentials to use, when credentials are needed;
- where the project lives on the node, and any folder naming convention.

`etc/portMachines/secrets-example.json`, which is committed, shows the expected format with
fake addresses and without real passwords. To set up a new test
environment, copy it and fill it in:

```sh
cp etc/portMachines/secrets-example.json etc/portMachines/secrets.json
```

The file is a JSON object with a `nodes` list. Each node has these fields
(fields that do not apply may be omitted or left empty):

| Field | Meaning |
|---|---|
| `id` | Short unique name of the node, used in reports. |
| `enabled` | `false` to skip the node without deleting its entry. |
| `platform` | One of the rows of the supported systems table, e.g. `dos-8086`, `dos-286`, `linux-i386`, `linux-x86_64`, `osf1-mips`, `tru64-alpha`, `irix-mips`, `solaris-sparc`, `macos-arm64`, `vitral-kernel-x86_64`. |
| `os`, `cpu` | Free text description of the system (version, CPU model). |
| `ports` | Ports of Vitral to test there (`turboc`, `cpp`, `java`, `typescript`). |
| `parallelTests` | `true` when the node runs the parallel tests: several CPUs and threads, so the raytracers run with `--parallel` (`runAll.sh --parallel`). `false` (single CPU, or no usable threads, i.e. DOS, Debian 1.1 or Tru64 on es40) runs them serially. The variants of the `vitral-kernel-qemu` node have their own flag: the scripts that boot with `-smp` (`run64.sh`, `run64smp.sh`, `run64smpSVGA.sh`, `run32.sh`) support it, and their `vitral_raytracer` always uses one thread per CPU. |
| `host` | Network name or IP address. Empty for a virtual machine driven only from its console. |
| `virtualMachine` | For virtual machines: hypervisor (`virtualbox`, `qemu`, ...), VM name, and how to start it. |
| `transfer` | How to copy the project: `method` (`scp`, `ftp`, `tar-ssh`, `iso`, `curl-ftp`, ...), port and notes. |
| `session` | How to log in: `method` (`ssh`, `telnet`, `vnc`, `vboxmanage-console`, ...), port and notes. |
| `user`, `password` | Credentials, only when required. Leave `password` empty when key based access is used. |
| `rootPassword` | Only when the node requires `su` for some setup step. |
| `workdir` | Folder of the project on the node. |
| `toolchain` | Compiler and build tools available on the node. |
| `notes` | Conventions and known quirks (slow links, clock skew, memory limits, ...). |

## Deployment to the test environment (instructions for AI agents)

When the user asks to deploy to the test environment (or to "test on the
platforms", "run the distributed tests", or similar), an agent must:

1. **Read `etc/portMachines/secrets.json`.** If it does not exist, stop and ask the user to
   create it from `secrets-example.json`. Never print passwords in the
   conversation, logs or files; use them only to connect.
2. **Select the nodes.** Keep the `enabled` nodes whose `platform` and
   `ports` match what the user asked for (all of them when nothing more
   specific was requested).
3. **Check which nodes are available.** Ping the host, probe the port of
   its `session` method, or, for a virtual machine, check its state
   (`VBoxManage showvminfo <name>`) and start it headless if the user
   allows it. Report unavailable nodes and continue with the rest.
4. **Replicate the port.** Send only the sub-project being tested (for
   example `turboc/` plus the needed files of `etc/`), using the node's
   `transfer` method and its `workdir`. Prefer one compressed archive over
   many files on slow links. Apply the transformations the platform needs
   before sending (for DOS: 8.3 name mapping and CRLF batch files).
5. **Build and test.** Open the node's `session` and run the port's build
   and test suite: a clean or incremental compilation, followed by the
   programs whose outputs are compared with the reference outputs of a
   modern platform (see the offline examples of each testsuite). When the
   node has `parallelTests: true`, run the raytracers with `--parallel`
   (`./runAll.sh --parallel`); otherwise run them serially. Run long
   builds in the background and poll them rather than holding a session
   open.
6. **Report.** Give one line per node: available or not, build result,
   tests passed or failed, and the relevant part of the error output.
   Do not fix code silently on the node: fixes go to the repository on the
   development host, and are then deployed again.

Agents do not commit, and do not change the system configuration of a
node (packages, boot files, users) without asking the user first.

### Example: DOS nodes

The DOS node is a VirtualBox virtual machine (`DOS`) running MS-DOS with
Windows for Workgroups 3.11, with Borland Turbo C++ 3.0 installed in
`C:\TURBOC` (`C:\TURBOC\BIN` on the `PATH`). There is no password: the
session is the VirtualBox console, reached through VNC (VRDE, port 5904 in
`secrets.json`) or directly with `VBoxManage controlvm DOS
keyboardputstring` / `keyboardputscancode 1c 9c` (Enter) and `VBoxManage
controlvm DOS screenshotpng`; typing in chunks of about 12 characters is
reliable. The mTCP tools of `C:\NETDOS` (`FTP.EXE`, a `TELNETD` at
192.168.1.250 when it is running) are not used for source trees.

Rules for agents on DOS machines:

- **Everything stays inside `C:\VITRAL`.** Never leave files or folders
  outside it: scratch tests, logs and probe batches go under `C:\VITRAL`
  too. `C:\TURBOC`, `C:\NETDOS` (djtar, mTCP) and `C:\DOSVIEW` are used
  read only, and `AUTOEXEC.BAT` / `CONFIG.SYS` are not changed without
  asking the user.
- **Never copy the whole repository.** DOS machines have little disk
  space: only `etc/` and `turboc/` go, mapped to 8.3 names, and never
  `etc/portMachines`.

Transfer (see "Validated DOS VM transfer procedure" in `turboc/README.md`):

1. `sh turboc/scripts/stageDosFull.sh` maps the port and `etc` to 8.3 names
   and prints a staging folder with `VITRAL.ISO`, which holds one
   `VITRAL.TGZ`.
2. `VBoxManage storageattach DOS --storagectl IDE --port 1 --device 0
   --type dvddrive --medium <staging>/VITRAL.ISO` changes the CD (`D:`)
   without rebooting.
3. In DOS: `CD C:\VITRAL`, `DELTREE /Y TURBOC`, `DELTREE /Y ETC`, then
   `C:\NETDOS\DJTAR -x D:\VITRAL.TGZ`. Compare the number of files with
   `FILES.LST` (`dir /s /b /a-d TURBOC | find /c /v ""`).

The same machine tests both DOS ports:

- **8086 real mode**: the menu-only BGI Scene Editor. From
  `C:\VITRAL\TURBOC`: `BASE\BASE86`, `BASE\BASELK86`, `BGI\BGI`,
  `BGI\BGILINK`, then `TESTSUIT\BGIEXAM\SCENEDIT\COMPILE` and `RUN`. The
  test passes when the menu bar opens.
- **80286 protected mode**: the raytracer. `BASE\BASE`, `BASE\BASELINK`,
  `DOS\DOS`, `DOS\DOSLINK` (which also build `CRT286.LIB` and
  `LOADER.EXE`), then `TESTSUIT\OFFLEXAM\RAYOFFEX\COMPILE`. Boot without
  HIMEM, EMM386 or other extended memory clients (F5 at the DOS startup)
  and run `RUN1.BAT`, which renders the Appel 3 scene to `APPEL3.PPM`. The
  test passes when `C:\DOSVIEW\DOSVIEW` shows that image.

Results of 2026-11-18:

- 8086: passes. `SCENEDIT.EXE` shows its menu bar (640x480, 256 colours)
  and F10 opens the File menu.
- 80286: blocked. The base, `CRT286.LIB`, `LOADER.EXE` and `DOS.LIB` build,
  but TLINK stops at "Relocation item exceeds 1Meg DOS limit": the payload
  needs about 1.27 MB (1.15 MB of code, most of it the PolyhedralBoundedSolid
  booleans that the Appel fixture uses, 64 KB of near data, 16 KB of far
  data and the 32 KB stack), and a TLINK image, whose segments are 16-bit
  paragraph numbers, ends at 1 MB even though the loader copies it to
  extended memory. All the linked modules are used. The planned solution
  is to overlay code segments (TLINK `/o`, compiled with `-Y`) and to have
  `LOADER.EXE` copy each overlaid segment to extended memory with its own
  selector, replacing the overlay stubs by direct far jumps.

`turboc/scripts/dosVm.sh` drives the console for agents (`type`, `wait`,
`full`, `patch`, and `get`, which copies guest files such as link maps to
the host through a scratch floppy image); see "Driving the DOS VM from the
host (agents)" in `turboc/README.md`.

### Example: Debian 1.1 node

The `debian-1.1-i386` node is a VirtualBox virtual machine (`Debian1.1`,
127 MB of RAM) with Debian 1.1 "Buzz": Linux 2.0, gcc 2.7.2.1, make 3.75,
libc5, X11R6 with xdm on `:0`, LessTif's `mwm`, and Mesa 3.0. There is no
CMake, so the `turboc` port is built with its Makefiles, and no usable
pthreads (`VITRAL_WITHOUT_PTHREADS`).

- **Transfer**: FTP (paths relative to the user's home). Send one
  `tar --format=gnu` archive (`curl -T`) and extract it with
  `gzip -dc file.tgz | tar xf -`. GNU tar 1.11 mishandles members whose
  path is exactly 100 characters long: check for them before packing. The
  guest clock may be ahead of the host's: `touch` the extracted files so
  that make rebuilds them.
- **Session**: telnet. Run long jobs with `nohup ... &` and poll their logs
  (FTP downloads of the log avoid opening new sessions). Graphical programs
  use `DISPLAY=:0`; the screen is visible through the VirtualBox VRDE
  server (VNC on port 5901) or `VBoxManage controlvm Debian1.1
  screenshotpng`.
- **Build**: `cd turboc && sh scripts/compileWithMake.sh`.
- **Raytracer test**: `sh runAll.sh` in
  `testsuite/OfflineExamples/RaytracingOfflineExample`. It passes when
  every MIT scene of `etc/geometry/mitscenes` gives its `.ppm` image.
- **Scene editor test**: `DISPLAY=:0 ./runXmOpenGL1.sh` in
  `testsuite/ApplicationCases/SceneEditorApplication`, then an MCP client
  on `vmdebian:1234` clears the scene, adds a sphere, a cone, a cylinder
  and a point light, and exports the workspace in OpenGL (Phong),
  wireframe, APPEL hidden lines and CPU raytracing modes
  (`workspace.export_jpg`), plus a raytraced image (`render.raytrace_png`).
  Without pthreads the MCP service runs in the Xt event loop.
- **Window manager**: the editor asks for position 0,0, and `mwm`
  ignores a program position of 0,0 by default (`usePPosition: nonzero`)
  and then waits for the user to place the window. The user's
  `~/.Xresources` sets `Mwm*usePPosition: on`.

Results of 2026-11-18:

- Build: passes (0 errors), including the Motif and Athena OpenGL 1
  variants of the scene editor.
- Raytracer: passes, the 30 MIT scenes rendered (about 14 minutes, single
  threaded).
- Scene editor: passes. The MCP session built 3 bodies and a light and
  exported the OpenGL, wireframe, APPEL and raytracing views, and the
  320x240 raytraced PNG.

### Example: Ubuntu 22.04 node

The `ubuntu22.04` node is a VirtualBox virtual machine (`LinuxUbuntu22.04`,
4 CPUs, 16 GB of RAM, bridged) with Ubuntu 22.04.3 LTS: gcc 11.4.0,
CMake 3.22.1, Mesa 23.2.1 (llvmpipe, OpenGL 4.5 core profile), Motif 2.3.8,
GLFW 3.3.6, libjpeg-turbo and libpng. It tests the `cpp` port.

- **Transfer**: the node keeps its own clone of the repository (origin is
  GitHub). Update it with `git fetch origin` and
  `git reset --hard origin/main`, after checking `git status`. Changes not
  pushed from the development host are not there.
- **Session**: ssh with a key. The node boots to `graphical.target` with
  `xdm` on `:0` (vt7). A test client authorizes itself with xdm's cookie
  (`/var/lib/xdm/authdir/authfiles/`), so nobody needs to log in. The
  screen is visible through the VirtualBox VRDE server (VNC on port 5902) or
  `VBoxManage controlvm LinuxUbuntu22.04 screenshotpng`.
- **Display**: the kernel boots with `nomodeset`, so Xorg can only use the
  `vesa` driver (`xserver-xorg-video-vesa`); a hung Xorg of this driver
  needed a reset of the virtual machine once. The editor needs the
  Helvetica fonts of `xfonts-75dpi` and `xfonts-100dpi` (without them it
  stops with `Could not create the UTF-8 menu font set`).
- **Build and unit tests**: `./scripts/compile.sh` in `cpp`.
- **Raytracer test**: `./runAll.sh --parallel` in
  `cpp/testsuite/OfflineExamples/RaytracingOfflineExample`.
- **Scene editor test**: `DISPLAY=:0 ./runXmOpenGL4.sh -s` in
  `cpp/testsuite/ApplicationCases/SceneEditorApplication`, then the same
  MCP session as on Debian 1.1 on `vm2204:1234`. OpenGL 4 viewports have no
  hidden line mode: for APPEL, the session switches to the Xlib technology
  (`render.set_technology`, `technology: xlib`), exports, and switches back
  to `opengl4`.

Results of 2026-11-18:

- Build: passes; ctest 631 of 631 tests passed (47 s).
- Raytracer: passes, the 30 MIT scenes rendered (4 min 15 s with 4
  threads).
- Scene editor (`xm_opengl4`): passes. The MCP session built 3 bodies and a
  light and exported the OpenGL (Phong), wireframe, APPEL (Xlib) and
  raytracing views, and the 320x240 raytraced PNG.

### Example: boobies node (Ubuntu 26.04)

The `boobies` node is a real server: two Intel Xeon X5650 sockets (12
cores, 24 threads) and an NVIDIA GeForce GTX 1060 3GB, with Ubuntu 26.04.1
LTS, g++ 15.2.0, CMake 4.2.3 and the NVIDIA driver 580.178.04 (OpenGL 4.6
core profile, direct rendering). It runs the same `cpp` protocol as the
Ubuntu 22.04 node, with `parallelTests: true`.

- **Transfer**: its own clone of the repository; `git status`, then
  `git pull --ff-only`.
- **Session**: ssh with a key. The graphical tests use the X session of
  jedilink on `:0` (Xorg 21.1.22 started by xdm, vt7); `~/.Xauthority` is
  enough.
- **Stale CMake cache**: after the upgrade of libfreetype-dev to 2.14.2, its
  headers are only in `/usr/include/freetype2`, and an old `cpp/build`
  failed at the generate step (`Imported target "Freetype::Freetype"
  includes non-existent path`). Remove `cpp/build` (or run
  `cmake -U FREETYPE_INCLUDE_DIR_freetype2`) after such an upgrade.
- **Minimalistic kernel**: `kernel64.bin` is installed in `/boot/vitral`,
  with the "Vitral Kernel (x86_64)" entry in `/etc/grub.d/40_custom` (BIOS
  GRUB 2.14, `i386-pc`; see `cpp/x86_64_smp_kernel/README.md`). The menu
  default stays `GRUB_DEFAULT=0`; `sudo grub-reboot "Vitral Kernel (x86_64)"`
  boots it only on the next restart. Backups of the GRUB files kept in
  `/etc/grub.d` must not be executable, or `update-grub` runs them too.
- **Kernel serial line**: boobies has no COM port. Its Prolific PL2303 USB
  to RS-232 adapter (`/dev/ttyUSB0` under Linux, with a getty) is wired to
  the `/dev/ttyS0` of the test host. Under the minimalistic kernel the
  adapter carries the kernel log and a `shell` (`/dev/ttyUSB0`), at 115200
  baud 8N1, so a test cycle needs no one at the machine: install and
  `grub-reboot` over ssh, reboot, read the log on the host's `ttyS0`, then
  type `reboot` there to come back to Ubuntu.
- **Kernel network**: the onboard Broadcom BCM5764M (`tg3`, PCI 1:0.0)
  works under the minimalistic kernel at 1000 Mb/s full duplex: ping, the
  `httpd` page, `/api/system` and the `system`, `cpu` and `log` WebSocket
  feeds at http://192.168.1.199/ (verified 2026-11-18).

Results of 2026-11-18 (commit `c8c514e7`):

- Build: passes; ctest 631 of 631 tests passed (2 min 18 s).
- Raytracer: passes, the 30 MIT scenes rendered with 24 threads in 40 s.
- Scene editor (`xm_opengl4`, OpenGL 4.1 context on the GTX 1060): passes.
  The MCP session built 3 bodies and a light and exported the OpenGL
  (Phong), wireframe, APPEL (Xlib) and raytracing views, and the 320x240
  raytraced PNG. `runXmOpenGL4.sh` then built without `-j` (about 5 minutes
  before the MCP service started); it now builds with one job per CPU.

## Testing the minimalistic kernel

The minimalistic kernel (`cpp/x86_64_smp_kernel`) is built from the `cpp`
port and booted in QEMU. Each run script follows a different path (CPU,
architecture, boot loader, display adapter), but they are all tested the
same way. The `vitral-kernel-qemu` node of `secrets.json` lists them.

The scripts must be run with `sudo`: their default network is bridged on
the host's interface, which needs root. Build first as the normal user
with `-n`, so that the build directories are not owned by root; the `sudo`
run then only boots QEMU. The procedure was tested from Linux Ubuntu 22.04
(x86_64 host) and macOS Golden Gate 27.2.1 (Apple M1 host).

```sh
cd cpp
./scripts/compile.sh                         # cpp port: build + ctest
cd x86_64_smp_kernel
scripts/run64.sh -n                          # build the image only
sudo env DISPLAY=:1 XAUTHORITY=/run/user/$(id -u)/gdm/Xauthority \
    scripts/run64.sh                         # Linux; on macOS just run the script
```

When the guest is up (`top` on console 0), the test set is:

1. `vitral_raytracer`: renders the default scene (`balls.ray`) in the
   highest available mode, with one thread per CPU, and shows the time in
   its HUD. ESC quits and prints
   `vitral_raytracer: ... rendered at WxH in S seconds (parallel).`
2. `sceneEditor`: opens the Scene Editor full screen, with its menus, tool
   bar, viewports, camera gizmo and side panel. File > Quit leaves it.

The commands can be typed on the QEMU window, or sent by an agent through
COM1: `QEMU_SERIAL=tcp:127.0.0.1:4555,server=on,wait=off` exposes the
serial shell, and `printf 'vitral_raytracer\r' | socat - TCP:127.0.0.1:4555`
starts a program (ESC is `printf '\033'`). Programs started from COM1 still
draw on the screen, which can be captured from the X display (for example
`import -window root` cropped to the QEMU window).

| Script | Expected result |
|---|---|
| `run64.sh`, `run64smp.sh`, `run64smpSVGA.sh`, `run32.sh` | Boots; `vitral_raytracer` and `sceneEditor` run. |
| `run32failon64.sh` | `CPU ERROR: PAE not supported: cannot build the paging tables for long mode.` |
| `run16.sh 8086` | `CPU ERROR: 8086/8088/80186 detected. 32-bit protected mode needs an 80386 or later.` |
| `run16.sh 286` | `CPU ERROR: 80286 detected. 32-bit protected mode needs an 80386 or later.` |

Notes:

- **Memory.** The scripts give the guest 512 MB of RAM and one CPU per host logical CPU. With the former
  default of 64 MB, hosts with many CPUs (72 on the Ubuntu 22.04 test host) ended the raytracer threads with
  `std::bad_alloc`; `QEMU_RAM` and `SMP` still override both values.
- **GRUB 2.** The same images boot from an installed GRUB 2 (see "Installing on a GRUB 2 system" in
  `cpp/x86_64_smp_kernel/README.md`). Tested in QEMU, `kernel32.bin` behaves as `run32.sh`; `kernel64.bin` runs
  the programs but draws no text, because the kernel copies its font from VGA plane 2, which GRUB's graphics
  framebuffer leaves empty.
- **16-bit CPUs.** The kernel has no 16-bit implementation. On an 8086,
  8088, 80186 or 80286 the boot loader detects the CPU before leaving real
  mode and prints the `CPU ERROR` above. For those machines use the DOS
  8086 real-mode port of `turboc` (or its 286 protected-mode port on an
  80286). QEMU cannot emulate them, so `run16.sh` builds an image that
  simulates the detection.
