# Vitral TypeScript packages

This directory contains the TypeScript port of the Vitral graphics toolkit.
It is a literal port of the Java one: the same packages, classes and method
names (`vsdk/toolkit/...`), over a small Java-like layer
(`base/src/main/java`), so a class can be read side by side with its Java
original.

## Packages

<!-- prettier-ignore -->
| Package | Folder | Contents |
|---|---|---|
| `@vitral/base` | `base/` | The toolkit core: linear algebra, scene model, boundary representation solids and their booleans, CSG, polygon triangulation and clipping, BVH, the CPU renderers (raytracer, hidden line removal, hidden surface removal with shadows, 2D rasterizer, software shaders), media, the interaction model (viewports, gizmos, controllers). Platform neutral: it has no Node filesystem imports, and it runs in a browser or in Node. |
| `@vitral/fs` | `fs/` | Node / server-only adapter for local files. Do not install or bundle it in browser applications. |
| `@vitral/webgl` | `webgl/` | WebGL renderers and the mapping of DOM events to toolkit events (`WebSystem`). |

Parallel work in the browser (i.e. the raytracer by tiles) uses the Web
Worker protocol exported by `@vitral/base`.

## Testsuite

- `testsuite/VitralWebTestsuiteContainer`: an Angular application that hosts
  the scene editor (`src/ApplicationCases/SceneEditorApplication`), the WebGL
  examples and the API tests. It has its own `package.json`; run `npm ci`
  inside it before `ng build` or `./run.sh`.
- `testsuite/OfflineExamples`: programs for Node that write images or text
  (raytracing, wireframe, 2D rasterizer, polygon triangulation, algebraic
  expressions), comparable with the outputs of the other ports.
- `testsuite/_APITests`: concurrency experiments (Web Workers, fibers).

## Build and test

Build and test tooling requires Node.js 22 or newer (the version enforced by
`package.json`). Install the pinned dependencies with `npm ci` and run
`./scripts/compile.sh` from this directory or any other directory.

```bash
npm ci
npm run build      # base, fs and webgl, in that order
npm test           # vitest suites of base and fs
npm run lint       # prettier, eslint and the attribute names check
```

The compiler runs in `strict` mode with `noImplicitOverride`.

## Status with respect to the Java port

The core follows `java/base` closely (see the feature table of the
[main README](../README.md#feature-status-by-port)). Differences to keep in
mind:

- The mesh readers are a subset (OBJ, MIT groups, altimetry, MD2, STL, part
  of STEP); there is no 3DS, ASE, PLY, VRML, VTK or GTS reader, and no XML
  scene importer.
- The web scene editor presents its viewports with one of two render
  technologies. The projection popup menu of a viewport always lists the
  four render modes, and choosing one that the technology in use does not
  offer changes the technology of the whole viewport set (the MCP tools
  `viewport.set_rendering` and `render.set_technology` do it too). A WebGL 2
  canvas is used only for the GPU (z-buffer) mode. A canvas drawn only with
  its 2D context gives the three CPU modes, with no WebGL context:
    - hidden lines, which is wireframe or Appel hidden line removal
      (`HiddenLineRenderer`);
    - hidden surfaces, by Weiler-Atherton (`HiddenSurfaceRenderer`), with
      shadows when `F10` is on; with the surfaces off and the lines on, only
      the contours of the visible polygons are drawn;
    - raytracing (Whitted), by tiles in Web Workers.

    The MCP names are `hidden_lines`, `hidden_surfaces` and `withShadows`.
    Java's AWT technology and cpp's Xlib technology correspond to the canvas
    2D technology.

- In the editor, `F10` toggles the shadows flag of the renderer
  configuration and `F11` requests the raytraced image, as in the other
  ports; a browser may keep `F11` for its own full screen mode.
- As the port is literal, nullable Java references became non-null
  assertions (`!`) in many places; see [doc/BUGS.txt](../doc/BUGS.txt).

See also [doc/typescriptPortPlan.md](../doc/typescriptPortPlan.md) and
[doc/typescriptParityAudit.md](../doc/typescriptParityAudit.md).
