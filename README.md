# Vitral

Vitral is a computer graphics testbed focused on experimentation and learning. It is designed to build small graphics applications quickly while switching between multiple algorithms on both CPU and GPU pipelines.

## What this project does

Vitral provides a compact but broad graphics toolkit with:

- Core math, geometry, scene, media, and processing foundations
- CPU-side rendering and shading (wireframe, hidden-line, rasterization, ray tracing, software shaders)
- GPU-side rendering adapters (OpenGL/JOGL backends)
- IO and persistence layers for geometry, images, XML, and scene resources
- A testsuite with offline examples, interactive apps, techniques demos, and benchmarks

This repository contains multiple implementations:

### Java Implementation

- `java/base`: dependency-light core toolkit
- `java/awt`: AWT/Swing integration layer
- `java/jogl2` and `java/jogl4`: JOGL rendering backends
- `java/testsuite`: runnable examples, API tests, and application cases

### C++ Port

- `cpp/base`: C++ core toolkit (see [C++ README](cpp/README.md) for details)
- `cpp/opengl4`: OpenGL 4.1 rendering backend via GLFW
- `cpp/glfw`: GLFW window management
- `cpp/glut`: GLUT/FreeGLUT backend
- `cpp/testsuite`: C++ examples and benchmarks

For building the C++ port and installing optional image format dependencies (JPEG, PNG), see the [C++ port documentation](cpp/README.md#requirements).

## Architecture and learning focus

Vitral uses a modular, organized design where model/domain code is separated from rendering technology-specific code. The rendering layer is decoupled from core entities, which keeps the codebase easier to understand, modify, and use for education.

A key property of the project is the "pure" base layer (`java/base`): it minimizes external dependencies and keeps core logic independent from specific rendering frameworks. This makes the toolkit suitable for:

- Learning graphics fundamentals with clear boundaries between concerns
- Swapping rendering implementations without rewriting the domain model
- Building new ports and supporting different technologies/platforms (for example Vulkan-oriented backends)

## Portability and ports

The Java code in this repository is a **port** of the toolkit architecture. The same high-level design used in `java/base` is intended to be replicated across other ports (such as C++, TypeScript, and others): a clean core model plus backend-specific rendering adapters.

## Turbo C++ 3.0 / DOS maintenance notes

The `turboc/` port is built on MS-DOS with Borland Turbo C++ 3.0. It is a
working legacy target. Keep these rules in mind when changing sources that
reach it:

- Transform every changed source and header with `turboc/scripts/map_to_83.py`
  before copying it to DOS. The guest uses mapped 8.3 paths, rewritten includes,
  and shortened class identifiers.
- Transferred sources and batch files must use **CRLF**. Verify the generated
  text and the mounted floppy image before executing a batch file.
- Do not use class temporaries in conditional (`?:`) expressions. TCC cannot
  reliably destroy them; use default construction followed by `if` assignment.
  This affects `String`, `ColorRgb`, `Vector3Dd`, and similar value types.
- Avoid `new T[count]()` and `for (int i = ...)`. Allocate normally, initialize
  elements explicitly, and declare loop indices before the loop.
- TCC rejects `delete` on const-qualified pointers. Keep a non-const local
  pointer when an owned `const T*` must be released.
- Its nested-class access rules are stricter. Under `__TURBOC__`, expose only
  required helper declarations and qualify calls to outer static members.
- With `VITRAL_WITHOUT_PTHREADS`, provide a synchronous path for code that
  otherwise creates `Future`, `FutureTask`, or `ExecutorService` objects.
- TCC response files need one option per line. Batch builds should print a
  compact `Compiling <source>` line, redirect successful output to a log, and
  print the log only after an error.
- DOS release batches use `-O1 -d -v-`: optimize for size, merge duplicate
  strings, and explicitly omit debug records. Debug the corresponding source
  on a modern platform instead.
- The generated `BASE.BAT` compiles the objects and `BASELINK.BAT` archives
  them into `BUILD\\BASE.LIB`. Run the latter separately when investigating a
  Turbo Librarian failure; it does not need to recompile the base library.

When a DOS error repeats, inspect the actual guest copy of the source before
changing the host version again.

### DOS runtime memory direction

For future DOS applications, prefer **XMS** as the backing store for code
overlays and large, pageable assets.  Keep a deliberately small conventional
memory core: application state needed at all times, the event loop, and the
active overlay buffer.  Turbo C++'s overlay manager can swap inactive code
segments through XMS; it does not make XMS directly executable.  Load file
I/O or rendering functionality only for the operation that needs it, and page
large images, meshes, and other assets rather than retaining them in the
conventional-memory model.

Splitting the base into thematic static libraries is still useful for build
and linker memory pressure, but static libraries are selected at link time.
It does not by itself make a DOS executable dynamically unloadable; that
requires overlay-aware module boundaries or an explicit runtime loader.

## Bibliographic traceability

The project maintains traceability to classic computer graphics literature used to guide algorithms and implementation decisions. The `doc/` folder includes architectural notes and a curated set of important references (including foundational papers) that support the toolkit's technical direction.

## Feature table

| Area | Main capabilities | Where to look |
|---|---|---|
| Core foundations | Linear algebra, color, data structures, symbolic algebra, utilities | `java/base/src/main/vsdk/toolkit/common` |
| Scene and geometry model | Cameras, lights, materials, backgrounds, geometric entities, scene containers | `java/base/src/main/vsdk/toolkit/environment` |
| CPU rendering | Wireframe, hidden line, 2D rasterizer, simple ray tracer, software shaders | `java/base/src/main/vsdk/toolkit/render` |
| Shader models (CPU/software) | Constant, flat, Gouraud-textured, Phong, Cook-Torrance, bump variants | `java/base/src/main/vsdk/toolkit/render/shaders` |
| Media and image data | RGB/RGBA images, palettes, z-buffers, pixel and color handling | `java/base/src/main/vsdk/toolkit/media` |
| Persistence and import/export | Image formats, geometry IO, XML importer, metadata persistence | `java/base/src/main/vsdk/toolkit/io` |
| Desktop integration | AWT/Swing rendering and GUI adapters | `java/awt/src/main/vsdk/toolkit` |
| GPU rendering adapters | JOGL2/JOGL4 renderers, fixtures, OpenGL resource helpers | `java/jogl2/src/main/vsdk/toolkit`, `java/jogl4/src/main/vsdk/toolkit` |
| Experiments and demos | Application cases, offline examples, benchmarks, API tests | `java/testsuite` |

## Build and run (Java)

From the `java/` directory:

```bash
./gradlew -q classes
```

To run a specific module/application, use its `run.sh` under `java/testsuite/...` or Gradle tasks from that module.

## Install the Java port

Prerequisites:

- Java 17
- Gradle (used to resolve and fetch dependencies)

From the repository java folder:

```bash
cd java
./scripts/clean_all.sh
gradle build
```

This will clean previous outputs, download required dependencies, and build all Java modules.

### Optional native libraries

Some toolkit operations have an optional native (C/C++) implementation,
contained in the CMake projects of the `pkgs` folder: a libpng based png
reader, LZW decompression and spherical harmonics. They are built with:

```bash
cd pkgs
cmake -S . -B build
cmake --build build -j
```

The Java side locates these libraries inside the source tree by itself, so
no installation nor `-Djava.library.path` is needed; when a library is not
built, the toolkit uses its pure Java implementation. See `pkgs/README.md`.

## Repository layout

- `java/`: main toolkit modules and testsuite
- `doc/`: architecture notes, references, and historical docs
- `etc/`: shaders, textures, materials, fonts, and sample assets
- `pkgs/`: optional native libraries used from Java through JNI (CMake projects)
