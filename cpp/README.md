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
- **Native library integration**: Direct bindings for image processing (libpng, libjpeg)

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
5. **Standard library**: C++11 STL instead of Java Collections

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
