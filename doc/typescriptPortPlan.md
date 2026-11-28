# Vitral Java-to-TypeScript Port Plan

## Purpose

Build a complete, mechanically traceable TypeScript port of the current Java Vitral implementation. The port already includes reusable TypeScript packages and a browser scene editor. Restore a buildable package baseline, then close the outstanding core, platform-adapter and worker gaps using the phase inventory below.

No phase may use placeholders, empty method bodies, unconditional dummy returns, "not implemented" exceptions, fake renderers, or compile-only stubs. Existing TypeScript files are candidate implementations, not proof of parity: each one must be compared method by method with the current Java source.

## Mandatory migration discipline

Every production migration must preserve the Java implementation textually and behaviorally: control flow, numeric tolerances, mutation order, diagnostics, comments that describe invariants, and deterministic ordering are ported 1:1 unless a runtime boundary makes that impossible. Such a boundary must be documented beside the code and verified by a dedicated parity test; replacing a complex Java algorithm with a simpler or merely equivalent-looking algorithm is prohibited.

For a complex algorithm, first map and migrate its complete private dependency graph (state types, predicates, allocators, ordering helpers, topology utilities, and nested result/specification types). Only then migrate its public entry points textually, followed by Java-derived regression and degeneracy tests. This rule applies to every phase, including already-completed phases when parity is re-audited.

## Current status and phase matrix

Review baseline: 2026-11-27, source revision `adecdd8c`. This matrix supersedes
the September gate records and inventories as a statement of current status.
The scope includes the current Java core, the web equivalents of the applicable
AWT/Swing and JOGL4 behavior, and the existing TypeScript applications.

| Production source root | Tracked source files |
|---|---:|
| `java/base/src/main` | 604 Java |
| `java/awt/src/main` | 15 Java |
| `java/jogl2/src/main` | 69 Java |
| `java/jogl4/src/main` | 63 Java |
| `typescript/base/src/main` | 685 TypeScript, including 576 under `vsdk` |
| `typescript/fs/src/main` | 19 TypeScript |
| `typescript/webgl/src/main` | 82 TypeScript |

Comparing the tracked Java core paths with the union of the three TypeScript
source roots gives 546 same-relative-path matches and 58 unmatched Java
paths. This is a file inventory. Renames, split classes, runtime equivalents,
nested declarations, package exports, and algorithm behavior require separate
review. Of the 63 JOGL4 file basenames, 62 have a matching WebGL basename after
replacing `Jogl4` with `WebGL`. The unmatched file is
`Jogl4VoxelVolumeRenderer`. The AWT/Swing comparison is by responsibility.

**Finalized** means the phase has implementation and local evidence without a
known phase-specific closure item in this review, or its inventory checkpoint
has been confirmed. **In progress** means relevant work exists and a concrete
implementation, parity or validation item is open. **Pending** means the
applicable implementation has yet to begin. The shared build, packaging and
browser verification apply to the release as a whole.

The matrix records 22 finalized phases, 23 in progress and one pending.

| Phase | Scope | Status | Evidence or remaining work |
|---:|---|---|---|
| 1 | Java runtime compatibility and packaging | In progress | All three tarballs pass standalone type and runtime checks. Object streams support multiple portable records. Java double literals and regex capture queries are covered. Direct binary32 parsing and regex dialect differences remain open. |
| 2 | Common entity foundation | In progress | Entity inspection applies the Java type whitelist and reads declared control types without evaluating getters. Accessor pairs without metadata still depend on runtime values. |
| 3 | Linear algebra | In progress | Matrix, vector and quaternion samples matched the Java bit patterns and exception types. Finish migrating the current Java assertions and resolve the `Complex.abs()` rounding difference. |
| 4 | Symbolic algebra | Finalized | The Java `StreamTokenizer` parser and recursive tree construction are ported. Compared tree rendering, values and errors with Java. |
| 5 | Color | Finalized | `ColorRgb`, `ColorRgba` and the new `Cie` model are present. The focused CIE and RGB suites passed. |
| 6 | Logging | Finalized | Reports match Java in the compared diagnostic text and order. Fatal reports preserve the message and cause. The web host receives an exception when Java would exit the process. |
| 7 | Memory management | Finalized | `MemoryPool` is exported from `@vitral/base`. The LIFO borrow/free sequence was compared with Java. |
| 8 | Quasi-Monte Carlo checkpoint | Finalized | Empty group. No corresponding implementation was found in either core. |
| 9 | Data structures | Finalized | Container implementations and `KDTree` have source matches, and the focused KD-tree tests passed. |
| 10 | Statistics | Finalized | Core counters, including `RaytraceStatistics`, have TypeScript counterparts. Earlier counter and aggregation checks are recorded below. |
| 11 | Command-line options checkpoint | Finalized | Empty library group. Entry-point arguments belong to the application host. |
| 12 | Progress monitors | Finalized | Core monitors and the parallel producer/consumer are present. The tile scheduler routes worker progress notices back to the owner. |
| 13 | Tangible interfaces | Finalized | Java, Node and Chrome accepted the same fragmented WebSocket pose. Tests cover handshake failure, reconnection, malformed records and listener dispatch. Browser WebSocket events handle asynchronous I/O. |
| 14 | Media and image buffers | In progress | Image buffers and HDR/solid textures are present. Recheck cloning, compressed-pixel errors and the mapping of Java object serialization. |
| 15 | General processing | In progress | The Cohen–Sutherland outcode and segment checks follow Java. Resolve the `LzwWrapper` and `SpharmonicKitWrapper` runtime mappings. |
| 16 | Materials | In progress | `ScatteringArithmetic` is available at the matching source and package paths. The previous `scatteringArithmetic` import still resolves. Continue the material and lobe review. |
| 17 | Geometry base | Finalized | The `Geometry` contract is present and used by the current geometry families. |
| 18 | Curves | In progress | Curve evaluation and rendering exist. Compare the shared TypeScript evaluator with Java's separate interpolation paths. |
| 19 | Geometry elements | In progress | Ported `closestPointOnTriangle` and aligned numeric containment codes. The reference agrees on edge-distance and collapsed-triangle cases. Continue the ray, hit and vertex audit. |
| 20 | Concrete geometry elements checkpoint | Finalized | The historical plural package entries map to the singular package covered by Phase 19. |
| 21 | Surfaces | In progress | Triangle mesh normal construction follows Java's edge normalization and incident order. Close the `_ParametricBiCubicPatchIntersector` gap and recheck the remaining mesh internals. |
| 22 | Volumes and boundary representation | In progress | B-rep operators, validators and the recent refactor are present. Verify the private decision paths and the newer Java `VoxelVolume` behavior. |
| 23 | Backgrounds | Finalized | All four classes were compared with Java. `FixedBackground` already returns Java's null result. The background suites cover fixed, uniform and cubemap sampling. |
| 24 | Cameras | Finalized | `Camera` is ported line by line from Java, including the three outcode overloads, both Cohen–Sutherland clippers and the ray projection. A reference driver matched Java bit for bit on five camera setups. `CameraProjectionTest` is ported, and the browser gizmos were exercised. |
| 25 | Geometric processing | In progress | Both clipping families build and export. The generic clipper suites pass. Revisit polygon-merger and CSG parity on the current sources. |
| 26 | Lights | Finalized | The five Java light types are represented. The historical parity record covers attenuation, direction and copying. |
| 27 | Tone-mapping checkpoint | Finalized | Empty group. Existing gamma and HDR operations remain in Phases 14 and 15. |
| 28 | Scene model | In progress | Scene containers, CSG and worker snapshot transfer exist. Revalidate scene-instance BVH, transforms and snapshot behavior after the current revisions. |
| 29 | Numerical analysis | Finalized | The four polynomial-source counterparts and both lookup tables were found. The historical graph group is empty. |
| 30 | I/O wrappers | In progress | `ShapeDescriptorPersistence` reads and writes the Java stream format. Continue the JNI capability mappings and browser resource-failure checks. |
| 31 | I/O context checkpoint | Finalized | The runtime split is explicit in the packages. Format and stream gaps are tracked in Phases 30 and 32–36. |
| 32 | Binary I/O | In progress | `PersistenceElement` legacy long helpers intentionally read and write four bytes, as Java does. Continue the audit of the pipe and decompression wrappers and of true 64-bit stream contracts. |
| 33 | Image I/O | In progress | Node raster import, browser decoding, SGI, compressed DDS, palette import and PNG/PPM export exist. Review TGA, uncompressed DDS, native decoding equivalents and remaining export formats. |
| 34 | XML I/O | Finalized | All four classes are exported. Java reference comparisons cover the legacy line tokenizer, including offsets, quotes, non-tag spans and malformed input. |
| 35 | VRML checkpoint | Finalized | The graph group is empty. The actual VRML and compressed input work is tracked in Phase 36. |
| 36 | Geometry I/O | In progress | OBJ and parallel PLY import are wired in the web editor and the Node asynchronous dispatcher. MIT/altimetry/MD2/STEP readers and STL and OBJ/GTS/VTK writers exist. Six other geometry readers, STEP writing, and curve, patch and viewpoint persistence remain open. |
| 37 | SGL checkpoint | Finalized | Empty group. The current GPU adapter belongs to Phase 41. |
| 38 | GUI model and controllers | In progress | DOM/CSS widgets, dialogs, events, viewports, gizmos and the four camera control schemes (Aquynza, Orbiter, Blender, Google Earth) are ported. The Orbiter, Blender and Google Earth controllers matched Java bit for bit in a reference driver. All 16 Java GUI test classes are ported, and their 195 tests pass. The testsuite examples use the ported Aquynza and Orbiter controllers, as their Java programs do, and were verified in the browser. The quality keys go through the ported `RendererConfigurationController`. The HUD icon, text configuration, viewport-window manager, information dialog and visual analytics classes are ported, with reference-driver parity. Verify worker progress through the renderer owner (Phases 12 and 40). |
| 39 | Software shaders | In progress | The shader family and a historical matching raytrace exist. Close the shader gate against current materials and the current CPU render path. |
| 40 | CPU rendering | In progress | Heckbert scan conversion is exported and matches Java in flat and depth-tested visibility passes. The browser raytrace completed on Web Workers. `AutoStereogramGenerator` matches Java byte for byte. `PolyhedralBoundedSolidDebugger` is ported with a plugin registry in place of Java's class loading. Every inventory class with a Java source is now ported. Run the phase gate. |
| 41 | GPU rendering | In progress | WebGL 2 renderers, context probing and offscreen color/depth readback are implemented. Map voxel rendering and verify shader, texture and context/resource lifecycle in a browser. |
| 42 | Animation | Finalized | All four animation classes are present. The timer preserves the tick and listener order of the Java loop and supports teardown. |
| 43 | Application framework | Pending | The three framework/shape-matching classes lack TypeScript counterparts. The existing editor has its own application orchestration. |
| 44 | GUI persistence | In progress | `GuiPersistence` and `DialogPersistence` load the editor's Aquynza JSON over HTTP. Check validation, resource errors and dialog behavior against Java. |
| 45 | Orphan-inventory closure | In progress | The source inventory was refreshed. Assign unmatched and new files to phases, and record package export and consumer reachability for each applicable entry. |
| 46 | Parity and dependency-closure audit | In progress | The historical audit and current build findings are inputs. Repeat the API, private-algorithm and runtime checks on the refreshed inventory. |

### Initial review before the clipper-path repair

- `cd typescript && npm run build` exits 1 in `@vitral/base` with eight
  `TS2307` errors. Seven exports in `base/src/main/index.ts` and one import
  in `HiddenSurfaceRenderer.ts` use `polygonClipper/`; the source files
  live in `genericPolygonClipper/`. The build stops before `fs` and
  `webgl`.
- Focused Vitest runs passed 19 tests across six files (color, viewport
  modes, tangible WebSocket, KD-tree and parallel raytracing). Two additional
  suites, Weiler–Atherton clipping and hidden-surface rendering, failed to
  load because of the stale path. The clipper and scan-conversion name filters
  found no dedicated Sutherland–Hodgman or Heckbert suite. The raytracer
  tests use an in-process executor to check scheduling and pixel output.
- `ConvexWindowClipper` and the Heckbert `polygonScanConverter` family are
  in both source trees. The TypeScript compiler file list includes the
  clipper and omits the new scan-conversion family. Review the raster entries
  in `base/tsconfig.json` and confirm both features work from an installed
  package.
- No clean packaging gate, Java test run or browser execution was performed
  as part of this review. September test counts and pass results apply to
  their recorded revisions.

### Execution update — 2026-11-27

- The public package emits the convex and generic clippers and the new
  scan-conversion modules. Three 12 × 9 visibility buffers matched the
  Java reference, including overlapping geometry, a restricted viewport,
  depth testing and reversed draw order.
- The symbolic-algebra parser matched Java on 24 expressions and error cases.
  The `MemoryPool` borrow/release sequence and scattering arithmetic samples
  also matched the Java reference. Geometry cases matched both the numeric
  containment codes and the clipped coordinates.
- Descriptor records matched Java byte for byte for primitive counts and
  Fourier data. Truncated records raise an I/O error at EOF rather than
  repeat the chunk loop. The XML line tokenizer preserves the Java byte-span
  and quoted-text behavior. The four core XML classes do not include the
  application's `XmlManager` integration.
- The Angular production build passed. The editor produced a 32 × 24 PNG in
  headless Chrome using eight real Web Workers. They sent 24 completed tiles
  and 24 progress notices, and all eight terminated on leaving the editor.
  A direct request to the same worker entry point returned a structured
  error and then a valid depth tile. A real Node worker also accepted another
  request after computation and structured-clone failures.
- The PLY reader supports ASCII, little-endian and big-endian input and
  returns point clouds with or without a mesh. The mug fixture and three
  synthetic Java comparisons matched point coordinates, triangle indices,
  and vertex and face normals byte for byte. Each runtime launches two jobs
  for mesh assembly. The browser container supplies its bundled worker
  factory. Chrome imported the mug from an HTTP resource and from a local
  file. Each import completed and terminated both workers. Node exposes
  `EnvironmentPersistence.importEnvironmentAsync` for the PLY route and
  retains the synchronous OBJ entry point.
- The clean workspace build and Angular production build passed. The
  packaged `@vitral/base` passed a strict declaration check from a temporary
  consumer importing the new APIs. Vitest passed 1,023 base tests with four
  test workers and 10 skips, and 21 fs tests with one test worker.

### Early-phase verification — 2026-11-27

- A clean workspace build produced the three tarballs. The dry run found
  2,669 files in `@vitral/base`, 85 in `@vitral/fs` and 334 in
  `@vitral/webgl`. All three shipped their entry-point JavaScript and
  declarations without source or build-cache files. An independent ESM
  package compiled and ran with the tarballs installed, including both
  polygon clippers and the Node and browser PLY entry points. The Angular
  production build passed.
- The portable object stream writes newline-delimited JSON and reads legacy
  unframed single records. The mapping of JVM-serialized image data remains
  in Phase 14. The floating-point adapters accept Java double literals,
  including hexadecimal notation, signed zero and overflow. Float parsing
  still rounds via binary64 before conversion to binary32. Regex capture
  counts, unmatched groups and offsets are covered for patterns supported by
  the ECMAScript engine. Java-specific regex constructs remain a runtime
  adapter item.
- Java and TypeScript reported the same sorted entity attributes for a
  metadata-backed model and for `Prism`. Without declared metadata, Java
  can distinguish primitive types and compare getter and setter signatures
  through reflection. Those types are erased in the emitted JavaScript.
- The Java linear-algebra suite passed 107 tests. A cross-runtime run matched
  143 numeric and exception results for matrix determinants, cofactors,
  inverses, projections, rotations, equality, hashing, bounds and vector and
  quaternion operations. The broader test migration remains active. For
  example, `Complex(1, 5).abs()` is one binary64 ULP apart between the JVM
  and Node because both ports use the host's `Math.hypot`.
- The warning and null-exception reports matched Java's stderr byte for
  byte. Tests cover fatal exception messages, cause identity and the
  configurable fatal-exception branch. Java's request to exit the JVM maps to
  a `VSDKFatalException` handled by the TypeScript host. Source names and
  stack frames come from the active runtime.
- A local WebSocket sent a fragmented frame with both a skipped record and a
  valid pose. The JVM, Node and Chrome reported a successful handshake and
  the same pose coordinates and quaternion. The WebSocket implementation
  assembles wire fragments before the browser's message event. CPU-parallel
  jobs continue to use Web Workers and Node worker threads.
- Vitest passed 1,036 base tests, with 10 existing skips, and 21 filesystem
  tests. The final focused run passed 67 tests for the early phases.

### Phase 24 closure — 2026-11-27

- `Camera.ts` previously used different algorithms for plane and
  canonical-volume clipping, `projectPoint`, `projectPointUsingRayMethod`,
  the projection matrices, `toString` and `clone`. It is now a textual port
  of `Camera.java`. Java behaviors kept as observable contracts: the far
  plane keeps the unnormalized `front` normal; `setUpMaintainingOrthogonality`
  stores the normalized argument; the ray method's view plane passes through
  `position + front`, because the near-distance product is discarded; and the
  plane clipper passes the near and far planes in swapped outcode slots. The
  clipped-point output parameters stay unobservable, as in Java. The
  TypeScript-only `*Result` methods were removed, and the gizmo and WebGL
  viewport callers now use `projectPointUsingRayMethod`, which returns the
  point or null as in Java.
- A Java/Node reference driver compared 387 lines of IEEE-754 bit patterns
  over default, perspective, orthogonal, rotated-orthogonal and
  `setUpMaintainingOrthogonality` cameras. It covered vectors, normalizing,
  view-volume, transformation and projection matrices, primary rays,
  bounding planes, both projections, both clippers, visibility, snapshots,
  `toString`, copy and `clone`. The outputs were identical. Java ran with
  `-XX:-UseLibmIntrinsic`.
- The TypeScript-only `Camera.spec.ts` was replaced by the port of
  `CameraProjectionTest`. Vitest passed 1,034 base tests with 10 skips.
  In Chrome, the CameraExample rendered and the scene editor's translate and
  rotate gizmos kept a finite size. An X-axis drag changed only the X
  coordinate. No console errors were reported.

### Gizmo test migration — 2026-11-27

- Six Java test classes were ported to Vitest: `TranslateGizmoTest`,
  `TranslateGizmoInteractionTechniqueTest`, `ScaleGizmoTest`,
  `ScaleGizmoInteractionTechniqueTest`, `RotateGizmoTest` and
  `RotateGizmoInteractionTechniqueTest`. Their 134 cases match the Java
  `@Test` count, and all pass against the current gizmo and camera ports
  without production changes. The cases cover picking, dragging, cursor
  wrapping, keyboard and typed-coordinate input, scaling, contours and
  ring strips.
- `RotateGizmoTest.given_gridOfRays_when_picked_then_agreesWithTheDistanceToTheRings`
  stays in the regular suite, as in Java, which has no `@Tag("slow")`.
  Its oracle allocates about 280 million temporary vectors, which takes
  about two minutes in V8 (`pickRing` itself handles 10,000 rays in 0.2 s).
  The test has a 300 s timeout and yields after each pixel row, so the
  vitest worker RPC stays responsive. The full base run passed 1,168 tests
  with 10 skips in 150 s.

### GUI test migration and camera controllers — 2026-11-27

- The remaining eight Java GUI test classes were ported to Vitest:
  `InputGizmoTest` (26 cases), `InputGizmoValueChangeRulesTest` (7),
  `RayGizmoRollIndicatorTest` (1), `RayGizmoViewThicknessTest` (6),
  `ReferenceFrameGizmoTest` (7), `CameraControllerInteractionTest` (4),
  `editor.GenericEditorTest` (8) and `viewport.ViewportSetCoordinatesTest`
  (2). Every Java GUI test class now has a Vitest counterpart with the same
  case count. `GenericEditorTest`'s inner `RecordingEditor` became a module
  class. It reads the protected `specifications` list through accessors,
  where Java reads the package-private field.
- `CameraControllerInteractionTest` needed `CameraControllerOrbiter`, which
  was missing. `CameraControllerOrbiter`, `CameraControllerBlender` and
  `CameraControllerGoogleEarth` were ported line by line and exported from
  `@vitral/base`.
  - Java's dead stores are dropped: Blender's `unicodeId` and `angleInc`,
    the `RayA` in Google Earth's press handler, and Orbiter's wheel
    `angleInc`. Each drop is commented in place.
  - The `expo` and `altura` locals that Java scopes across Google Earth's
    `KEY_z` and `KEY_Z` cases are hoisted.
  - `Math.round(Math.log10(z))` goes through the Java `Math` port.
- A reference driver ran 46 events through Java and TS: keys, numeric-pad
  keys with and without CTRL, raw codes 107 and 109, presses, drags with each
  button mask, and wheel clicks in both directions. It covered Orbiter,
  Blender, Google Earth and Aquynza, dumping position, focus, rotation, fov,
  orthogonal zoom, clipping distances and projection mode after each event.
  All 184 lines matched bit for bit (`-XX:-UseLibmIntrinsic`).

### Testsuite camera controllers — 2026-11-28

- The testsuite examples now use the ported controllers that their Java
  programs construct: `CameraControllerAquynza` in `CameraExample` and
  `ImageExample`, and `CameraControllerOrbiter` in `MeshExample`,
  `SolidTextureExample`, `MD2Example`, `ShadersExample`,
  `PolygonClippingExample` and `PolyhedralBoundedSolidExample`.
- `_shared/ExampleCameraInteraction` is now only an event adapter, the
  counterpart of `AwtSystem.awt2vsdkEvent`. It replaces a 320-line orbit
  imitation.
  - Pointer events go through `WebSystem.toVitralMouseEvent` /
    `toVitralWheelEvent`, with AWT button masks and wheel notches.
  - A move with a button held goes to `processMouseDraggedEvent`; any other
    move goes to `processMouseMovedEvent`.
  - `WebKeyEvent` key names map to the `KeyEvent.KEY_*` codes.
- Workarounds the stand-in needed, now removed:
  - The `adoptCameraState()` calls.
  - Re-applying `configureInitialView()` in `MD2Example`, `ShadersExample`
    and `PolyhedralBoundedSolidExample`.
- Java behavior restored:
  - `MD2Example`'s `setDeltaMovement(10)`.
  - `setPointOfInterest` in `CameraFaceFocusInteraction`,
    `recenterOrbiterAfterModelChange` (with the previous point read from
    the orbiter) and `focusCameraOnCurrentScene`.
  - The camera key-release calls in the PolyhedralBoundedSolid,
    PolygonClipping, Shaders and MD2 keyboard techniques.
- Checks:
  - `tsc` on the testsuite app is clean.
  - In Chrome: PolyhedralBoundedSolid (drag orbit, wheel, arrow keys),
    CameraExample (Aquynza drag and keys), MD2 (orbit and the 10-unit `x`
    step) and Shaders (orbit) rendered and responded, with no console
    errors.
- Follow-up the same day: `ExampleRendererConfigurationInteraction` now
  wraps the ported `RendererConfigurationController` instead of copying it.
  The copy lacked Java's `KEY_F10` (`changeWithShadows`) binding, which the
  examples now get. The `WebKeyEvent` to `KeyEvent` conversion that both
  adapters use lives in `_shared/web-key-event-conversion.ts`.

### Remaining GUI classes — 2026-11-28

- The nine Phase 38 inventory classes that had no TypeScript counterpart
  were ported line by line into `@vitral/base` and exported from it:
  - `TextVisualConfiguration` and `HudIcon`.
  - `ViewportWindow` and `ViewportWindowSetManager`.
  - `dialog.InformationDialog`.
  - `visualAnalytics.VisualDoubleVariable`, `VisualVariableSet`,
    `PercentageWheelWidget` and `PercentageWheelWidgetController`.
- `HudIcon` reads its image file as Java's
  `ImagePersistence.importRGBA(new File(name))` does. The read goes through
  an importer that each runtime registers with `HudIcon.setImageImporter`.
  `@vitral/fs` registers its PNG/PPM reader when it is loaded, the way it
  already installs its deflater. Without an importer, a named file throws
  inside `init`, and the icon takes Java's "Image not found" branch
  (warning, null image).
- Java's quirks are kept and commented in place:
  - `ViewportWindowSetManager.doLayout4` switches on `style % 7` while its
    caller passes `style % 5`.
  - `doLayout4` declares the unused `p25`/`p75`, both 0.25.
- Reference drivers compared Java and TS output:
  - `PercentageWheelWidgetController` sector picking: 10,854 samples with 4,
    7 and 12 sectors (2,034 hits). Identical.
  - `ViewportWindowSetManager`: 0 to 5 views, styles 0 to 9, full-viewport
    on and off, then pointer picking. 266 lines identical, 22 distinct
    layouts.
  - `InformationDialog`'s animation clamps: identical.
  - `HudIcon` with three PNGs from `etc`, a null name and a missing file:
    `toString`, `getMapTag`, `doIntersection` and
    `doIntersectionWithGravity` are identical.
- Phase 14 parity finding (not fixed): `RGBImageUncompressed.putPixelRgb`
  out of the buffer. Java's direct `ByteBuffer` throws
  `IndexOutOfBoundsException` after the in-range components are written.
  The TypeScript typed array drops the write silently. `HudIcon.overWritePixels`
  at a negative position, or past the image edge, therefore throws in Java
  and completes in TS. The same holds for the other `get`/`put` accessors of
  the uncompressed image classes. A fix adds a bounds check to every pixel
  access, so it is left for a decision on the render hot paths.
- Inventory note: 18 entries of `61_gui.txt` do not exist anywhere in the
  current Java tree, so they have nothing to port. The inventory was captured
  from an older source tree. The 18 entries are:
  - `ExceptionGuiBadName` and `ExceptionGuiParseError`.
  - `Gui`, `GuiButtonGroup`, `GuiCommand`, `GuiCommandExecutor`,
    `GuiDialog`, `GuiElement`, `GuiMenu`, `GuiMenuElement` and
    `GuiMenuItem`.
  - The seven `variable.Gui*Variable` classes.
  - The gizmo entries live one package deeper (`gui.gizmo`), as already
    recorded under Phase 38.
- Remaining for Phase 38:
  - Verify worker progress through the renderer owner (Phases 12 and 40).
  - The Jogl2 `VisualAnalyticsPercentagesWheelExample` has no browser
    renderer yet.

### Work order

1. Keep validating the workspace, package consumer and Angular entry points
   as implementation gaps close.
2. Exercise stale viewport results and cooperative cancellation in
   the browser raytracing path. Continue the remaining Phase 36 readers.
3. Close the format and integration gaps by following the application
   entry points into the reusable packages. Compare AWT/Swing interactions
   and JOGL4 rendering to their web counterparts through observable behavior.
4. Update the symbol and export inventory as each gap closes. Revisit the
   September parity findings against the current implementation and record
   current reference tests or explicit runtime mappings before closing
   Phases 45 and 46.

## Historical gate records (September 2026)

Phase 1 gate record: `npm run verify` completed successfully after `npm ci`; TypeScript compile, packaging, tarball declaration-consumer validation, and the current test command passed. The current TypeScript test suite contains zero migrated test files and reports zero skipped tests. No Java test source has a dependency closure limited to Phase 2, so no test is eligible to migrate in this phase.

Phase 2 gate record: `npm run verify` completed successfully. The seven applicable public contracts are emitted through `@vitral/base`; `VSDKJ2ME` is intentionally excluded because it targets obsolete Java ME devices and does not apply to the Web port. `VSDK` fatal-report configuration is kept in a platform-neutral internal contract which the phase-6 `Logger` implementation will use. This avoids a Node-only logging dependency in the browser-facing base package.

Phase 3 gate record: `npm run verify` completed successfully. All sixteen inventory symbols are exported through `@vitral/base`; the eleven Java linear-algebra test-source contracts are represented by eleven deterministic Vitest cases, all passing. The package tarball and declaration-consumer validation also passed.

Phase 4 gate record: `npm run verify` completed successfully. No Java symbolic-algebra test source has a production dependency closure at this phase. The only related source, `AlgebraicIdentityRegressionTest`, depends on later polyhedral-solid production classes and remains pending under the test-port rule.

Phase 5 gate record: `npm run verify` completed successfully. No dedicated Java color test source is eligible at this dependency stage.

Phase 6 gate record: `npm run verify` completed successfully. Logger reporting is platform-neutral and throws `VSDKFatalException` for fatal reports, rather than importing Node process APIs into `@vitral/base`.

Phase 7 gate record: the authoritative group contains only its header. The import and generated-dependency audit found no memory-management symbol to port; no compatibility stub was added. `npm run verify` completed successfully.

Phase 8 gate record: the authoritative group contains only its header. The Java and TypeScript production-source audit found no Quasi-Monte Carlo implementation to port; no placeholder was added. `npm run verify` completed successfully.

Phase 9 gate record: `npm run verify` completed successfully. All twelve inventory symbols are exported through `@vitral/base`; six deterministic TypeScript parity tests cover primitive numeric storage and sorting, circular-list mutation and cursor behavior, and N-ary/binary tree topology. No Java test source has a production dependency closure limited to this phase.

Phase 10 gate record: `npm run verify` completed successfully. All three inventory symbols and the two supplemental current-Java statistics symbols are exported through `@vitral/base`; three deterministic TypeScript parity tests cover aggregation, reset behavior, exact long counters, and browser-safe optional instrumentation. The complete current Java `vsdk.toolkit.common` package is now available in TypeScript except the intentionally excluded Web-inapplicable `VSDKJ2ME`. The eleven Java tests located under `common/linealAlgebra` remain represented by passing TypeScript tests. `LinearAlgebraStrategiesConsistencyTest` is a `processing.linealAlgebra` test whose dependency closure requires later strategy-engine production classes, so its translation remains deferred to that phase.

Phase 11 gate record: the authoritative group contains only its header. The production-source audit found no command-line-options parser to port; references to command lines or arguments were unrelated to this group, and no placeholder parser was added. `npm run verify` completed successfully.

Phase 12 gate record: `npm run verify` completed successfully. All four progress-monitor contracts are exported through `@vitral/base`; three deterministic TypeScript parity tests cover in-memory progress, compact console milestones, and long-format console progress. No dedicated Java test source exists for this phase.

Phase 13 gate record: `npm run verify` completed successfully. All four inventory symbols are exported through `@vitral/base`; three deterministic TypeScript parity tests cover pose-event accessors, WebSocket-frame parsing, listener subscription/removal, notification order, malformed-frame handling, and the nested `FrameListener` contract. The tangible-event-to-gizmo mappers are outside this inventory and depend on later camera and gizmo production classes.

Phase 14 gate record: `npm run verify` completed successfully. All twenty media contracts are exported through `@vitral/base`; image data uses typed byte, float, and unsigned-16-bit buffers while preserving Java row orientation and signed-byte pixel semantics. No Java test source has a production dependency closure limited to this phase.

Phase 16 gate record: `npm run verify` completed successfully. All five material inventory entries and the supporting `Material` contract are exported; CSV loading is isolated to the server-only `@vitral/fs` package. No dedicated Java material test source exists. This phase was closed at user direction while Phase 15 remains in progress.

Phase 17 gate record: `npm run verify` completed successfully. The Geometry root contract is exported with its containment constants, local bounding-volume contract, visibility default, and structural ray/hit contracts to be implemented by Phase 19. No Java test source has a dependency closure limited to this root contract. This phase was completed out of normal order while Phase 15 remains in progress.

Phase 18 gate record: `npm run verify` completed successfully. Both curve contracts are exported, including parametric linear, quadratic, Hermite, Bézier and uniform B-spline evaluation, sampling, approximate bounds, and containment. The only related Java test depends on later polyhedral-solid production classes. This phase was completed out of normal order while Phase 15 remains in progress.

Phase 19 gate record: `npm run verify` completed successfully. All six geometry elements are exported, including immutable rays, configurable ray-hit records, triangle intersection and containment, and vertex data contracts. No Java test source has a dependency closure limited to these elements. This phase was completed out of normal order while Phase 15 remains in progress.

Phase 20 gate record: the six `vsdk.toolkit.environment.geometry.elements.*` graph entries have no Java source mapping in the current source of record; their singular-package counterparts were completed in Phase 19. No duplicate compatibility aliases were added. `npm run verify` completed successfully.

Phase 22 gate record: `npm run verify` completed successfully on 2026-09-10. All thirty inventory entries are exported through `@vitral/base`; the Java B-rep reference suite compiled and passed. Fifty-eight deterministic TypeScript tests in forty-three modules pass, including primitive B-rep exports, numeric/strict/topological validation, point-in-solid and quantitative-invisibility edge cases, topology editing, node ownership, and the Euler inverse and wrapper contracts (`mvfs`/`kvfs`, `lmev`/`lkev`, `lmef`/`lkef`, `lkemr`/`lmekr`, `lringmv`, and `lkimrh`/`lmikrh`). The face-plane corner fallback preserves the Java behavior for open-wire `lmef`/`lkef` inverse sequences. This phase was completed out of normal order while Phase 15 remains in progress.

Phase 23 gate record: `npm run verify` completed successfully on 2026-09-10. All four background models are exported through `@vitral/base`; deterministic tests cover uniform colours, the Java-compatible unfinished fixed background, cubemap face selection, image replacement, and camera ownership.

Phase 24 gate record: the standard gate completed successfully on 2026-09-10: clean build, lint, seventy deterministic tests in forty-seven modules, package creation, and a temporary-consumer import all pass. Both camera symbols are exported through `@vitral/base`. Tests cover snapshots, perspective and orthogonal ray generation, reference-frame orthogonality, modification versioning, projection, world and canonical clipping, view-volume matrices, and viewport conversion. Since `Vector3Dd` is immutable in both ports, Java's ineffective output-vector signatures remain boolean compatibility methods while explicit `*Result` variants return usable projected or clipped vectors.

Phase 26 gate record: `npm run verify` completed successfully on 2026-09-12
(exit 0), with clean strict builds, 382 passing and 7 skipped tests in 75
modules, zero unhandled errors, and the package creation and downstream
consumption checks passing.
Parity was established by diffing a Java reference driver against its TypeScript
twin: 428 identical lines covering, for eleven light configurations, the raw
IEEE-754 bits of every position, emission, direction, shadow-distance limit and
attenuation factor, the copy contracts, and the nested record's accessors,
equality, hash, and text. Three source-level divergences were corrected: `Light`
extended `FundamentalEntity` instead of Java's `Entity`; `Light.LightDirection`
lacked the `equals`, `hashCode`, and `toString` contracts that a Java record
declaration generates; and the base `VSDK.formatDouble` and `Double.toString`
helpers that the record's text contract depends on did not reproduce Java's
`DecimalFormat` and `Double.toString` output. No Java test source exists for
this package, so no test was eligible for migration and none was invented.

Phase 27 gate record, 2026-09-12: every gate check passes on its own merits —
clean strict builds, 382 passing and 7 skipped tests in 75 modules with no
failing test, and the package creation and downstream consumption checks — but
`npm run verify` exits 1 because vitest reports an unhandled
`[vitest-worker]: Timeout calling "onTaskUpdate"`. This is the documented
worker-RPC budget of the Phase 25.7 accommodation, not a regression of this
phase: the same run on the preceding commit, before any change recorded here,
produced two such errors, and across four runs the count was 2, 0, 1, and 1
while the slowest spec stayed at 481 s, 516 s and 481 s. The specs that raise it
never reference the modules changed here. Raising the budget or widening the
`yieldToEventLoop()` coverage belongs to the Phase 25.7 harness accommodation
and is left open.

The authoritative group is empty and no placeholder was created. Verifying the blocks that do represent tone mapping exposed three
byte-level defects in already-closed phases, all confirmed against Java
reference drivers and fixed here: `ImageProcessing.gammaCorrection` bound the
clamping `putPixel(int)` overload where Java binds `putPixel(byte)`, so every
indexed pixel whose corrected value exceeded 127 became zero; and
`RGBImageUncompressed.putPixel` and `RGBAImageUncompressed.putPixel` re-applied
the unsigned-to-signed conversion to arguments that Java declares as already
signed bytes, zeroing every channel above 127 and turning the `-1` alpha into
`0`. Two drivers now match Java exactly: 208 lines over eight gamma values on
indexed and RGB images, and 63 lines over the four `NormalMap` exports and the
three `ZBuffer` exports, which contain 676 negative channel values that the
TypeScript port previously returned as zero.

Phase 27 also found production files in the current Java base that no phase
inventory lists and that have no TypeScript counterpart: the nine files of
`vsdk.toolkit.media.solidTexture` (`TextureUtils`, `ProceduralNoise`,
`BumpTextureFixture`, `ColorTextureFixture`, `ImageTexture`,
`SolidTextureCoordinateMapper`, `ControlledRGBAImageHDRUncompressed`,
`ImageToSolidTextureInterpolationTypes`, and
`ImageToSolidTextureProjectionMethods`) plus
`vsdk.toolkit.media.IndexedColorImageHDRUncompressed` and
`vsdk.toolkit.media.RGBAColorPalette`, which the last two depend on. The
exhaustiveness check in Analyzed State covered only `vsdk.toolkit.common`, so
these eleven files are orphans of the 580-entry graph rather than a missed
obligation of Phase 14. They were recorded here as the first named input to
Phase 45, and no placeholder was created for them. All eleven were ported on
2026-09-13, to unblock the `SolidTextureExample` module; see the Phase 14
record for the advance and its parity evidence.

## Base and WebGL parity sweep — 2026-09-16

This sweep answers one question: with the test-suite migration frozen at the
accepted minimum, what is still missing for `@vitral/base`, `@vitral/fs` and
`@vitral/webgl` to be a 1:1 port of `java/base` and `java/jogl4`? It measures
the production trees only; it changes no code.

Method: a same-path file inventory diff between `java/base/src/main` and the
three TypeScript `src/main` trees; a per-class comparison of every Java method
name declared with an access modifier against the text of its TypeScript
counterpart; a name-mapped (`Jogl4*` → `WebGL*`, `render.jogl` →
`render.webgl`) comparison for `java/jogl4`; a reference and export check on
every TypeScript class with no Java counterpart; and a clean
`npm run build`, which passes with zero errors for the three packages.

### 1. File coverage

296 of the 377 Java production files have a TypeScript counterpart. The 81 that
do not are not scattered: they are five blocks, and each block is one open
phase.

| Java package | Files | Phase |
|---|---:|---|
| `vsdk.toolkit.gui` (controllers, events, viewport windows, HUD icon, text configuration) | 14 | 38 |
| `vsdk.toolkit.gui.widget` and `gui.widget.variable` | 18 | 38 |
| `vsdk.toolkit.gui.visualAnalytics` | 4 | 38 |
| `vsdk.toolkit.gui.gizmo` (translate, rotate, scale) | 3 | 38 |
| `vsdk.toolkit.gui.dialog` | 1 | 38 |
| `vsdk.toolkit.io.geometry` readers and writers | 14 | 36 |
| `vsdk.toolkit.io.geometry.stepCad.writer` | 8 | 36 |
| `vsdk.toolkit.io.image` | 4 | 33 |
| `vsdk.toolkit.io.xml` plus `io.XmlException` | 4 | 34 |
| `vsdk.toolkit.io.metadata.ShapeDescriptorPersistence` | 1 | 30 |
| `vsdk.toolkit.io.gui.GuiPersistence` | 1 | 44 |
| `vsdk.toolkit.io.FileLocator` | 1 | 45 (unlisted orphan) |
| `vsdk.toolkit.render.AutoStereogramGenerator`, `render.PolyhedralBoundedSolidDebugger` | 2 | 40 |
| `vsdk.toolkit.processing.LzwWrapper`, `processing.SpharmonicKitWrapper` | 2 | 15 |
| `vsdk.framework` (component and shape-matching plugins) | 3 | 43 |
| `vsdk.toolkit.common.VSDKJ2ME` | 1 | excluded by the Web-target decision |

`io.FileLocator` appears in no phase inventory and is recorded here as a new
named input to Phase 45.

### 2. JOGL4 versus WebGL

`@vitral/webgl` covers 24 of the 25 `java/jogl4` classes, and for each of them
the Java method inventory is present. The single class with no counterpart is
`Jogl4Renderer`, whose whole body is a `verifyOpenGLAvailability()` that returns
`true`; porting it is a Phase 41 bookkeeping item, not a capability gap.

The only method-level differences are the documented runtime boundaries, and
each is a JOGL artifact rather than behavior:

  - the NIO buffer plumbing (`toArray`, `toBuffer`, `toFloatArray`,
    `toColumnMajorFloatArray`, `importJOGL`), which a `Float32Array` makes
    unnecessary;
  - the GLU tessellator callbacks of `Jogl4Polygon2DRenderer` and
    `Jogl4PolyhedralBoundedSolidRenderer` (`begin`, `combine`, `emitTriangle`,
    `error`, `end`), replaced by `_Polygon2DOddWindingTessellator` because
    WebGL has no GLU;
  - the private upload-buffer accessors of `Jogl4SolidTextureRenderer` and the
    `ThickLineMesh` record and `drawThinLines` of `Jogl4LineRenderer`, whose
    two drawing paths (thin to `LINES`, thick expanded on the host) are both
    present in `WebGLLineRenderer` under a different member shape.

`WebGLShaderPreprocessor` and `_Polygon2DOddWindingTessellator` are the two
TypeScript-only classes of the package; both are recorded WebGL boundaries.

### 3. Same-path classes that are missing Java methods

31 of the 296 ported classes declare fewer methods than their Java source. Four
were already recorded (`Matrix4x4f` and `ParametricBiCubicPatch` in the audit,
`AlgebraicExpression` under Phase 4, `ImagePersistence` under Phase 33). The
rest are new findings of this sweep, and the first three are behavioral:

  - `ParametricCurve` — Java evaluates a segment through five separate bodies
    (`evaluateLinear`, `evaluateQuadratic`, `evaluateHermite`, `evaluateBezier`,
    `evaluateBspline`, 652 lines with their invariants); TypeScript (163 lines)
    replaces all five with one `basis`/`evalControls` pair. Phase 18 is
    reopened as parity-open.
  - `Camera.clipLineCohenSutherlandPlanes` — Java runs the outcode
    Cohen--Sutherland cycle over the six view-volume planes with
    `calculateOutcodeBits` and its trivial accept/reject tests; TypeScript
    clips sequentially against each plane instead, and has no outcode at all.
    The canonical-volume clipper is unaffected. Phase 24 is reopened as
    parity-open.
  - `PolygonTopologicalMerger` — the `compareKeys`, `compareTo`, `equals` and
    `hashCode` ordering contracts of its point and edge keys, and
    `splitAndAccumulateSegments`, have no counterpart; the TypeScript file is
    259 lines against 578. `PolygonProcessor.binaryTreeContainContour` is also
    absent. Both are Phase 25 polygon-clipper findings, distinct from the CSG
    operators that phase closed.

The remaining findings, by phase: `Entity.typeIsSupported` (2); the
`Matrix4x4d`, `Matrix4x4f` and `MatrixNxM` private helpers (3);
`Logger.processFatalError` (6); `TangibleInterfaceNetworkClient.connectAndListen`,
`extractNumbers`, `extractString` (13); `Image.clone`, `Image.positiveMod`,
`MediaEntity.clone`, `NormalMap.positiveMod`,
`RGBAImageCompressed.reportUnsupportedPixelAccess`, and the `readObject` /
`writeObject` serialization of `RGBImageUncompressed` and
`RGBAImageUncompressed` with `pixelBaseIndex` (14);
`ComputationalGeometry.computeCohenSutherland2DCode` (15);
`Triangle.closestPointOnTriangle` (19); `TriangleMesh.appendTriangles`,
`appendVertices`, `calculateMinMaxPositions`, `doIntersectionInternal` and its
array-clone helpers, the min/max helpers of `QuadMesh`, `TriangleStripMesh` and
`TriangleMeshGroup`, `FunctionalExplicitSurface.init` and `coord`, and
`Md2Mesh.fillAniInfo` / `getAniInfo` (21);
`_InsertionBatchSchedule.log2Value` (25); `PersistenceElement.verifyLibrary` and
`extractExtensionFromFile` (30); `ImagePersistenceHelper.exportGIF` and
`Md2Persistence.loadImagefile` (33 and 36); and the `fromInt` decoders of the
two `ImageToSolidTexture*` enums (14 orphan set).

Java-serialization and `Cloneable` members are the one group that may close as
a documented runtime boundary rather than as code; every other item above is
missing behavior.

### 4. Unreachable TypeScript files — kept by decision of 2026-09-16

A reachability analysis over the 666 TypeScript sources of the three packages
and the test suite, rooted at each package `index.ts`, every `*.spec.ts` and
every test-suite program, found 45 files under `src/main` that nothing reaches.
They were briefly deleted on 2026-09-16 and restored the same day: **the user's
decision of record is that all 45 stay.** They are part of the `vitral` package
for completeness and for the applications that will be built on it, and they are
not to be treated as dead code by a later audit. This supersedes the audit's
"legacy TypeScript seed classes must be removed or explicitly classified"
finding, which is hereby classified rather than removed.

What the decision obliges: these files are held to the same literal-parity rule
as the rest of the port. Where a Java class of record exists, the TypeScript
file must track it 1:1; no test covers any of them today, and none is invented
for them, so any parity claim about them must come from a source comparison or a
reference driver, never from a green test run.

Two groups, with different parity anchors.

**Group A — 29 files with no Java class at their own path.** They carry no
inventory entry and no Java source of record, so there is nothing to diff them
against today; the obligation is prospective, to be honored if and when the
Java base grows a counterpart:

| Package | Files | Nearest Java classes |
|---|---|---|
| `common/linealAlgebra` | `Vector2D`, `Vector3D`, `Vector4D`, `Matrix2x2`, `Matrix4x4`, `Numeric`, `Ray`, `CoordinateAxis`, `CoordinateSystem`, `Jacobian`, `package-info` | The current Java package holds `Vector3Dd`/`Vector3Df` and their siblings, all ported and reachable; Java's `Ray` is `environment.geometry.element.Ray`, ported there. The C++ port has the same `*d`/`*f` split and no counterpart either |
| `common/dataStructures` | `CircularList`, `CircularListBase`, `CircularListBaseIterator`, `CircularListIterator`, `CircularListLink`, `CircularListNode`, `KDTree`, `KDTreeNode`, `BalancedKDTreeNode`, `KDQuery`, `LookUpTable`, `LookUpEntity`, `LookUpBehaviors`, `package-info` | The Java package holds `CircularDoubleLinkedList`, `NAryTree`, `BinaryTreeNode` and the `ArrayListOf*` buffers, all ported |
| `common` | `Random48`, `color/Cie`, `package-info` | None in Java. `color/Cie` was realigned on 2026-11-26 with the C++ `common/color/Cie` (an immutable instance per set of primaries, no longer a static singleton) and is now exported and covered by `CieTest`, whose expected values come from a C++ driver. `Random48` documents itself as the POSIX `rand48` sequence of the C++ port, which is where a future parity check for it must come from; the C++ tree has no such file today |
| `environment/material` | `RendererConfigurationBehavior` | `RendererConfiguration`, ported. Note at the code: this module patches `compareTo`, `getUseVertexColors` and `toString` onto the prototype, and the ported class already declares all three, so importing it would install a second implementation of existing members. It is kept unimported |

All 29 entered the tree in the initial TypeScript port commits of 2026-09-09
(`b0388e7b`, `5b9677fe`).

**Group B — 16 `java.*` runtime-compatibility files.** Ten are named entries of
the Phase 1 inventory, which the port owes whether or not a Vitral class uses
them yet; six are not named by any inventory but belong to the JDK surface that
the `java/` layer exists to provide:

| File | Anchor |
|---|---|
| `java/io/Serial`, `java/io/Serializable` | Phase 1 inventory; the serialization marker surface Java classes declare |
| `java/lang/Deprecated`, `java/lang/Override`, `java/lang/SuppressWarnings`, `java/lang/FunctionalInterface` | Phase 1 inventory; annotation shims a literal port writes at the declaration site |
| `java/util/Map`, `java/util/Map/Entry`, `java/util/Set` | Phase 1 inventory; the collection interfaces the ported containers implement |
| `java/net/http/WebSocket/Listener` | Phase 1 inventory; the nested listener contract of the tangible-interface client |
| `java/io/PrintStream`, `java/lang/System`, `java/lang/ProcessBuilder`, `java/util/Formatter`, `java/util/HashMapEntry`, `java/lang/Serial` | JDK surface, not named by any inventory. `java/lang/Serial` duplicates `java/io/Serial`, the only package the JDK declares it in; that duplicate is the one open bookkeeping item of this group |

Phase 46 must therefore audit these 45 files for literal parity like any other
file, and must not propose deleting them.

### 5. What closure now requires

With the test suite frozen, the remaining 1:1 obligations are: the 81 unported
files of §1, the 31 method gaps of §3, the audit items
still open for Phases 3, 21, 22 and 25, and the phase gates that were never run
for the phases advanced out of order (29--45). No new inventory was discovered
by this sweep other than `io.FileLocator`.

## Offline Example Programs

The September example sections record the original migration and comparisons.
The eight browser example modules and five Node offline programs remain in the
tree. The 2026-09-16 scope decision keeps that example set as the accepted
minimum. The Scene Editor was added separately and is covered below.

The Java offline examples under `java/testsuite/OfflineExamples` are migrated to
`typescript/testsuite/OfflineExamples` as minimal Node console projects, one
directory per Java project, each depending on `@vitral/base` and `@vitral/fs`
as packages and carrying the same `run*.sh` entry points as its Java
counterpart. They are consumers, not workspaces: the root build does not
include them, and each is built by its own `run*.sh`.

Their purpose is executable parity evidence. Each example must produce, for the
same arguments, the same output as the Java program built from `java/base`
(plus `java/awt` where the Java testsuite classpath includes it).

Migrated so far:

  - `AlgebraicExpressionExample` — 2026-09-12. Console output matches Java for
    no-argument, multi-argument, right-associative power, and named-constant
    cases. Two divergences remain, both inside the already-ported
    `AlgebraicExpression` and not in the example: Java folds a negative literal
    into a constant (`(-4.00)`) where the port emits a unary node
    (`(-(4.00))`), and parse errors carry different exception text. The cause is
    that `AlgebraicExpression.ts` (86 lines) is a regex plus precedence-climbing
    rewrite rather than a port of the Java `StreamTokenizer` recursive-descent
    parser (308 lines); it is recorded here as a Phase 4 literal-parity finding.
  - `Rasterizer2DExample` — 2026-09-12. `LineTest`, `PolygonTest`, and
    `SmoothPolygonTest` write 640x480 PNG files whose decoded pixels are
    identical to the Java reference, 0 differing pixels each. It required the
    out-of-phase advances recorded under Phases 33 and 40.
  - `PolygonTriangulation` — 2026-09-12. Full 1:1 port of the Java project,
    including its `io`, `model`, `options`, and `render` application packages
    and both `run.sh` and `runAll.sh` entry points. It is the public oracle for
    the monotone decomposition triangulator: run over all 60 fixtures in
    `etc/polygons`, the TypeScript program produces byte-for-byte identical
    console output (triangle count, indices, and order) and 1024x512 PNG panels
    whose decoded pixels match the Java reference exactly, 0 differing pixels in
    all 60 cases. No change to `MonotoneDecompositionTriangulator` or its
    `monotoneDecomposition` backend was needed. Three JDK facilities with no
    Vitral counterpart are mapped to their Node equivalents, as the Java program
    itself uses the JDK directly: `Files.readAllLines` to `node:fs`
    `readFileSync`, `Path.of(...).getFileName()` to `node:path` `basename`, and
    `java.util.regex` to a `RegExp`. Numeric parsing goes through the ported
    `Integer.parseInt` and `Double.parseDouble` so that malformed input fails
    the Java way, and the `(byte)` palette literals go through
    `VSDK.unsigned8BitInteger2signedByte`.
  - `RaytracingOfflineExample` — 2026-09-12. Full 1:1 port of the Java
    project (`RaytracerSimple`, `CommandOptionsProcessor`, `ImageExporter`,
    `RaytracerExecutor`, `RaytracerSerialExecutor`, `RaytracerParallelExecutor`)
    plus `run.sh`. (Update 2026-09-23: the three `Raytracer*Executor` classes
    were removed in Java, C++ and TypeScript alike; the example now calls
    `SimpleRaytracer` or the new base class `ParallelRaytracer`, which the
    `ShadersExample`s and the Java `SceneEditorApplication` share. Images and
    console output are still identical across the three ports.) For `etc/geometry/mitscenes/balls.ray` at 1920x1080 the
    TypeScript program writes a PPM that is byte-for-byte identical to the Java
    one, and prints byte-for-byte identical console output, in both the
    single-threaded and the parallel mode; the Java serial, Java parallel,
    TypeScript serial and TypeScript parallel images are all the same file. It
    required the out-of-phase advances recorded under Phases 1, 12, 36, 39 and
    40. Three Java branches are unported because raster image *import* does not
    exist in TypeScript yet: the scene reader's `texture`, `bumpmap` and
    `backgroundcubemap` commands, and `ImageExporter`'s `.jpg` output; each
    raises a message naming the missing capability instead of degrading
    silently.

## WebGL Example Programs

Historical migration and verification notes. Camera-control shortcuts and
runtime adaptations recorded here apply to the named example and revision.

The Java interactive examples under `java/testsuite/Jogl4Examples` are migrated
into the single Angular frontend application
`typescript/testsuite/VitralWebTestsuiteContainer`: one Angular module per Java
project, under `src/WebGLExamples/<JavaProjectName>/`, reached from the
container's explorer tree. This is the strategy of record for that directory,
and it differs from the one-project-per-example shape used for the offline
console examples, for three reasons:

  - A Java example is a `JFrame` that owns a JOGL `GLCanvas`; a browser has no
    windows to own. The container application is the window manager, its
    explorer tree is the program launcher, and each module owns one `<canvas>`.
  - Every example needs the same served `etc/` tree (GLSL shaders, images,
    textures, models). The container publishes it once, through
    `scripts/sync-etc.sh` into `public/etc`, and every module reaches those
    resources by URL where Java reaches them by relative file path.
  - A browser reaches shaders and image resources over `fetch`, so GPU resource
    setup is asynchronous. Sharing one application shell keeps that
    asynchronous boundary in one place instead of repeating a build per
    example.

Mapping rules, applied to every module:

  - The `GLEventListener` callbacks map onto the Angular lifecycle: `init` onto
    `ngAfterViewInit`, `reshape` onto a `ResizeObserver`, `display` onto the
    module's frame method, and `dispose` onto `ngOnDestroy`. Closing the Java
    `JFrame` (and its `System.exit(0)`) maps onto a `deactivate` output that
    returns the container to its explorer, which is also what `KEY_ESC` does.
  - AWT mouse, wheel and key events reach the Vitral event shape through
    `vsdk.toolkit.gui.WebSystem`, the Web counterpart of `AwtSystem`.
  - Rendering goes exclusively through the `vsdk.toolkit.render.webgl`
    counterparts of the `vsdk.toolkit.render.jogl` classes the Java example
    uses. A Java example that reaches a JOGL renderer with no WebGL counterpart
    yet is blocked on porting that renderer, never on an inline substitute
    inside the module.
  - A frame must not `await` a network read. A WebGL drawing buffer is
    presented and cleared at a browser task boundary unless
    `preserveDrawingBuffer` is set, so a frame that compiled a shader on demand
    would lose everything drawn before the boundary. Each renderer that needs a
    GLSL source therefore exposes an asynchronous `prepare(gl)`, which the
    module calls where a JOGL program does its `init(GLAutoDrawable)` work, and
    the frames that follow are free of real awaits. This cost the first
    `ImageExample` frame its corridor and matrix gizmo until `prepare` was
    added, and it retroactively fixed the same latent defect in
    `CameraExample`.
  - The Java examples delegate camera interaction to a
    `vsdk.toolkit.gui.CameraController` (`CameraControllerAquynza` in every
    example migrated so far). That family belongs to the decision-gated Phase
    38 and is not ported, so the modules share
    `src/WebGLExamples/_shared/example-camera-interaction.ts`, which keeps the
    `processX(...)`-answers-whether-to-repaint shape of the Java
    `CameraController` contract. It is the one recorded behavioral divergence
    of every module: the initial camera pose and the drag/zoom mapping are not
    `CameraControllerAquynza`'s. Replacing it with the ported controller must
    be a change of construction only.

Migrated so far:

  - `CameraExample` — the first module, and the one that established the
    container, `@vitral/webgl`, and the mapping rules above. See the Phase 41
    partial advance.
  - `ImageExample` — 2026-09-12. This is the first example to exercise image
    handling, and specifically compressed images: it draws
    `etc/images/render.jpg`, decoded by the platform's own JPEG decoder into an
    `RGBImageUncompressed`, and `etc/textures/earth.dds`, read as DXT1 blocks
    that stay compressed all the way to the GPU, each of them twice — as a
    depth-biased quad in the world and as a HUD overlay. It required the
    out-of-phase advances recorded under Phases 33 and 41.

    Verified on 2026-09-12 against the Java program, built from `java/base` +
    `java/jogl4` and run under Xvfb with software Mesa: both programs draw the
    same corridor, the same two world quads and the same two HUD overlays, with
    the same images, the same image orientation, the same sizes in pixels
    (320x240 and 256x256), the same lower-left and upper-left HUD placement,
    and the same black letterboxing inside the 256x256 DXT1 earth texture. The
    camera pose differs, which is the recorded `CameraController` divergence
    above.

    The compressed path was verified byte for byte rather than visually. A Java
    reference driver reads a DDS file through `ImagePersistence.importRGB` and
    invokes `Jogl4RGBAImageCompressedRenderer.decompressToRGBA` reflectively;
    its TypeScript twin does the same through
    `WebGLRGBAImageCompressedRenderer`. Over four inputs the two agree on every
    line: `etc/textures/earth.dds` (256x256 DXT1, 16389 identical lines
    covering the header values and all 262144 decoded RGBA bytes) and three
    synthetic 64x64 DDS files carrying deterministic pseudo-random blocks with
    the `DXT1`, `DXT3` and `DXT5` FourCCs (1029 identical lines each), which is
    what exercises the DXT1 `c0 > c1` and transparent-black rules, the DXT3
    4-bit alpha nibbles, and the DXT5 interpolated alpha table and its 48-bit
    index word.

    Both GPU paths of the compressed renderer were exercised in a real
    browser, headless Chrome 147 over software Mesa: the `WEBGL_compressed_
    texture_s3tc` path, where the DXT1 blocks reach the GPU untouched, and the
    CPU decompression fallback, forced by hiding that extension from the page,
    which emits the Java warning and draws the same image. The one visible
    difference between them is mipmapping, which is the documented WebGL
    boundary: a compressed texture cannot have mipmaps generated for it, so
    that path minifies with `LINEAR` while the decoded fallback keeps Java's
    `LINEAR_MIPMAP_LINEAR`.

    One parity boundary remains open and is not claimable: `render.jpg` is
    decoded by the browser's JPEG decoder where Java uses `javax.imageio`, so
    byte-exact agreement on that image is not expected and was not measured.
    The transfer of the decoded samples into the Vitral image is a port of
    `AwtRGBImageUncompressedRenderer.importFromAwtBufferedImage`, traversal
    order and byte masking included; only the decoder differs.

  - `MeshExample` — 2026-09-12. The first module whose scene is not a fixture
    compiled into the program: it reads a mesh the user names, which is what
    makes it the module that settles how a browser program reaches geometry.

    Java's `MeshExample(String fileName)` takes the name from the command line
    and, when none was given, from `awt.FileSelectorDialog`, a `JFileChooser`
    over `etc/geometry` narrowed by one `awt.ObjectFilter` per format. A
    browser has neither a command line nor a file system, so the resource is
    always named by URL and the chooser is a modal dialog in the container,
    reached by right-clicking `MeshExample` in the explorer tree — and by
    left-clicking it the first time, which is Java's "no file named yet"
    branch. The dialog lists the same six formats in the same order with the
    same descriptions, marking `obj` as the one with a reader, and carries a
    second section for the tangible-interface service described below. The
    selection reaches the module as inputs, and entering another mesh URL
    reloads the scene, which is what re-running the Java program with another
    file does.

    Reading it is `WebEnvironmentPersistence` in `@vitral/webgl`, the URL
    counterpart of `EnvironmentPersistence` in `@vitral/fs`, over the one
    `ReaderObj`; see the Phase 36 record for the relocation and the seam that
    made a single reader serve both runtimes. Evidence: `cow.obj`,
    `dumptruck.obj` and `skull.obj` imported through the URL path and through
    the `java.io.File` path produce identical scenes — same body count, same
    per-mesh vertex and index counts, same coordinate sums.

    `render.Jogl4DebuggerRenderer` is ported as the module's
    `WebGLDebuggerRenderer`, drawing through `WebGLRendererConfigurationShader
    Selector`, `WebGLImageRenderer`, `WebGLCameraRenderer` and the new
    `WebGLLightRenderer` and `WebGLRayGizmoRenderer`. `model.MeshModel`,
    `gui.MeshMouseInteractionTechniques`, `gui.MeshKeyboardInteractionTechniques`,
    `gui.TangibleInterfaceInteractionTechniques` and
    `animation.AnimationController` are ported beside it;
    `options.CommandLineOptions` has no counterpart as a class, because a page
    has no command line; what it reads from `-tangibleServer` is asked for in
    the same explorer dialog, which therefore carries both of the Java
    program's startup inputs. That section adds one control Java has no
    counterpart for, a connect checkbox that starts **unchecked**: Java's
    `main` always builds a `TangibleInterfaceNetworkClient` and calls `run()`,
    which costs nothing when no server answers because the attempt happens on
    its own thread, whereas a browser `WebSocket` to an absent server logs a
    failed-connection error on the page's console every time the module starts.
    Unchecking the box on a live module closes the connection and a new URL
    replaces it, which is what makes the dialog reopenable rather than
    startup-only.

    Two recorded divergences, each marked at the code:

      - WebGL has no `glPolygonMode`, so the wireframe pass draws the three
        edges of every triangle as `LINES` over the geometry the surface pass
        filled, instead of asking the rasterizer to outline a filled
        primitive; `POLYGON_OFFSET_LINE` is equally absent, and the pass keeps
        the depth-func and depth-mask state Java also sets for it.
      - WebGL has no `glPointSize`: in GLSL ES the vertex shader must write
        `gl_PointSize` itself. `WebGLShaderPreprocessor` now adds that write
        and a `pointSizeLocal` uniform to every vertex shader that does not
        have one, falling back to OpenGL's own default of 1.0 when the uniform
        is unset, and the points pass sets it where Java calls `glPointSize`.

    Java's `AnimationEventGenerator` thread becomes the page's own timer at
    the same one-second period the Java listener filters down to; the
    animation package itself was ported later the same day, under Phase 42,
    and this module was not rewritten on top of it, for the reason recorded
    under `SolidTextureExample`.

    The ray gizmo is drawn: `display` acquires the snapshot and then calls
    `WebGLRayGizmoRenderer`, exactly as Java does, over the arrow, sphere and
    min/max renderers added in the Phase 41 fourth partial advance. It shows
    nothing while no tangible-interface server is publishing, which is Java's
    behavior too: `RayGizmo.update()` hides the gizmo after
    `DEFAULT_DISABLE_TIME` (2 s) without data, and the animation tick calls it
    once a second.

    Rendering evidence covers the mesh, the quality toggles and the light
    gizmo, all captured in headless Chrome over software Mesa; the ray-gizmo
    path is built and type-checked but its rendering has not been measured,
    since exercising it needs a live `TangibleInterfaceMarkersDetectorServer`.

  - `SolidTextureExample` — 2026-09-13. The module that exercises 3D textures
    and Perlin noise, and the first with a heads-up display. The mesh it reads
    is only the shape a procedurally generated volume is carved into: a cube of
    `size^3` RGB samples uploaded as a `TEXTURE_3D`, with the body's own
    bounding box normalizing each object-space position into a texture
    coordinate, so the texture looks carved rather than wrapped. `[4]` and `[5]`
    step through the sixteen textures, `[2]` and `[3]` halve and double the
    side, `[1]` switches to showing the volume's slices as a stack of quads,
    `[r]` runs the rotation animation and `[h]` hides the HUD.

    It reaches its geometry the way `MeshExample` does and for the same reason,
    through its own copy of the chooser — Java duplicates
    `awt.FileSelectorDialog` and `options.CommandLineOptions` into this project
    with one string changed, and the container duplicates the Angular dialog to
    match, one module per Java project. Both of the Java program's startup
    inputs are in it: the mesh URL and the `-tangibleServer` value with its
    connect checkbox, which starts unchecked for the reason recorded under
    `MeshExample`. This program listens for two markers rather than one:
    `rayCube1` drives the ray gizmo and `cuttingPlane1` drives an infinite-plane
    gizmo that clips both drawing paths.

    `model.SolidTextureModel`, `model.OperationMode`,
    `model.SolidTextureExampleColorNames`, `gui.SolidTextureMouseInteraction
    Techniques`, `gui.SolidTextureKeyboardInteractionTechniques`,
    `gui.TangibleInterfaceInteractionTechniques`, `animation.AnimationController`
    and the three `render` classes are ported. The renderers draw through the
    new `WebGLSolidTextureRenderer` and `WebGLInfinitePlaneGizmoRenderer` of the
    Phase 41 fifth partial advance, over the solid-texture library of the Phase
    14, 29 and 38 advances recorded below.

    Parity evidence is byte-level, not visual, and covers the whole generation
    chain. A Java reference driver builds `model.SolidTextureModel` against
    `java/base`, steps through all sixteen textures at side 16 and prints the
    length, the byte sum and an FNV-1a digest of `getSolidTextureVolumeRgb8()`;
    its TypeScript twin does the same over the ported model. The two agree on
    every one of the sixteen lines, which exercises `ProceduralNoise` and its
    permutation table, `TextureUtils`, every `ColorTextureFixture` function,
    `ImageTexture` with its planar mapping and both interpolation modes,
    `RGBAColorPalette`, `IndexedColorImageHDRUncompressed`, and the 16-bit to
    8-bit channel reduction.

    Rendering was then exercised in headless Chrome over software Mesa: the cow
    carved out of the checker volume with the HUD showing its four lines, `[5]`
    switching to the noise-driven `CHECKER_TEXTURE_TEXTURE`, `[1]` switching to
    the slice stack, `[h]` hiding the HUD, and `[3]` rebuilding the volume at
    side 64 with visibly finer detail. The gizmo paths are built and
    type-checked but their rendering has not been measured, since exercising
    them needs a live `TangibleInterfaceMarkersDetectorServer`.

    Four divergences are recorded, each marked at the code:

      - GLSL ES 3.00 has no `gl_ClipDistance` and WebGL has no
        `GL_CLIP_DISTANCE0` enable, so `WebGLShaderPreprocessor` carries the
        clip distance across as an ordinary varying and discards on its sign in
        the fragment shader. The result is per-fragment rather than
        per-primitive clipping, which for a plane cut through a solid is the
        same image; what it costs is early-depth rejection. The translation is
        unconditional on both stages, because a shader is preprocessed without
        knowing what it will be linked against and a fragment `in` with no
        matching vertex `out` is a link error.
      - GLSL ES 3.00 gives `sampler2D` a default precision but gives
        `sampler3D` none, so `solidTexturePixelShader.glsl` fails to compile
        with "No precision specified" as it stands. The preprocessor now states
        `precision highp sampler3D;` in both stage headers. OpenGL 4.1 core has
        no precision qualifiers at all, which is why no VitralSDK shader carries
        this.
      - `Graphics2D` on a `BufferedImage` becomes a 2D canvas context: the HUD's
        `fillRect` and `fillText` calls sit on the Java baselines with the same
        bold 18-pixel sans-serif face, and one `getImageData` replaces the
        `getRGB` loop. Text metrics are the platform's in both cases, so the
        glyphs are the host's, not Java's.
      - `render.Jogl4DebuggerRenderer` carries a private `drawSimpleBody` path,
        with its uniform, material and mesh-upload helpers and the vertex array
        and three buffers `init` creates for them, that nothing calls: the file
        was derived from `MeshExample`'s renderer and kept that path while
        `display` goes through `Jogl4SolidTextureRenderer` instead. It is not
        carried over. It has no behavior to preserve, and porting it would mean
        inventing WebGL substitutes for `glPolygonMode` and `glPointSize` in a
        path that never runs; the live counterpart of those passes is
        `MeshExample`'s own `WebGLDebuggerRenderer`.

    Java's twenty-four-tick-a-second `AnimationEventGenerator` thread becomes
    the page's own timer at the same rate; the animation package itself was
    ported later the same day, under Phase 42, and this module was not
    rewritten on top of it because its Java listener drives repaints and gizmo
    aging rather than a mesh. One cost of the port is Java's too and not a defect:
    rebuilding the volume is O(size^3) evaluations of multi-octave noise, and a
    browser runs it on the one thread it draws on, so the page stops responding
    for the duration where the Java program's window merely stops redrawing.

  - `MD2Example` — 2026-09-13. The module that exercises Quake II MD2 reading
    and playback (`Md2MeshExample`). An MD2 resource is a stack of vertex
    frames grouped into named animations plus an OpenGL command list of
    triangle strips and fans that index into them; every frame the renderer
    interpolates between the current vertex frame and the next by the fraction
    of a frame period the mesh's elapsed time has reached. `[1]` and `[2]` step
    back and forward through the model's sixteen animations, `[3]` and `[4]`
    choose whether the XYZ keys move the camera or one of the three lights, and
    the HUD names both.

    `model.DebuggerModel`, `io.DebuggerReader`, `gui.KeyboardInteraction
    Techniques`, `gui.MouseInteractionTechniques`,
    `animation.DebuggerAnimationController` and both `render` classes are
    ported. This is the first module whose animation is the Java one rather
    than a stand-in: Phase 42 is closed by this work, so
    `animation.DebuggerAnimationController` builds a real
    `AnimationEventGenerator` with a real `Md2AnimationListener` on it, as Java
    does. It draws through the new `WebGLMd2MeshRenderer` of the Phase 41 sixth
    partial advance, over the `Md2Persistence` and `WebMd2Persistence` of the
    Phase 36 third advance.

    Java's `Md2MeshExample.init()` hard-codes `etc/md2/samourai.md2` and
    `etc/md2/samourai.jpg` relative to the program's own directory, and the
    file name its constructor takes from the command line is ignored, so this
    project has no `awt.FileSelectorDialog` of its own. A browser has no `etc`
    directory beside the program, so both resources are named by URL, and the
    modal dialog reached by right-clicking `MD2Example` in the explorer tree is
    where they are given, with the two Java paths as its defaults. It has no
    tangible-interface section, because this program opens no such connection.

    Parity evidence is byte-level and covers the whole reader. A Java reference
    driver reads `samourai.md2` through `Md2Persistence` against `java/base`
    and prints the sixteen header words, the skin names, the frame names, the
    per-animation ranges and an FNV-1a digest of the texture coordinates, the
    triangle table, all 198 vertex frames, all 198 normal-index frames and the
    four OpenGL command tables; its TypeScript twin does the same over the
    ported reader. The two agree on every line. Closing that diff needed one
    literal correction: Java evaluates `coord * scale[k] + translate[k]` in
    `float`, rounding after every operation, and TypeScript's only number is a
    `double`, so `Math.fround` puts the intermediate rounding back — without
    it the frame vertices differ from Java's in their last bits.

    Rendering was then exercised in headless Chrome over software SwiftShader:
    the textured samurai in its STAND animation with the HUD reading
    `1/16 STAND` and the three light billboards, `[2]` twice and `[1]` once
    walking the animation list with the HUD following, `[4]` moving the XYZ
    selection to `Light 1`, `F2` drawing the wireframe pass over the animated
    mesh, and a reopened dialog pointed at a missing skin producing Java's
    64x64 test pattern on the model.

    Three divergences are recorded, each marked at the code:

      - `Md2Persistence` walks a `RandomAccessFile` with `seek` and `read`. A
        browser has no file system, so the reader is split the way the `.obj`
        reader already is: the parser is platform-neutral in `@vitral/base` and
        takes the resource's bytes, over a cursor that plays the part of the
        file pointer, and `WebMd2Persistence` in `@vitral/webgl` fetches the
        model and decodes the skin. Java's `loadImagefile` goes to the browser
        half, because only that half knows the resource name: an unnamed skin
        answers null and leaves the mesh's skin slot empty, and a named one
        that cannot be read prints to the console and answers the test pattern.
      - `glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)`, `POLYGON_OFFSET_LINE` and
        `glPointSize` have no WebGL counterpart, so the wireframe pass draws
        every triangle's three edges as `LINES` over a second buffer set with
        the depth state Java also sets for that pass, and the point size
        travels as the `pointSizeLocal` uniform. This is the substitution
        `MeshExample`'s renderer already makes.
      - `Graphics2D` on a `BufferedImage` becomes a 2D canvas context, as in
        `SolidTextureExample`: `fillText` on the Java baseline with the same
        bold 20-pixel sans-serif face, `measureText` for the right-aligned
        second line, and one `getImageData` replacing the `getRGB` loop.

    The camera controller is Java's `CameraControllerOrbiter` with
    `setDeltaMovement(10)`, fed DOM events by the container's
    `ExampleCameraInteraction` (since 2026-11-28; until then a stand-in that
    imposed its own default view, and which this module had to work around).

- `ShadersExample` — `typescript/testsuite/VitralWebTestsuiteContainer/src/
    WebGLExamples/ShadersExample`, ported 2026-09-13.

    The module that exercises the shader library in full and puts the GPU path
    beside the CPU raytracer on one scene. A unit sphere carrying the
    `miniearth` texture and the `earth.bw` bump map is drawn either by the
    GLSL programs of `WebGLSphereRenderer` or by `SimpleRaytracer`, and `[.]`
    switches between them. Every shading model the shader selector can reach is
    reachable from the keyboard: `[g]`, `[p]` and `[n]` name Gouraud, Phong and
    no-lighting directly, `[F7]` walks the whole cycle including
    Cook-Torrance, `[t]` and `[b]` toggle the texture and the bump map, `[m]`
    walks the microfacet material list, `[q]`/`[Q]`/`[w]`/`[W]` change the mesh
    resolution, `[l]`/`[k]`/`[j]`/`[u]`/`[9]`/`[0]` move the light, `[r]` and
    the space bar start the sphere and light animations, and `[h]` shows or
    hides the HUD.

    `model.ShadersModel`, `model.ShaderOperationMode`, `gui.Animation`,
    `gui.ShadersKeyboardInteractionTechniques`,
    `gui.ShadersMouseInteractionTechniques`, `render.JogHudRenderer` and
    `render.SoftwareRaycaster` are ported. `options.CommandLineOptions`,
    `OfflineControl` and `render.OpenGlOfflineSphereRenderer` are not: they are
    the offline batch path, which the interactive `main` never reaches — it
    ignores its command line entirely — and which has no place in a module
    inside the container. It draws through the existing `WebGLSphereRenderer`
    and `WebGLRendererConfigurationShaderSelector`; no new GPU renderer was
    needed. It did need three library advances: `ImagePersistenceSGI` and
    `ImagePersistence.importIndexedColor` under Phase 33,
    `MicroFacetedMaterial.fromCsvText` under Phase 16, and the first browser
    use of the Phase 40 `render.raytracing` family.

    Java's `ShadersModel.initializeDefaults` hard-codes three paths relative to
    the program's own directory — `etc/textures/miniearth.png`,
    `etc/bumpmaps/earth.bw` and `etc/materials/microFacetMAterials.csv` — and
    this project has no file selector of its own. A browser has no `etc`
    directory beside the program, so all three are named by URL, and the modal
    dialog reached by right-clicking `ShadersExample` in the explorer tree is
    where they are given, with the three Java paths as its defaults.

    The shader question this module was ported to answer was checked first and
    directly: all twenty GLSL files of `etc/glslShaders` were put through
    `WebGLShaderPreprocessor` and compiled in a real WebGL2 context, and all
    eleven programs the library builds from them — the nine of the shader
    selector plus the line and solid-texture pairs — were linked. Twenty of
    twenty compile and eleven of eleven link under GLSL ES 3.00, with the
    microfacet uniforms (`cookRoughness`, `cookAlpha`, `cookKd`, `cookKs`,
    `cookF0`) live in the linked Cook-Torrance programs. No OpenGL 4.1 shader
    of this tree needed rewriting, and the preprocessor needed no new rule.

    Parity evidence is byte-level and covers the whole CPU chain. A Java
    reference driver builds, against `java/base`, exactly what `ShadersModel`
    and `SoftwareRaycaster.buildSceneSnapshot` build — the same camera, sphere,
    light, materials, texture and normal map, with the `--rotation 35
    --light-rotation 20` of `runOffline.sh` — and prints the SGI bump map's
    size, digest and diagonal sample, the normal map's digest, every field of
    three microfacet materials, the texture's digest, and an FNV-1a digest of
    the raytraced frame under both Phong-textured-bump and Cook-Torrance
    shading. The TypeScript twin agrees on all twelve lines.

    Rendering was then exercised in headless Chrome over software SwiftShader:
    the textured, bump-mapped sphere under Phong with the HUD reading Java's
    `Number of meridians: 100 Number of parallels: 50 Number of triangles:
    9800` and `Mode [.]: GPU`; `[g]` and `[n]` changing the shading; `[F7]`
    reaching Cook-Torrance with the HUD adding `SimpleMaterial [m]: Copper`
    and `[m]` walking on to `Iron`; shifted `[Q]` and `[W]` raising the counts
    to 103 and 53 with the triangle count following Java's
    `(parallels - 1) * meridians * 2`; and `[.]` producing the raytraced sphere
    with the HUD reading `Raytracing` and `Mode [.]: CPU`, framed identically
    to the GPU one.

    Three divergences are recorded, each marked at the code:

      - Java's `SoftwareRaycaster` runs one JVM thread per processor over a
        shared `RGBImageUncompressed`, each thread polling a
        `ConcurrentLinkedQueue` of tiles. A browser has no threads and its Web
        Workers share no heap, so the workers are `BrowserWorkerExecutor`s
        handed the next tile by the owner — the queue is the same, the polling
        has moved to the side that can see all the workers — and each returns
        the bytes of its band, which the owner splices in. The pixels are
        identical, by the same property that lets Java's threads share the
        image: a tile only reads the scene and only writes its own rows. The
        pool is also capped at eight, because a Web Worker is a module
        instantiation as well as a thread; the host this was verified on
        reports 72 processors, and 72 workers never produced a frame. The
        number decides only how `RasterTileGenerator` bands the frame, so no
        pixel depends on it.
      - `ImagePersistenceSGI` and the microfacet CSV both read files. Both are
        split at the same seam the other readers use: the parse is
        platform-neutral in `@vitral/base` and takes the resource's bytes or
        text, and the fetch is the browser's half. See the Phase 33 and Phase
        16 records.
      - `Graphics2D` on a `BufferedImage` becomes a 2D canvas context, as in
        `SolidTextureExample` and `MD2Example`: `fillText` on Java's two
        baselines with the same monospaced 16-point face, `measureText` for the
        two right-aligned strings, and one `getImageData` replacing the
        `getRGB` loop.

    The camera controller is Java's `CameraControllerOrbiter`, fed DOM events
    by the container's `ExampleCameraInteraction` (since 2026-11-28). One
    browser-only step has no Java counterpart: Java drives both animations from one `javax.swing.Timer`, whose repaint
    coalescing the component's own repaint flag already provides; the host
    timer takes its place at the same 30-frames-a-second period, with the same
    `Animation.tick` and the same elapsed-time light step.

- `PolygonClippingExample` — `typescript/testsuite/VitralWebTestsuiteContainer/src/
    WebGLExamples/PolygonClippingExample`, ported 2026-09-15.

    The module that exercises the Weiler--Atherton clipper. Forty-three
    fixtures, each naming two contour files of `etc/polygons`, are walked with
    `[1]` and `[2]`; `[3]` cycles the four boolean operations; and the result
    is laid out on three panels — the two annotated input polygons in place,
    the primary result on a panel translated in -Z and the secondary one on a
    panel translated in +X — with the vertices the clipper paired drawn in
    their own colour. `[C]`, `[S]`, `[I]`, `[O]` and `[P]` switch the four
    polygons and the intersection points, `[G]` fills or unfills, `[T]` swaps
    the surface tessellation, the space bar shows or hides the reference frame,
    `[F]` goes fullscreen and `[H]` takes a snapshot.

    `model.PolygonClippingDebuggerModel`, `model.PolygonClippingFixtures`,
    `model.PolygonClippingTestCase`, `model.PolygonClippingOperation`,
    `model.PolygonSurfaceTessellationMode`,
    `model.PolygonClippingModelingTools`,
    `gui.PolygonClippingKeyboardInteractionTechniques`,
    `gui.PolygonClippingMouseInteractionTechniques`,
    `render.JoglPolygonClippingRenderer`,
    `render.JoglPolygonClippingHudRenderer` and `render.JoglTriangularRenderer`
    are ported. `options.CommandLineOptions` and
    `render.JoglPolygonClippingOfflineRenderer` are not, for the reason
    `ShadersExample` records: they are the offline batch path, and the
    interactive `main` ignores its command line entirely.

    It needed one library advance, recorded under Phase 41:
    `WebGLPolygon2DRenderer`, the counterpart of
    `vsdk.toolkit.render.jogl.Jogl4Polygon2DRenderer`. That is also where this
    program's one real incompatibility lies, and it is the only one the
    migration has met so far that could not be answered by a translation:

      - Java fills a polygon surface with the GLU tessellator,
        `GLU.gluNewTess()`. GLU is a companion library of desktop OpenGL and a
        browser has neither it nor any way to reach one. What
        `Jogl4Polygon2DRenderer` asks of it, though, is narrow: it sets no
        winding rule, so the tessellator runs under its default,
        `GLU_TESS_WINDING_ODD`, and it keeps nothing but the triangle
        coordinates — no vertex identity survives the callback. So
        `_Polygon2DOddWindingTessellator` computes that region directly and
        exactly: every pair of crossing edges is split at the crossing, the
        distinct endpoint ordinates become the bands of a sweep, and inside a
        band the crossings are paired two by two, which is the odd rule. The
        triangles are not GLU's triangles — a sweep that cuts on every ordinate
        makes more, and thinner, ones — but the filled region is the same
        region, which is all this pass can show: it is drawn flat in one
        colour, and the mode that reveals a triangulation is
        `render.JoglTriangularRenderer`'s monotone decomposition, ported as it
        stands.
      - No shader needed rewriting. The two programs this module links,
        `line` and `constant`, are the ones the Phase 41 record already reports
        compiling and linking under `WebGL GLSL ES 3.00`, and the eight-pixel
        point size reaches the vertex shader through the `pointSizeLocal`
        uniform `WebGLShaderPreprocessor` injects, as in every other module
        that draws points.

    Three browser-only steps have no Java counterpart.
    `PolygonClippingModelingTools.rebuildScene` reads its two contour files
    every time it runs, which is at startup and on every `[1]`, `[2]` and `[3]`
    keystroke; a frame must not wait on a network read, so `io.
    PolygonClippingReader` fetches each of the fifty-eight files the fixture
    table names once, when the module opens, and `rebuildScene` then stays
    synchronous, as Java's is. `toggleFullscreenMode`, which in Java disposes
    the `JFrame` and rebuilds it undecorated on the default screen device with
    a separate path for macOS, asks the browser for its fullscreen element
    instead, which is why the three window fields of the Java model have no
    counterpart. And the `[H]` snapshot, which Java writes beside the program
    with `ImagePersistence.exportPNG`, is handed to the browser as a download
    under the same `frameNNNN.png` name; its `readPixels` is issued where Java
    issues it, inside the frame, because a drawing buffer is cleared at the end
    of the task that drew it.

    Verified against the Java program on the same host: the offline renderer of
    the Java project and the module were put side by side on
    `WINDOW_WITH_ISLANDS` and `SUBJECT_WITH_BANDS`, and the HUD lines, the loop
    counts, the intersection count and the geometry agree. A parity driver over
    `java/base` and `typescript/base/dist` was also run on five fixtures,
    printing every vertex of both results of `clipPolygons`; the two outputs
    are identical. One difference is visible and is the Java side's: on
    `WINDOW_WITH_ISLANDS` the GLU fill bridges the first two contours of the
    secondary result into one blob, where the wires of that same result — and
    the port — show the two separate triangles the clipper actually produced.
    The tessellator was checked independently as well, by comparing the area of
    the triangles it emits against an odd-rule ray-casting grid over thirty-two
    polygons; the largest disagreement is 0.12 %, which is the grid's own
    boundary-cell error.

- `PolyhedralBoundedSolidExample` — `typescript/testsuite/VitralWebTestsuiteContainer/src/
    WebGLExamples/PolyhedralBoundedSolidExample`, ported 2026-09-15 in two
    stages.

    The half-edge solid debugger. The selected model is built with the Euler
    operators, the primitives, the sweeps, the gluing of [MANT1988].12.4, the
    boolean set operations, the plane split, an extruded glyph or a STEP
    import. It is shown with:

      - its faces shaded under the configured shading type;
      - its edges, points, normals and bounds on the quality keys;
      - its two lights as billboards;
      - the boundary loops of the selected face (or of all of them, `[1]`/`[2]`)
        as curved arrows, with the face filled in red;
      - the vertex ids on `[v]`;
      - the edge-visibility debug view on `[0]`/`[9]`;
      - the Appel hidden-line result on `[8]`.

    `[c]` frames the selected face, `[3]`/`[4]` walk the models,
    `[q]`/`[Q]`/`[w]`/`[W]` change the subdivisions, `[r]` turns the solid,
    `[I]` prints the face and its neighbours, `[m]` exports STL, `[.]` takes a
    screenshot, `[g]` goes fullscreen and `[h]` hides the HUD.

    The CSG models (`CSG_DIRECT`, `CSG_OPERAND1_PARTIAL`,
    `CSG_OPERAND2_PARTIAL`, and `CSG_MOON_BLOCK`, which is outside the
    sequence) also take `[5]` for the operation, `[6]` for the sample pair,
    `[d]` for a traced run of `setOp`, and `[e]`/`[E]` for the Kurlander bowl
    motif; their two operands are drawn as insets in the lower corners, and
    the set-operation statistics issues are printed above the build error.

    **Stages.** The first stage left the models built by set operations
    (`HOLLOW_BOX`, `CSG_LAMP_SHELL`, `CSG_MOON_BLOCK`, `CSG_DIRECT`,
    `CSG_OPERAND1_PARTIAL`, `CSG_OPERAND2_PARTIAL`) and by splitting
    (`SPLIT_TEST_PART_1` to `_3`) out of the model sequence. The second stage
    ports their builders over the `geometricProcessing` operators of Phase 25
    (`PolyhedralBoundedSolidModeler.setOp` and `split`,
    `SimpleTestGeometryLibrary`, `CsgKurlanderBowlFixture`):
    `createHollowBox`, `buildCsgMoonBlock`, `createCsgLampShell`, `splitTest`,
    `buildCsgTest2`, `buildCsgTest4`, `buildCsgTest5`, `csgTest`,
    `createCsgOperands`, `createCsgHudPreviewOperands` and the progress
    messages, and restores Java's 28-entry sequence together with the four
    `PolyhedralBoundedSolidModelingTools` delegates. Java's overloads with a
    trailing motif index are one method with that index defaulting to 0, and
    `splitTest` hands the splitter the plain arrays its port takes where Java
    passes `ArrayList`s.

    Ported: `models.*`,
    `gui.CameraFaceFocusInteraction`, `gui.DebuggerKeyboardInteractionTechniques`,
    `gui.DebuggerMouseInteractionTechniques`,
    `animation.SolidAnimationController`, `render.Jogl4DebuggerRenderer`,
    `render.Jogl4DebuggerHudRenderer`, `PolyhedralBoundedSolidModelingTools`,
    and the interactive half of `PolyhedralBoundedSolidExample` with
    `InteractiveDebugger`. Not ported: `options.CommandLineOptions`,
    `render.Jogl4HeadlessRenderer` and `main`'s offline, motif-sweep and
    Appel-dump paths, for the reason `ShadersExample` records; and
    `render.Jogl4HudOperandsRenderer`, which nothing in the Java program
    constructs.

    It needed library advances recorded under Phases 36, 40 and 41: the STEP
    reader, the STL writer and `FontReader`; `HiddenLineRenderer`; and the
    WebGL polyhedral-solid renderer family with `WebGLSimpleMaterialRenderer`
    and `WebFontReader`. Two Java runtime adapters came with them:
    `java.util.IntegerKeyHashMap`, a `HashMap<Integer, V>` that iterates in the
    JVM's order, resize splits and tree bins included, because the STEP builder
    numbers vertices and orders edges by walking such maps; and
    `InputStream.readAllBytes`.

    Browser boundaries, each marked at the code:

      - Java tessellates faces with the GLU tessellator. The solid renderer
        needs triangles made of the face's own vertices, because smoothed
        normals are looked up by exact vertex position, so it triangulates with
        the ported `MonotoneDecompositionTriangulator` (odd-depth fill, like
        `GLU_TESS_WINDING_ODD`) in the face plane and winds every triangle with
        the face normal. The triangles differ from GLU's; the covered region,
        the vertices and the winding match.
      - AWT gives Java the glyph outline, from FreeType on this host.
        `WebFontReader` reads the TrueType tables and reproduces FreeType's
        unhinted 26.6 scaling and `FT_Outline_Decompose`, then runs
        `AwtFontReader.extractGlyph` statement by statement.
      - The STEP file and the font are fetched once when the module opens,
        from URLs chosen in the explorer dialog (defaults: the two Java paths),
        so the builder stays synchronous. `IMPORT_OR_FEATURED_OBJECT` always
        builds the featured object, because a page has no `/tmp/solid.bin`.
      - The two `HashMap`s keyed by half-edge in `_StepSolidBuilder` iterate by
        identity hash on a JVM, so their order is not fixed there; they keep
        insertion order.
      - Screenshots and STL files are handed to the browser as downloads named
        `screenshot.png` and `output.stl`. The PNG is encoded by the ported
        `ImagePersistence.exportPNG`, from pixels read as RGBA because that is
        the only format WebGL2 guarantees.
      - Every program a frame can reach is compiled in `init`, per the
        drawing-buffer rule. Before that change, the first frames lost their
        grey background.
      - The fullscreen request is handled as in the other modules. Since
        2026-11-28 the camera controller is Java's `CameraControllerOrbiter`,
        and `recenterOrbiterAfterModelChange` and `[c]` set its point of
        interest, as Java does.

    Parity was checked byte for byte with a reference driver. It compiles the
    Java example's `models` and `gui` classes and the three JOGL4 polyhedral
    renderers against `java/base`, and runs them next to an esbuild bundle of
    the TypeScript module over `base/dist` and `webgl/dist`. For each of the 19
    models the module builds, it prints:

      - the solid's full `toString`, with vertex, edge, face and half-edge ids;
      - `validateIntermediate`, and every vertex position as raw bits;
      - digests of the debug renderer's line buffers: edges, normals,
        degenerate faces, face arrows (all faces and face 2), and edge debug
        (all edges and edge 0);
      - the vertex-label groups and their visibility;
      - the Appel contour, visible and hidden lines under a 30-degree model
        rotation;
      - the STL bytes, or the rejection message;
      - the camera after `[c]` on face 2, and the `[I]` dump of that face.

    Both outputs are identical over 29,415 lines, including the 2,186 vertices
    and 3,396 edges of `kurlanderBowl.step`. Both kernels print the same four
    validation reports for `GLUED_CYLINDERS`. The glyph reader was also diffed
    against `AwtFontReader` over printable ASCII plus accented and Cyrillic
    letters, in `cyrvetic.ttf` and `notoSansLight.ttf`, with no differences.

    In headless Chrome over SwiftShader, every model of the sequence renders,
    along with the HUD, the face arrows, the vertex ids, the three hidden-line
    modes on the holed box and on the STEP bowl, the debug edges with their
    face normals and QI samples, the five shading types, the animation, the STL
    export and the screenshot.

    The second stage has its own driver pair. It runs 81 builds through
    `buildSolid`: `HOLLOW_BOX`, `CSG_MOON_BLOCK`, `CSG_LAMP_SHELL` at the
    default and at 24×12 subdivisions, the three split parts, `CSG_DIRECT`
    for each of the twelve samples under each of the four operations,
    `CSG_OPERAND1_PARTIAL` and `_2` for each sample, a traced (`[d]`) union,
    and the bowl at motif 3; then the walk of `[3]`/`[4]` across the whole
    sequence. For each build it prints the statistics counters, both preview
    operands and the result (full `toString` and every vertex as raw bits),
    `validateIntermediate`, the Appel lines, the STL bytes, and everything
    the kernel writes to standard output and error. Nine builds end in a strict
    validation rejection, and both kernels reject the same nine with the same
    topology summary.

    Against a JVM run with `-XX:+UnlockDiagnosticVMOptions
    -XX:-UseLibmIntrinsic` both outputs match over 77,625 lines except for the
    class name in the `Logger` header, which the bundler renames in
    TypeScript. With the default HotSpot intrinsics, 17 vertices of the 24×12
    lamp shell and that build's Appel digests differ by 1 ulp, from the
    `Math.sin`/`Math.cos` difference recorded in Phase 25. The driver also
    found two base defects, both fixed: `_PolyhedralBoundedSolidSetOperator`
    printed the bounds in its strict-validation message with JavaScript number
    text (`0`, `1`) where Java's `Arrays.toString(double[])` prints `0.0`,
    `1.0`; and `_PolyhedralBoundedSolidSetVertexFaceClassifier` wrote the
    second half of a traced line through `console.log` after writing its
    `  * ` prefix through `platformPrint`, so the two halves could land on
    different channels. The 24 Phase 25 operator test files (245 tests) pass
    after both fixes.

    In headless Chrome over SwiftShader, the bowl difference with its operand
    insets and motif line, the `MANT1986_2` pair under two operations, the
    moon-block union, the three split parts, the lamp shell and the hollow
    box, with its inner box in the hidden-line view, all render. The browser
    builds as synchronously as Java's event thread does, so a bowl build
    holds the page for a few seconds.

## Inventory baseline

The 45 original dependency groups were imported from
`/paradigmas/master/VSDK/dependencyGraphAnalyzer/etc/vitralJava` with 580 graph
entries. The blocks below preserve that reference inventory. Use the current
source roots and matrix to identify package moves, new source, platform
equivalents and supplemental entries. Phase 46 is the final review of the
expanded inventory. The source-of-record path and revision in a phase's dated
record describe the comparison performed at that time.

## Scope Rules

1. Port the current Java implementation, not an inferred API and not the older TypeScript seed. Preserve public names, visibility intent, constructor and overload behavior, inheritance, interfaces, nested types, constants, exceptions, side effects, mutation/immutability, ordering, and serialization-visible data.
2. Treat every fully qualified inventory entry as an independently accountable symbol. A nested Java class may become an exported TypeScript class plus namespace/companion export, but its externally visible identity and behavior must remain reachable.
3. Reuse existing TypeScript code only after a line-by-line semantic comparison. Rewrite it when it diverges. A same-named file does not count as migrated.
4. Native TypeScript/JavaScript mappings are acceptable only when they preserve all behavior used by Vitral. Otherwise implement a complete adapter. Java annotations that have no runtime behavior may be recorded as verified no-op language metadata, not represented by empty classes.
5. Preserve Java numeric semantics deliberately: signed bytes, 32-bit integer overflow, 64-bit values, float rounding, NaN/infinity, array bounds, division behavior, and endianness. Use `bigint`, typed arrays, `Math.fround`, and explicit checks where required.
6. Preserve Java collection semantics deliberately: equality, hashing, insertion/iteration order, nullability, fail-fast or snapshot behavior where observable, and map-entry behavior.
7. Keep platform-neutral model/algorithm code free of DOM, Node filesystem, and GPU dependencies. Put unavoidable runtime bindings behind concrete adapters and package subpath exports.
   The TypeScript filesystem adapters belong in a separate `typescript/fs/` workspace package (for example, `@vitral/fs`), which depends on `@vitral/base` and is built/installed only in server-side runtimes with filesystem I/O. The browser-facing `@vitral/base` artifact must neither import nor package `File`-family implementations.
8. Reuse current Java tests for the behavior being ported. Preserve assertions, fixtures and tolerances. The 59 listed files are the original migration inventory. The eight migrated browser examples and five Node examples are the accepted program scope.
9. Migrate an applicable Java test once its production dependency closure exists. Record any pending dependencies and document browser-specific checks for web runtime boundaries.
10. Map AWT/Swing to browser UI and Canvas 2D, and JOGL4 to WebGL 2. Compare user-facing behavior and rendering results at those boundaries.
11. Move Java parallel computation to Web Workers in the browser. Use host callbacks for UI scheduling, timers and asynchronous network I/O. Give parallel compute tasks a worker entry point and a common protocol for inputs, results, progress, cancellation, errors and lifecycle.

## Concurrency and browser workers

| Java responsibility | TypeScript implementation and closure work |
|---|---|
| `ParallelRaytracer` thread pool and progress consumer (Phases 12 and 40) | `ParallelRaytracer` schedules tiles over `BrowserWorkerExecutor` in the editor and shader example and `NodeWorkerExecutor` in the offline example. `SimpleSceneSnapshotTransfer` encodes the scene for worker-local reconstruction. The owner collects color and optional depth tiles and forwards progress notices. Verify serial/parallel output, teardown and stale frame handling through the browser entry point. |
| `ReaderPly` two-task mesh assembly (Phase 36) | The reader dispatches vertex preparation and triangle filtering as independent worker jobs over cloneable data. It joins both before creating the mesh, normals and material ranges. Workers terminate when the mesh is assembled, the caller aborts, or a job fails. |
| `TangibleInterfaceNetworkClient` connection thread (Phase 13) | The WebSocket event loop performs asynchronous I/O. Verify callback ordering, fragmented/error input behavior and disconnect/reconnect semantics. Apply the worker contract if CPU-parallel pose processing is added. |
| `AnimationEventGenerator` thread and Swing/AWT event dispatch (Phases 38 and 42) | The generator uses a cancellable host timer. DOM handlers and the web frame scheduler own UI work. The editor MCP bridge dispatches commands in the page through a promise-based API. |
| Java `ThreadLocal` scratch and mutable kernel state (Phases 21, 22, 25 and 40) | Audit each compute module used by workers. Give workers isolated scene and scratch state, and verify reentrancy at the appropriate call boundary. |

`WorkerProtocol.ts` defines structured-clone request, result, error and progress
messages. The executors support request cancellation and termination. A long
running job must observe cancellation at safe boundaries, or the owner must
discard obsolete results and terminate workers when appropriate. The protocol
alone does not establish in-flight cancellation in a tile renderer. Use typed
arrays and transferable buffers where appropriate, with explicit ownership.
The scheduler and DOM stay on the owning thread. For worker modules, use deep
package imports as existing entry points do. `CooperativeFiberScheduler` shares
one event loop and is used for cooperative scheduling.

## Persistence runtime architecture

- `@vitral/base` owns the stream helpers and format-level parsing and writing.
  Preserve EOF, signedness, endianness, exceptions and resource lifetime.
- `@vitral/fs` extends the shared `PersistenceElement` with local-file operations
  including `extractExtensionFromFile`. Java's `verifyLibrary` handles JNI
  discovery. Record the decoder or wrapper that replaces each applicable JNI
  capability in the target runtime.
- Browser adapters use `fetch` for HTTP resources, browser-provided file data
  for user-selected uploads, and downloads for exports. The editor publishes
  fixture data through the Angular container's `etc` assets. The original
  dedicated read-only test-server tool is unimplemented. Verify HTTP loading
  and failures through the existing fixture server or an equivalent read-only
  test host.

## Package and Build Foundation

Phase 1 established this foundation. Use these checks when restoring or validating it:

1. Make `typescript/` the npm workspace root and `typescript/base/` a package named `@vitral/base` (or the repository-approved final name), with a pinned lockfile.
2. Use an ESM library build with strict TypeScript project references. The base compiler configuration must enable `strict`, `noImplicitOverride`, `noUncheckedIndexedAccess`, `exactOptionalPropertyTypes`, `noFallthroughCasesInSwitch`, `noEmitOnError`, declarations, declaration maps, source maps, and incremental builds.
3. Add a deliberate public entry point and package `exports`; do not expose build-directory internals accidentally. Emit JavaScript and `.d.ts` files under `typescript/base/dist/`.
4. Add `typescript/scripts/clean.sh`. It must use `set -eu`, resolve its own directory, and remove only known outputs: package `dist`, TypeScript build-info files, coverage, and package artifacts.
5. Add `typescript/scripts/compile.sh`. It must use `set -eu`, invoke the workspace build from a path-independent repository location, stop on compiler errors, and create a versioned npm tarball under `typescript/artifacts/`.
6. Fix root npm commands: `clean`, `build`, `test`, `pack`, and `verify`. `verify` must run clean, build, tests, and package validation in that order.
7. Add a TypeScript test project and Vitest (or a repository-approved equivalent), configured for deterministic serial execution where shared Java test fixtures require it.
8. Validate the package with `npm pack --dry-run`, inspect its file list, install the resulting tarball into a temporary consumer project, and compile an import against its declarations. This is packaging validation, not an invented behavioral test.
9. Document Node.js 22+ for workspace tooling and the runtime-specific exports. Validate the Angular application separately with its own lockfile and scripts.

Expected user workflow after Phase 1:

```sh
cd typescript
npm ci
./scripts/clean.sh
./scripts/compile.sh
npm test -- --run
```

Expected artifacts:

```text
typescript/base/dist/**/*.js
typescript/base/dist/**/*.d.ts
typescript/artifacts/vitral-base-<version>.tgz
```

## Mechanical Port Procedure

Apply this sequence to every class in every phase:

1. Locate the current Java source and any nested classes; record the source path and source revision in a migration ledger.
2. Locate all direct Java imports, subclasses, implementors, call sites, and tests before translating.
3. Compare any existing TypeScript candidate against every field, constructor, method, nested type, and error path.
4. Translate the complete implementation. Resolve language differences explicitly; never suppress them with `any`, non-null assertions used as guesses, or disabled compiler checks.
5. Export the symbol through the intended package path and update imports without introducing circular initialization.
6. Port the existing Java test sources whose dependency closure is complete, preserving assertions, data, tolerances, and expected failures.
7. Record the symbol as complete only after source/API review, compile, tests, and package checks pass.

## Standard Phase Gate

Apply the relevant checks when closing a phase. Phases with independent dependencies can proceed in parallel. Empty checkpoints close by checking the current source inventory:

- Inventory: every listed entry has a source mapping and completed TypeScript export; no unaccounted omission or duplicate exists.
- Completeness scan: no placeholder, empty implementation, unconditional dummy result, skipped test, `todo`, focused test, or blanket type suppression was introduced.
- Java reference: the relevant existing Java tests compile and pass against the source of record.
- TypeScript compile: a clean `tsc --build` completes with zero errors and zero emitted output from failed projects.
- TypeScript tests: all migrated tests pass; test counts and skipped-test counts are recorded, with zero unexplained skips.
- Packaging: declarations are emitted, public imports resolve, `npm pack --dry-run` succeeds, and the package contains no source-tree/build-cache leakage.
- Review: public API and observable behavior are compared with Java, including error paths and edge cases covered by existing tests.

Use `npm run verify` for the TypeScript workspace and run the relevant Java reference tests and browser checks separately:

```sh
cd typescript
npm run verify
```

## Web platform mapping

| Java platform | Web target |
|---|---|
| AWT/Swing windows, menus, dialogs, events and generic editor | HTML and CSS/SCSS, DOM event bindings, `WebSystem` event translation and the reusable HTML renderers and editor in `@vitral/webgl`. The Angular container hosts the applications. |
| AWT paint and image buffers | Canvas 2D and browser image APIs. The Scene Editor uses Canvas 2D for wireframe/hidden lines, hidden surfaces and parallel raytrace display. |
| JOGL4 GPU drawing | WebGL 2, the browser graphics API based on OpenGL ES 3.0, with GLSL ES 3.00. Map NIO uploads to typed arrays and validate capability probing, shaders, buffers, textures, framebuffer lifecycle and depth readback with the browser context. |
| Java desktop integration and dispatch | Browser-hosted resource loading, file selection and downloads, event-loop dispatch and Web Workers for CPU-parallel work. |

Use the [WebGL 2.0 specification](https://registry.khronos.org/webgl/specs/latest/2.0/)
for the browser API surface. Record the rendering behavior of GLU tessellation,
point sizing, polygon modes and desktop-only capabilities as explicit WebGL
adaptations. Track AWT/Swing responsibilities in Phases 33, 38, 40 and 44 and
JOGL4 behavior in Phase 41. JOGL2 remains a legacy reference where its observable
behavior is used by a migrated feature.

## Execution plan and original phase inventories

Phases 1–45 retain the graph order and historical class lists. Phase 46 covers
parity and dependency closure. The current matrix governs active status and
sequencing. Dated advances and gate results below refer to their source
revisions.

### Phase 1: Java runtime compatibility

Source group: `09_java.txt` (86 class entries).

Work: Bootstrap the package and implement every required Java-library semantic as a native TypeScript mapping or a complete compatibility adapter. This phase also creates the build, clean, test, and package scripts described above. Establish the browser-worker-compatible concurrency contract here for `Thread`, concurrent utilities, and all later multithreaded ports.

Class inventory:

```text
java.io.BufferedInputStream
java.io.BufferedOutputStream
java.io.BufferedReader
java.io.ByteArrayInputStream
java.io.ByteArrayOutputStream
java.io.DataInputStream
java.io.File
java.io.FileInputStream
java.io.FileOutputStream
java.io.FileReader
java.io.InputStream
java.io.InputStreamReader
java.io.IOException
java.io.ObjectInputStream
java.io.ObjectOutputStream
java.io.OutputStream
java.io.RandomAccessFile
java.io.Reader
java.io.Serial
java.io.Serializable
java.io.StreamTokenizer
java.io.StringReader
java.lang.ArithmeticException
java.lang.Boolean
java.lang.Class
java.lang.ClassLoader
java.lang.CloneNotSupportedException
java.lang.Comparable
java.lang.Deprecated
java.lang.Double
java.lang.Exception
java.lang.IllegalArgumentException
java.lang.IllegalStateException
java.lang.IndexOutOfBoundsException
java.lang.Integer
java.lang.Long
java.lang.Object
java.lang.Override
java.lang.reflect.Method
java.lang.Runnable
java.lang.RuntimeException
java.lang.StackTraceElement
java.lang.String
java.lang.StringBuffer
java.lang.StringBuilder
java.lang.SuppressWarnings
java.lang.ThreadLocal
java.lang.Throwable
java.lang.UnsupportedOperationException
java.nio.ByteBuffer
java.text.DecimalFormat
java.text.FieldPosition
java.text.SimpleDateFormat
java.util.ArrayList
java.util.Collection
java.util.concurrent.atomic.AtomicLong
java.util.concurrent.atomic.LongAdder
java.util.concurrent.ConcurrentLinkedQueue
java.util.Date
java.util.HashMap
java.util.HashSet
java.util.Iterator
java.util.List
java.util.Map
java.util.Map.Entry
java.util.Stack
java.util.StringTokenizer
java.util.zip.GZIPInputStream
org.w3c.dom.Document
org.w3c.dom.Element
org.w3c.dom.NamedNodeMap
org.w3c.dom.Node
org.w3c.dom.NodeList
java.lang.FunctionalInterface
java.util.Comparator
java.util.LinkedHashMap
java.util.Set
java.lang.CharSequence
java.lang.Thread
java.net.http.HttpClient
java.net.http.WebSocket
java.net.http.WebSocket.Listener
java.util.concurrent.CompletionStage
java.util.LinkedHashSet
java.util.regex.Matcher
java.util.regex.Pattern
```

Literal-parity finding — 2026-09-12. `java.io.StreamTokenizer` was not a port:
it was a 50-line regular-expression splitter with no character-type table, no
`commentChar`, `quoteChar`, `ordinaryChar`, `wordChars`, `parseNumbers`,
`eolIsSignificant`, `lowerCaseMode`, `slashSlash`/`slashStarComments` or
`lineno()`, and a default syntax unrelated to the JDK's. No TypeScript module
consumed it, so nothing depended on the approximation, but the three Java
readers that need it (`ReaderMitScene`, `ReaderAse`, `ReaderVrml`) and
`AlgebraicExpression` could not be ported against it. It is now a literal port
of the JDK algorithm, character-type table, octal escapes, `peekc`/`SKIP_LF`
lookahead and `toString` form included; only the deprecated
`StreamTokenizer(InputStream)` constructor is left out, since the ported
`Reader` hierarchy is the supported input. This also removes the stated cause
of the `AlgebraicExpressionExample` divergence recorded under Offline Example
Programs, though rewriting `AlgebraicExpression.ts` against it remains open as
a Phase 4 finding.

Exit: satisfy the applicable standard phase checks.

### Phase 2: Common entity foundation

Source group: `12_common.txt` (8 class entries).

Work: Port the applicable entity and exception hierarchies, VSDK utilities and PresentationElement dependency. J2ME is outside the web scope.

Class inventory:

```text
vsdk.toolkit.common.Entity
vsdk.toolkit.common.FundamentalEntity
vsdk.toolkit.common.ModelElement
vsdk.toolkit.common.VSDK
vsdk.toolkit.common.VSDKException
vsdk.toolkit.common.VSDKFatalException
vsdk.toolkit.common.VSDKJ2ME
vsdk.toolkit.gui.PresentationElement
```

Exit: satisfy the applicable standard phase checks.

### Phase 3: Linear algebra

Source group: `12_lineal_algebra.txt` (16 class entries).

Work: Port all double- and float-oriented vectors, matrices, quaternions, complex arithmetic, and matrix exceptions. Preserve Java float behavior where observable, overload behavior, bounds checks, immutability, and exception behavior.

Class inventory:

```text
vsdk.toolkit.common.linealAlgebra.Complex
vsdk.toolkit.common.linealAlgebra.exceptions.MatrixDimensionMismatchException
vsdk.toolkit.common.linealAlgebra.exceptions.MatrixIndexOutOfBoundsException
vsdk.toolkit.common.linealAlgebra.exceptions.MatrixNotSquareException
vsdk.toolkit.common.linealAlgebra.exceptions.MatrixSingularException
vsdk.toolkit.common.linealAlgebra.Matrix4x4d
vsdk.toolkit.common.linealAlgebra.Matrix4x4f
vsdk.toolkit.common.linealAlgebra.MatrixNxM
vsdk.toolkit.common.linealAlgebra.Quaterniond
vsdk.toolkit.common.linealAlgebra.Quaternionf
vsdk.toolkit.common.linealAlgebra.Vector2Dd
vsdk.toolkit.common.linealAlgebra.Vector2Df
vsdk.toolkit.common.linealAlgebra.Vector3Dd
vsdk.toolkit.common.linealAlgebra.Vector3Df
vsdk.toolkit.common.linealAlgebra.Vector4Dd
vsdk.toolkit.common.linealAlgebra.Vector4Df
```

Exit: satisfy the applicable standard phase checks.

### Phase 4: Symbolic algebra

Source group: `12_symbolic_algebra.txt` (7 class entries).

Work: Port the parser/evaluator and complete expression-node hierarchy, including error behavior and operator precedence.

Class inventory:

```text
vsdk.toolkit.common.symbolicAlgebra._AlgebraicExpressionBinaryOperatorNode
vsdk.toolkit.common.symbolicAlgebra._AlgebraicExpressionConstantNode
vsdk.toolkit.common.symbolicAlgebra._AlgebraicExpressionNode
vsdk.toolkit.common.symbolicAlgebra._AlgebraicExpressionUnaryOperatorNode
vsdk.toolkit.common.symbolicAlgebra._AlgebraicExpressionVariableNode
vsdk.toolkit.common.symbolicAlgebra.AlgebraicExpression
vsdk.toolkit.common.symbolicAlgebra.AlgebraicExpressionException
```

Exit: satisfy the applicable standard phase checks.

### Phase 5: Color

Source group: `13_color.txt` (1 class entries).

Work: Maintain the ColorRgb, ColorRgba and Cie contracts against the current Java source.
Supplemental current-Java class required for complete `common`: `vsdk.toolkit.common.color.ColorRgba`.

Class inventory:

```text
vsdk.toolkit.common.color.ColorRgb
```

Exit: satisfy the applicable standard phase checks.

### Phase 6: Logging

Source group: `13_logging.txt` (1 class entries).

Work: Port logging levels, formatting, output routing, and failure behavior without no-op logging methods.

Class inventory:

```text
vsdk.toolkit.common.logging.Logger
```

Exit: satisfy the applicable standard phase checks.

### Phase 7: Memory management checkpoint

Source group: `13_memory_management.txt` (0 class entries).

Work: Maintain the exported MemoryPool and its Java object-reuse semantics.

Class inventory:

```text
(no class entries; the file contains only its group header)
```

Exit: satisfy the applicable standard phase checks.

### Phase 8: Quasi-Monte Carlo checkpoint

Source group: `14_quasi_monte_carlo.txt` (0 class entries).

Work: The authoritative group has no class entries. Confirm that no current Java production class belongs here; create no placeholder.

Class inventory:

```text
(no class entries; the file contains only its group header)
```

Exit: satisfy the applicable standard phase checks.

### Phase 9: Data structures

Source group: `15_data_structures.txt` (12 class entries).

Work: Port all containers and tree/list traversal behavior, preserving mutation, iteration order, generic contracts, bounds behavior, and primitive numeric semantics.

Class inventory:

```text
vsdk.toolkit.common.dataStructures._CircularDoubleLinkedListNode
vsdk.toolkit.common.dataStructures._NAryTreeIntermediateNode
vsdk.toolkit.common.dataStructures._NAryTreeLeafNode
vsdk.toolkit.common.dataStructures._NAryTreeNode
vsdk.toolkit.common.dataStructures.ArrayListOfBytes
vsdk.toolkit.common.dataStructures.ArrayListOfDoubles
vsdk.toolkit.common.dataStructures.ArrayListOfInts
vsdk.toolkit.common.dataStructures.ArrayListOfLongs
vsdk.toolkit.common.dataStructures.BinaryTreeNode
vsdk.toolkit.common.dataStructures.CircularDoubleLinkedList
vsdk.toolkit.common.dataStructures.NAryTree
vsdk.toolkit.common.dataStructures.NAryTreeTraverser
```

Exit: satisfy the applicable standard phase checks.

### Phase 10: Statistics

Source group: `17_statistics.txt` (3 class entries).

Work: Maintain the listed counters and reports, including GeometryStatistics, SolidTextureStatistics and the current raytrace counters.
Supplemental current-Java classes required for complete `common`: `vsdk.toolkit.common.statistics.GeometryStatistics` and `vsdk.toolkit.common.statistics.SolidTextureStatistics`.
Milestone gate: phases 1 through 10 plus the two supplemental classes provide the entire current Java `vsdk.toolkit.common` package except the intentionally excluded Web-inapplicable `VSDKJ2ME`. All eleven Java tests located under `common/linealAlgebra` have passing TypeScript translations. `LinearAlgebraStrategiesConsistencyTest` belongs to `processing.linealAlgebra` and remains deferred until its strategy-engine production dependencies are ported.

Class inventory:

```text
vsdk.toolkit.common.statistics.PolyhedralBoundedSolidStatistics
vsdk.toolkit.common.statistics.RaytraceStatistics
vsdk.toolkit.common.statistics.RenderingStatistics
```

Exit: satisfy the applicable standard phase checks.

### Phase 11: Command-line options checkpoint

Source group: `18_command_line_options.txt` (0 class entries).

Work: The authoritative group has no class entries. Record the verified absence and create no placeholder parser.

Class inventory:

```text
(no class entries; the file contains only its group header)
```

Exit: satisfy the applicable standard phase checks.

### Phase 12: GUI progress-monitor contracts

Source group: `18_gui_progress_monitors.txt` (4 class entries).

Work: Port event, producer, consumer, reporter, and listener contracts as platform-neutral TypeScript behavior.

Class inventory:

```text
vsdk.toolkit.gui.feedback.ProgressMonitor
vsdk.toolkit.gui.feedback.ProgressMonitorConsole
vsdk.toolkit.gui.feedback.ProgressMonitorConsoleLongFormat
vsdk.toolkit.gui.feedback.ProgressMonitorInRam
```

Re-audit and advance — 2026-09-12. Two literal-parity defects were found and
fixed in the already-closed console monitors: both wrote every fragment through
`console.log`, which terminates the line, where Java uses `System.out.print`
and deliberately builds one long bar on a single line, and
`ProgressMonitorConsoleLongFormat.end()` dropped the trailing `\n` of the Java
format. Both now go through `_PlatformConsole`'s `platformPrint`, which keeps
Java's layout on any runtime that exposes a standard output stream and falls
back to `console.log` in a browser. Verified against Java: the progress bars of
`testsuite/OfflineExamples/RaytracingOfflineExample` are byte-for-byte
identical to the Java program's, in both the compact and the long format.

This phase's inventory is also incomplete against the current Java base, like
the Phase 27 orphans: `vsdk.toolkit.gui.feedback.parallel` contains four
production files that no phase lists —
`ParallelProgressMonitorCommand`, `ParallelProgressMonitorEvent`,
`ParallelProgressMonitorProducer` and `ParallelProgressMonitorConsumer`. All
four are now ported 1:1 and exported through `@vitral/base`. One runtime
boundary: Java runs the consumer on a dedicated `Thread` and blocks it with
`Thread.sleep(50)` when the queue runs dry; JavaScript has no blocking sleep on
its single event loop, so `run()` is `async` and awaits a 50 ms timer, and the
owner awaits the returned promise where Java would `join()`. Event ordering and
console output are unchanged.

Exit: satisfy the applicable standard phase checks.

### Phase 13: Tangible-interface contracts

Source group: `18_gui_tangible_interfaces.txt` (4 class entries).

Work: Verify tangible input, browser WebSocket lifecycle and controller contracts through the web editor.

Class inventory:

```text
vsdk.toolkit.gui.tangibleInterfaces.TangibleInterfaceEvent
vsdk.toolkit.gui.tangibleInterfaces.TangibleInterfaceListener
vsdk.toolkit.gui.tangibleInterfaces.TangibleInterfaceNetworkClient
vsdk.toolkit.gui.tangibleInterfaces.TangibleInterfaceNetworkClient.FrameListener
```

Exit: satisfy the applicable standard phase checks.

### Phase 14: Media and image buffers

Source group: `19_media.txt` (20 class entries).

Work: Port images, pixel formats, palettes, z-buffers, and related data using typed arrays where Java byte/short/int layout matters.

Class inventory:

```text
vsdk.toolkit.media.Calligraphic2DBuffer
vsdk.toolkit.media.FourierShapeDescriptor
vsdk.toolkit.media.GeometryMetadata
vsdk.toolkit.media.GrayScalePalette
vsdk.toolkit.media.Image
vsdk.toolkit.media.IndexedColorImageUncompressed
vsdk.toolkit.media.MediaEntity
vsdk.toolkit.media.NormalMap
vsdk.toolkit.media.PrimitiveCountShapeDescriptor
vsdk.toolkit.media.RGBAImageCompressed
vsdk.toolkit.media.RGBAImageUncompressed
vsdk.toolkit.media.RGBAPixel
vsdk.toolkit.media.RGBColorPalette
vsdk.toolkit.media.RGBImageUncompressed
vsdk.toolkit.media.RGBPixel
vsdk.toolkit.media.RGBProceduralColorPalette
vsdk.toolkit.media.ShapeDescriptor
vsdk.toolkit.media.ZBuffer
vsdk.toolkit.media.RGBAImageHDRUncompressed
vsdk.toolkit.media.RGBAPixelHDR
```

Partial advance — 2026-09-13 (out of phase order, to unblock the
`SolidTextureExample` module of the WebGL testsuite container): the eleven
unlisted `media` / `media.solidTexture` files that Phase 27 recorded as the
first named input to Phase 45 are now ported, and that orphan set is closed.
They are `vsdk.toolkit.media.RGBAColorPalette`,
`vsdk.toolkit.media.IndexedColorImageHDRUncompressed`, and the nine files of
`vsdk.toolkit.media.solidTexture`: `TextureUtils`, `ProceduralNoise`,
`ColorTextureFixture`, `BumpTextureFixture`, `ImageTexture`,
`SolidTextureCoordinateMapper`, `ControlledRGBAImageHDRUncompressed`,
`ImageToSolidTextureProjectionMethods` and
`ImageToSolidTextureInterpolationTypes`. None of them belongs to this phase's
twenty-entry inventory, which is unchanged and stays complete; they are
recorded here because this is where the `media` package lives.

Three spellings differ from Java and are documented beside the code:

  - `ProceduralNoise` and `TextureUtils` each declare a private nested
    `CRandom`, the C library linear congruential generator that makes the
    permutation table reproducible across languages. Java evaluates
    `state * 1103515245L + 12345L` in 64-bit arithmetic, and with a state below
    2^31 that product reaches about 2^61 — past the 2^53 a TypeScript `number`
    represents exactly. The multiplication is split at 2^16, the high half
    contributing only through its low 15 bits since everything above bit 30 is
    masked away, so both partial products stay inside the exact range and the
    sequence is Java's term for term. Java duplicates the nested class in both
    files and so does this port.
  - `ProceduralNoise.rTable` is seeded by a CRC-16 over the little-endian bytes
    of three doubles, which is why `checksumVector` builds a `DataView` rather
    than doing arithmetic.
  - The two enums are string enums, as the rest of the TypeScript edition
    spells a Java enum that can cross a structured clone; their Java integer
    codes survive as the `value` and `fromInt` functions beside them. The
    `double[]` and `int[]` out parameters of `SolidTextureCoordinateMapper` and
    `ImageTexture` become the mutable `SolidTextureCoordinate` and index
    holders, which keeps the read-modify-write sequences of `bumpMap` intact.
    The four `Vector3Dd[]` out-parameter overloads of `ProceduralNoise` and
    `BumpTextureFixture` are not ported, since each already has a returning
    flavor that is.

Parity evidence is the sixteen-line byte-level agreement recorded for
`SolidTextureExample` under WebGL Example Programs, which drives every one of
these files. No Java test source exists for them.

Exit: satisfy the applicable standard phase checks.

### Phase 15: General processing

Source group: `20_processing.txt` (22 class entries).

Work: Port general processing algorithms and supporting classes; migrate only the Java tests whose dependencies are now complete.

Class inventory:

```text
vsdk.toolkit.processing.ComputationalGeometry
vsdk.toolkit.processing.ComputationalGeometry.ClippedLine2DResult
vsdk.toolkit.processing.Containment
vsdk.toolkit.processing.ImageProcessing
vsdk.toolkit.processing.linealAlgebra.DeterminantStrategy
vsdk.toolkit.processing.linealAlgebra.GaussCpuStrategy
vsdk.toolkit.processing.linealAlgebra.InverseStrategy
vsdk.toolkit.processing.linealAlgebra.LinearAlgebraBackend
vsdk.toolkit.processing.linealAlgebra.LinearAlgebraEngine
vsdk.toolkit.processing.linealAlgebra.LinearAlgebraEngine.DefaultBackend
vsdk.toolkit.processing.linealAlgebra.LuCpuStrategy
vsdk.toolkit.processing.linealAlgebra.LuCpuStrategy.LuDecomposition
vsdk.toolkit.processing.linealAlgebra.MatrixAlgorithmsSupport
vsdk.toolkit.processing.linealAlgebra.NaiveCofactorCpuStrategy
vsdk.toolkit.processing.linealAlgebra.StrategySelector
vsdk.toolkit.processing.linealAlgebra.StrategySelector.ComputeStrategy
vsdk.toolkit.processing.LzwWrapper
vsdk.toolkit.processing.ProcessingElement
vsdk.toolkit.processing.SignalProcessing
vsdk.toolkit.processing.SolverPolynomialQuarticBairstow
vsdk.toolkit.processing.SpharmonicKitWrapper
vsdk.toolkit.processing.StopWatch
```

Exit: satisfy the applicable standard phase checks.

### Phase 16: Materials

Source group: `21_material.txt` (5 class entries).

Work: Port material models and presets, preserving numeric defaults, equality, copying, and serialization-visible behavior.

Class inventory:

```text
vsdk.toolkit.environment.material.MicroFacetedMaterial
vsdk.toolkit.environment.material.MicroFacetedMaterial.MicrofacetConfig
vsdk.toolkit.environment.material.RendererConfiguration
vsdk.toolkit.environment.material.ShadingType
vsdk.toolkit.environment.material.SimpleMaterial
```

Completion of a deferred constructor — 2026-09-13 (needed by
`testsuite/VitralWebTestsuiteContainer/src/WebGLExamples/ShadersExample`):
`MicroFacetedMaterial(String csvFileName, String materialName)` was the one
part of the group left unported, because it reads a file. It is split the way
the readers are: the parse is `MicroFacetedMaterial.fromCsvText` in
`@vitral/base` and is literal — the header row naming the columns, the
case-insensitive match on `material_name`, every per-field default including
`alpha` falling back to `roughness * roughness`, the unit-interval clamps, and
the `readAsciiLine` behavior that never sees a final line without a
terminating newline. Java's `resolveCsvFile`, which tries the name as given and
then under `etc/materials`, is the file-system half and stays with the caller;
a URL is already resolved, so the browser's half is a fetch.

One divergence is marked at the code: Java's `cycleCookTorranceMaterial`
constructs `new MicroFacetedMaterial(csvPath, name)` on every `[m]`, re-reading
the file each time. The text is read once and kept, and the same parse runs
against it, since re-fetching a static resource on every keypress would only
add latency to an identical answer.

Parity evidence: a Java reference driver loads `Copper`, `Aluminium` and `Gold`
from `etc/materials/microFacetMAterials.csv` against `java/base` and prints
every field of each resulting material; the TypeScript twin agrees on all three
lines.

Exit: satisfy the applicable standard phase checks.

### Phase 17: Geometry base

Source group: `22_geometry.txt` (1 class entries).

Work: Port the geometry root contract and intersection/bounding-volume semantics.

Class inventory:

```text
vsdk.toolkit.environment.geometry.Geometry
```

Exit: satisfy the applicable standard phase checks.

### Phase 18: Curves

Source group: `22_geometry_curve.txt` (2 class entries).

Work: Port parametric curves and curve aggregates with equivalent evaluation and boundary behavior.

Class inventory:

```text
vsdk.toolkit.environment.geometry.curve.Curve
vsdk.toolkit.environment.geometry.curve.ParametricCurve
```

Exit: satisfy the applicable standard phase checks.

### Phase 19: Geometry elements

Source group: `22_geometry_element.txt` (6 class entries).

Work: Port the geometry element abstractions and their ownership/reference semantics.

Class inventory:

```text
vsdk.toolkit.environment.geometry.element.Intersection
vsdk.toolkit.environment.geometry.element.Ray
vsdk.toolkit.environment.geometry.element.RayHit
vsdk.toolkit.environment.geometry.element.Triangle
vsdk.toolkit.environment.geometry.element.Vertex
vsdk.toolkit.environment.geometry.element.Vertex2D
```

Exit: satisfy the applicable standard phase checks.

### Phase 20: Concrete geometry elements

Source group: `22_geometry_elements.txt` (6 class entries).

Work: Port concrete geometry elements and exact topology relationships.

Class inventory:

```text
vsdk.toolkit.environment.geometry.elements.Intersection
vsdk.toolkit.environment.geometry.elements.Ray
vsdk.toolkit.environment.geometry.elements.RayHit
vsdk.toolkit.environment.geometry.elements.Triangle
vsdk.toolkit.environment.geometry.elements.Vertex
vsdk.toolkit.environment.geometry.elements.Vertex2D
```

Exit: satisfy the applicable standard phase checks.

### Phase 21: Surfaces

Source group: `22_geometry_surface.txt` (13 class entries).

Work: Port all surfaces, mesh containers, polygon contours, and parametric evaluation behavior.

Class inventory:

```text
vsdk.toolkit.environment.geometry.surface._AnimationInfo
vsdk.toolkit.environment.geometry.surface.FunctionalExplicitSurface
vsdk.toolkit.environment.geometry.surface.HalfSpace
vsdk.toolkit.environment.geometry.surface.InfinitePlane
vsdk.toolkit.environment.geometry.surface.Md2Mesh
vsdk.toolkit.environment.geometry.surface.ParametricBiCubicPatch
vsdk.toolkit.environment.geometry.surface.polygon._Polygon2DContour
vsdk.toolkit.environment.geometry.surface.polygon.Polygon2D
vsdk.toolkit.environment.geometry.surface.QuadMesh
vsdk.toolkit.environment.geometry.surface.Surface
vsdk.toolkit.environment.geometry.surface.TriangleMesh
vsdk.toolkit.environment.geometry.surface.TriangleMeshGroup
vsdk.toolkit.environment.geometry.surface.TriangleStripMesh
```

Exit: satisfy the applicable standard phase checks.

### Phase 22: Volumes and boundary representation

Source group: `22_geometry_volume.txt` (30 class entries).

Work: Port primitive volumes and the complete polyhedral bounded-solid topology, predicates, validators, and Euler operations.

Class inventory:

```text
vsdk.toolkit.environment.geometry.volume.Arrow
vsdk.toolkit.environment.geometry.volume.Box
vsdk.toolkit.environment.geometry.volume.Cone
vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid._GeometricFaceOrientationStrategy
vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid._GeometricPlanarityStrategy
vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid._GeometricStrictFaceIntersectionsStrategy
vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid._GeometricStrictLoopsStrategy
vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid._PolyhedralBoundedSolidBooleanTopologyPredicates
vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid._PolyhedralBoundedSolidTopologicalValidator
vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid._PolyhedralBoundedSolidValidationStrategy
vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid._TopologicalIntegrityStrategy
vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidEdge
vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace
vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace.PointInsideResult
vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge
vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop
vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidVertex
vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid
vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidEulerOperators
vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidGeometricValidator
vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy
vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy.ToleranceContext
vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidTopologyEditing
vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine
vsdk.toolkit.environment.geometry.volume.Solid
vsdk.toolkit.environment.geometry.volume.Sphere
vsdk.toolkit.environment.geometry.volume.Torus
vsdk.toolkit.environment.geometry.volume.VoxelVolume
vsdk.toolkit.environment.geometry.volume.Volume
vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidPredicates
```

Exit: satisfy the applicable standard phase checks.

### Phase 23: Backgrounds

Source group: `23_background.txt` (4 class entries).

Work: Port background models and sampling behavior independently of any display backend.

Class inventory:

```text
vsdk.toolkit.environment.background.Background
vsdk.toolkit.environment.background.CubemapBackground
vsdk.toolkit.environment.background.FixedBackground
vsdk.toolkit.environment.background.SimpleBackground
```

Exit: satisfy the applicable standard phase checks.

### Phase 24: Cameras

Source group: `23_camera.txt` (2 class entries).

Work: Port camera models, projection math, clipping, and ray generation.

Class inventory:

```text
vsdk.toolkit.environment.camera.Camera
vsdk.toolkit.environment.camera.CameraSnapshot
```

Exit: satisfy the applicable standard phase checks.

### Phase 25: Geometric processing

Source group: `23_geometric_processing.txt` (148 class entries).

Work: Port the full geometric-processing and CSG inventory in listed order, including all nested result/specification classes. Preserve tolerances, topology mutations, diagnostic behavior, and deterministic ordering.

For the Weiler--Atherton and B-rep/CSG algorithms, do not implement public boolean operations until all original private dependencies have been migrated textually and tested. In particular, migrate the clipping graph node insertion/order, cut classification, contour/hole classification, traversal, and all coincident/parallel-edge branches before `clipPolygons` or `unionPolygons`; migrate each CSG subsystem's predicates and topology helpers before its set-operation entry point.

### Phase 25.2 completion record — 2026-09-10

The Weiler--Atherton sub-block is closed. The TypeScript port follows the original Java source at `java/base/src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polygonClipper/WeilerAthertonPolygonClipper.java`: its 2D predicates and ordered insertion, `updatePolygonsAndListsWithCuts`, complete `makeCut` endpoint/parallel decision tree, contour and hole classification, inner/outer graph traversals, `clipPolygons`, and `unionPolygons` are implemented without substituting a different clipping algorithm.

Eight deterministic tests cover proper and vertical crossings, all four endpoint-coincidence orientations, collinear overlap, disjoint polygons, containment in both directions, holes, intersecting non-convex contours, exterior/difference loops, union, and duplicate/internal-edge cleanup. A compiled Java reference driver confirmed the exact output contour and vertex ordering for crossing, hole, non-convex, and union cases. The focused geometric-processing suite passed 15 tests in 5 modules; the full suite passed 85 tests in 52 modules. `npm run lint-fix`, clean strict compilation, `git diff --check`, package creation, declaration consumption from a temporary project, and the final `npm run verify` all passed. The package gate also caught and eliminated internal `vsdk/*` aliases from emitted declarations; geometric-processing production imports now remain consumable relative paths.

### Phase 25.3 completion record — 2026-09-10

The independent surface-processing block is closed: `GeometryTriangulator`, `SurfaceRayIntersection`, `TriangleMeshVoxelization`, `TriangleMeshGroupVoxelization`, `FunctionalExplicitSurfaceVoxelization`, and `Voxelization` follow their Java counterparts. The dispatch order for mesh, group, and functional surfaces; inverse-transform mesh rasterization; voxel tolerance `2 / xSize`; containment fallback; signed-byte occupancy (`-1`/`255`); and bounding-box ray rejection are preserved.

The deterministic `Voxelization` suite now verifies generic containment, mesh-space rasterization (including the legacy independently-clamped barycentric behaviour), mesh dispatch, triangulator identity/conversion rules, and accepted/rejected bounded mesh-ray hits. The focused geometric-processing suite passed 18 tests in 5 modules. `npm run lint-fix`, the complete 88-test suite, strict build, `git diff --check`, and the package/consumer gate in `npm run verify` all passed. The next sub-block is monotone polygon triangulation; port its missing dependency graph before exposing `MonotoneDecompositionTriangulator`.

### Phase 25.4 completion record — 2026-09-10

The deterministic data and scheduling layer for monotone decomposition is closed: `_TriangulationSegment`, `_VertexChain`, `_MonotoneChainNode`, `_ContourData`, `_IndexedVertex`, `_InsertionBatchSchedule`, and `_RandomSegmentOrder`. Regressions lock the Java Park--Miller permutation, log-star batching arithmetic, and the zero-index-compatible default segment state. Phase 25.5 may now port the trapezoid query graph and incremental insertion. Do not begin `_Monotone`, `_ContourAwarePolygonTriangulator`, or `MonotoneDecompositionTriangulator` until that graph is complete.

### Phase 25.5 continuation — 2026-09-10

Port only the trapezoidal-map construction layer: `_TriangulationTrapezoid`, `_TriangulationTrapezoidQueryNode`, `_SegmentTableBuilder`, `_Construct`, and `_IncrementalSegmentInserter`. Preserve query-node routing, allocated-table bounds, segment endpoint swapping, neighbor relinking, invalid-trapezoid marking, and all Java degeneracy branches. Add graph-level deterministic tests and run the phase gate. Stop after a successful 25.5 gate: 25.6 owns contour-aware extraction, `_Monotone`, and the public triangulator.

Completion: the 1-based storage, numeric predicates, circular segment-table construction, X/Y/SINK query-node routing, trapezoid representation, four-cell bootstrap DAG, and complete incremental insertion are ported. `_IncrementalSegmentInserter.addSegment` preserves Java's endpoint normalization, endpoint splitting, every lower-neighbour relinking branch, X-node/sink ownership, traversal, merging, and invalidation. `_Construct.constructTrapezoids` preserves Java's randomized log-star batch schedule and query-root refresh pass. Direct tests cover standalone `addSegment` and complete scheduled map construction.

Literal-parity audit (2026-09-11): `_Construct` and `_IncrementalSegmentInserter` now have one-for-one Java method inventories, including the complete `constructTrapezoids` and `addSegment` entry points. The temporary TypeScript-only `splitCellBySegment` helper was removed. Phase 25.5 is closed.

The Java and C++ repositories contain the future public-oracle examples at `java/testsuite/OfflineExamples/PolygonTriangulation` and `cpp/testsuite/OfflineExamples/PolygonTriangulation` (the latter includes numbered rendered outputs). Do not copy them into TypeScript during 25.5: use them in 25.6 only, after the extraction layer and public `MonotoneDecompositionTriangulator` are ready to receive polygon-with-hole regressions.

Phase 25.6 completion: `MonotoneDecompositionTriangulator` has the public contour path with flattened original indices, contour nesting/orientation, visible-bridge selection, and ear clipping. Bridge selection follows the Java checks against the outer boundary, the active hole, remaining holes, and midpoint containment. Its four private Java stages (`stage1PrepareAndOrder`, `stage2BootStrap`, `stage3IncrementalBatchedInsertion`, and `stage4FinalizeAndExtractTriangles`) are ported and connected to the completed 25.5 backend. A direct regression bypasses the contour-aware result and verifies the full Seidel construction and monotone extraction path on a convex polygon.

Literal-parity audit (2026-09-11): `_ContourAwarePolygonTriangulator` has the same 26 methods as Java; `_Monotone` has the same 11 methods, including every `SP_*` branch and both extraction methods; and `MonotoneDecompositionTriangulator` has the same four private stages plus `triangulate`. Deterministic regressions cover a convex contour, one annular hole (with exact Java triangle ordering), two disjoint holes, an island nested inside a hole, a degenerate contour, and a concave outer contour with a hole. The broader 60-file Java/C++ testsuite fixture replication remains intentionally deferred, as requested, and is not part of the 25.6 implementation gate. Phase 25.6 is closed.

Public-oracle validation (2026-09-12): the deferred 60-fixture replication is now covered outside the test suite by the migrated `PolygonTriangulation` offline example, which runs the public triangulator over every file in `etc/polygons` and matches the Java reference exactly in both triangle lists and rendered output. See the Offline Example Programs section. The triangulator required no correction.

Final 25.5/25.6 gate (2026-09-11): `npm run verify` completed successfully, including clean builds of `@vitral/base` and `@vitral/fs`, all 106 tests in 55 modules, package creation, and the downstream compile/package-consumption checks. `git diff --check` is clean. Phase 25.7 is active. The shared operator now has Java's complete method inventory; the Mantyla splitter (including its two Java-named helper classes), modeler wrapper, sector/sector and sector/face records, and profile-difference fallback are ported. Sixteen Java production source files remain absent, concentrated in the boolean classification/intersection/connect/finish pipeline, the remaining structural fallbacks, and fixtures.

### Phase 25.7 execution start — 2026-09-11

The B-rep/CSG operator sub-block is now active. Migration proceeds from the
dependency leaves upward: the curve-build state and profile-difference
fallback specification are ported first; the operator base and its complete
topology services must precede boolean classifiers, intersectors, finishers,
and public set operations. No placeholder base class is allowed.

Class inventory:

```text
vsdk.toolkit.processing.polygonClipper._CircularDoubleLinkedList
vsdk.toolkit.processing.polygonClipper._DoubleLinkedListNode
vsdk.toolkit.processing.polygonClipper._Polygon2DContourWA
vsdk.toolkit.processing.polygonClipper._Polygon2DWA
vsdk.toolkit.processing.polygonClipper._VertexNode2D
vsdk.toolkit.processing.polygonClipper.PolygonProcessor
vsdk.toolkit.processing.polygonClipper.WeilerAthertonPolygonClipper
vsdk.toolkit.processing.polyhedralBoundedSolidOperators._BoundaryRepresentationFromCurveBuildState
vsdk.toolkit.processing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidIdNamespace
vsdk.toolkit.processing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidOperator
vsdk.toolkit.processing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidProfileDifferenceFallbackSpec
vsdk.toolkit.processing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetClassifier
vsdk.toolkit.processing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetFinisher
vsdk.toolkit.processing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetGeometricPredicateProcessor
vsdk.toolkit.processing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetGeometricPredicateProcessor.CoplanarAngleBasis
vsdk.toolkit.processing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetGeometricPredicateProcessor.CoplanarAngularInterval
vsdk.toolkit.processing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetGeometricPredicateProcessor.SectoroverlapTraceEntry
vsdk.toolkit.processing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetIntersector
vsdk.toolkit.processing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetIntersector.BoundaryHit
vsdk.toolkit.processing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetIntersector.GenerationResult
vsdk.toolkit.processing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetNonIntersectingClassifier
vsdk.toolkit.processing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetNullEdgesConnector
vsdk.toolkit.processing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetNullEdgesConnector.ConnectResult
vsdk.toolkit.processing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetNullEdgesConnector.NullEdgePair
vsdk.toolkit.processing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetOperatorNullEdge
vsdk.toolkit.processing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace
vsdk.toolkit.processing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector
vsdk.toolkit.processing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex
vsdk.toolkit.processing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetOperatorVertexFace
vsdk.toolkit.processing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetOperatorVertexVertex
vsdk.toolkit.processing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetVertexFaceClassifier
vsdk.toolkit.processing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetVertexVertexClassifier
vsdk.toolkit.processing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetVertexVertexClassifier.VertexVertexClassificationData
vsdk.toolkit.processing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSplitter
vsdk.toolkit.processing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSplitterNullEdge
vsdk.toolkit.processing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSplitterSectorClassification
vsdk.toolkit.processing.polyhedralBoundedSolidOperators.CsgKurlanderBowlFixture
vsdk.toolkit.processing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidModeler
vsdk.toolkit.processing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidSetOperator
vsdk.toolkit.processing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidSetOperator.AxisAlignedCellBooleanBuilder
vsdk.toolkit.processing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidSetOperator.OffsetCylinderDifferenceFallbackSpec
vsdk.toolkit.processing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidSetOperator.OrthogonalProfileBooleanFallbackSpec
vsdk.toolkit.processing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidSetOperator.OrthogonalProfileOperandSpec
vsdk.toolkit.processing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidSetOperator.ProfileCellBooleanBuilder
vsdk.toolkit.processing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidSetOperator.SeparateEdgeSequenceResult
vsdk.toolkit.processing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidSetOperator.VerticalCylinderOperandSpec
vsdk.toolkit.processing.polyhedralBoundedSolidOperators.SimpleTestGeometryLibrary
vsdk.toolkit.processing.CurveModeler
vsdk.toolkit.processing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidSetOperator.DebugSolidExporter
vsdk.toolkit.environment.geometry.geometricProcessing.polygonClipper._CircularDoubleLinkedList
vsdk.toolkit.environment.geometry.geometricProcessing.polygonClipper._DoubleLinkedListNode
vsdk.toolkit.environment.geometry.geometricProcessing.polygonClipper._Polygon2DContourWA
vsdk.toolkit.environment.geometry.geometricProcessing.polygonClipper._Polygon2DWA
vsdk.toolkit.environment.geometry.geometricProcessing.polygonClipper._VertexNode2D
vsdk.toolkit.environment.geometry.geometricProcessing.polygonClipper.PolygonProcessor
vsdk.toolkit.environment.geometry.geometricProcessing.polygonClipper.WeilerAthertonPolygonClipper
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._BoundaryRepresentationFromCurveBuildState
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidIdNamespace
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidOperator
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidProfileDifferenceFallbackSpec
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetClassifier
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetFinisher
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetGeometricPredicateProcessor
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetGeometricPredicateProcessor.CoplanarAngleBasis
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetGeometricPredicateProcessor.CoplanarAngularInterval
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetGeometricPredicateProcessor.SectoroverlapTraceEntry
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetIntersector
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetIntersector.BoundaryHit
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetIntersector.GenerationResult
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetNonIntersectingClassifier
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetNullEdgesConnector
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetNullEdgesConnector.ConnectResult
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetNullEdgesConnector.NullEdgePair
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetOperatorNullEdge
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetOperatorVertexFace
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetOperatorVertexVertex
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetVertexFaceClassifier
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetVertexVertexClassifier
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetVertexVertexClassifier.VertexVertexClassificationData
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSplitter
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSplitterNullEdge
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSplitterSectorClassification
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.CsgKurlanderBowlFixture
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidModeler
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidSetOperator
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidSetOperator.AxisAlignedCellBooleanBuilder
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidSetOperator.DebugSolidExporter
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidSetOperator.OffsetCylinderDifferenceFallbackSpec
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidSetOperator.OrthogonalProfileBooleanFallbackSpec
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidSetOperator.OrthogonalProfileOperandSpec
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidSetOperator.ProfileCellBooleanBuilder
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidSetOperator.SeparateEdgeSequenceResult
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidSetOperator.VerticalCylinderOperandSpec
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.SimpleTestGeometryLibrary
vsdk.toolkit.environment.geometry.geometricProcessing.FunctionalExplicitSurfaceVoxelization
vsdk.toolkit.environment.geometry.geometricProcessing.GeometryTriangulator
vsdk.toolkit.environment.geometry.geometricProcessing.polygonClipper.PolygonTopologicalMerger
vsdk.toolkit.environment.geometry.geometricProcessing.polygonClipper.PolygonTopologicalMerger.DirectedEdge
vsdk.toolkit.environment.geometry.geometricProcessing.polygonClipper.PolygonTopologicalMerger.EdgeKey
vsdk.toolkit.environment.geometry.geometricProcessing.polygonClipper.PolygonTopologicalMerger.PointKey
vsdk.toolkit.environment.geometry.geometricProcessing.polygonClipper.PolygonTopologicalMerger.Segment2D
vsdk.toolkit.environment.geometry.geometricProcessing.polygonClipper.PolygonTopologicalMerger.SplitPoint
vsdk.toolkit.environment.geometry.geometricProcessing.polygonTriangulation.monotoneDecomposition._Construct
vsdk.toolkit.environment.geometry.geometricProcessing.polygonTriangulation.monotoneDecomposition._ContourAwarePolygonTriangulator
vsdk.toolkit.environment.geometry.geometricProcessing.polygonTriangulation.monotoneDecomposition._ContourData
vsdk.toolkit.environment.geometry.geometricProcessing.polygonTriangulation.monotoneDecomposition._IncrementalSegmentInserter
vsdk.toolkit.environment.geometry.geometricProcessing.polygonTriangulation.monotoneDecomposition._IncrementalSegmentInserter.BoolRef
vsdk.toolkit.environment.geometry.geometricProcessing.polygonTriangulation.monotoneDecomposition._IndexedVertex
vsdk.toolkit.environment.geometry.geometricProcessing.polygonTriangulation.monotoneDecomposition._InsertionBatchSchedule
vsdk.toolkit.environment.geometry.geometricProcessing.polygonTriangulation.monotoneDecomposition._Monotone
vsdk.toolkit.environment.geometry.geometricProcessing.polygonTriangulation.monotoneDecomposition._MonotoneChainNode
vsdk.toolkit.environment.geometry.geometricProcessing.polygonTriangulation.monotoneDecomposition._RandomSegmentOrder
vsdk.toolkit.environment.geometry.geometricProcessing.polygonTriangulation.monotoneDecomposition._SegmentTableBuilder
vsdk.toolkit.environment.geometry.geometricProcessing.polygonTriangulation.monotoneDecomposition._TriangulationSegment
vsdk.toolkit.environment.geometry.geometricProcessing.polygonTriangulation.monotoneDecomposition._TriangulationTrapezoid
vsdk.toolkit.environment.geometry.geometricProcessing.polygonTriangulation.monotoneDecomposition._TriangulationTrapezoidQueryNode
vsdk.toolkit.environment.geometry.geometricProcessing.polygonTriangulation.monotoneDecomposition._VertexChain
vsdk.toolkit.environment.geometry.geometricProcessing.polygonTriangulation.MonotoneDecompositionTriangulator
vsdk.toolkit.environment.geometry.geometricProcessing.polygonTriangulation.MonotoneDecompositionTriangulator.Triangle
vsdk.toolkit.environment.geometry.geometricProcessing.SurfaceRayIntersection
vsdk.toolkit.environment.geometry.geometricProcessing.TriangleMeshGroupVoxelization
vsdk.toolkit.environment.geometry.geometricProcessing.TriangleMeshVoxelization
vsdk.toolkit.environment.geometry.geometricProcessing.Voxelization
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidAxisAlignedCellFallback
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidAxisAlignedCellFallback.AxisAlignedCellBooleanBuilder
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidFaceValidator
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidFaceValidator.FaceSegment
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidFallbackGeometry
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidOffsetCylinderFallback
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidOffsetCylinderFallback.OffsetCylinderDifferenceFallbackSpec
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidOffsetCylinderFallback.VerticalCylinderOperandSpec
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidOrthogonalProfileFallback
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidOrthogonalProfileFallback.OrthogonalProfileBooleanFallbackSpec
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidOrthogonalProfileFallback.OrthogonalProfileOperandSpec
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidOrthogonalProfileFallback.ProfileCellBooleanBuilder
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidProfileDifferenceFallback
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetIntersectionCurveBuilder
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetIntersectionCurveBuilder.FacePairGroup
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetIntersectionCurveBuilder.Report
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetNonIntersectingClassifier._PreflightCache
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetOperator
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetOperator.DebugSolidExporter
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidSetOperator.SeparateEdgeSequenceResult
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._SetOperationContext
vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._SetOperationTrace
```

### Phase 25.7 closure — 2026-09-12

Every Java class of the inventory above is ported. The migration order was:
sector-classification records, geometric predicate processor, intersector and
intersection-curve builder, vertex/vertex and vertex/face classifiers, set
classifier, non-intersecting classifier (with `_PreflightCache`), null-edges
connector (with `ConnectResult` and `NullEdgePair`), finisher, offset-cylinder
fallback, `_PolyhedralBoundedSolidSetOperator` (with `DebugSolidExporter` and
`SeparateEdgeSequenceResult`), the `PolyhedralBoundedSolidModeler.setOp`
wrappers (recording `PolyhedralBoundedSolidStatistics.recordSetOpCall`),
`vsdk.toolkit.processing.CurveModeler`, `SimpleTestGeometryLibrary` and
`CsgKurlanderBowlFixture`.

Two earlier-phase classes were re-ported to literal form because the boolean
pipeline depends on them: `InfinitePlane` (Java's one-argument
`doIntersectionFirstHit(Ray)` and the negated-`t` `doIntersectionWithNegative`)
and the sector/sector and sector/face classification records. The
`java.lang.Math.round` port was corrected to Java's tie-towards-positive-infinity
contract.

Runtime boundaries introduced here are documented in
`doc/typescriptParityAudit.md`: no JVM system properties (`System.getProperty`
and the base `_PlatformProperties` adapter answer `null`), no Java
serialization (a node-by-node `deepCloneSolid` adapter), no
`System.identityHashCode` (a `WeakMap` identity table), and no filesystem in
the base package (`debugSolid` delegates to the `DebugSolidExporter`).

Parity verification: a Java reference driver and its Node twin print the full
`PolyhedralBoundedSolid.toString()` dumps for MANT1986_2, MANT1988_15_1 and
MANT1988_3 under UNION/INTERSECTION/SUBTRACT plus APPE1967_1; the 1,388-line
outputs are byte-identical.

Test migration (rule 9: only Java sources, once their production dependencies
exist). All 24 Java CSG test sources were translated in this phase:
`IntersectorWeldTest`, `IntersectorParametricOrderingTest`,
`IntersectionCurveBuilderTest`, `VertexVertexEndpointRecoveryTest`,
`PolyhedralBoundedSolidPreflightTest`, `SetOpConnectScanJoinTest`,
`SetOpConnectNoLooseInvariantTest`, `SetOpFinishInvariantsTest`,
`PolyhedralBoundedSolidStrictSetOpTest`, `VertexFaceClassifierCoplanarTest`,
`SectoroverlapTraceDiagnosticTest`,
`PolyhedralBoundedSolidSetOperatorCoplanarPredicateTest`,
`PolyhedralBoundedSolidSetOperatorTest`,
`PolyhedralBoundedSolidSplitterMantylaRegressionTest`,
`AlgebraicIdentityRegressionTest`, `Stage6FaceSubdivisionDiagnosticTest`,
`KurlanderBowlStarInvariantTest`, `KurlanderBowlMotifSweepRegressionTest`,
`CsgKurlanderBowlFirstStarRegressionTest`,
`CsgKurlanderBowlAllMotifsRegressionTest`,
`CsgMoonCylinderDifferenceDegeneracyTest`,
`BooleansFromReferenceObjectPairsTest`, `KurlanderMotif4OperationMatrixTest` and
`StepperMotorGuideStrictValidationTest`, together with the Java test fixtures
`PolyhedralBoundedSolidTestFixtures`, `CsgSampleCorpus`,
`CsgSampleCorpusFixtures` and `StepperMotorGuideCsgFixture`. The ten Java
tests of `environment/geometry/volume/polyhedralBoundedSolid` (Euler operators,
validators, predicates, numeric policy, topology summary, kimrh/mikrh,
quantitative invisibility and the QI scan) were migrated in the same pass.
`Mant1988Section15_2UnionDiagnostic` is deliberately not migrated: it has zero
`@Test` methods and, run against the current Java kernel, fails with
`NoSuchMethodException` on members deleted by Stage 7 R5.

Java `@Disabled` tests are preserved as skipped tests with the Java rationale
quoted, and Java's own baselines (reference matrix, motif summaries, sweep
counts, curve reports) are asserted unchanged.

Phase 25.7 gate record: `npm run verify` completed successfully on 2026-09-12
(exit 0), with clean strict builds of `@vitral/base` and `@vitral/fs`, 382
passing and 7 skipped tests in 75 modules, zero unhandled errors, and the
package creation and downstream consumption checks passing. The seven skipped
cases correspond one-to-one to the six Java `@Disabled` annotations after
parameter expansion. Reaching zero unhandled errors required inserting the
documented `yieldToEventLoop()` harness accommodation in the five spec files
whose total run time approaches vitest's fixed 60 s worker-RPC budget
(`KurlanderMotif4OperationMatrixTest`, `KurlanderBowlMotifSweepRegressionTest`,
`KurlanderBowlStarInvariantTest`, `Stage6FaceSubdivisionDiagnosticTest`,
`PredicatesQiScanTest`); the budget is per spec file, not per test.

Exit: satisfied. Phase 26 may start.

### Phase 26: Lights

Source group: `23_light.txt` (2 class entries).

Work: Port light models and attenuation/color behavior.

Class inventory:

```text
vsdk.toolkit.environment.light.Light
vsdk.toolkit.environment.light.LightType
```

Inventory drift: `LightType` has no source mapping in the current Java source of
record. The only remaining references are in `java/jogl2`, whose module is
deferred with the rest of the UI and render decision. No compatibility enum was
invented for it. The same package instead contains the abstract `Light`, its
nested `LightDirection` record, and four concrete subclasses (`AmbientLight`,
`DirectionalLight`, `PointLight`, and `SpotLight`), so the phase is accounted as
six ported symbols against the two listed entries.

Exit: satisfied on 2026-09-12. See the Phase 26 gate record.

### Phase 27: Tone-mapping checkpoint

Source group: `23_tonemap.txt` (0 class entries).

Work: The authoritative group has no class entries. Verify that tone mapping remains represented elsewhere and create no placeholder.

Class inventory:

```text
(no class entries; the file contains only its group header)
```

Audit result: the current Java base has no tone-mapping class. A
case-insensitive scan for `tonemap`, `tone_map`, `toneMapping`, and
`tone mapping` over `java/base/src`, `java/awt`, `java/jogl2`, and `java/jogl4`
returns nothing, and the same scan over the TypeScript tree confirms that no
placeholder was ever created. The functionality the group name refers to is
represented by two already-ported blocks: the gamma transfer functions in
`vsdk.toolkit.processing.ImageProcessing` (Phase 15) and the high-dynamic-range
buffers `RGBAImageHDRUncompressed` and `RGBAPixelHDR` (Phase 14). No
compatibility symbol was added.

Exit: satisfied on 2026-09-12. See the Phase 27 gate record.

### Phase 28: Scene model

Source group: `24_scene.txt` (4 class entries).

Work: Port scene containers, object bindings, and traversal behavior.

Class inventory:

```text
vsdk.toolkit.environment.scene.SimpleBody
vsdk.toolkit.environment.scene.SimpleBodyGroup
vsdk.toolkit.environment.scene.SimpleScene
vsdk.toolkit.environment.scene.SimpleSceneSnapshot
```

All four entries have a source mapping in the current Java base and are ported
and exported through `@vitral/base`. Two Java runtime adapters the group needs
were added with them: `java.util.Objects.requireNonNull` with
`java.lang.NullPointerException`, and `java.util.Collections.unmodifiableList`
as a read-only view whose mutators throw `UnsupportedOperationException`.

Java's overload pairs are preserved as TypeScript overload signatures on one
method: `SimpleBody.doIntersectionFirstHit(Ray)` / `(Ray, RayHit)` and the
three `SimpleScene.exportToSimpleSceneSnapshot` forms. As in the Phase 24
`Camera` port, the Java `AtomicLong` modification counter is a plain counter
field, since the TypeScript port is single-threaded per body and the
observable `getModificationVersion()` sequence is unchanged.

No Java test source has a dependency closure limited to this phase: the only
two Java tests that reference the package, `HiddenLineRendererTest` and
`ReaderMitSceneTest`, also depend on later renderer and geometry-I/O
production classes. No test was invented.

Exit: satisfied on 2026-09-12. See the Phase 28 gate record.

### Phase 29: Numerical-analysis checkpoint

Source group: `25_numerical_analysis.txt` (0 class entries).

Work: The authoritative group has no class entries. Audit for moved numerical code and create no placeholder.

Class inventory:

```text
(no class entries; the file contains only its group header)
```

Partial advance — 2026-09-13 (out of phase order, to unblock the
`SolidTextureExample` module of the WebGL testsuite container): the audit this
phase calls for found two production files in
`vsdk.toolkit.numericalAnalysis.lookUpTables` that no phase inventory lists,
`LookUpTableSine` and `LookUpTableChecksum16`, both reached by
`ProceduralNoise`. They are **orphans** of the 580-entry graph, as with the
`media`/`solidTexture` set recorded under Phase 14, and both are ported 1:1
into `@vitral/base` under their Java package. `LookUpTableSine`'s two
constructors become one optional parameter, and `LookUpTableChecksum16.eval`'s
buffer overload is named `evalBuffer`, since TypeScript cannot overload on the
argument list alone; its buffer is a `Uint8Array` where Java reads signed
`byte`s, which changes nothing because Java masks the exclusive-or with
`0xff`. No placeholder was created, the source group remains an empty
checkpoint, and the phase gate has not been run.

Exit: satisfy the applicable standard phase checks.

### Phase 30: I/O wrappers

Source group: `31_io_wrapper.txt` (2 class entries).

Work: Complete the Node and browser resource mappings and port ShapeDescriptorPersistence. Verify the existing HTTP fixture publication and error handling. Review the shared stream semantics with Phase 32.

Class inventory:

```text
vsdk.toolkit.io.metadata.ShapeDescriptorPersistence
vsdk.toolkit.io.PersistenceElement
```

Exit: satisfy the applicable standard phase checks.

### Phase 31: I/O context checkpoint

Source group: `32_io_context.txt` (0 class entries).

Work: The authoritative group has no class entries. Document the runtime boundary and create no empty context type.

Class inventory:

```text
(no class entries; the file contains only its group header)
```

Exit: satisfy the applicable standard phase checks.

### Phase 32: Binary I/O checkpoint

Source group: `33_io_bin.txt` (0 class entries).

Work: Verify binary semantics in the shared wrappers, including Java long, signedness, endianness and EOF. Account for the current pipe-stream and decompression wrappers.

Class inventory:

```text
(no class entries; the file contains only its group header)
```

Exit: satisfy the applicable standard phase checks.

### Phase 33: Image I/O

Source group: `34_io_image.txt` (8 class entries).

Work: Port image readers/writers and format detection. Keep Node filesystem concerns behind a concrete runtime adapter and preserve exact binary behavior.

Class inventory:

```text
vsdk.toolkit.io.image._NativeImageReaderWrapperHeaderInfo
vsdk.toolkit.io.image.ImageNotRecognizedException
vsdk.toolkit.io.image.ImagePersistence
vsdk.toolkit.io.image.ImagePersistenceHelper
vsdk.toolkit.io.image.ImagePersistenceSGI
vsdk.toolkit.io.image.ImagePersistenceTarga
vsdk.toolkit.io.image.NativeImageReaderWrapper
vsdk.toolkit.io.image.RGBColorPalettePersistence
```

Partial advance — 2026-09-12 (out of phase order, to unblock
`testsuite/OfflineExamples/Rasterizer2DExample`): the image-export path is
ported, the importers and the SGI/Targa/native readers are not. Three of the
eight entries are touched: `ImageNotRecognizedException` and
`ImagePersistenceHelper` in full, and `ImagePersistence` restricted to its
export surface (`exportPNG`, `exportPNG_24bitRgb`, `exportPPM`, the helper
registry). The phase stays Pending until the remaining entries and the
importers land, and its gate has not been run.

The architecture departs from Java deliberately, and the departure is the
design of record for image formats:

  - Encoding is platform-neutral and lives in `@vitral/base`, so a browser
    frontend can build image bytes in memory and ship them to a remote service.
    `ImagePersistence.exportPNGToByteArray` and `exportPPMToByteArray` are the
    in-memory entry points; they have no Java counterpart because the JVM
    always had a file system.
  - PPM is hand-written and dependency-free, as in the Java and C++ ports.
  - PNG is written by `ImagePersistencePng`, a Vitral-owned encoder with no
    Java counterpart: Java reaches PNG through `javax.imageio` and C++ through
    `libpng`, and neither exists in a browser. Its `IDAT` payload comes from a
    replaceable `ImageDeflater`; the built-in default emits stored DEFLATE
    blocks and needs no dependency, and `@vitral/fs` swaps in `node:zlib` for
    real compression. Both produce standard PNG files.
  - `java.io.File` never appears in `@vitral/base`. The file-bound flavors are
    a `@vitral/fs` subclass of `ImagePersistence`, consistent with the
    PersistenceElement Runtime Architecture section.
  - Formats that warrant a third-party codec (JPEG, for one) are to be added as
    NPM dependencies of `@vitral/fs`, never of `@vitral/base`. For those, and
    for any format handed to an external codec, the port is functionally
    equivalent to Java rather than textually 1:1.

These export classes are therefore explicitly exempt from the 1:1 textual
requirement; the rasterization and geometry code they serve is not.

Partial advance — 2026-09-12 (out of phase order, to unblock
`testsuite/VitralWebTestsuiteContainer/src/WebGLExamples/ImageExample`): the
first importer is ported, `ImagePersistence.importDDSCompressed`, which reads a
DXT-compressed DDS file and keeps its blocks compressed. It is a literal port of
the Java private method, including the 128-byte minimum, the `DDS ` signature,
the 124-byte header size, the little-endian header reads, the FourCC-flag check
and the `DXT1`/`DXT3`/`DXT5` mapping; only its input differs, because reading
the bytes is the one file-system-bound step in Java and DDS parsing itself is
pure byte arithmetic. It therefore receives the already-read bytes plus the
resource name used in `ImageNotRecognizedException`, and each runtime supplies
its own reading step. The SGI, Targa and native readers, and the remaining
importers of `importRGB` / `importRGBA` / `importIndexedColor`, are still
unported, so the phase stays Pending and its gate has not been run.

The browser's reading step and extension dispatch live in `@vitral/webgl` as
`vsdk.toolkit.io.image.WebImagePersistence`, which is to that package what the
`ImagePersistence` subclass in `@vitral/fs` is to Node: it ports Java's
`importRGB(File)` / `importRGBA(File)` dispatch with a URL in place of a
`java.io.File`, sends `dds` to the reader above, and sends every other format
to the platform's own decoder through `createImageBitmap`. That decoder is the
browser's counterpart of the AWT `ImagePersistenceHelper`
(`ImagePersistenceAwt`, which reaches `javax.imageio`), and the transfer of the
decoded samples into a Vitral image is a port of
`AwtRGBImageUncompressedRenderer.importFromAwtBufferedImage` and its RGBA twin,
traversal order and byte masking included. As recorded above for the export
path, a format handed to an external codec is functionally equivalent to Java
rather than textually 1:1; no third-party codec was introduced in either
package.

Partial advance — 2026-09-13 (out of phase order, to unblock
`testsuite/VitralWebTestsuiteContainer/src/WebGLExamples/ShadersExample`):
`ImagePersistenceSGI` is ported, together with the `importIndexedColor`
half of `ImagePersistence` that reaches it. Five of the eight entries are
now touched.

The reader is literal, limits included: the 512-byte header with its magic
number 474, the channel count choosing between an indexed-color, an RGB and an
RGBA image, the RLE offset and length tables, the high bit of a run byte
selecting a literal copy over a repeat, the `ySize - y - 1` vertical flip, the
refusal of a colormap or a non-RLE storage format, the scan-line loop that
casts every image to `IndexedColorImageUncompressed` and so reads only the
grayscale subformat, and the catch-all that logs the failure and answers
whatever was built.

One boundary is crossed, the one `Md2Persistence` already crosses. Java opens a
`RandomAccessFile` and builds a `FileInputStream` from the very same file
descriptor, so `fd.seek` and the stream reads share one file pointer; the port
takes the resource's bytes and a cursor plays the part of that single pointer.
`ImagePersistence.importIndexedColor` therefore takes bytes and a resource
name, as `importDDSCompressed` already does, and `extractExtensionFromName`
reproduces `PersistenceElement.extractExtensionFromFile` over that name —
`StringTokenizer` semantics and all, so a name with no dot answers the whole
name and the case is left as found. Java's helper round is the `java.io.File`
flavor, which this platform-neutral class does not declare, so it stays with
the `@vitral/fs` subclass. `WebImagePersistence.importIndexedColor` adds the
browser's fetch.

Parity evidence: a Java reference driver reads `etc/bumpmaps/earth.bw` through
`ImagePersistence.importIndexedColor` against `java/base` and prints its size,
an FNV-1a digest of the whole 1024x1024 index buffer and a sixteen-point
diagonal sample; the TypeScript twin over the ported reader agrees on every
line. The same driver carries the `NormalMap` built from it, so the digest of
`exportToRgbImage()` is compared too, and agrees.

The Targa and native readers, and the remaining importers of `importRGB` /
`importRGBA`, are still unported, so the phase stays Pending and its gate has
not been run.

Deferred with the phase: `ImagePersistence` does not yet extend
`PersistenceElement` (Phase 30), which remains unported.

Exit: satisfy the applicable standard phase checks.

### Phase 34: XML I/O

Source group: `35_io_xml.txt` (4 class entries).

Work: Preserve Java's line-tokenizer behavior, node parameters, byte spans and malformed-input results in the shared package.

Class inventory:

```text
vsdk.toolkit.io.xml.XmlImporter
vsdk.toolkit.io.xml.XmlNode
vsdk.toolkit.io.xml.XmlNodeParam
vsdk.toolkit.io.XmlException
```

Exit: satisfy the applicable standard phase checks.

### Phase 35: VRML I/O checkpoint

Source group: `36_io_vrml.txt` (0 class entries).

Work: Record the VRML reader and its compressed-input dependencies under Phase 36.

Class inventory:

```text
(no class entries; the file contains only its group header)
```

Exit: satisfy the applicable standard phase checks.

### Phase 36: Geometry I/O

Source group: `37_io_geometry.txt` (51 class entries).

Work: Port every listed geometry importer/exporter and STEP/STL helper, including nested parsing/build state. Migrate the existing Java geometry-I/O tests only.

Class inventory:

```text
vsdk.toolkit.io.geometry._Reader3dsChunk
vsdk.toolkit.io.geometry._Reader3dsMaterialMapping
vsdk.toolkit.io.geometry._ReaderAseMeshCache
vsdk.toolkit.io.geometry._ReaderAseTriangleCache
vsdk.toolkit.io.geometry._ReaderObjVertex
vsdk.toolkit.io.geometry._ReaderPlyElement
vsdk.toolkit.io.geometry._ReaderPlyElementReader
vsdk.toolkit.io.geometry._ReaderPlyElementReaderAscii
vsdk.toolkit.io.geometry._ReaderPlyElementReaderBinaryBigEndian
vsdk.toolkit.io.geometry._ReaderPlyElementReaderBinaryLittleEndian
vsdk.toolkit.io.geometry._WriterGtsEdge
vsdk.toolkit.io.geometry._WriterGtsTriangle
vsdk.toolkit.io.geometry.EnvironmentPersistence
vsdk.toolkit.io.geometry.FontReader
vsdk.toolkit.io.geometry.Md2Persistence
vsdk.toolkit.io.geometry.ParametricBiCubicPatchPersistence
vsdk.toolkit.io.geometry.ParametricCurvePersistence
vsdk.toolkit.io.geometry.Reader3ds
vsdk.toolkit.io.geometry.ReaderAse
vsdk.toolkit.io.geometry.ReaderBinNeedForSpeed
vsdk.toolkit.io.geometry.ReaderGts
vsdk.toolkit.io.geometry.ReaderMitScene
vsdk.toolkit.io.geometry.ReaderMitScene.ImportContext
vsdk.toolkit.io.geometry.ReaderObj
vsdk.toolkit.io.geometry.ReaderPly
vsdk.toolkit.io.geometry.ReaderVrml
vsdk.toolkit.io.geometry.ReaderVtk
vsdk.toolkit.io.geometry.stepCad.reader._StepEntity
vsdk.toolkit.io.geometry.stepCad.reader._StepSolidBuilder
vsdk.toolkit.io.geometry.stepCad.reader._StepTokenizer
vsdk.toolkit.io.geometry.stepCad.reader.StepReader
vsdk.toolkit.io.geometry.stepCad.StepLengthUnit
vsdk.toolkit.io.geometry.stepCad.writer._StepEntityBuffer
vsdk.toolkit.io.geometry.stepCad.writer._StepGeometryEmitter
vsdk.toolkit.io.geometry.stepCad.writer._StepHeaderWriter
vsdk.toolkit.io.geometry.stepCad.writer._StepProductStructureEmitter
vsdk.toolkit.io.geometry.stepCad.writer._StepSolidValidator
vsdk.toolkit.io.geometry.stepCad.writer._StepTopologyEmitter
vsdk.toolkit.io.geometry.stepCad.writer._StepUnitContextEmitter
vsdk.toolkit.io.geometry.stepCad.writer.StepWriter
vsdk.toolkit.io.geometry.ViewpointBinaryPersistence
vsdk.toolkit.io.geometry.WriterGts
vsdk.toolkit.io.geometry.WriterObj
vsdk.toolkit.io.geometry.WriterVtk
vsdk.toolkit.io.geometry.stl._StlFacetEmitter
vsdk.toolkit.io.geometry.stl._StlFacetEmitter.Facet
vsdk.toolkit.io.geometry.stl._StlFaceTriangulator
vsdk.toolkit.io.geometry.stl._StlFaceTriangulator.FaceBasis
vsdk.toolkit.io.geometry.stl._StlFaceTriangulator.ProjectedVertex
vsdk.toolkit.io.geometry.stl._StlSolidValidator
vsdk.toolkit.io.geometry.stl.StlWriter
```

Partial advance — 2026-09-12 (out of phase order, to unblock the offline
examples): `EnvironmentPersistence`, `ReaderObj` with `_ReaderObjVertex`, and
now `ReaderMitScene` with its `ImportContext` are ported 1:1 into `@vitral/fs`,
which is where they belong because they resolve `java.io.File` paths.
`EnvironmentPersistence` dispatches only its `obj` branch; the other seven
extensions fall through and leave the scene untouched, which is what Java does
for an extension it does not recognize, and each gets its branch back as its
reader is ported. `ReaderMitScene`'s `texture`, `bumpmap` and
`backgroundcubemap` commands raise a message naming the missing capability,
because they need raster image *import*, which Phase 33 has not reached;
`ReaderObj.obtainTextureFromFile` is the same gap and currently always takes
Java's failure branch. The remaining 46 entries are not ported, the phase stays
Pending, and its gate has not been run.

Second partial advance — 2026-09-12 (to unblock the `MeshExample` module of the
WebGL testsuite container): `ReaderObj` and `_ReaderObjVertex` moved from
`@vitral/fs` to `@vitral/base`, under the Java package they have in
`java/base`, and took an `ObjResourceProvider` for the two `java.io`
operations they perform: `new FileReader(name)` and `new File(name).
getParentFile()`. That is the reader's own recorded `\todo` — "should not
recieve a filename, but a previously opened stream, to make it independent of
filesystems, and generalize it to URLs or whatever other connection" — and it
is what lets one 1:1 port of the Java parser serve both runtimes: `@vitral/fs`
keeps a `ReaderObj` facade with the `java.io.File` signature over a
`FileObjResourceProvider`, and `@vitral/webgl` gains
`WebEnvironmentPersistence` over a provider that replays text it has already
fetched. The parse text itself is untouched; only those three call sites moved
behind the seam.

The provider answers synchronously, as Java does, so the browser side fetches
the `.obj` and every `.mtl` its `mtllib` lines name *before* the parse begins.
That pre-pass is the only addition to the Java control flow. Evidence that the
relocation changed nothing: `WireframeOfflineExample` re-rendered to a
byte-identical `output.png`, and the browser path and the `java.io.File` path
produce identical scenes for `cow.obj`, `dumptruck.obj` and `skull.obj`. The
phase inventory is unchanged and the phase stays Pending; `EnvironmentPersistence`
stays in `@vitral/fs`, since it is the `File` dispatcher, and the URL dispatcher
beside it is `WebEnvironmentPersistence`.

Third partial advance, 2026-09-13 (to unblock the `MD2Example` module):
`Md2Persistence` is ported, bringing the phase to 6 / 51 entries. It follows the
seam the second advance established. The parser lives in `@vitral/base` under
its Java package and takes the MD2 resource's bytes, walked by a cursor that
plays the part of Java's `RandomAccessFile` pointer — on a local file the two
are the same thing, since `RandomAccessFile` is random access over exactly those
bytes. `WebMd2Persistence` in `@vitral/webgl` is the URL dispatcher beside it,
fetching the model and decoding the skin through `WebImagePersistence`, and it
carries Java's `loadImagefile` in full, because only the half that knows the
resource name can tell an unnamed skin from one that could not be read.

Everything between those two boundaries is Java's: the sixteen-word header and
its `844121161` / version 8 check, the unsigned-short masking, the
`triangles[i * 2]` / `triangles[i * 2 + 1]` interleaving of vertex and texture
indices, the per-frame scale and translate, the sign of the OpenGL command count
telling a strip from a fan, and the gamma correction of the four-argument
`read`. Two details are spelled out rather than inherited, both marked at the
code: `Math.fround` restores the `float` rounding Java performs after every
operation of `coord * scale[k] + translate[k]` and `coord / skinWidth`, and
Java's `String.trim()`, which strips every character at or below the space and
so removes the NUL padding of the MD2 name fields, is written out because
JavaScript's own `trim` strips Unicode whitespace, which NUL is not. The parity
evidence is the byte-identical reference-driver diff recorded under
`MD2Example`. The phase inventory is unchanged and the phase stays Pending.

Fourth partial advance — 2026-09-15 (out of phase order, to unblock the first
stage of `PolyhedralBoundedSolidExample`): the STEP reader
(`stepCad.reader._StepEntity`, `_StepTokenizer`, `_StepSolidBuilder`,
`StepReader`) and `stepCad.StepLengthUnit`, the STL writer
(`stl._StlFacetEmitter` with `Facet`, `_StlFaceTriangulator` with `FaceBasis`
and `ProjectedVertex`, `_StlSolidValidator`, `StlWriter`), and the abstract
`FontReader` are ported 1:1 into `@vitral/base` (13 entries, 19 / 51).
`StepReader` carries only the stream overload, because the `File` overload only
opens a stream. The builder's integer-keyed maps are
`java.util.IntegerKeyHashMap`, so vertex ids and edge order follow the JVM's
`HashMap` iteration. The concrete font factory is `WebFontReader` in
`@vitral/webgl`, recorded under Phase 41. Parity is the reference-driver diff
recorded under `PolyhedralBoundedSolidExample`. The StepWriter family and the
other readers are not ported; the phase stays Pending.

Exit: satisfy the applicable standard phase checks.

### Phase 37: SGL checkpoint

Source group: `41_sgl.txt` (0 class entries).

Work: The authoritative group has no class entries. Keep this as a verified dependency-graph checkpoint.

Class inventory:

```text
(no class entries; the file contains only its group header)
```

Exit: satisfy the applicable standard phase checks.

### Phase 38: GUI model and controllers

Source group: `61_gui.txt` (47 class entries).

Work: Complete the platform-neutral interaction model and verify the existing DOM/CSS and Canvas bindings. Compare each camera-control scheme, widget, dialog and gizmo with its Java behavior. Verify worker progress through the renderer owner in Phases 12 and 40.

Class inventory:

```text
vsdk.toolkit.gui.CameraController
vsdk.toolkit.gui.CameraControllerAquynza
vsdk.toolkit.gui.CameraControllerBlender
vsdk.toolkit.gui.CameraControllerGoogleEarth
vsdk.toolkit.gui.CameraControllerOrbiter
vsdk.toolkit.gui.CommandListener
vsdk.toolkit.gui.Controller
vsdk.toolkit.gui.dialog.InformationDialog
vsdk.toolkit.gui.ExceptionGuiBadName
vsdk.toolkit.gui.ExceptionGuiParseError
vsdk.toolkit.gui.feedback.parallel.ParallelProgressMonitorCommand
vsdk.toolkit.gui.feedback.parallel.ParallelProgressMonitorConsumer
vsdk.toolkit.gui.feedback.parallel.ParallelProgressMonitorEvent
vsdk.toolkit.gui.feedback.parallel.ParallelProgressMonitorProducer
vsdk.toolkit.gui.Gizmo
vsdk.toolkit.gui.Gui
vsdk.toolkit.gui.GuiButtonGroup
vsdk.toolkit.gui.GuiCommand
vsdk.toolkit.gui.GuiCommandExecutor
vsdk.toolkit.gui.GuiDialog
vsdk.toolkit.gui.GuiElement
vsdk.toolkit.gui.GuiMenu
vsdk.toolkit.gui.GuiMenuElement
vsdk.toolkit.gui.GuiMenuItem
vsdk.toolkit.gui.HudIcon
vsdk.toolkit.gui.KeyEvent
vsdk.toolkit.gui.LightGizmoOmniBillboard
vsdk.toolkit.gui.LightGizmoStyle
vsdk.toolkit.gui.MouseEvent
vsdk.toolkit.gui.RendererConfigurationController
vsdk.toolkit.gui.RotateGizmo
vsdk.toolkit.gui.ScaleGizmo
vsdk.toolkit.gui.TextVisualConfiguration
vsdk.toolkit.gui.TranslateGizmo
vsdk.toolkit.gui.variable.GuiBooleanVariable
vsdk.toolkit.gui.variable.GuiColorRgbVariable
vsdk.toolkit.gui.variable.GuiDoubleVariable
vsdk.toolkit.gui.variable.GuiIntegerVariable
vsdk.toolkit.gui.variable.GuiStringVariable
vsdk.toolkit.gui.variable.GuiVariable
vsdk.toolkit.gui.variable.GuiVector3DVariable
vsdk.toolkit.gui.ViewportWindow
vsdk.toolkit.gui.ViewportWindowSetManager
vsdk.toolkit.gui.visualAnalytics.PercentageWheelWidget
vsdk.toolkit.gui.visualAnalytics.PercentageWheelWidgetController
vsdk.toolkit.gui.visualAnalytics.VisualDoubleVariable
vsdk.toolkit.gui.visualAnalytics.VisualVariableSet
```

Historical advance — 2026-09-12: the `MeshExample` port brought `Gizmo`,
`LightGizmoStyle` and `LightGizmoOmniBillboard` into `@vitral/base`. They
live in `vsdk.toolkit.gui.gizmo` in the current
Java source, one package deeper than the inventory above records, which was
captured before that move; the inventory names are left as they are.
`LightGizmoStyle` is a string enum so that a style survives a structured clone,
which is what the rest of the TypeScript edition does for a Java enum that can
cross a worker boundary.

Two classes of the same family are **orphans**: `vsdk.toolkit.gui.gizmo.RayGizmo`
with its `RaySnapshot` record, and
`vsdk.toolkit.gui.tangibleInterfaces.TangibleInterfaceEvent2RayGizmoMapper`.
Neither appears in any authoritative source group, as with the
`ParallelProgressMonitor` family recorded under Phase 12; both are ported into
`@vitral/base` under their Java packages. `RayGizmo` holds its pending snapshot
in a plain field rather than an `AtomicReference`: a page runs producer and
consumer on one event loop, where a plain field already has the get-and-set
semantics the class's documented thread-safety contract asks for, and the
contract itself is unchanged.

Second historical advance — 2026-09-13 (to unblock `SolidTextureExample`):
two more **orphans** of the same family were
ported into `@vitral/base` under their Java packages —
`vsdk.toolkit.gui.gizmo.InfinitePlaneGizmo` with its `PlaneSnapshot` record,
and `vsdk.toolkit.gui.tangibleInterfaces.TangibleInterfaceEvent2InfinitePlane
GizmoMapper`. Neither appears in any authoritative source group, as with the
`RayGizmo` pair above, and neither touches the Swing question. The gizmo holds
its pending snapshot in a plain field rather than an `AtomicReference` for the
reading already recorded for `RayGizmo`: a page runs producer and consumer on
one event loop, where a plain field already has the get-and-set semantics the
documented contract asks for. Java's two `setPlane` overloads map to one
signature pair over an optional first argument.

The current library includes `RendererConfigurationController`,
`Controller`, `KeyEvent`, `CameraController` and
`CameraControllerAquynza`. The example-specific camera and renderer bindings
recorded in September remain comparison points for the behavior of the
individual examples.

Exit: satisfy the applicable standard phase checks.

### Phase 39: Software shaders

Source group: `71_render_shaders.txt` (18 class entries).

Work: Port all CPU/software shader implementations and preserve lighting, texture sampling, interpolation, and numeric edge cases.

Class inventory:

```text
vsdk.toolkit.render.shaders.ConstantShader
vsdk.toolkit.render.shaders.ConstantTextureShader
vsdk.toolkit.render.shaders.CookTorranceShader
vsdk.toolkit.render.shaders.CookTorranceShader.LightDirection
vsdk.toolkit.render.shaders.CookTorranceShader.MicrofacetParams
vsdk.toolkit.render.shaders.CpuTextureSamplingConfig
vsdk.toolkit.render.shaders.FlatShader
vsdk.toolkit.render.shaders.FlatTexturedShader
vsdk.toolkit.render.shaders.GouraudTextureShader
vsdk.toolkit.render.shaders.LightingShader
vsdk.toolkit.render.shaders.PhongBumpShader
vsdk.toolkit.render.shaders.PhongShader
vsdk.toolkit.render.shaders.PhongTextureBumpShader
vsdk.toolkit.render.shaders.PhongTextureShader
vsdk.toolkit.render.shaders.Shader
vsdk.toolkit.render.shaders.Shader.LocalShadingResult
vsdk.toolkit.render.shaders.ShaderSelector
vsdk.toolkit.render.TraceWorkspace
```

Completed — 2026-09-12 (out of phase order, to unblock
`testsuite/OfflineExamples/RaytracingOfflineExample`). All eighteen entries are
ported 1:1 and exported through `@vitral/base`. `Shader.LocalShadingResult` and
`CookTorranceShader.MicrofacetParams` are Java records, so they carry their
generated accessors; the record's `equals`/`toString` are reproduced for
`LocalShadingResult` and left off the module-private `_MicrofacetParams`, which
nothing outside the shader observes. The inventory's
`CookTorranceShader.LightDirection` has no Java source of its own: the shader
imports `Light.LightDirection`, which Phase 26 ported. `CookTorranceShader`
also guards on `info.t == null`, which cannot happen in the port because
`RayHit.t` is a non-nullable field; the unreachable half is documented in
place. The parity evidence is the example above, whose rendered image exercises
`GouraudTextureShader` (through `LightingShader`) with ambient and point
lights, shadow rays, and the specular term, and matches Java byte for byte.

Exit: satisfy the applicable standard phase checks.

### Phase 40: CPU rendering

Source group: `72_render.txt` (35 class entries).

Work: Port CPU renderers, tiling, rasterization, hidden-line, wireframe, stereogram, and ray-tracing behavior. Migrate the existing rendering tests only.

Class inventory:

```text
vsdk.toolkit.render._AppelEdgeCache
vsdk.toolkit.render._AppelEdgeSegment
vsdk.toolkit.render.AutoStereogramGenerator
vsdk.toolkit.render.HiddenLineRenderer
vsdk.toolkit.render.PolyhedralBoundedSolidDebugger
vsdk.toolkit.render.Rasterizer2D
vsdk.toolkit.render.RenderContext
vsdk.toolkit.render.RenderingElement
vsdk.toolkit.render.SimpleRaytracer
vsdk.toolkit.render.SimpleRaytracer.SceneObjectRenderData
vsdk.toolkit.render.SimpleRaytracer.SceneRenderCache
vsdk.toolkit.render.Tile
vsdk.toolkit.render.TileGenerationStrategy
vsdk.toolkit.render.TileGenerator
vsdk.toolkit.render.WireframeRenderer
vsdk.toolkit.render.Rasterizer2D.FillEdge
vsdk.toolkit.render.Rasterizer2D.SpanShader
vsdk.toolkit.render.hiddenLine._AppelEdgeCache
vsdk.toolkit.render.hiddenLine._AppelEdgeSegment
vsdk.toolkit.render.hiddenLine.HiddenLineRenderer
vsdk.toolkit.render.hiddenLine.HiddenLineRenderer.AppelAlgorithmDump
vsdk.toolkit.render.hiddenLine.HiddenLineRenderer.AppelEdgeDump
vsdk.toolkit.render.hiddenLine.HiddenLineRenderer.AppelEventDump
vsdk.toolkit.render.hiddenLine.HiddenLineRenderer.AppelSegmentDump
vsdk.toolkit.render.hiddenLine.WireframeRenderer
vsdk.toolkit.render.raster.Rasterizer2D
vsdk.toolkit.render.raster.Rasterizer2D.FillEdge
vsdk.toolkit.render.raster.Rasterizer2D.SpanShader
vsdk.toolkit.render.raytracing.RasterTileArea
vsdk.toolkit.render.raytracing.RasterTileGenerationStrategy
vsdk.toolkit.render.raytracing.RasterTileGenerator
vsdk.toolkit.render.raytracing.RenderContext
vsdk.toolkit.render.raytracing.SimpleRaytracer
vsdk.toolkit.render.raytracing.SimpleRaytracer.SceneObjectRenderData
vsdk.toolkit.render.raytracing.SimpleRaytracer.SceneRenderCache
```

Partial advance — 2026-09-12 (out of phase order, to unblock
`testsuite/OfflineExamples/Rasterizer2DExample`): `RenderingElement` and
`vsdk.toolkit.render.raster.Rasterizer2D` (with its `FillEdge` and
`SpanShader` members) are ported 1:1 and exported through `@vitral/base`.
Java's `Collections.sort` over the active-edge table maps onto the ported
`java.util.Collections.sort`, so the TimSort comparison sequence and the
`Double.compare` / `Integer.compare` tie-breaks are preserved; every `(int)`
cast is `Math.trunc`. The remaining 32 entries, including the second
`vsdk.toolkit.render.Rasterizer2D` copy, the hidden-line and ray-tracing
families, are not ported, the phase stays Pending, and its gate has not been
run.

Verified against the Java reference on 2026-09-12: the three
`Rasterizer2DExample` programs produce images whose decoded pixels are
identical to those of the Java build (`base` + `awt`), 0 differing pixels in
640x480 for all three.

Second partial advance — 2026-09-12 (out of phase order, to unblock
`testsuite/OfflineExamples/RaytracingOfflineExample`): the whole ray-tracing
family is ported 1:1 and exported through `@vitral/base` —
`vsdk.toolkit.render.raytracing.RasterTileArea`,
`RasterTileGenerationStrategy`, `RasterTileGenerator`, `RenderContext`,
`SimpleRaytracer`, and the latter's `SceneObjectRenderData` and
`SceneRenderCache`. Java's `(byte)` narrowing of the shaded colour is
reproduced explicitly (`Math.trunc` then the low eight bits, signed) rather
than through the clamping `VSDK.unsigned8BitInteger2signedByte`, because Java
narrows rather than clamps; the `ThreadLocal<TraceWorkspace>` maps onto the
ported `java.lang.ThreadLocal`, which is per-runtime and therefore per worker,
exactly the reentrancy property the Java field provides per thread.
`RasterTileGenerationStrategy` is a string enum so a strategy survives a
structured-clone hop to a worker. Parity: the 1920x1080 render of
`etc/geometry/mitscenes/balls.ray` is byte-identical to Java's, serial and
parallel.

Still not ported after this advance: `AutoStereogramGenerator`,
`PolyhedralBoundedSolidDebugger`, the hidden-line family
(`_AppelEdgeCache`, `_AppelEdgeSegment`, `HiddenLineRenderer` and its four
dump records) beyond `WireframeRenderer`, and the stale
`vsdk.toolkit.render.*` duplicates of classes that now live under
`raster/`, `raytracing/` and `hiddenLine/`. The phase stays Pending and its
gate has not been run.

Third partial advance — 2026-09-15 (out of phase order, to unblock the first
stage of `PolyhedralBoundedSolidExample`): `render.hiddenLine.HiddenLineRenderer`
is ported 1:1 with its package-private `_AppelEdgeCache` and `_AppelEdgeSegment`
and its four dump records (`AppelAlgorithmDump`, `AppelEdgeDump`,
`AppelSegmentDump`, `AppelEventDump`), as members of the `HiddenLineRenderer`
namespace (7 entries, 19 / 35). The diagnostic trace's `String.format("%.5f")`
becomes `toFixed(5)`. The contour, visible and hidden line buffers are
byte-identical to Java's for every model of the module; see the
reference-driver record under `PolyhedralBoundedSolidExample`. The phase stays
Pending.

Fourth partial advance — 2026-11-20 (following the Java commit that adds the
Weiler-Atherton hidden surface renderer): `render.hiddenSurface.HiddenSurfaceRenderer`
is ported 1:1 with its package-private `_WeilerAthertonArea`,
`_WeilerAthertonSourcePolygon`, `_WeilerAthertonPiece`,
`_WeilerAthertonLitFace`, `_WeilerAthertonEdgeGrid` and
`_WeilerAthertonShadedPart`, and its public `VisiblePolygon` as a member of
the `HiddenSurfaceRenderer` namespace; `HiddenSurfaceRendererTest` is
translated. Its dependencies were aligned with Java at the same time: the
abandoned-traversal guard of `WeilerAthertonPolygonClipper`
(`isLastClipAbandoned`), `RendererConfiguration.withShadows` (F10 in
`RendererConfigurationController`), the render modes of the viewport set
(`Viewport.RENDER_MODE_HIDDEN_LINES` / `RENDER_MODE_HIDDEN_SURFACES`,
`commandForRenderMode`, `renderModeForCommand`, the available render modes of
`ViewportSet`, their cycling in `ViewportSetInteractionTechniques`, with
`ViewportSetRenderModesTest`), and `SimpleBody.transformNormalToWorld` /
`transformPointToWorld`, which now use the rotation quaternion and the scale
as Java does instead of the inverse placement matrix. Parity: the visible
polygons (body, face, lights and every vertex as raw IEEE-754 bits) of the
scenes of `HiddenSurfaceRendererTest`, plus a 16-body scene that exercises the
screen subdivision and the two shadow scenes with one light each, are
identical to Java's (5521 polygons, Java run with
`-XX:-UseLibmIntrinsic`). The phase stays Pending.

Fifth partial advance — 2026-11-28: the last two inventory classes with a
Java source are ported 1:1 and exported through `@vitral/base`.

- `render.AutoStereogramGenerator`: Java's `int` arithmetic is kept, with
  every `(int)` cast and integer division as `Math.trunc`. The defaults
  overload delegates as Java does. Reference driver: 3 image sizes, 2 tile
  patterns, the defaults overload and 3 explicit parameter sets (negative
  and over-256 animation offsets included). The 24 output images are
  byte-identical to Java's.
- `render.PolyhedralBoundedSolidDebugger`: Java loads
  `vsdk.toolkit.render.jogl.Jogl2PolyhedralBoundedSolidDebugger` by name
  and answers null when the class is missing. A JavaScript runtime has no
  class path, so a package registers a factory under that Java name with
  `registerPlugin`. Until one does, `createOfflineRenderer` answers null,
  which is Java's result without JOGL. No browser renderer registers one
  yet.
- The rest of the inventory has nothing left to port:
  - The nested names (`FillEdge`, `SpanShader`, `SceneObjectRenderData`,
    `SceneRenderCache`, the four `Appel*Dump` records) are ported as
    namespace members.
  - The `vsdk.toolkit.render.*` duplicates (`_AppelEdgeCache`,
    `_AppelEdgeSegment`, `Tile`, `TileGenerationStrategy`, `TileGenerator`,
    `Rasterizer2D` and its members) have no source in the current Java tree.
- Phase 14/15 parity fix found by the stereogram driver: `Image.getColorRgbBiLinear`
  folded Java's two-stage interpolation (`FU0`, `FU1`, then
  `FU0 + dv * (FU1 - FU0)`) into one regrouped expression. That rounds
  differently, so `ImageProcessing.resize` upscaling changed some pixels
  (64x64 to 128, 149, 150 or 200). The method now keeps Java's grouping.
  Resizing 64x64, 100x37 and 31x17 tiles to 8 upscaled and 2 downscaled
  sizes is byte-identical to Java.

Exit: satisfy the applicable standard phase checks.

### Phase 41: GPU rendering architecture checkpoint

Source group: `72_render_opengl.txt` (0 class entries).

Work: Complete the applicable current JOGL4 behavior in WebGL 2. Verify canvas initialization, shader compilation, geometry and texture upload, framebuffer readback, resource disposal and context recovery in the examples and editor.

Class inventory:

```text
(no class entries; the file contains only its group header)
```

Partial advance — 2026-09-12 (selected browser/WebGL runtime): created the
`@vitral/webgl` workspace as the browser GPU integration package and added
`vsdk.toolkit.render.webgl.WebGLCameraRenderer`,
`vsdk.toolkit.render.webgl.WebGLMatrixRenderer`, and
`vsdk.toolkit.fixtures.WebGLSimpleCorridorSample`. The camera renderer ports
the activation and camera-volume/frustum line generation pattern from
`java/jogl4/src/main/vsdk/toolkit/render/jogl/Jogl4CameraRenderer.java` to
WebGL2, and the corridor fixture ports the generated quad corridor used by
`java/jogl4/src/main/vsdk/toolkit/fixtures/Jogl4SimpleCorridorSample.java`.
Both use `@vitral/base` math/camera models and upload geometry through reusable
WebGL resources. This is a partial GPU-backend advance only: the source group
remains an empty checkpoint, the broader JOGL4 renderer family is not declared
complete, and the phase gate has not been run.
To support the browser event boundary selected for this runtime,
`vsdk.toolkit.gui.WebSystem` was also added in `@vitral/webgl` as the Web
counterpart to the AWT/GLFW event adapters; Phase 38 camera controllers remain
unported.

Partial advance — 2026-09-12 (out of phase order, to unblock the `ImageExample`
module of the WebGL testsuite container): the image-texture renderer family and
the shader plumbing it needs are ported from
`java/jogl4/src/main/vsdk/toolkit/render/jogl/` to
`vsdk.toolkit.render.webgl`, namely `WebGLImageRenderer`,
`WebGLRGBImageUncompressedRenderer`, `WebGLRGBAImageUncompressedRenderer`,
`WebGLRGBAImageCompressedRenderer`,
`WebGLRendererConfigurationShaderSelector`, `WebGLShaderProgramUtil` and
`WebGLShaderLoader`. The selection rules, the nine-program set and its GLSL
file pairs, the uniform activation sequence, the textured-quad vertex
submission at attribute locations 0 and 2, the lower-left overlay geometry, and
the DXT1/DXT3/DXT5 CPU decompressor are the Java ones; the DXT parity evidence
is recorded under WebGL Example Programs. The recurring runtime boundaries,
each documented beside the code, are: a texture is an object rather than an
`int`, so Java's `textureId <= 0` failure sentinel is `null`; per-process
static GL state is held per `WebGL2RenderingContext` in a `WeakMap`, because a
page can own several contexts; a GLSL source arrives over `fetch`, so program
creation is asynchronous and the nine programs compile on first selection
instead of all at once; WebGL has no `glPolygonMode` and no
`glBindFragDataLocation`, and cannot generate mipmaps for a compressed texture,
which is why the compressed path uploads level 0 with a non-mipmapped
minification filter. `WebGLSimpleCorridorSample` and `WebGLImageRenderer` also
gained an asynchronous `prepare(gl)`, which has no Java counterpart and exists
for the drawing-buffer rule recorded under WebGL Example Programs. The broader
JOGL4 renderer family — arrows, lights, lines, MD2 meshes, min/max, 2D
polygons, ray and plane gizmos, solid textures and spheres — is still unported,
the source group remains an empty checkpoint, and the phase gate has not been
run.

Third partial advance — 2026-09-12 (out of phase order, to unblock the
`MeshExample` module of the WebGL testsuite container): `Jogl4LineRenderer` and
`Jogl4LightRenderer` are ported to `vsdk.toolkit.render.webgl` as
`WebGLLineRenderer` and `WebGLLightRenderer`. Both drawing paths of the line
renderer are the Java ones — `LINES` for a line one pixel wide or thinner, and
the host-side expansion into screen-space quads for a thicker one, with its
six-plane clip-volume test and pixel-space perpendicular offset unchanged — and
both light gizmo styles are the Java ones, including the orthogonal and
perspective branches of the screen-size calculation. The recurring boundaries
are the ones already recorded for this phase: per-process static GL state held
per context in a `WeakMap`, a shader source reached over `fetch` so drawing is
asynchronous, and `glGetUniformLocation`'s negative-int sentinel becoming
`null`. Two more are specific to these two classes: `gl.lineWidth` exists in
WebGL2 but every engine in practice supports only 1.0, which is why the thick
path is what actually delivers a wide line; and Java's overload that takes the
loose `GL` interface and checks `gl.isGL4()` has no counterpart, since
`WebGL2RenderingContext` is already the narrowed type.

`WebGLShaderPreprocessor` also gained a `gl_PointSize` write and its
`pointSizeLocal` uniform, for the reason recorded under `MeshExample`: OpenGL
sizes a point with the client-side `glPointSize`, which WebGL does not have,
so in GLSL ES the vertex shader must write the size itself, and no VitralSDK
shader does because none has to under OpenGL 4.

Fourth partial advance — 2026-09-12 (completing the gizmo chain the previous
advance left open): `Jogl4ArrowRenderer`, `Jogl4SphereRenderer`,
`Jogl4MinMaxRenderer` and `Jogl4RayGizmoRenderer` are ported to
`vsdk.toolkit.render.webgl` as `WebGLArrowRenderer`, `WebGLSphereRenderer`,
`WebGLMinMaxRenderer` and `WebGLRayGizmoRenderer`. The procedurally tessellated
meshes are the Java ones vertex for vertex — the arrow's shaft side, shaft
bottom cap, annular head cap and cone side with its slant normals, at sixteen
slices; the sphere's `(stacks - 1) * slices * 2` triangles built from the
`Sphere`'s own `spherePosition` / `sphereNormal` / `sphereTangent` /
`sphereBinormal` with the `1 - s` texture-coordinate flip; the min/max box's
twelve axis-aligned edges; and the ray gizmo's indicator fin. So are the four
sphere passes and their materials, the microfacet uniform block with its
non-microfacet defaults, the normal-overlay line construction, the
`ensureMesh` rebuild condition, and the gizmo's per-body dispatch over the
scene `RayGizmo.buildScene()` answers.

The boundaries crossed are this phase's recurring ones — per-context `WeakMap`
state instead of static fields, `fetch`-borne GLSL making every drawing entry
point asynchronous, a texture object instead of an `int` so `textureId > 0`
becomes `!== null`, and `glGetUniformLocation`'s negative sentinel becoming
`null` — plus the two already recorded for `MeshExample`: no `glPolygonMode`,
so the sphere's wires pass draws triangle edges as indexed `LINES`, and no
`glPointSize`, so its points pass sets the `pointSizeLocal` uniform. One Java
behavior is kept deliberately rather than corrected: `Jogl4ArrowRenderer.
ensureMesh` builds its mesh once and ignores the `Arrow` of every later call,
which is sound because the gizmo that drives it builds all of its arrows from
one `Arrow` instance.

With this the `MeshExample` module draws its ray gizmo, and the divergence
recorded for it under WebGL Example Programs is closed. Of the JOGL4 renderer
family, MD2 meshes, 2D polygons, plane gizmos and solid textures remain
unported. The source group remains an empty checkpoint and the phase gate has
not been run.

Fifth partial advance — 2026-09-13 (out of phase order, to unblock the
`SolidTextureExample` module of the WebGL testsuite container):
`Jogl4SolidTextureRenderer` and `Jogl4InfinitePlaneGizmoRenderer` are ported to
`vsdk.toolkit.render.webgl` as `WebGLSolidTextureRenderer` and
`WebGLInfinitePlaneGizmoRenderer`. The volume upload keyed on its revision, the
per-body bounding-box uniforms that normalize an object-space position into a
texture coordinate, the eight-light uniform sequence, the plane gizmo's tangent
frame and its area-fraction sizing with both projection branches, and the
shaders themselves — `solidTextureVertexShader.glsl` and
`solidTexturePixelShader.glsl` unchanged — are the Java ones.

Beyond this phase's recurring boundaries, three are specific to these two
classes and are described in full under `SolidTextureExample`: GLSL ES has no
`gl_ClipDistance` and WebGL no `GL_CLIP_DISTANCE0`, so `WebGLShaderPreprocessor`
gained a clip-distance-to-varying translation with a matching fragment discard,
applied unconditionally on both stages so that any shader pair still links; GLSL
ES gives `sampler3D` no default precision, so the preprocessor now states
`precision highp sampler3D;` in both stage headers, without which
`solidTexturePixelShader.glsl` does not compile; and Java's constructor takes
the `Path` of the shader directory, which has no counterpart because
`WebGLShaderLoader` owns the served base URL, so the renderer takes no
constructor argument.

With this the `SolidTextureExample` module draws both of its operation modes and
its cutting plane.

Sixth partial advance, 2026-09-13 (to unblock the `MD2Example` module):
`WebGLMd2MeshRenderer` is added, the port of
`vsdk.toolkit.render.jogl.Jogl4Md2MeshRenderer`. The frame interpolation, the
strip and fan expansion with its even/odd winding correction, the `1.0 - v`
texture-coordinate flip, the three passes with their materials and depth state,
and the uniform sequence are the Java ones. Beyond this phase's recurring
boundaries — static GL state held per context in a `WeakMap`, `fetch`-borne GLSL
making the draw asynchronous, a texture object instead of an `int`, and
`getUniformLocation` answering `null` instead of a negative int — it makes the
same two substitutions `MeshExample`'s renderer makes, for `glPolygonMode(...,
GL_LINE)` with `POLYGON_OFFSET_LINE` and for `glPointSize`; both are described
under `MD2Example`. It also gained an asynchronous `prepare(gl)`, which has no
Java counterpart and exists for the drawing-buffer rule recorded under WebGL
Example Programs.

Sixth partial advance — 2026-09-15 (out of phase order, to unblock the
`PolygonClippingExample` module of the WebGL testsuite container):
`Jogl4Polygon2DRenderer` is ported to `vsdk.toolkit.render.webgl` as
`WebGLPolygon2DRenderer`. The three passes are the Java ones and run in the
Java order — the filled surface, then one wire loop per contour, then every
contour vertex as a point, each behind its `RendererConfiguration` flag — and
the uniform names, the two-pixel line width, the eight-pixel point size and the
two attribute layouts are unchanged. Java's five `int` object names travel
together as one record, because a WebGL name is an object; the caller still
creates and owns them in its own `init`, as `JoglPolygonClippingRenderer` does.

This is the one place in the migration so far where a Java dependency has no
browser counterpart at all, rather than a different spelling. Java fills a
surface with `GLU.gluNewTess()`, and GLU is a companion library of desktop
OpenGL that a page cannot reach. What `Jogl4Polygon2DRenderer` asks of it is
narrow — it sets no winding rule, so the tessellator runs under its default,
`GLU_TESS_WINDING_ODD`, and the collector keeps nothing but the triangle
coordinates — so `_Polygon2DOddWindingTessellator` computes that region
directly: crossing edges are split at their crossings, the distinct endpoint
ordinates become the bands of a sweep, and the crossings inside a band are
paired two by two, which is the odd rule. The region is exact; the particular
triangles are not GLU's, and nothing downstream can tell, because the surface
pass is drawn flat in one colour and the mode that reveals a triangulation is
the monotone decomposition `JoglTriangularRenderer` already carries. The
evidence is recorded with the program under WebGL Example Programs.

With this the JOGL4 renderer family is fully ported. The source group remains an
empty checkpoint and the phase gate has not been run.

Shader-compatibility evidence — 2026-09-13 (gathered for
`testsuite/VitralWebTestsuiteContainer/src/WebGLExamples/ShadersExample`, which
reaches every program the selector can build): all twenty GLSL files of
`etc/glslShaders` were put through `WebGLShaderPreprocessor` and compiled in a
real WebGL2 context, and all eleven programs built from them were linked —
the nine of `WebGLRendererConfigurationShaderSelector` plus the line and
solid-texture pairs. Twenty of twenty compile and eleven of eleven link under
GLSL ES 3.00, with every microfacet uniform live in the linked Cook-Torrance
programs. No OpenGL 4.1 shader of this tree needs rewriting to run under WebGL2,
and the three translations the preprocessor already performs — the
`layout(location = N) out` rewrite, the `gl_PointSize` injection, the
clip-distance-to-varying pair and the `sampler3D` precision default — are
sufficient for the whole set.

Seventh partial advance — 2026-09-15 (for `PolyhedralBoundedSolidExample`):
the JOGL4 polyhedral-solid family is added to `@vitral/webgl` as
`render.webgl.polyhedralBoundedSolid.WebGLPolyhedralBoundedSolidRenderer`,
`WebGLPolyhedralBoundedSolidDebugRenderer` and
`WebGLPolyhedralBoundedSolidDebugHUDRenderer`, the last drawing into a 2D canvas
context where Java draws into a `Graphics2D`. It comes with
`WebGLSimpleMaterialRenderer`, the active material per context, and with
`io.geometry.WebFontReader`, the browser's `FontReader`.

`WebFontReader` reproduces FreeType's unhinted outline as AWT reports it and
matches `AwtFontReader` exactly on the two fonts of `etc/fonts`. The solid
renderer replaces the GLU face tessellator with
`MonotoneDecompositionTriangulator`, keeping the face's own vertices and the
contour winding, for the reason recorded under the module.
`WebGLLineRenderer.prepare` and `WebGLPolyhedralBoundedSolidRenderer.prepare`
compile their programs ahead of the first frame. `_PolyhedralBoundedSolidFaceValidator`
is now exported from the `@vitral/base` barrel.

Exit: satisfy the applicable standard phase checks.

### Phase 42: Animation

Source group: `75_animation.txt` (4 class entries).

Work: Port animation controllers, timers, and frame state against the selected runtime's scheduling model while preserving deterministic state transitions.

Class inventory:

```text
vsdk.toolkit.animation.AnimationEvent
vsdk.toolkit.animation.AnimationEventGenerator
vsdk.toolkit.animation.AnimationListener
vsdk.toolkit.animation.Md2AnimationListener
```

Ported 2026-09-13, 4 / 4 entries, out of normal phase order, to give
`MD2Example` the animation its Java program runs on. `AnimationEvent`,
`AnimationListener` and `Md2AnimationListener` are literal: the single elapsed-
seconds field with its accessors, the abstract `tick` beside the two
do-nothing pause methods, and the listener that copies the event's time into
the mesh, comment lines included.

`AnimationEventGenerator` crosses one boundary, and it is the class's whole
reason for existing in Java. It `implements Runnable`, and its `run()` is an
unbounded `while ( true )` with a `Thread.sleep(1000/fps)` in it, which a caller
hands to a `Thread` for the life of the JVM. A browser has one thread and a task
that never yields freezes the page, so `run()` arms the host's own interval timer
at the same twenty-four ticks a second and returns. The loop is rotated around
the wait, which is what a timer callback is: Java reads the elapsed time *before*
the sleep and dispatches *after* it, so the event a listener sees carries the
time of one period earlier, and that ordering is preserved. A `stop()` is added,
which Java needs no counterpart for because its thread is a daemon that dies with
the process, while a module inside the container is torn down while the page
lives on. The single reused `AnimationEvent` object and the registration-order
dispatch are Java's.

The two example modules that predate this phase keep their own timers:
`MeshExample` and `SolidTextureExample` each carry an `animation.Animation
Controller` whose Java original drives repaints and gizmo aging rather than a
mesh, and neither is rewritten on top of the generator here. The source group's
four entries are complete; the standard phase gate has not been run.

Exit: satisfy the applicable standard phase checks.

### Phase 43: Application framework

Source group: `81_framework.txt` (3 class entries).

Work: Port the reusable Component and shape-descriptor generator contracts. The web editor's application orchestration already exists.

Class inventory:

```text
vsdk.framework.Component
vsdk.framework.shapeMatching.plugins.ShapeDescriptor2DGeneratorFourier
vsdk.framework.shapeMatching.ShapeDescriptor2DGenerator
```

Exit: satisfy the applicable standard phase checks.

### Phase 44: GUI persistence

Source group: `81_io_gui.txt` (1 class entries).

Work: Verify the implemented Aquynza GUI and dialog JSON loading paths. Cover command images, translated labels, validation, resource failures and dialog bindings in the browser host.

Class inventory:

```text
vsdk.toolkit.io.gui.GuiPersistence
```

Exit: satisfy the applicable standard phase checks.

### Phase 45: Orphan-inventory closure

Source group: `999_orphans.txt` (0 class entries).

Work: The authoritative group has no class entries. Re-run the inventory comparison, resolve every unmatched production symbol, and close the migration only when no unexplained orphan remains.

Class inventory:

```text
(no class entries; the file contains only its group header)
```

Exit: satisfy the standard phase gate and then complete the final inventory audit.

### Phase 46: Parity and dependency closure

Refresh the source and export ledger after each applicable phase closes. Check
the retained September source findings and the current unmatched paths. Verify
the public API, private algorithm dependencies, recorded technology mappings,
test coverage and installed-package entry points. Distinguish file reachability
from emitted-package availability. Keep the files retained by the 2026-09-16
decision in the review.

## Original Java test-source inventory

This inventory records the tests and fixtures selected for the original
migration. Use it with the current Java test tree and dependency closure.

```text
java/base/src/test/vsdk/toolkit/common/linealAlgebra/Matrix4x4Test.java
java/base/src/test/vsdk/toolkit/common/linealAlgebra/Matrix4x4fTest.java
java/base/src/test/vsdk/toolkit/common/linealAlgebra/MatrixNxMTest.java
java/base/src/test/vsdk/toolkit/common/linealAlgebra/QuaternionTest.java
java/base/src/test/vsdk/toolkit/common/linealAlgebra/QuaternionfTest.java
java/base/src/test/vsdk/toolkit/common/linealAlgebra/Vector2DTest.java
java/base/src/test/vsdk/toolkit/common/linealAlgebra/Vector2DfTest.java
java/base/src/test/vsdk/toolkit/common/linealAlgebra/Vector3DTest.java
java/base/src/test/vsdk/toolkit/common/linealAlgebra/Vector3DfTest.java
java/base/src/test/vsdk/toolkit/common/linealAlgebra/Vector4DTest.java
java/base/src/test/vsdk/toolkit/common/linealAlgebra/Vector4DfTest.java
java/base/src/test/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperatorsTest.java
java/base/src/test/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidGeometricValidatorTest.java
java/base/src/test/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidKimrhMikrhTest.java
java/base/src/test/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicyTest.java
java/base/src/test/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidPredicatesTest.java
java/base/src/test/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidQuantitativeInvisibilityTest.java
java/base/src/test/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTestFixtures.java
java/base/src/test/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologicalValidatorTest.java
java/base/src/test/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologySummaryTest.java
java/base/src/test/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngineTest.java
java/base/src/test/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PredicatesQiScanTest.java
java/base/src/test/vsdk/toolkit/gui/CameraControllerInteractionTest.java
java/base/src/test/vsdk/toolkit/io/geometry/ReaderMitSceneTest.java
java/base/src/test/vsdk/toolkit/io/geometry/stl/StlWriterTest.java
java/base/src/test/vsdk/toolkit/processing/CurveModelerGlyphWeldTest.java
java/base/src/test/vsdk/toolkit/processing/CurveModelerRotationalSweepTest.java
java/base/src/test/vsdk/toolkit/processing/linealAlgebra/LinearAlgebraStrategiesConsistencyTest.java
java/base/src/test/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/AlgebraicIdentityRegressionTest.java
java/base/src/test/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/BooleansFromReferenceObjectPairsTest.java
java/base/src/test/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/CsgKurlanderBowlAllMotifsRegressionTest.java
java/base/src/test/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/CsgKurlanderBowlFirstStarRegressionTest.java
java/base/src/test/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/CsgMoonCylinderDifferenceDegeneracyTest.java
java/base/src/test/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/CsgSampleCorpus.java
java/base/src/test/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/CsgSampleCorpusFixtures.java
java/base/src/test/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/IntersectionCurveBuilderTest.java
java/base/src/test/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/IntersectorParametricOrderingTest.java
java/base/src/test/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/IntersectorWeldTest.java
java/base/src/test/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/KurlanderBowlMotifSweepRegressionTest.java
java/base/src/test/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/KurlanderBowlStarInvariantTest.java
java/base/src/test/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/KurlanderMotif4OperationMatrixTest.java
java/base/src/test/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/Mant1988Section15_2UnionDiagnostic.java
java/base/src/test/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidPreflightTest.java
java/base/src/test/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidSetOperatorCoplanarPredicateTest.java
java/base/src/test/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidSetOperatorTest.java
java/base/src/test/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidSplitterMantylaRegressionTest.java
java/base/src/test/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidStrictSetOpTest.java
java/base/src/test/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/SectoroverlapTraceDiagnosticTest.java
java/base/src/test/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/SetOpConnectNoLooseInvariantTest.java
java/base/src/test/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/SetOpConnectScanJoinTest.java
java/base/src/test/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/SetOpFinishInvariantsTest.java
java/base/src/test/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/Stage6FaceSubdivisionDiagnosticTest.java
java/base/src/test/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/StepperMotorGuideCsgFixture.java
java/base/src/test/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/StepperMotorGuideStrictValidationTest.java
java/base/src/test/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/VertexFaceClassifierCoplanarTest.java
java/base/src/test/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/VertexVertexEndpointRecoveryTest.java
java/base/src/test/vsdk/toolkit/render/HiddenLineRendererTest.java
java/base/src/test/vsdk/toolkit/render/TileGeneratorTest.java
java/testsuite/VSDKExamples/MatrixExample/src/test/matrixexample/MatrixExampleSlowStrategiesTest.java
```

For the complete-common milestone at Phase 10, migrate the eleven tests under `java/base/src/test/vsdk/toolkit/common/linealAlgebra/` and `java/testsuite/VSDKExamples/MatrixExample/src/test/matrixexample/MatrixExampleSlowStrategiesTest.java`. Later test files move with the first phase that completes their production dependency closure.

## Completion criteria

- Close the 46 phases against the current inventory, including newly added
  Java sources and the applicable explicit platform mappings.
- Verify observable core behavior with Java reference tests and the corresponding
  TypeScript checks, including numeric, error and degeneracy cases.
- Pass the clean workspace build, Vitest suites, packaging and temporary-consumer
  declaration checks. Record skips and their rationale.
- Build and run the browser container. Verify DOM input, persistence, Canvas 2D
  and WebGL 2 on the migrated entry points, including lifecycle and teardown.
- Verify parallel jobs through real Web Workers and the Node adapter where used.
  Compare outputs, progress and failure behavior with the reference paths.

## Historical Phase 1 checkpoint — 2026-09-09

The package and build foundation is operational with Node.js 22+, a pinned
lockfile, strict compilation, a versioned artifact, and a temporary-consumer
check that installs the `.tgz` and compiles a public import. `@vitral/base`
remains free of filesystem APIs; `@vitral/fs` retains server-side local adapters.

Date and formatting adapters (`Date`, `SimpleDateFormat`, `DecimalFormat`),
binary and character I/O, ordered collections, and atomic/queue utilities are
complete. Concurrency uses a Web Worker contract with structured-cloneable
messages, cooperative cancellation, propagated errors, and idempotent
termination; no main-thread workload execution was added as a worker substitute.

The mechanical inventory is 86 of 86 symbols. Local adapters (`FileReader` and
`RandomAccessFile`) are exported only from `@vitral/fs`. DOM contracts alias
native DOM APIs, while HTTP/WebSocket and gzip use Web APIs. Phase 1 could close
only after recording the full gate result and reviewing serialization-adapter
semantics before any Vitral class depended on them.

Gate run on 2026-09-09: `npm run verify` passed (strict compilation of
`@vitral/base` and `@vitral/fs`, packaging, dry-run inspection, and import from
a temporary consumer). Vitest ran serially and reported zero test files: no
allowed Java test yet had a production closure contained in that phase. The
source scan found no `TODO`, `FIXME`, `@ts-ignore`, `@ts-nocheck`, focused tests,
or skipped tests.

## ApplicationCases/SceneEditorApplication

Port of the Java `SceneEditorApplication` (AWT/Swing + JOGL4) in
`typescript/testsuite/VitralWebTestsuiteContainer/src/ApplicationCases/SceneEditorApplication`,
listed in the explorer tree of the container under `ApplicationCases`. The initial
port was recorded on 2026-10-01. The architecture below reflects the current
source.

- UI: the GUI is built with the DOM from the same Aquynza GUI definitions
  (`etc/gui/english.json`, `spanish.json`; languages listed by the generated
  `etc/gui/languages.json`). Reusable widgets (menus, buttons, tabs, split
  pane, windows, file chooser, generic editor) live in `@vitral/webgl`
  (`render/html`, `gui/editor`) with `styles/vitral-html-gui.scss`; the look and
  feel themes (Motif, Metal, Windows, GTK) are CSS classes.
- Rendering: the WebGL 2 mode draws into an offscreen framebuffer for color
  and depth readback (`depthPack*.glsl`). The Canvas 2D backend presents
  wireframe and hidden lines, Weiler–Atherton hidden surfaces with optional
  shadows, and the raytraced image. The editor selects the backend by viewport
  mode.
- Raytracing: Web Workers (`ObjectGraphTransfer` + `SimpleSceneSnapshotTransfer`).
- Files: fixture reads use `etc` (browsed through `etc/index.json`), and
  exports are downloads. The GUI controller loads the language definitions and
  `dialogs.json` through the shared persistence classes.
- Agent API: the Java MCP protocol (same tools and JSON) is served in-page by
  `window.vitralEditorMCP.request(line)` when the page URL carries `?arg=-s`.
- Class names: a production bundle renames classes, so Java names come from
  an `instanceof` mapper (`model/java-class-name.ts`), installed in
  `GenericEditor.setSimpleClassNameResolver` for editor titles.

The generic environment importer dispatches OBJ. Additional model formats
are tracked in Phase 36, XML in Phase 34 and browser integration checks in
Phases 38, 40, 41 and 44.
