# Vitral C++ Port

This directory contains the C++ port of the Vitral graphics toolkit. It follows the same architectural principles as the Java implementation while using C++ and native libraries for performance-critical operations.

## Overview

The C++ port includes:

- **Core module** (`base/`): Linear algebra, geometry, media, and IO foundations
- **Rendering backends**: OpenGL 4.1 core with GLSL shaders (`opengl4/`),
  OpenGL 1.2 fixed function pipeline without shaders (`opengl1/`, a
  mechanical port of `opengl4/` without Phong / Cook-Torrance shading nor
  bump mapping), GLFW, GLUT, and others
- **Testsuite** (`testsuite/`): Examples and benchmarks
- **Native library integration**: Direct bindings for image processing (libpng,
  libjpeg), computer vision (OpenCV), and CAD/B-Rep interoperability
  (Open CASCADE Technology, OCCT)

## Build

From the `cpp/` directory:

```bash
mkdir -p build
cd build
cmake ..
cmake --build .
```

### Legacy toolchains (GNU make only)

The turboc port targets legacy compilers (gcc 4.1 on Ubuntu 7.04, gcc 2.7.2
on Debian 1.1 and, eventually, Turbo C++ 3.0 for DOS). On systems without
CMake, the base library and the offline examples are built with plain
Makefiles, from the `turboc/` directory:

```bash
make                         # base/build/libvitral_base.a and the examples
make WITH_POSIX_THREADS=1    # optional: WITH_JPEG=1, WITH_PNG=1, WITH_FREETYPE=1
make clean
```

The objects go to `buildmk/` folders; the Makefiles do not track header
dependencies, so run `make clean` after changing headers. Each example is
then run from its folder (i.e. `./build/RaytracingOfflineExample`).

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
- No member templates, no `template <>` specializations, no `typename`
  and no static data members in class templates.

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
- **CMake** 3.16 or higher

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
- **Platform compatibility**: Uses standard C++ with careful platform-specific handling
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
