# Vitral C++ Port (turboc legacy port)

This directory contains the turboc port of the Vitral graphics toolkit: a
legacy derivative of the C++ port (`cpp/`) that keeps the same architecture
and the same toolkit code, written in a conservative C++ subset that builds
with compilers from 1992 to 2007, including Borland Turbo C++ 3.0 for DOS.

## Overview

The turboc port includes:

- **Core module** (`base/`): Linear algebra, geometry, media, and IO
  foundations, over the Java-like layer of `base/src/main/java` instead of
  the C++ standard library
- **Rendering backend**: OpenGL 1.2 fixed function pipeline without shaders
  (`opengl1/`) and GLUT (`glut/`); unlike the cpp port there is no OpenGL
  4.1 (`opengl4/`), GLFW nor GTK 4
- **Xt GUI layer**: `xt/`, with the Motif (`xm/`) and Athena (`xaw/`)
  widget sets
- **Testsuite** (`testsuite/`): offline examples (no window system), OpenGL
  1.2 / GLUT examples, VGA examples (SVGALIB), API tests and the Xt scene
  editor
- **Native library integration**: optional libpng, libjpeg and FreeType

## Gradual derivation from the cpp port

The port was derived from the C++ port in successive steps, each one
targeting an older toolchain while keeping the previous ones working:

1. **Copy of the cpp port (C++11).** The starting point was a copy of
   `cpp/`, which uses C++11 and the C++ standard library.
2. **gcc 4.1.2 (Ubuntu 7.04, 2007).** The C++11 and newer extensions were
   removed (no `auto`, lambdas, `nullptr`, range `for`, smart pointers,
   ...), the OpenGL 4.1 / GLFW / GTK backends were replaced by OpenGL 1.2,
   GLUT and Xt (Motif and Athena), and the code was moved to a pre-ISO C++
   style.
3. **gcc 2.7.2.1 (Debian 1.1, 1996).** The C++ standard library containers,
   strings, streams and algorithms were replaced by the Java-like classes of
   `base/src/main/java`; the code avoids member templates, template
   specializations and the other constructs gcc 2.7.2 does not support or
   crashes on, and builds with plain Makefiles (no CMake), libc 5, X11R6.1,
   LessTif 0.76 and Mesa 2.2 / 3.0, with or without POSIX threads.
4. **Borland Turbo C++ 3.0 (DOS, 1992).** The language was reduced to the
   subset of Turbo C++ 3.0 (no `bool`, no `mutable`, no static member
   initializers, 32 bit `long long`, 32 character identifiers, ...) through
   the macros of `vsdk/toolkit/common/VSDK.h`, with `#ifdef __TURBOC__`
   alternatives for the few C library and POSIX services DOS lacks.

The rules that came out of each step are listed below, in
[Legacy toolchains](#legacy-toolchains-gnu-make-only).

### Platforms and stack

| Platform | Compiler | Build | Window system / graphics |
|---|---|---|---|
| Ubuntu 7.04 (i686) | g++ 4.1.2 | CMake 2.4.6 or Makefiles | X11, Xt, Motif 2.2.3, Athena, Mesa 6.5.2, freeglut 2.4.0, SVGALIB |
| Debian 1.1 (i386) | gcc 2.7.2.1 | Makefiles | X11R6.1, Xt, LessTif 0.76, Athena, Mesa 2.2 / 3.0 |
| DOS | Borland Turbo C++ 3.0 | Turbo C++ | none: `base` and the offline examples only |

DOS has none of SVGALIB, X11, Xt, Xaw, Motif nor OpenGL, so only the core
library and the programs that need no window system are built there.
Despite that, the code base is the same for every platform: there is no
DOS specific fork, and the same sources build and run unchanged on the
three toolchains, with the platform differences confined to a few
`#ifdef`s in `base`.

Container replacements must preserve the semantics of the corresponding
`cpp/` containers, including pointer identity. Turbo C++ compares only the
offsets of far pointers with its built-in `<`; pointers in different
segments can therefore compare equivalent despite being unequal.
`naturalOrderLess` includes the selector when ordering Turbo pointers, and
TreeSet, TreeMap and compound half-edge keys use it. Without this total
order, BREP maps can merge unrelated nodes and topology summaries can lose
vertices depending on heap placement. `dos/src/test/vsdk/kernel/286/vitralLibC/ordtest.cpp` checks keys
with identical offsets in different segments, as well as ordinary value
keys. Keep boolean operations and their validation aligned with `cpp/`;
do not weaken topology checks to accommodate a container-port defect.

### 32 bit CPUs

The turboc port is meant to run on 32 bit CPUs (i386 and later), unlike the
cpp port, which mainly targets 64 bit CPUs. The code does not assume 64 bit
pointers nor a 64 bit `long`, and on Turbo C++ `long long` is a 32 bit
`long` (see `VSDK_LL` / `VSDK_HAS_LONG_LONG` below).

### No threads nor `Future` on DOS

DOS has no SMP nor any equivalent (and supporting it would make no sense
there), so the threads / `Future` model of the cpp port is not supported
on DOS. Without POSIX threads (`VITRAL_WITHOUT_PTHREADS`, the default of
the Makefiles) `java/lang/PosixThreads.h` supplies a single threaded subset
(no-op mutexes and conditions, and a `pthread_create` that fails), and
`ExecutorService::execute` runs each task right away in the calling thread.

## Build

From the `turboc/` directory (systems with CMake, i.e. Ubuntu 7.04):

```bash
mkdir -p build
cd build
cmake ..
cmake --build .
```

### DOS build and transfer (Turbo C++ 3.0)

The DOS backends are separate: `TURBO_C_8086_REAL_MODE_ENABLED` selects
real-mode/XMS storage; `TURBO_C_286_ENABLED` selects the ring-0 runtime and
`Protected286ModeRgbImage`, with direct selector-based RGB storage. Do not
define both. The default `DOS.BAT`/`DOSLINK.BAT` build the 286 backend without
XMS objects; passing `8086` builds the real-mode XMS library instead (all
BASE/application objects must use the matching mode macro).

The 286 image reserves a physical RAM window using the BIOS-reported end,
capped at 16 MiB, excluding the HMA and its loaded executable. It requires
a bare boot without HIMEM, EMM386, DPMI, SMARTDRV or other extended-memory
clients. Use DOS's F5 startup bypass when testing the executable already
copied to C:. Build and transfer first, since bypassing CONFIG.SYS also
disables the CD-ROM driver. See [dos/src/main/vsdk/kernel/286/README.md](dos/src/main/vsdk/kernel/286/README.md).

The protected build currently requires a hardware 287-compatible FPU.
Its CRT initializes C++ globals and calls the portable raytracer entry;
native 287 math replaces Borland's real-mode interrupt shortcuts. `RUN1.BAT`
loads the staged Appel 3 scene via a relative path and renders 320x240 to
`APPEL3.PPM` in the current directory, then waits for a key and resets.

The DOS build uses an 8.3 copy of `etc/` and `turboc/` under
`C:\VITRAL`.  DOS cannot consume the source tree directly: its directory
depth and long file names exceed the limits of the DOS filesystem and tools.
`scripts/map_to_83.py` produces the mapped layout and updates source includes
to use it.

### Validated DOS VM transfer procedure

Copying the mapped layout into the DOS VM needs a deliberately reproducible
procedure.  Agents maintaining this port should install `djtar` and its
DJGPP runtime requirements (`GO32.EXE` and `CWSDPMI.EXE`) in `C:\NETDOS`,
and use a mounted ISO image as the normal transfer medium.

1. Produce a temporary DOS staging tree with exactly two top-level
   directories, `ETC` and `TURBOC`.  It must contain only the 8.3-mapped
   source/data files selected by `scripts/mappings_83.csv`, plus the files
   generated by `scripts/map_to_83.py`.  In particular, retain
   `TURBOC/BASE/SRC/MAIN/JAVA/IO/NAMES83.H`: it is generated rather than
   listed as a source file, and `File.cpp` cannot compile for Turbo C++
   without its tables.

   **Never copy a source file directly from the normal checkout into this
   staging tree.**  Mapping changes not only file and directory names, but
   also `#include` spellings (for example, `vsdk/toolkit/...` becomes its
   8.3 `VSDK/...` counterpart).  A long-name source file placed in an
   otherwise 8.3 archive can therefore look valid by pathname yet fail to
   compile in DOS with missing-header errors.  Re-run the mapping process on
   changed sources, or apply the change to their already-normalized staging
   counterpart, before making the archive.
2. Validate the staging tree before packaging: every path component must be
   an 8.3 DOS name, and the build batches (`BASE.BAT`, `BASELINK.BAT`,
   `DOS.BAT`, `DOSLINK.BAT`, and the Rasterizer `COMPILE.BAT` / `RUN*.BAT`)
   must be present.  Validate normalized include directives as well as path
   components.  Do not copy the unmapped X11, OpenGL, or host build subtrees
   merely because they exist in the working checkout.  In particular, the
   `VITRAL.TGZ` put on the ISO must be built from this staging tree only,
   never from the ordinary repository checkout.
3. On the host, archive the two directories and create an ISO containing
   only that archive.  A gzip-compressed TAR is appropriate because `djtar`
   reads it directly:

   ```bash
   tar -C /tmp/vitraltc -czf /tmp/vitral-iso/VITRAL.TGZ ETC TURBOC
   xorriso -as mkisofs -iso-level 1 -V VITRAL83 \
     -o /path/to/VITRAL-83.iso /tmp/vitral-iso
   ```

4. Replace the VM's existing virtual CD medium in place.  The DOS VM uses
   the IDE optical device at port 1, device 0, so this does not require a
   guest reboot:

   ```bash
   VBoxManage storageattach DOS --storagectl IDE --port 1 --device 0 \
     --type dvddrive --medium /path/to/VITRAL-83.iso
   ```

5. In DOS, start from `C:\VITRAL`.  When replacing a complete tree, remove
   the previous extracted copies first, otherwise `djtar` may refuse to
   overwrite files or prompt once per file.  Then extract the mounted CD:

   ```dos
   CD C:\VITRAL
   DELTREE ETC
   DELTREE TURBOC
   C:\NETDOS\DJTAR -x D:\VITRAL.TGZ
   ```

`djtar` recreates the directory hierarchy and performs the filename
conversions needed by DOS.  It automatically reads gzip-compressed TAR
archives; its extraction switch is `-x`, so do not use GNU tar's combined
`xvfz` syntax.  A successful extraction prints the archived entries and
returns to the `C:\VITRAL` prompt.

Avoid mTCP FTP for a complete source-tree copy whenever an ISO can be used.
FTP requires creating every destination directory and transferring every
file separately, so a dropped command, stale manifest, case mismatch, or an
interrupted session can silently leave an incomplete tree.  DOS floppy disks
are also unsuitable for this job: their capacity is tiny, they require many
manual swaps, and they make it easy to lose a file or a directory level.  A
floppy is acceptable only as a small bootstrap medium for tools such as
`djtar`; it is not the source-transfer mechanism.  An ISO avoids both
limitations, preserves one auditable archive, and can be inserted or
replaced as CD media without rebooting the VM.

After the mapped tree has been copied, open a DOS prompt and build in this
order:

```dos
cd C:\VITRAL\TURBOC\BASE
BASE
BASELINK

cd ..\DOS
DOS
DOSLINK

cd ..\TESTSUIT\OFFLEXAM\RAS2DEXA
COMPILE
RUN
RUN1
RUN2
RUN3
```

`BASE.BAT` compiles the core objects and `BASELINK.BAT` splits them into the
partial `BASE*.LIB` libraries.  `DOS.BAT` and `DOSLINK.BAT` respectively
compile and archive the DOS/XMS services as `DOS.LIB`.  `COMPILE.BAT` then
builds `RASTERIZ.EXE`, the non-GUI Rasterizer2D offline example.  `RUN.BAT`
runs the default line test; `RUN1.BAT`, `RUN2.BAT`, and `RUN3.BAT` run the
line, polygon, and smooth-polygon cases and write `OUTPUT1.PPM`,
`OUTPUT2.PPM`, and `OUTPUT3.PPM`.

`RASTERIZ.EXE` accepts optional dimensions after the mode and output name:
`RASTERIZ line OUTPUT1.PPM 640 480`.  Its scene is authored in a logical
320×240 coordinate system and is scaled independently in each axis to the
requested output image, so a different resolution fills the new canvas rather
than leaving the scene in its upper-left quadrant.  The default remains
640×480.

### Legacy toolchains (GNU make only)

The turboc port targets legacy compilers (gcc 4.1 on Ubuntu 7.04, gcc 2.7.2
on Debian 1.1 and Turbo C++ 3.0 for DOS, see
[Gradual derivation from the cpp port](#gradual-derivation-from-the-cpp-port)). On systems without
CMake, the base library and the offline examples (the testsuite programs
that need no window system) are built with plain Makefiles:

```bash
./scripts/compileWithMake.sh                       # library and examples
./scripts/compileWithMake.sh WITH_POSIX_THREADS=1  # also WITH_JPEG=1, WITH_PNG=1, WITH_FREETYPE=1
./scripts/compileWithMake.sh clean
```

The library goes to `base/build/libvitral_base.a` and each example to the
`build/` folder of its project, so `./run.sh` works from there (without
CMake, `run.sh` rebuilds through `compileWithMake.sh`). The objects go to
`buildmk/` folders; the Makefiles do not track header dependencies, so clean
after changing headers. With gcc 2.x the Makefiles compile with
`-O0 -fhandle-exceptions -frtti -fno-inline` and force
`vsdk/toolkit/common/Gcc2TypeInfo.h` into every file: gcc 2.7.2 only
catches exceptions by a base class with RTTI (which needs `<typeinfo>`),
crashes when inlining, and its optimized exception tables refer to deleted
labels.

When OpenGL is found (`<GL/gl.h>` in `/usr/include`, `/usr/X11R6/include`
or `$GL_DIR/include`), `compileWithMake.sh` also builds the opengl1, xt and
xaw libraries and the Athena variant of SceneEditorApplication, run with
`./runXawOpenGL1.sh` from its folder; with Motif (`<Xm/Xm.h>`, i.e. LessTif
0.76 on Debian 1.1) the xm library and the Motif variant
(`./runXmOpenGL1.sh`); and with GLUT (`<GL/glut.h>`, i.e. the one of Mesa
3.0) the glut library and the programs of `testsuite/OpenGL1Examples` and
`testsuite/_APITests`. Only `testsuite/VGAExamples` needs CMake.

LessTif 0.76 implements Motif 1.2: `XmWidgetSupport.h` maps the Motif 2
render tables, toggle states and popup values the port uses to their Motif
1.2 counterparts (font lists, Booleans) when `XmVersion < 2000`. On Debian 1.1 OpenGL is Mesa, built
from its sources in `/usr/local/src` (i.e. `/usr/local/src/Mesa-3.0`, found
by `scripts/findLegacyGlDir.sh`) or given as `GL_DIR` (i.e. Mesa 2.2, the
`mesa2` and `mesa-dev` packages of Debian 1.3, unpacked in a folder):

```bash
./scripts/compileWithMake.sh               # Mesa in /usr/local/src/Mesa-*
GL_DIR=$HOME/mesa/usr ./scripts/compileWithMake.sh
cd testsuite/ApplicationCases/SceneEditorApplication && ./runXawOpenGL1.sh
```

The Makefiles do not track headers: after changing the OpenGL
implementation, `./scripts/compileWithMake.sh clean` first.

Mesa 2.2 offers OpenGL 1.1 and GLU 1.1 (Mesa 3.0, OpenGL 1.2 and GLU 1.1):
`OpenGL1Loader` accepts OpenGL 1.1 (the toolkit then avoids its few OpenGL
1.2 enumerants), `OpenGL1GluTessellator.h` implements the GLU 1.2 tessellator over the GLU
1.1 one (without `GLU_TESS_COMBINE`), and the scene editor chooses its
visual with `glXChooseVisual` when GLX 1.3 is missing. Mesa 3.0 restores
texture bindings in `glPopAttrib` without their reference counts, so the
toolkit binds no texture while the texture attributes are saved or
restored.

X11R6.1 (XFree86 3.2) has no usable Unicode: no `Xutf8*` functions, no ISO
10646 fonts, and its `en_US.utf` locale does not decode UTF-8. The GUI texts
are UTF-8, so `XtTextEncoding` selects a UTF-8 locale when the system has
one and ISO 8859-1 otherwise, converting the texts given to X (labels,
titles, text fields, `XlibUtf8Text.h` drawing) to Latin-1. Its Xlib keeps
its own locale (`X_LOCALE`): `legacy.mk` detects it (`_Xsetlocale` in
libX11) and defines `VITRAL_X_LOCALE`. The menus use Helvetica, or the
`fixed` fonts when it is missing (Debian 1.1 installs only the misc fonts).

Without `WITH_POSIX_THREADS=1` the Makefiles define `VITRAL_WITHOUT_PTHREADS`
for the library and the examples: `java/lang/PosixThreads.h` then replaces
pthreads with a single threaded subset (no-op mutexes and conditions, and a
`pthread_create` that fails), for systems without a usable pthreads library
such as Debian 1.1.

To keep the code compatible with those compilers, the port follows these
rules:

- No C++ standard library containers, algorithms, strings, streams or
  exceptions: the Java-like classes of `base/src/main/java` replace them
  (`String`, `StringBuilder`, `ArrayList` with `sort(Comparator)`,
  `TreeMap`, `TreeSet`, `ArrayDeque`, `HashMap`, `Collections<T>`,
  `Files::readString`, and the `Throwable` / `Exception` /
  `RuntimeException` / `IllegalArgumentException`... hierarchy).
- C library headers in their C form (`<stdio.h>`, not `<cstdio>`).
- Conventional casts and runtime type descriptions supplied by
  `java/lang/Class`.
- The language subset of Turbo C++ 3.0, through the macros of
  `vsdk/toolkit/common/VSDK.h` (included directly by every file that uses
  them):
  - No `bool`, `true` and `false`: `BOOLEAN` (an `int`), `TRUE` and
    `FALSE`. A `BOOLEAN` does not overload an `int` (i.e.
    `StringBuilder::appendBoolean`). Building with
    `EXTRA_CXXFLAGS=-DVSDK_CHECK_BOOLEAN` (gcc) makes `BOOLEAN` a class that
    only takes `bool` values, so the compiler reports any other value stored
    in one (an `int` keeps i.e. `flags & MASK` where a `bool` kept 1).
  - No initializers for the static members in a class: integral constants
    are enumerators (`enum { MAX_STEPS = 16 };`, in a 16 bit `int` on
    Turbo C++), other constants are defined in a `.cpp`.
  - No `mutable` (const methods cast `this`), no declarations in
    conditions (`if ( T* x = ... )`), and a variable declared in a `for`
    lives until the end of the enclosing block, so it is not declared
    again there (declare it before the first loop).
  - `long long` is a 32 bit `long` on Turbo C++: its literals are written
    `VSDK_LL(...)` / `VSDK_ULL(...)`, and overloads for both `long` and
    `long long` only declare the `long long` one with `VSDK_HAS_LONG_LONG`.
  - `explicit` and `__attribute__` are macros without effect on Turbo C++,
    and `snprintf` / `vsnprintf` are supplied there (without size checks).
  - Identifiers, including macros and include guards, are distinct within
    their first 32 characters.
  - `NAN`, `copysign`, `strcasecmp`, `gettimeofday`, `nanosleep` and
    `<unistd.h>` only behind `#ifdef __TURBOC__` alternatives
    (`Cpp98Support::nan`, `String::equalsIgnoreCase`, `ftime`, `delay`).
- No member templates, no `template <>` specializations, no `typename`
  and no static data members in class templates.
- `#include "java/lang/PosixThreads.h"` instead of `<pthread.h>`.
- No member functions of class templates that store the result of
  `new T[]` and then test it (gcc 2.7.2 crashes on them with
  `-fhandle-exceptions`): allocate through a function template, as
  `ArrayList::init` does. No `return new ...` inside a `try` block either;
  the Makefiles also compile with `-fno-inline` on gcc 2.x, as its inlining
  of functions with exception cleanups crashes. No conditional operator
  with `String` (or other class) temporaries as operands inside a larger
  expression, i.e. `f((c ? a.toString() : String("x")).c_str())` or
  `throw E(c ? String("x") : text)`: use `if` / `else` and a local. In
  long functions with exceptions, prefer `list[i]` (a reference) to
  `list.get(i)` (a `String` copy) to keep the temporaries few.
- Constructors, assignment and destructor of the container templates
  defined in the class body (calling out of class members): gcc 2.7.2 does
  not instantiate out of class members that are only used by the copy
  constructors and assignments it synthesizes.
- Template arguments spelled out in base classes, not through a typedef
  (gcc 2.7.2 takes `Comparator<IndexList>` as a type other than
  `Comparator<ArrayList<int> >`).
- Complete element types for the containers: gcc 2.7.2 instantiates every
  member of a class template where the class is named, even through a
  pointer, so a forward declaration of the element type is not enough, and
  a type cannot hold containers of itself (hold pointers instead).
- No identifier `PI` without an `#undef PI` first (libc 5 defines it as a
  macro in `<math.h>`), no `getaddrinfo` (use `gethostbyname`), no
  `uint16_t` and similar `<stdint.h>` types, no C99 `INFINITY` / `NAN`
  (use `HUGE_VAL`).
- `NULL` may be `((void *)0)`: cast it when its type matters, as in
  `cond ? new A() : (A *)NULL` or as a template argument.
- X11R6 headers do not include their prerequisites: `<X11/Intrinsic.h>`
  before `<X11/StringDefs.h>`, `<X11/Shell.h>` and the Xaw / Xmu headers,
  `<X11/Xlib.h>` before `<X11/Xutil.h>`.
- Classes nested in nested classes named from their full path
  (`XtDialogHost::Form::Button`), and no `T x(U(), ...)` declarations
  (gcc 2.7.2 reads them as function declarations: name the `U` first).

### Build Options

The following CMake options are available:

```bash
cmake -DWITH_JPEG=ON -DWITH_PNG=ON -DVITRAL_BUILD_TESTS=ON ..
```

- `WITH_JPEG` (ON/OFF): Enable JPEG image format support
- `WITH_PNG` (ON/OFF): Enable PNG image format support
- `VITRAL_BUILD_TESTS` (ON/OFF): Build test suites

## Requirements

The C++ port requires the following dependencies:

### Core Requirements

- **C++ compiler** compatible with the port's legacy language subset
- **CMake** 2.4.6 or higher (Ubuntu 7.04), or GNU make alone (Debian 1.1)

### Optional: Image Format Support

To build with JPEG and PNG image support, install the corresponding development libraries:

#### Linux (Debian/Ubuntu)

**PNG Support:**

```bash
sudo apt-get update
sudo apt-get install libpng-dev
```

**JPEG Support:**

```bash
sudo apt-get update
sudo apt-get install libjpeg-dev
```

**Both (recommended):**

```bash
sudo apt-get update
sudo apt-get install libpng-dev libjpeg-dev
```

#### macOS (using Homebrew)

**PNG Support:**

```bash
brew install libpng
```

**JPEG Support:**

```bash
brew install libjpeg
```

**Both (recommended):**

```bash
brew install libpng libjpeg
```

#### Verification

After installation, verify the libraries are available:

**Linux:**

```bash
pkg-config --cflags --libs libpng
pkg-config --cflags --libs libjpeg
```

**macOS:**

```bash
ls /usr/local/opt/libpng/include
ls /usr/local/opt/libjpeg/include
```

### Optional: OpenGL Rendering

To build with OpenGL support:

#### Linux (Debian/Ubuntu)

```bash
sudo apt-get install libglfw3-dev libglu1-mesa-dev freeglut3-dev
```

#### macOS

```bash
brew install glfw3
```

### Optional: GTK 4 frontend

The native GTK 4 frontend (`vitral_gtk4`) and the GTK variant of
`SceneEditorApplication` use GTK development headers and `pkg-config`.
On Ubuntu 22.04, install the available GTK 4.6 development package with:

```bash
sudo apt-get update
sudo apt-get install libgtk-4-dev pkg-config
```

The current GTK target supports GTK 4.6 or newer.  Newer GTK APIs must remain
behind version checks so the Ubuntu 22.04 baseline (GTK 4.6.9) keeps working.

### Optional: OpenCV and OCCT

Some testsuite tools use native libraries outside the core toolkit:

- **OpenCV** is used by `TangibleInterfaceMarkersDetectorServer` for camera
  capture, calibration, image processing, and preview.  It is optional; CMake
  skips that tool when OpenCV is absent.
- **Open CASCADE Technology (OCCT)** is used by the optional
  `booleanSetOperator` API test for CAD/B-Rep boolean-operation comparison.
  It is not a core dependency.

For Ubuntu/Debian systems, OpenCV can be installed with:

```bash
sudo apt-get install libopencv-dev
```

Ubuntu packages OCCT by component; the following development packages provide
the usual foundation, modeling and data-exchange modules:

```bash
sudo apt-get install libocct-foundation-dev libocct-modeling-data-dev \
  libocct-modeling-algorithms-dev libocct-data-exchange-dev
```

The OCCT comparison target also accepts a self-built OCCT installation through
`-DOCCT_ROOT=/path/to/occt` (or the `OCCT_ROOT` environment variable), which
is the recommended route when its CMake package configuration is needed.

### Optional: Xt GUI with Motif or Athena (Linux only)

The `xt` module (`vitral_xt`) is the Xt layer of the toolkit, the C++
counterpart of the Java `awt` module: conversion of X events to vitral ones
(`XtSystem`), Xlib image renderers (`render/xlib`), the interfaces of the
widget sets over the Xt Intrinsics (`XtWidgetSet`, `XtPanelWidgets`,
`XtGuiRenderer`) and the Xt generic editor. It depends only on libXt.

The widgets come from one of two sibling modules, which implement those
interfaces and never use each other:

- `xm` (`vitral_xm`): Motif, the default.
- `xaw` (`vitral_xaw`): Athena.

The widget set is chosen when building, with the `VITRAL_XT_WIDGET_SET`
CMake option (`xm` or `xaw`): each executable links only one of them, since
Motif and Athena must not share a process. Only the chosen module is built.
They are built on Linux only, and need:

```bash
sudo apt-get install libxt-dev libmotif-dev   # Motif (default)
sudo apt-get install libxt-dev libxaw7-dev    # Athena
cmake -DVITRAL_XT_WIDGET_SET=xaw ..           # to build with Athena
```

The `xt_layers` test (`scripts/checkXtLayers.py`) checks that the Xt layer
uses neither widget set and that they do not use each other.

## Building with Image Format Support

Once you have installed the required libraries, build with support enabled:

```bash
mkdir build
cd build
cmake -DWITH_JPEG=ON -DWITH_PNG=ON ..
cmake --build .
```

### Without Optional Dependencies

If you want to build the core library without image format support, simply use:

```bash
mkdir build
cd build
cmake ..
cmake --build .
```

The build will succeed but JPEG and PNG image loading will display an error message at runtime suggesting which option to recompile with.

## Runtime Behavior

### Missing Library Warning

If you attempt to load a JPEG or PNG image without having compiled that support, the program will print a message like:

```
ERROR: ImagePersistence: JPEG support not compiled in.
       Recompile with -DWITH_JPEG=ON to enable JPEG support.
```

or

```
ERROR: ImagePersistence: PNG support not compiled in.
       Recompile with -DWITH_PNG=ON to enable PNG support.
```

This allows you to defer building optional dependencies until needed.

## Legacy C++98 Build (turboc port)

This is a legacy C++ port of the `cpp/` tree, aimed to build with old
older toolchains. The `base`, `opengl1`, `glut`,
`xt`, `xaw` and `xm` libraries are known to build on Ubuntu 7.04 (i686) with
CMake 2.4.6, g++ 4.1.2, Mesa 6.5.2, freeglut 2.4.0, libXt 1.0.5, libXaw7
1.0.3 and Motif 2.2.3:

```bash
scripts/compile.sh     # builds base, opengl1, glut, xt, xaw and xm, in that order,
                       # and the testsuite projects of the port (OpenGL1Examples,
                       # _APITests, SceneEditorApplication), ready for ./run.sh
scripts/clean.sh       # removes every <module>/buildtc and <module>/build
```

`compile.sh` forwards its arguments to each `cmake` call (e.g.
`scripts/compile.sh -DWITH_JPEG=ON`). Intermediate files are written to
`<module>/buildtc` and the libraries to `<module>/build/libvitral_*.so`;
the other modules link against `base/build/libvitral_base.so`, and `xaw`
(Athena) and `xm` (Motif) against `xt/build/libvitral_xt.so` too (an
application links one of them). There are
no tests in this port. FreeType support is disabled by default
(`-DWITH_FREETYPE=ON` to enable it).

`opengl1` only uses what OpenGL 1.2 supports directly: OpenGL 1.0/1.1
functions (immediate mode, vertex arrays, texture objects), OpenGL 1.2
enumerants (`GL_CLAMP_TO_EDGE`, `GL_BGRA`, `GL_SEPARATE_SPECULAR_COLOR`,
`GL_RESCALE_NORMAL`) and GLU 1.2 (`gluBuild2DMipmaps`, tessellator). No
extensions, no function loader, no 3D textures and no compressed textures:
DXT images are decoded in the CPU and uploaded as 2D RGBA textures. Solid
textures stay in `base`, evaluated in the CPU by the software renderers.

### Implementation notes

The port keeps source code in a conservative language subset. Collections,
callbacks, exceptions and runtime type information use the Java-like layer in
`base/src/main/java`; platform services use C and POSIX facilities.

### Xt layer (`xt`, `xaw`, `xm`)

The `String` and `Object` types of Xt collide with the vitral classes: they
are renamed `XtString` and
  `XtObject` (as `Widget` already was `XtIntrinsicWidget`) with `#define`s
  around every include of Xt, Athena and Motif headers (see
  `XtIntrinsics.h`). The modules are compiled with `XTSTRINGDEFINES` and
  `XMSTRINGDEFINES`, so the resource names (`XtNwidth`, `XmNlabelString`,
  ...) are string literals instead of casts to `String`.
- CMake 2.4 `FindX11` does not look for libXt, libXaw and libXmu: they are
  found with `find_library`. The libraries still link with
  `-Wl,--no-undefined`.

### OpenGL 1.2 / GLUT examples (`testsuite/OpenGL1Examples`)

The `OpenGL4Examples` of the `cpp/` tree (GLFW and OpenGL 4.1), migrated to
GLUT and the OpenGL 1.2 fixed function pipeline of `opengl1`:

```bash
testsuite/OpenGL1Examples/CameraExample/run.sh
testsuite/OpenGL1Examples/ImageExample/run.sh
testsuite/OpenGL1Examples/MeshExample/run.sh            # etc/geometry/cow.obj by default
testsuite/OpenGL1Examples/PolygonClippingExample/run.sh [--offline] [--fixture N]
```

Each `run.sh` builds the examples (in `testsuite/OpenGL1Examples/buildtc`,
building the libraries first if missing, see `scripts/runTestsuiteProgram.sh`)
and runs one of them from its folder. The resources are read from `etc/` of
the VITRAL repository (`../../../../etc` from each example).

Changes from the OpenGL 4 versions:
- GLFW -> GLUT: callbacks for display, reshape, keys (pressed and released,
  with `glutKeyboardUpFunc`), mouse and motion; the buttons down during a
  drag and the wheel (buttons 3 and 4) are tracked by `GlutSystem`. Classic
  GLUT never returns from `glutMainLoop`, so the examples release their
  resources and call `exit` when closed (ESC).
- Repaints are event driven (`glutPostRedisplay`). The animation and
  tangible interface threads of `MeshExample` only raise a flag, polled by a
  GLUT timer, since GLUT is not thread safe.
- GLSL programs, vertex array objects and buffer objects -> fixed function
  state (`OpenGL1RendererConfigurationStateSelector`) and client side vertex
  arrays (`MeshExample` renderer, polygon renderers of
  `PolygonClippingExample`).
- Labels (input gizmo boxes) are rasterized by
  `GlutSystem::calculateLabelImage` with the X11 core fonts
  (`GlutLabelImageProvider`).
- `PolygonClippingExample --offline` draws once in the back buffer of a
  normal window (GLUT has no hidden windows) and writes `output.png`.
- Windows are limited to the size of the screen (i.e. 800x600 displays).

### Scene editor (`testsuite/ApplicationCases/SceneEditorApplication`)

Unlike the cpp port, only its Xt + OpenGL 1.2 variants exist: `xm_opengl1`
(Motif, default) and `xaw_opengl1` (Athena); no OpenGL 4.1 nor GTK.

```bash
testsuite/ApplicationCases/SceneEditorApplication/runXmOpenGL1.sh
testsuite/ApplicationCases/SceneEditorApplication/runXawOpenGL1.sh
testsuite/ApplicationCases/SceneEditorApplication/compileAllVariants.sh
```

See its `README.md` for build and usage details.

### OpenGL 1.2 API tests (`testsuite/_APITests`)

```bash
testsuite/_APITests/_OpenGL1HelloWorld/run.sh              # GLUT window
testsuite/_APITests/_OpenGL1PbufferExample/run.sh          # OSMesa, writes output.ppm
testsuite/_APITests/_OpenGL1PbufferExample/run.sh --glut   # GLUT window + glReadPixels
```

`_OpenGL1HelloWorld` draws a line with the constant state of
`OpenGL1RendererConfigurationStateSelector` (the counterpart of the constant
GLSL program). `_OpenGL1PbufferExample` renders offscreen: GLUT has no
hidden windows nor pbuffers, so two ways were compared on Ubuntu 7.04 (Mesa
6.5.2), both giving the same image:
- Mesa offscreen rendering (OSMesa, the default when `GL/osmesa.h` and
  `libOSMesa` are found): Mesa renders into a buffer of the program. No X11
  display nor window is needed and every pixel is defined. `libOSMesa` is a
  whole OpenGL implementation, so this variant links it instead of `libGL`
  (only with `vitral_base`, not with `opengl1`/`glut`).
- Color buffer of a GLUT window (`--glut`): the image is drawn in the back
  buffer of a normal window and read with `glReadPixels`. It needs a display
  and shows a window; with hardware OpenGL, the pixels of covered or
  offscreen parts of the window are undefined (pixel ownership test).

A GLX pbuffer (GLX 1.3) also works there, but is slower and still needs a
display.

## Porting Notes

### Differences from Java Implementation

1. **Image I/O**: Uses native `libpng` and `libjpeg` instead of Java AWT or JNI bindings
2. **Memory management**: Explicit `new`/`delete` (no garbage collection)
4. **Build system**: CMake instead of Gradle
5. **Collections**: Java-like collections are used throughout the port

### Design Principles

- **Minimal dependencies**: Core (`base/`) avoids external libraries where possible
- **Gradual feature enablement**: Optional features (JPEG, PNG) compile conditionally
- **Platform compatibility**: One code base for Ubuntu 7.04, Debian 1.1 and DOS, in a C++ subset common to g++ 4.1.2, gcc 2.7.2.1 and Turbo C++ 3.0, with platform-specific handling confined to a few `#ifdef`s
- **Educational value**: Code is structured for clarity and learning

## Related Documentation

- [Main README](../README.md)
- [C++ Implementation Summary](base/src/main/vsdk/toolkit/io/IMPLEMENTATION_SUMMARY.md)
- [Java Port](../java/README.md)

## Contributing

When porting features from Java or adding new capabilities:

1. Maintain architectural consistency with `java/base`
2. Follow the coding conventions established in existing C++ files
3. Use `new`/`delete` explicitly (no smart pointers per project style)
4. Preserve academic references and documentation comments
5. Test with both optional dependencies enabled and disabled
