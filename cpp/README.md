# Vitral C++ Port

This directory contains the C++ port of the Vitral graphics toolkit. It follows the same architectural principles as the Java implementation while using C++ and native libraries for performance-critical operations.

## Overview

The C++ port includes:

- **Core module** (`base/`): linear algebra, scene model, the solid modeling
  kernel (boundary representation solids, booleans, CSG), computational
  geometry (triangulation, Weiler-Atherton clipping, BVH), the CPU renderers
  (raytracer, hidden line, hidden surface with shadows, rasterizer, software
  shaders), media and IO. It is written in C++11 without the containers,
  strings or streams of the C++ standard library: `base/src/main/java` holds
  a Java-like layer (`java::lang`, `java::util`, `java::io`,
  `java::util::concurrent`) that the toolkit uses instead, which keeps the
  code close to the Java port and lets `turboc/` derive from it. The math,
  geometry, material and IO wrapper classes are shared, file by file, with
  the rpkCpp project, from which radiosity and global illumination are being
  brought in (they are not in this port yet).
- **Rendering backends**: OpenGL 4.1 core with GLSL shaders (`opengl4/`),
  OpenGL 1.2 fixed function pipeline without shaders (`opengl1/`, a
  mechanical port of `opengl4/` without Phong / Cook-Torrance shading nor
  bump mapping), plain X11 drawing (`xlib/`) and SVGALIB (`svgalib/`)
- **Window systems and GUI toolkits**: GLFW (`glfw/`), GLUT (`glut/`),
  GTK 4 (`gtk4/`), Xt (`xt/`) with Motif (`xm/`) or Athena (`xaw/`)
- **Minimalistic kernel** (`x86_64_smp_kernel/`): a small preemptive SMP
  kernel for i386 and x86_64 that runs the raytracer and the scene editor
  over `base/` without an operating system (see its README)
- **Testsuite** (`testsuite/`): the scene editor (`ApplicationCases`),
  offline examples, OpenGL 4 and VGA examples, API tests and tools
- **Native library integration**: optional libpng, libjpeg and FreeType in
  the core; OpenCV and Open CASCADE Technology (OCCT) only in tools of the
  testsuite (the marker detector server and the boolean comparison tool)

## Build

From the `cpp/` directory:

```bash
mkdir -p build
cd build
cmake ..
cmake --build .
```

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

- **C++ compiler** with C++11 support (GCC, Clang, MSVC, or Apple Clang)
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

## Porting Notes

### Differences from Java Implementation

1. **Image I/O**: Uses native `libpng` and `libjpeg` instead of Java AWT or JNI bindings
2. **Memory management**: Explicit `new`/`delete` (no garbage collection)
3. **Namespaces**: Avoid introducing project-specific C++ namespaces; preserve only `java::` where needed for the Java-compatibility layer
4. **Build system**: CMake instead of Gradle
5. **Standard library**: the Java-like layer of `base/src/main/java` instead
   of the STL containers, strings and streams; from the standard library
   only the C functions (`<cmath>`, `<cstring>`, `<cstdio>`) are used

### Design Principles

- **Minimal dependencies**: Core (`base/`) avoids external libraries where possible
- **Gradual feature enablement**: Optional features (JPEG, PNG) compile conditionally
- **Platform compatibility**: Uses standard C++ with careful platform-specific handling
- **Educational value**: Code is structured for clarity and learning

## Related Documentation

- [Main README](../README.md)
- [Portability and test environment](../doc/portability.md)
- [Known bugs and technical debt](../doc/BUGS.txt)
- [Minimalistic kernel](x86_64_smp_kernel/README.md)
- [Legacy port](../turboc/README.md)

## Contributing

When porting features from Java or adding new capabilities:

1. Maintain architectural consistency with `java/base`
2. Follow the coding conventions established in existing C++ files
3. Use `new`/`delete` explicitly (no smart pointers per project style), and
   state in the header who owns each pointer a class keeps or returns
4. No `friend` declarations, no `goto`, no project namespaces
5. Preserve academic references and documentation comments
6. Test with both optional dependencies enabled and disabled
