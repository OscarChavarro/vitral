# Vitral

Vitral is a computer graphics testbed focused on experimentation and learning. It is designed to build small graphics applications quickly while switching between multiple algorithms on both CPU and GPU pipelines.

## What this project does

Vitral provides a compact but broad graphics toolkit with:

- Core math, geometry, scene, media, and processing foundations
- CPU-side rendering and shading (wireframe, hidden-line, rasterization, ray tracing, software shaders)
- GPU-side rendering adapters (OpenGL/JOGL backends)
- IO and persistence layers for geometry, images, XML, and scene resources
- A testsuite with offline examples, interactive apps, techniques demos, and benchmarks

This repository currently contains the Java implementation organized as:

- `java/base`: dependency-light core toolkit
- `java/awt`: AWT/Swing integration layer
- `java/jogl2` and `java/jogl4`: JOGL rendering backends
- `java/testsuite`: runnable examples, API tests, and application cases

## Architecture and learning focus

Vitral uses a modular, organized design where model/domain code is separated from rendering technology-specific code. The rendering layer is decoupled from core entities, which keeps the codebase easier to understand, modify, and use for education.

A key property of the project is the "pure" base layer (`java/base`): it minimizes external dependencies and keeps core logic independent from specific rendering frameworks. This makes the toolkit suitable for:

- Learning graphics fundamentals with clear boundaries between concerns
- Swapping rendering implementations without rewriting the domain model
- Building new ports and supporting different technologies/platforms (for example Vulkan-oriented backends)

## Portability and ports

The Java code in this repository is a **port** of the toolkit architecture. The same high-level design used in `java/base` is intended to be replicated across other ports (such as C++, TypeScript, and others): a clean core model plus backend-specific rendering adapters.

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
| Desktop integration | AWT/Swing rendering and GUI adapters | `java/awt/src/vsdk/toolkit` |
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

## Repository layout

- `java/`: main toolkit modules and testsuite
- `doc/`: architecture notes, references, and historical docs
- `etc/`: shaders, textures, materials, fonts, and sample assets
- `pkgs/`: auxiliary native/third-party experiments
