# Vitral

Vitral is a computer graphics toolkit and testbed focused on experimentation
and learning. It is designed to build small graphics applications quickly
while switching between several algorithms for the same problem (visibility,
shading, solid modeling), on both CPU and GPU pipelines, and to keep the same
code base alive on platforms from a 16-bit DOS machine to a modern
workstation or a web browser.

## What this project does

The toolkit core (`base` in every port) contains, with no window system and
no GPU needed:

- **Foundations**: linear algebra, color, data structures, symbolic algebra,
  numerical analysis (polynomial solvers, look-up tables).
- **Scene model**: cameras, lights, materials, backgrounds, bodies with
  rigid or affine placement, scene containers.
- **Solid modeling kernel**: boundary representation solids
  (`PolyhedralBoundedSolid`, a half-edge structure built with Euler
  operators), boolean set operations and slicing over them, CSG trees
  evaluated by ray crossing membership, and analytic or implicit volumes
  (box, sphere, cone, tube, torus, quadrics, polynomial surfaces, blobs,
  height fields, voxels, half spaces), most of them exportable to a
  boundary representation.
- **Computational geometry**: polygon triangulation, the general 2D polygon
  clipper of Weiler and Atherton, bounding volume hierarchies (binned SAH)
  for triangle meshes and scene instances.
- **Visibility and rendering on the CPU**: wireframe, hidden line removal
  (Appel's quantitative invisibility), hidden surface removal by polygon area
  sorting (Weiler and Atherton, with shadows from several lights and screen
  subdivision), a 2D rasterizer, a Whitted style raytracer (serial and
  parallel by tiles), software shaders (constant, flat, Gouraud, Phong,
  Cook-Torrance, bump mapping), dithering.
- **Media and IO**: RGB / RGBA / indexed images, palettes, z-buffers, image
  formats, mesh and scene readers and writers (in Java: OBJ, PLY, 3DS, ASE,
  VRML, VTK, GTS, MD2, STL and part of STEP; a subset in the other ports).
- **Interaction model**: viewports and viewport sets, gizmos, key and mouse
  events and the controllers that interpret them, independent of any GUI
  toolkit.

Rendering technologies (OpenGL, WebGL, X11, a frame buffer) and GUI toolkits
(AWT / Swing, Xt with Motif or Athena, GTK 4, HTML) live in separate
adapter modules over that core.

Global illumination (Galerkin radiosity, stochastic raytracing,
bidirectional path tracing, photon maps, tone mapping) is **not part of
this repository yet**: it is being brought in from the rpkCpp project (a
modernization of RenderPark), whose math, geometry and material classes are
already shared, file by file, with `cpp/base`.

## Implementations (ports)

The same architecture and, as far as each language allows, the same classes
and method names are kept in four ports. Java is the reference for names
(`doc/attributesGettersAndSettersNames.md`).

| Port | Core | Rendering and GUI adapters | Applications |
|---|---|---|---|
| `java/` | `java/base` (558 source files) | `awt` (AWT / Swing), `jogl2`, `jogl4` (OpenGL through JOGL), `android`, `jme` | `java/testsuite`: scene editor, offline examples, techniques, benchmarks, tools |
| `cpp/` | `cpp/base` (C++11, no STL containers: a Java-like layer in `base/src/main/java`) | `opengl4`, `opengl1`, `glfw`, `glut`, `gtk4`, `xlib`, `xt` / `xm` / `xaw`, `svgalib` | `cpp/testsuite`: scene editor (Xt + OpenGL, Xlib, frame buffer), offline and OpenGL examples |
| `cpp/x86_64_smp_kernel/` | uses `cpp/base` | its own VGA / VBE video and drivers | a small preemptive SMP kernel for i386 and x86_64 that runs the raytracer and the scene editor without an operating system |
| `turboc/` | `turboc/base` (pre-ISO C++ subset of `cpp/base`) | `opengl1`, `glut`, `xt` / `xm` / `xaw`, `bgi`, `dos` | scene editor (Xt), offline examples, BGI examples for DOS |
| `typescript/` | `@vitral/base` (platform neutral), `@vitral/fs` (Node files) | `@vitral/webgl` | Angular web container with the scene editor and examples; offline examples for Node |

### Feature status by port

The table lists what is present in the production code of each core (not
what is planned). ✅ present, ◐ partial, — absent.

| Capability | java | cpp | turboc | typescript |
|---|---|---|---|---|
| Linear algebra, color, numerical analysis | ✅ | ✅ | ✅ | ✅ |
| Scene model, CSG trees | ✅ | ✅ | ✅ | ✅ |
| Boundary representation solids and booleans | ✅ | ✅ | ✅ | ✅ |
| Polygon triangulation and clipping, BVH | ✅ | ✅ | ✅ | ✅ |
| Raytracer (serial) | ✅ | ✅ | ✅ | ✅ |
| Raytracer (parallel) | ✅ threads | ✅ POSIX threads | ◐ only where threads exist (not on DOS) | ✅ Web Workers |
| Hidden line removal | ✅ | ✅ | ✅ | ✅ |
| Hidden surface removal with shadows | ✅ | ✅ | ✅ | ✅ (core only: the web editor does not offer the mode) |
| Software shaders | ✅ | ✅ | ✅ | ✅ |
| Mesh and scene readers | ✅ | ◐ | ◐ | ◐ |
| STEP and STL | ✅ | ◐ STEP in part | — | ◐ STEP in part |
| XML scene importer | ✅ | — | — | — |
| Animation | ✅ | ◐ | — | ✅ |
| GPU rendering | JOGL 2 / 4 | OpenGL 1.2 and 4.1 | OpenGL 1.2 | WebGL |
| Scene editor application | ✅ | ✅ | ✅ | ✅ |
| Radiosity and global illumination | — | — (in rpkCpp) | — | — |

## Architecture and learning focus

Model and domain code is separated from rendering technology code: the
entities of the core know nothing about how they are drawn, and each
technology adds its renderers in its own module. This keeps the code easier
to understand, modify and use for education, and is what makes the ports
and the legacy platforms possible.

The core of each port is meant to be "pure", with these current exceptions:

- `java/base` depends on `jackson-databind` (JSON) and uses `org.w3c.dom` /
  `javax.xml` for the XML importer; optional JNI libraries (`pkgs/`) are
  loaded when present.
- `cpp/base` and `turboc/base` optionally link libpng, libjpeg and FreeType.
- `@vitral/base` has no Node imports; file access is in `@vitral/fs`.

Known deviations from the design rules, defects and technical debt of the
production code are listed in [doc/BUGS.txt](doc/BUGS.txt).

## Portability and ports

The same high-level design used in `java/base` is replicated in every port:
a clean core model plus backend-specific rendering adapters. Ports are kept
in step by porting literally (class by class, method by method), so a fix
or a feature is expected to land in all of them; the differences that
remain are in the feature table above. There is no continuous integration:
the test suites of each port, and the deployment to the test machines, are
run on request.

## Supported platforms

Vitral aims to be portable across CPU architectures (from the 16-bit Intel 8086 to x86_64, MIPS, Alpha, SPARC and Apple M1), GPUs (from none to OpenGL 4.x), operating systems (MS-DOS, a minimalistic SMP kernel, classic Unix workstations, legacy and modern Linux, macOS) and development stacks (from Turbo C++ 3.0 and gcc 2.7.2 to modern clang, Java and TypeScript). Ports are verified on a distributed set of real and virtual test machines, described in a local, git-ignored `etc/portMachines/secrets.json` file (see `etc/portMachines/secrets-example.json`). See [doc/portability.md](doc/portability.md) for the table of supported systems and the deployment procedure of the test environment.

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
- Do not name variables or parameters `near`, `far`, `huge`, `pascal`,
  `cdecl`, `interrupt` or `asm`: they are Turbo C++ keywords.
- A variable declared in a `for` header belongs to the enclosing block
  (pre-ISO scoping): two `for (int i ...)` loops in one block, or one and
  a plain `int i`, are a "Multiple declaration". No declarations inside
  `if`/`while` conditions either.
- Do not define a nested class outside its enclosing class
  (`class Outer::Inner { ... };`): make it a top-level class and name it
  inside `Outer` with a `typedef`. Legacy compilers (TCC, gcc 2.7.2) do not
  let a nested class read the private members of its enclosing class: give
  it what it needs through public members or constructor parameters.
- Vitral does not use `friend` declarations (in any port).
- No `static` function templates, and no template functions whose
  parameter must be deduced through another template (`List<T*>&`): use
  overloads, generated with a macro when there are several types.
- Overloads that take a derived pointer and `const void*` are ambiguous in
  TCC (it ranks both conversions equally); use an ellipsis fallback.
- In the definition of a static array member, qualify the constants of its
  bound (`Class::SIZE`, not `SIZE`).
- No loops in an `inline` constructor defined outside its class
  (`inline A::A(...) { for (...) ... }` in a header): TCC reports
  "Expression syntax". Define such a constructor in the `.cpp`
  (constructors with loops written inside the class body compile).
- No function template deduction from a pointer argument through
  `const T &`: add a non-template overload (i.e. `hashMapKey(const void*)`).
- Run `turboc/scripts/checkTurboCpp.py` before a transfer: it finds most
  of these constructs on the host.
- TCC rejects `delete` on const-qualified pointers. Keep a non-const local
  pointer when an owned `const T*` must be released.
- Its nested-class access rules are stricter: qualify calls to outer static
  members.
- With `VITRAL_WITHOUT_PTHREADS`, provide a synchronous path for code that
  otherwise creates `Future`, `FutureTask`, or `ExecutorService` objects.
- TCC response files need one option per line. Batch builds should print a
  compact `Compiling <source>` line, redirect successful output to a log, and
  print the log only after an error.
- DOS release batches use `-O1 -d -v- -vi-`: optimize for size, merge
  duplicate strings, explicitly omit debug records, and do not expand inline
  functions (`-vi-` after `-v-`, which enables expansion again; without it
  the code is some 15% larger, and a TLINK image must fit in 1 MB). Debug the
  corresponding source on a modern platform instead.
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

## Where to look (Java paths; the other ports mirror them)

| Area | Main capabilities | Where to look |
|---|---|---|
| Core foundations | Linear algebra, color, data structures, symbolic algebra, utilities | `java/base/src/main/vsdk/toolkit/common` |
| Scene and geometry model | Cameras, lights, materials, backgrounds, geometric entities, scene containers, CSG | `java/base/src/main/vsdk/toolkit/environment` |
| Solid modeling | Half-edge solids, Euler operators, validators | `.../environment/geometry/volume/polyhedralBoundedSolid` |
| Solid operations | Booleans, slicing, polygon clipping and triangulation | `.../environment/geometry/geometricProcessing` |
| CPU rendering | Wireframe, hidden line, hidden surface, 2D rasterizer, raytracer, software shaders | `java/base/src/main/vsdk/toolkit/render` |
| Media and image data | RGB/RGBA images, palettes, z-buffers, pixel and color handling | `java/base/src/main/vsdk/toolkit/media` |
| Persistence and import/export | Image formats, geometry IO, STEP, STL, XML importer | `java/base/src/main/vsdk/toolkit/io` |
| Interaction model | Viewports, gizmos, events, controllers | `java/base/src/main/vsdk/toolkit/gui` |
| Desktop integration | AWT/Swing rendering and GUI adapters | `java/awt/src/main/vsdk/toolkit` |
| GPU rendering adapters | JOGL2/JOGL4 renderers, OpenGL resource helpers | `java/jogl2/src/main/vsdk/toolkit`, `java/jogl4/src/main/vsdk/toolkit` |
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

Java 19 or newer is recommended. Java 17 works, but its `Double.toString`
sometimes prints more digits than the shortest text that reads back the same
value (JDK-4511638, fixed in JDK 19), e.g. `-3.8518598887744717E-34` instead
of `-3.851859888774472E-34`. The values are equal, but text outputs (i.e. the
`--appelDump` JSON files of `PolyhedralBoundedSolidExample`) then differ from
the C++ and TypeScript ports, which print the shortest text as JDK 19+ does.

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

- `java/`: Java port (core, AWT, JOGL, Android and jME adapters, testsuite)
- `cpp/`: C++11 port, its rendering and GUI backends, and the minimalistic
  kernel (`cpp/x86_64_smp_kernel`); see [cpp/README.md](cpp/README.md)
- `turboc/`: legacy C++ port for compilers from 1992 to 2007 and DOS; see
  [turboc/README.md](turboc/README.md)
- `typescript/`: TypeScript packages and the web testsuite; see
  [typescript/README.md](typescript/README.md)
- `doc/`: architecture notes, plans, references, known bugs
  ([doc/BUGS.txt](doc/BUGS.txt)) and the portability table
  ([doc/portability.md](doc/portability.md))
- `etc/`: shaders, textures, materials, fonts, GUI definitions, sample
  assets and the description of the test machines (`etc/portMachines`)
- `pkgs/`: optional native libraries used from Java through JNI (CMake projects)
- `scripts/`: checks run on the host (attribute names, Xt layers)
