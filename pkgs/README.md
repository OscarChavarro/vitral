# Vitral Native Packages

This directory contains the native (C / C++) libraries used by the Vitral
Java toolkit through JNI, plus the third party code they are based on:

| Project            | Library                   | Used from                                   |
| ------------------ | ------------------------- | ------------------------------------------- |
| `NativeImageReader`| `libNativeImageReader.so` | `vsdk.toolkit.io.image.NativeImageReaderWrapper` |
| `LempelZivWelch`   | `libLZW.so`               | `vsdk.toolkit.processing.LzwWrapper`        |
| `SpharmonicKit27`  | `libspharmonickit.so`     | `vsdk.toolkit.processing.SpharmonicKitWrapper` |

## Building

All the projects are built with CMake (they used hand written Makefiles
before, which depended on `javah`, a tool removed in JDK 10):

```bash
cd pkgs
cmake -S . -B build
cmake --build build -j
```

Each project leaves its shared library inside its own `build` directory, and
can also be built alone:

```bash
cd pkgs/NativeImageReader
cmake -S . -B build/cmake
cmake --build build/cmake -j
```

Requirements: a C/C++ compiler, CMake 3.16+, a JDK (for `jni.h` and for the
`javac -h` header generation) and, for `NativeImageReader`, libpng
(`libpng-dev` on Debian / Ubuntu, `brew install libpng` or `port install
libpng` on MacOSX).

On MacOSX the projects also look for libpng under `/opt/homebrew`,
`/opt/local` and `/usr/local`, as CMake does not search the Homebrew and
MacPorts prefixes by default on Apple Silicon. If your libpng is somewhere
else, point CMake to it:

```bash
cmake -S . -B build -DCMAKE_PREFIX_PATH="$(brew --prefix libpng)"
```

If the JDK is not found, export `JAVA_HOME` (i.e.
`export JAVA_HOME=$(/usr/libexec/java_home -v 17)`) before configuring. Note
the libraries are named `libNativeImageReader.dylib`, `libLZW.dylib` and
`libspharmonickit.dylib` there, which is exactly what
`System.mapLibraryName` reports, so the Java side finds them unchanged. The
JVM and the libraries must be built for the same architecture: an arm64
library is not loadable from an x86_64 JVM running under Rosetta (that case
is now reported as a diagnostic instead of aborting the application).

## How Java Finds The Libraries

`vsdk.toolkit.common.NativeLibraryLoader` locates the libraries by itself,
so neither a system wide installation nor `-Djava.library.path` are needed.
It searches, in this order:

1. The file named by the `vsdk.nativeLibrary.<library>` system property.
2. The directories listed in the `VSDK_NATIVE_LIB_PATH` environment variable.
3. `pkgs/<project>/build` inside the Vitral source tree, located from the
   current directory and from the place the toolkit classes were loaded from.
4. `java.library.path` (`System.loadLibrary`).
5. The usual system library directories.

All these libraries are optional: when one is missing, the toolkit falls
back to its pure Java implementation (i.e. the AWT based png reader) instead
of failing.

Note that `NativeImageReader` reports an ABI version to its Java binding, so
obsolete copies left in system directories by old `make install` runs are
detected and ignored instead of breaking the application. If you have one
(i.e. `/usr/lib/libNativeImageReader.so`), you can safely delete it.

## JNI Header Generation

`NativeImageReader` generates `vsdk_toolkit_io_image_NativeImageReaderWrapper.h`
during the build, with `javac -h`, directly from the Java binding classes, so
the native signatures can not drift from the Java ones. Its Java binding
classes are written to compile stand alone (they do not extend any toolkit
class) precisely to make that generation cheap.
