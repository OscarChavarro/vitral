# Vitral Java-to-TypeScript Port Plan

## Purpose

Build a complete, mechanically traceable TypeScript port of the current Java Vitral implementation. The immediate milestone is a fully migrated `vsdk.toolkit.common` package plus reliable user-facing clean and compile scripts that produce a consumable TypeScript/JavaScript library package. The longer plan follows the supplied dependency groups exactly.

No phase may use placeholders, empty method bodies, unconditional dummy returns, "not implemented" exceptions, fake renderers, or compile-only stubs. Existing TypeScript files are candidate implementations, not proof of parity: each one must be compared method by method with the current Java source.

## Mandatory migration discipline

Every production migration must preserve the Java implementation textually and behaviorally: control flow, numeric tolerances, mutation order, diagnostics, comments that describe invariants, and deterministic ordering are ported 1:1 unless a runtime boundary makes that impossible. Such a boundary must be documented beside the code and verified by a dedicated parity test; replacing a complex Java algorithm with a simpler or merely equivalent-looking algorithm is prohibited.

For a complex algorithm, first map and migrate its complete private dependency graph (state types, predicates, allocators, ordering helpers, topology utilities, and nested result/specification types). Only then migrate its public entry points textually, followed by Java-derived regression and degeneracy tests. This rule applies to every phase, including already-completed phases when parity is re-audited.

## Current Status

Phases 1 through 14 have recorded gates. A literal-parity re-audit on
2026-09-11 found that green gates had not proved source parity; see
`doc/typescriptParityAudit.md`. Phases 3, 15, 21, 22, 23--24, and 25 have open
parity findings, and no later phase may be treated as a 1:1 baseline until
those findings are closed.

| Phase | Status | Completed | In progress | Remaining |
|---|---|---:|---|---|
| 1 — Java runtime compatibility | Complete | 86 / 86 inventory entries | — | — |
| 2 — Common entity foundation | Complete | 7 / 7 applicable inventory entries | — | `VSDKJ2ME` excluded by Web-target decision |
| 3 — Linear algebra | Inventory complete, parity open | 16 / 16 inventory entries; 11 / 11 tests | `Matrix4x4f` literal completion | Audit gate |
| 4 — Symbolic algebra | Complete | 7 / 7 inventory entries | — | The only related Java test depends on later polyhedral-solid production classes |
| 5 — Color | Complete | 2 / 2 applicable inventory entries | — | — |
| 6 — Logging | Complete | 1 / 1 inventory entries | — | — |
| 7 — Memory management checkpoint | Complete | 0 / 0 inventory entries | — | — |
| 8 — Quasi-Monte Carlo checkpoint | Complete | 0 / 0 inventory entries | — | — |
| 9 — Data structures | Complete | 12 / 12 inventory entries; 6 / 6 TypeScript parity tests | — | — |
| 10 — Statistics | Complete | 3 / 3 inventory entries; 2 / 2 supplemental complete-common entries; 3 / 3 TypeScript parity tests | — | — |
| 11 — Command-line options checkpoint | Complete | 0 / 0 inventory entries | — | — |
| 12 — GUI progress-monitor contracts | Complete | 4 / 4 inventory entries; 3 / 3 TypeScript parity tests | — | — |
| 13 — Tangible-interface contracts | Complete | 4 / 4 inventory entries; 3 / 3 TypeScript parity tests | — | — |
| 14 — Media and image buffers | Complete | 20 / 20 inventory entries | — | — |
| 15 — General processing | In progress | 20 / 22 inventory entries | Native-runtime adaptations for LZW and SpharmonicKit | 2 symbols and literal-parity gate |
| 16 — Materials | Complete | 5 / 5 inventory entries | — | — |
| 17 — Geometry base | Complete | 1 / 1 inventory entries | — | — |
| 18 — Curves | Complete | 2 / 2 inventory entries | — | — |
| 19 — Geometry elements | Complete | 6 / 6 inventory entries | — | — |
| 20 — Concrete geometry elements | Complete | 0 / 0 applicable entries | Graph contains six obsolete duplicate package names | — |
| 21 — Surfaces | Inventory complete, parity open | 13 / 13 inventory entries | Java-derived `TriangleMesh.slice` regression and remaining bicubic audit | Audit gate |
| 22 — Volumes and boundary representation | Inventory complete, parity open | 30 / 30 inventory entries | Java primitive/Euler B-rep construction and complete topology/validation decision graphs | Audit gate |
| 23–24 | Complete, parity audit open | 6 / 6 inventory entries | Source-level parity corrections | Audit gate |
| 25 — Geometric processing | In progress | Polygon clipping, voxelization, and monotone triangulation blocks only | Full B-rep/CSG operator block | 32 current-Java source files plus tests and standard gate |
| 26 — Lights | Complete | 6 / 6 current-Java symbols | — | `LightType` has no source mapping in the current Java base |
| 27 — Tone-mapping checkpoint | Complete, harness exit open | 0 / 0 inventory entries | — | Eleven unlisted `media`/`solidTexture` orphans recorded for Phase 45; vitest worker-RPC budget still makes `verify` exit 1 |
| 28 — Scene model | Complete | 4 / 4 inventory entries | — | No Java test source has a dependency closure limited to this phase |
| 29–45 | Pending | 0 | — | All planned inventory entries and decision gates |

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
obligation of Phase 14. They are recorded here as the first named input to
Phase 45, and no placeholder was created for them.

## Offline Example Programs

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

## Analyzed State

- Java production source of record: `java/base/src/main`; the repository also separates desktop and GPU integrations into `java/awt`, `java/jogl2`, and `java/jogl4`.
- The Java base currently contains 377 production `.java` files. Its `vsdk.toolkit.common` package contains 50 production files.
- The existing TypeScript seed contains 60 `.ts` files, 34 under its common tree. Several names and APIs represent an older model; for example, the mutable `Matrix4x4.ts` does not match the current immutable Java `Matrix4x4d` contract.
- `typescript/package.json` declares the `base` workspace, but `typescript/base/package.json` and `typescript/base/tsconfig.json` do not exist. The root build command invokes the same missing workspace twice, the root TypeScript references include a missing testsuite project, and no clean/compile shell scripts currently exist.
- Dependency-group source of record: `/paradigmas/master/VSDK/dependencyGraphAnalyzer/etc/vitralJava`.
- Running `ls | sort -n` in that mirror yields 45 files. After removing one bracketed header from each file, they contain exactly 580 entries and 580 unique fully qualified class names.
- Exhaustiveness check: the current Java common package has three public classes absent from the 580-entry graph: `ColorRgba`, `GeometryStatistics`, and `SolidTextureStatistics`. They are added to Phases 5 and 10 without changing the 45-file order. Therefore the numbered inventory remains exactly 580, while the complete-common milestone has three explicit supplemental obligations.
- The Java tree contains 59 files in Gradle test source sets. Fifty-four contain JUnit/AssertJ tests; the remaining five are existing fixtures/diagnostics needed by those tests. These 59 files are the complete allowed test-port inventory.

## Scope Rules

1. Port the current Java implementation, not an inferred API and not the older TypeScript seed. Preserve public names, visibility intent, constructor and overload behavior, inheritance, interfaces, nested types, constants, exceptions, side effects, mutation/immutability, ordering, and serialization-visible data.
2. Treat every fully qualified inventory entry as an independently accountable symbol. A nested Java class may become an exported TypeScript class plus namespace/companion export, but its externally visible identity and behavior must remain reachable.
3. Reuse existing TypeScript code only after a line-by-line semantic comparison. Rewrite it when it diverges. A same-named file does not count as migrated.
4. Native TypeScript/JavaScript mappings are acceptable only when they preserve all behavior used by Vitral. Otherwise implement a complete adapter. Java annotations that have no runtime behavior may be recorded as verified no-op language metadata, not represented by empty classes.
5. Preserve Java numeric semantics deliberately: signed bytes, 32-bit integer overflow, 64-bit values, float rounding, NaN/infinity, array bounds, division behavior, and endianness. Use `bigint`, typed arrays, `Math.fround`, and explicit checks where required.
6. Preserve Java collection semantics deliberately: equality, hashing, insertion/iteration order, nullability, fail-fast or snapshot behavior where observable, and map-entry behavior.
7. Keep platform-neutral model/algorithm code free of DOM, Node filesystem, and GPU dependencies. Put unavoidable runtime bindings behind concrete adapters and package subpath exports.
   The TypeScript filesystem adapters belong in a separate `typescript/fs/` workspace package (for example, `@vitral/fs`), which depends on `@vitral/base` and is built/installed only in server-side runtimes with filesystem I/O. The browser-facing `@vitral/base` artifact must neither import nor package `File`-family implementations.
8. Do not port examples or invent new tests. Port only the 59 Java test-source files listed in this plan, adapting test-runner syntax and fixtures without weakening assertions or tolerances.
9. When a Java test depends on classes from later phases, migrate it in the first phase where all production dependencies exist. Until then, record it as pending rather than disabling it.
10. Swing/AWT and JOGL are not to be guessed. The platform-sensitive final phases remain deferred until the user selects the target architecture. No placeholder UI components or render backends may be committed while that decision is pending.
11. Every Java behavior that uses multiple threads must be ported through workers compatible with a browser frontend. Worker entry points, message payloads, cancellation, error propagation, and lifecycle management must be designed for Web Workers; platform-specific worker launchers may be supplied only behind that common contract.

## Concurrency and Browser Workers

Do not translate Java multithreading directly to Node-only threads or to synchronous main-thread code. Any ported feature that uses concurrent execution must define a worker-compatible implementation that can run in a browser frontend using Web Workers. Keep the worker protocol and computational payloads platform-neutral, use message passing for inputs/results/errors and cancellation, and ensure that browser-visible state (DOM, UI, and other main-thread-only APIs) remains outside worker code. A backend or standalone runtime may provide an adapter for its own worker facility, but it must preserve the same worker contract rather than becoming the only implementation.

## PersistenceElement Runtime Architecture

`PersistenceElement` must be implemented as a class hierarchy with runtime-specific concrete implementations; it must not expose a single filesystem-based implementation as though it worked in every runtime.

- The backend/standalone implementation may access local files and is the only implementation permitted to use filesystem APIs directly.
- The WEB implementation must load resources published by remote web servers (for example, through `fetch` over HTTP(S)). A browser frontend runs in a sandbox and has no arbitrary local-file access, so it must not attempt to read filesystem paths.
- Define a shared persistence contract that preserves the Java-facing behavior while making runtime selection explicit. Format-specific persistence classes must depend on that contract, not directly on Node or browser-only file APIs.
- Add to `testsuite/Tools` a program that serves a filesystem tree over HTTP for persistence integration tests. It may be an NGINX-based, read-only server; its configuration and test fixture setup must prohibit write methods and make the published root explicit. Browser-mode tests must use this server to exercise remote resource loading.

## Package and Build Foundation

Implement this foundation in Phase 1 so every later phase is continuously compilable:

1. Make `typescript/` the npm workspace root and `typescript/base/` a package named `@vitral/base` (or the repository-approved final name), with a pinned lockfile.
2. Use an ESM library build with strict TypeScript project references. The base compiler configuration must enable `strict`, `noImplicitOverride`, `noUncheckedIndexedAccess`, `exactOptionalPropertyTypes`, `noFallthroughCasesInSwitch`, `noEmitOnError`, declarations, declaration maps, source maps, and incremental builds.
3. Add a deliberate public entry point and package `exports`; do not expose build-directory internals accidentally. Emit JavaScript and `.d.ts` files under `typescript/base/dist/`.
4. Add `typescript/scripts/clean.sh`. It must use `set -eu`, resolve its own directory, and remove only known outputs: package `dist`, TypeScript build-info files, coverage, and package artifacts.
5. Add `typescript/scripts/compile.sh`. It must use `set -eu`, invoke the workspace build from a path-independent repository location, stop on compiler errors, and create a versioned npm tarball under `typescript/artifacts/`.
6. Fix root npm commands: `clean`, `build`, `test`, `pack`, and `verify`. `verify` must run clean, build, tests, and package validation in that order.
7. Add a TypeScript test project and Vitest (or a repository-approved equivalent), configured for deterministic serial execution where shared Java test fixtures require it.
8. Validate the package with `npm pack --dry-run`, inspect its file list, install the resulting tarball into a temporary consumer project, and compile an import against its declarations. This is packaging validation, not an invented behavioral test.
9. Document the supported Node version for build/test tooling and the runtime-specific exports. Do not make browser claims until the final UI/render decisions are implemented and verified.

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

Every phase, including a phase whose source group is empty, must pass all applicable checks before work starts on the next phase:

- Inventory: every listed entry has a source mapping and completed TypeScript export; no unaccounted omission or duplicate exists.
- Completeness scan: no placeholder, empty implementation, unconditional dummy result, skipped test, `todo`, focused test, or blanket type suppression was introduced.
- Java reference: the relevant existing Java tests compile and pass against the source of record.
- TypeScript compile: a clean `tsc --build` completes with zero errors and zero emitted output from failed projects.
- TypeScript tests: all migrated tests pass; test counts and skipped-test counts are recorded, with zero unexplained skips.
- Packaging: declarations are emitted, public imports resolve, `npm pack --dry-run` succeeds, and the package contains no source-tree/build-cache leakage.
- Review: public API and observable behavior are compared with Java, including error paths and edge cases covered by existing tests.

Canonical gate command after the scripts exist:

```sh
cd typescript
npm run verify
```

## UI and Visualization Decision Boundary

The 580-entry inventory describes the base dependency graph. The Java `awt`, `jogl2`, and `jogl4` module classes are not separately enumerated in these 45 files and must not be silently folded into unrelated phases.

Before Phase 38, the user must choose and document:

- Runtime shape: browser-only, Node plus browser, or separate core/frontend packages.
- UI mapping: HTML5 plus CSS3 component strategy and event model for Swing/AWT responsibilities.
- Rendering mapping: WebGL, WebGPU, Canvas 2D, or an explicit combination, including fallback policy.
- Resource lifecycle, shader compilation, texture/mesh upload, offscreen rendering, and test strategy.
- Browser support and headless CI environment.

Phases 38, 41, and 44 are therefore decision-gated. Keep them unimplemented, rather than stubbed, until this contract exists. A separate follow-on inventory for the 12 AWT, 65 JOGL2, and 25 JOGL4 Java source files should be generated after the decision; that follow-on is outside the current 580-entry list.

## 45-Phase Execution Plan

The following sequence is the exact numeric order produced by `ls | sort -n`. Class order inside each phase is preserved exactly from its source file.

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

Exit: satisfy the standard phase gate before starting Phase 2.

### Phase 2: Common entity foundation

Source group: `12_common.txt` (8 class entries).

Work: Port the entity hierarchy, exception hierarchy, VSDK utilities, J2ME compatibility behavior, and PresentationElement dependency with matching public contracts.

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

Exit: satisfy the standard phase gate before starting Phase 3.

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

Exit: satisfy the standard phase gate before starting Phase 4.

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

Exit: satisfy the standard phase gate before starting Phase 5.

### Phase 5: Color

Source group: `13_color.txt` (1 class entries).

Work: Port ColorRgb and also the current Java-only ColorRgba supplemental class identified by the source audit.
Supplemental current-Java class required for complete `common`: `vsdk.toolkit.common.color.ColorRgba`.

Class inventory:

```text
vsdk.toolkit.common.color.ColorRgb
```

Exit: satisfy the standard phase gate before starting Phase 6.

### Phase 6: Logging

Source group: `13_logging.txt` (1 class entries).

Work: Port logging levels, formatting, output routing, and failure behavior without no-op logging methods.

Class inventory:

```text
vsdk.toolkit.common.logging.Logger
```

Exit: satisfy the standard phase gate before starting Phase 7.

### Phase 7: Memory management checkpoint

Source group: `13_memory_management.txt` (0 class entries).

Work: The authoritative group has no class entries. Audit imports and generated dependency data; create no empty compatibility files.

Class inventory:

```text
(no class entries; the file contains only its group header)
```

Exit: satisfy the standard phase gate before starting Phase 8.

### Phase 8: Quasi-Monte Carlo checkpoint

Source group: `14_quasi_monte_carlo.txt` (0 class entries).

Work: The authoritative group has no class entries. Confirm that no current Java production class belongs here; create no placeholder.

Class inventory:

```text
(no class entries; the file contains only its group header)
```

Exit: satisfy the standard phase gate before starting Phase 9.

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

Exit: satisfy the standard phase gate before starting Phase 10.

### Phase 10: Statistics

Source group: `17_statistics.txt` (3 class entries).

Work: Port the listed counters and reports, plus GeometryStatistics and SolidTextureStatistics because they exist in the current Java common package but are absent from the graph inventory. This closes the complete-common milestone.
Supplemental current-Java classes required for complete `common`: `vsdk.toolkit.common.statistics.GeometryStatistics` and `vsdk.toolkit.common.statistics.SolidTextureStatistics`.
Milestone gate: phases 1 through 10 plus the two supplemental classes provide the entire current Java `vsdk.toolkit.common` package except the intentionally excluded Web-inapplicable `VSDKJ2ME`. All eleven Java tests located under `common/linealAlgebra` have passing TypeScript translations. `LinearAlgebraStrategiesConsistencyTest` belongs to `processing.linealAlgebra` and remains deferred until its strategy-engine production dependencies are ported.

Class inventory:

```text
vsdk.toolkit.common.statistics.PolyhedralBoundedSolidStatistics
vsdk.toolkit.common.statistics.RaytraceStatistics
vsdk.toolkit.common.statistics.RenderingStatistics
```

Exit: satisfy the standard phase gate before starting Phase 11.

### Phase 11: Command-line options checkpoint

Source group: `18_command_line_options.txt` (0 class entries).

Work: The authoritative group has no class entries. Record the verified absence and create no placeholder parser.

Class inventory:

```text
(no class entries; the file contains only its group header)
```

Exit: satisfy the standard phase gate before starting Phase 12.

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

Exit: satisfy the standard phase gate before starting Phase 13.

### Phase 13: Tangible-interface contracts

Source group: `18_gui_tangible_interfaces.txt` (4 class entries).

Work: Port tangible input devices and controller contracts without selecting a browser UI or GPU backend yet.

Class inventory:

```text
vsdk.toolkit.gui.tangibleInterfaces.TangibleInterfaceEvent
vsdk.toolkit.gui.tangibleInterfaces.TangibleInterfaceListener
vsdk.toolkit.gui.tangibleInterfaces.TangibleInterfaceNetworkClient
vsdk.toolkit.gui.tangibleInterfaces.TangibleInterfaceNetworkClient.FrameListener
```

Exit: satisfy the standard phase gate before starting Phase 14.

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

Exit: satisfy the standard phase gate before starting Phase 15.

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

Exit: satisfy the standard phase gate before starting Phase 16.

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

Exit: satisfy the standard phase gate before starting Phase 17.

### Phase 17: Geometry base

Source group: `22_geometry.txt` (1 class entries).

Work: Port the geometry root contract and intersection/bounding-volume semantics.

Class inventory:

```text
vsdk.toolkit.environment.geometry.Geometry
```

Exit: satisfy the standard phase gate before starting Phase 18.

### Phase 18: Curves

Source group: `22_geometry_curve.txt` (2 class entries).

Work: Port parametric curves and curve aggregates with equivalent evaluation and boundary behavior.

Class inventory:

```text
vsdk.toolkit.environment.geometry.curve.Curve
vsdk.toolkit.environment.geometry.curve.ParametricCurve
```

Exit: satisfy the standard phase gate before starting Phase 19.

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

Exit: satisfy the standard phase gate before starting Phase 20.

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

Exit: satisfy the standard phase gate before starting Phase 21.

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

Exit: satisfy the standard phase gate before starting Phase 22.

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

Exit: satisfy the standard phase gate before starting Phase 23.

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

Exit: satisfy the standard phase gate before starting Phase 24.

### Phase 24: Cameras

Source group: `23_camera.txt` (2 class entries).

Work: Port camera models, projection math, clipping, and ray generation.

Class inventory:

```text
vsdk.toolkit.environment.camera.Camera
vsdk.toolkit.environment.camera.CameraSnapshot
```

Exit: satisfy the standard phase gate before starting Phase 25.

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

Exit: satisfy the standard phase gate before starting Phase 30.

### Phase 30: I/O wrappers

Source group: `31_io_wrapper.txt` (2 class entries).

Work: Port stream wrappers with explicit byte, endianness, EOF, resource-lifetime, and exception semantics. Implement `PersistenceElement` using the required runtime-specific hierarchy: a local-file implementation restricted to backend/standalone use and a WEB implementation that reads resources from remote HTTP(S) servers. Add the read-only filesystem web-server tool under `testsuite/Tools` and use it for browser-mode persistence integration tests.

Class inventory:

```text
vsdk.toolkit.io.metadata.ShapeDescriptorPersistence
vsdk.toolkit.io.PersistenceElement
```

Exit: satisfy the standard phase gate before starting Phase 31.

### Phase 31: I/O context checkpoint

Source group: `32_io_context.txt` (0 class entries).

Work: The authoritative group has no class entries. Document the runtime boundary and create no empty context type.

Class inventory:

```text
(no class entries; the file contains only its group header)
```

Exit: satisfy the standard phase gate before starting Phase 32.

### Phase 32: Binary I/O checkpoint

Source group: `33_io_bin.txt` (0 class entries).

Work: The authoritative group has no class entries. Confirm binary behavior is owned by the wrapper/format phases.

Class inventory:

```text
(no class entries; the file contains only its group header)
```

Exit: satisfy the standard phase gate before starting Phase 33.

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

Deferred with the phase: `ImagePersistence` does not yet extend
`PersistenceElement` (Phase 30), which remains unported.

Exit: satisfy the standard phase gate before starting Phase 34.

### Phase 34: XML I/O

Source group: `35_io_xml.txt` (4 class entries).

Work: Port XML nodes/importers and parameter handling using a real XML parser appropriate to the selected runtime; preserve malformed-input errors.

Class inventory:

```text
vsdk.toolkit.io.xml.XmlImporter
vsdk.toolkit.io.xml.XmlNode
vsdk.toolkit.io.xml.XmlNodeParam
vsdk.toolkit.io.XmlException
```

Exit: satisfy the standard phase gate before starting Phase 35.

### Phase 35: VRML I/O checkpoint

Source group: `36_io_vrml.txt` (0 class entries).

Work: The authoritative group has no class entries. Verify no production source is omitted; do not add a VRML stub.

Class inventory:

```text
(no class entries; the file contains only its group header)
```

Exit: satisfy the standard phase gate before starting Phase 36.

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

Exit: satisfy the standard phase gate before starting Phase 37.

### Phase 37: SGL checkpoint

Source group: `41_sgl.txt` (0 class entries).

Work: The authoritative group has no class entries. Keep this as a verified dependency-graph checkpoint.

Class inventory:

```text
(no class entries; the file contains only its group header)
```

Exit: satisfy the standard phase gate before starting Phase 38.

### Phase 38: GUI model and controllers

Source group: `61_gui.txt` (47 class entries).

Work: Deferred platform-sensitive phase. Before implementation, obtain the user's UI target decision. Then port the platform-neutral GUI model/controllers and implement the chosen HTML5/CSS3 event/view bindings completely; do not emulate Swing with empty adapters. For the `ParallelProgressMonitor` symbols, support server runtime mode only. Browser-worker mode is explicitly unsupported until a complete design is selected; do not provide a compatibility stub.

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

Exit: satisfy the standard phase gate before starting Phase 39.

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

Exit: satisfy the standard phase gate before starting Phase 40.

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

Exit: satisfy the standard phase gate before starting Phase 41.

### Phase 41: GPU rendering architecture checkpoint

Source group: `72_render_opengl.txt` (0 class entries).

Work: Deferred platform decision checkpoint. The group is empty, but the user must choose WebGL, WebGPU, Canvas, or a defined combination before any JOGL-equivalent backend work. Do not add a fake renderer.

Class inventory:

```text
(no class entries; the file contains only its group header)
```

Exit: satisfy the standard phase gate before starting Phase 42.

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

Exit: satisfy the standard phase gate before starting Phase 43.

### Phase 43: Application framework

Source group: `81_framework.txt` (3 class entries).

Work: Port framework orchestration only after UI/render contracts are concrete.

Class inventory:

```text
vsdk.framework.Component
vsdk.framework.shapeMatching.plugins.ShapeDescriptor2DGeneratorFourier
vsdk.framework.shapeMatching.ShapeDescriptor2DGenerator
```

Exit: satisfy the standard phase gate before starting Phase 44.

### Phase 44: GUI persistence

Source group: `81_io_gui.txt` (1 class entries).

Work: Deferred platform-sensitive phase. Port GUI persistence against the selected browser or runtime storage/file-selection APIs with complete behavior.

Class inventory:

```text
vsdk.toolkit.io.gui.GuiPersistence
```

Exit: satisfy the standard phase gate before starting Phase 45.

### Phase 45: Orphan-inventory closure

Source group: `999_orphans.txt` (0 class entries).

Work: The authoritative group has no class entries. Re-run the inventory comparison, resolve every unmatched production symbol, and close the migration only when no unexplained orphan remains.

Class inventory:

```text
(no class entries; the file contains only its group header)
```

Exit: satisfy the standard phase gate and then complete the final inventory audit.

## Existing Java Test Sources Allowed for Migration

Port these files and only these files as tests. Support fixtures and diagnostics in this list may be translated because they are already part of a Java test source set. Do not convert examples under ordinary `src/` directories into new tests.

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
java/base/src/test/vsdk/toolkit/processing/polyhedralBoundedSolidOperators/AlgebraicIdentityRegressionTest.java
java/base/src/test/vsdk/toolkit/processing/polyhedralBoundedSolidOperators/BooleansFromReferenceObjectPairsTest.java
java/base/src/test/vsdk/toolkit/processing/polyhedralBoundedSolidOperators/CsgKurlanderBowlAllMotifsRegressionTest.java
java/base/src/test/vsdk/toolkit/processing/polyhedralBoundedSolidOperators/CsgKurlanderBowlFirstStarRegressionTest.java
java/base/src/test/vsdk/toolkit/processing/polyhedralBoundedSolidOperators/CsgMoonCylinderDifferenceDegeneracyTest.java
java/base/src/test/vsdk/toolkit/processing/polyhedralBoundedSolidOperators/CsgSampleCorpus.java
java/base/src/test/vsdk/toolkit/processing/polyhedralBoundedSolidOperators/CsgSampleCorpusFixtures.java
java/base/src/test/vsdk/toolkit/processing/polyhedralBoundedSolidOperators/IntersectionCurveBuilderTest.java
java/base/src/test/vsdk/toolkit/processing/polyhedralBoundedSolidOperators/IntersectorParametricOrderingTest.java
java/base/src/test/vsdk/toolkit/processing/polyhedralBoundedSolidOperators/IntersectorWeldTest.java
java/base/src/test/vsdk/toolkit/processing/polyhedralBoundedSolidOperators/KurlanderBowlMotifSweepRegressionTest.java
java/base/src/test/vsdk/toolkit/processing/polyhedralBoundedSolidOperators/KurlanderBowlStarInvariantTest.java
java/base/src/test/vsdk/toolkit/processing/polyhedralBoundedSolidOperators/KurlanderMotif4OperationMatrixTest.java
java/base/src/test/vsdk/toolkit/processing/polyhedralBoundedSolidOperators/Mant1988Section15_2UnionDiagnostic.java
java/base/src/test/vsdk/toolkit/processing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidPreflightTest.java
java/base/src/test/vsdk/toolkit/processing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidSetOperatorCoplanarPredicateTest.java
java/base/src/test/vsdk/toolkit/processing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidSetOperatorTest.java
java/base/src/test/vsdk/toolkit/processing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidSplitterMantylaRegressionTest.java
java/base/src/test/vsdk/toolkit/processing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidStrictSetOpTest.java
java/base/src/test/vsdk/toolkit/processing/polyhedralBoundedSolidOperators/SectoroverlapTraceDiagnosticTest.java
java/base/src/test/vsdk/toolkit/processing/polyhedralBoundedSolidOperators/SetOpConnectNoLooseInvariantTest.java
java/base/src/test/vsdk/toolkit/processing/polyhedralBoundedSolidOperators/SetOpConnectScanJoinTest.java
java/base/src/test/vsdk/toolkit/processing/polyhedralBoundedSolidOperators/SetOpFinishInvariantsTest.java
java/base/src/test/vsdk/toolkit/processing/polyhedralBoundedSolidOperators/Stage6FaceSubdivisionDiagnosticTest.java
java/base/src/test/vsdk/toolkit/processing/polyhedralBoundedSolidOperators/StepperMotorGuideCsgFixture.java
java/base/src/test/vsdk/toolkit/processing/polyhedralBoundedSolidOperators/StepperMotorGuideStrictValidationTest.java
java/base/src/test/vsdk/toolkit/processing/polyhedralBoundedSolidOperators/VertexFaceClassifierCoplanarTest.java
java/base/src/test/vsdk/toolkit/processing/polyhedralBoundedSolidOperators/VertexVertexEndpointRecoveryTest.java
java/base/src/test/vsdk/toolkit/render/HiddenLineRendererTest.java
java/base/src/test/vsdk/toolkit/render/TileGeneratorTest.java
java/testsuite/VSDKExamples/MatrixExample/src/test/matrixexample/MatrixExampleSlowStrategiesTest.java
```

For the complete-common milestone at Phase 10, migrate the eleven tests under `java/base/src/test/vsdk/toolkit/common/linealAlgebra/` and `java/testsuite/VSDKExamples/MatrixExample/src/test/matrixexample/MatrixExampleSlowStrategiesTest.java`. Later test files move with the first phase that completes their production dependency closure.

## Completion Criteria

The port is complete only when all of the following are true:

- All 46 phases are closed in the prescribed order.
- All 580 unique graph entries are fully implemented or mapped to a verified native semantic equivalent; the three supplemental current-common classes are also fully implemented.
- Every current Java `vsdk.toolkit.common` production file has a behaviorally equivalent TypeScript implementation.
- No placeholder/stub patterns or unexplained inventory gaps remain.
- The clean build reports zero TypeScript errors.
- All migrated tests from the 59-file allowed inventory pass with no unexplained skip, exclusion, or weakened assertion.
- `./scripts/clean.sh` is path-independent and removes only generated output.
- `./scripts/compile.sh` produces JavaScript, declarations, source maps, and an installable npm package.
- A clean temporary consumer can install the tarball and compile imports from the documented public API.
- UI and visualization phases implement the user's chosen HTML5/CSS3 and WebGL/WebGPU/Canvas architecture; no Swing/AWT or JOGL behavior is represented by a stub.

## Phase Progress

| Phase | Scope | Status |
| --- | --- | --- |
| 1 | Java runtime compatibility | Complete |
| 2 | Common entity foundation | Complete (`VSDKJ2ME` excluded for the Web target) |
| 3 | Linear algebra | Complete |
| 4 | Symbolic algebra | Complete — 7 / 7 symbols; gate passed |
| 5 | Color | Complete — 2 / 2 applicable symbols; gate passed |
| 6 | Logging | Complete — 1 / 1 symbol; gate passed |
| 7 | Memory management checkpoint | Complete — empty authoritative group; gate passed |
| 8 | Quasi-Monte Carlo checkpoint | Complete — empty authoritative group; gate passed |
| 9 | Data structures | Complete — 12 / 12 symbols; 6 / 6 parity tests; gate passed |
| 10 | Statistics | Complete — 3 / 3 inventory symbols; 2 / 2 supplemental symbols; 3 / 3 parity tests; gate passed |
| 11 | Command-line options checkpoint | Complete — empty authoritative group; gate passed |
| 12 | GUI progress-monitor contracts | Complete — 4 / 4 symbols; 3 / 3 parity tests; gate passed |
| 13 | Tangible-interface contracts | Complete — 4 / 4 symbols; 3 / 3 parity tests; gate passed |
| 14 | Media and image buffers | Complete — 20 / 20 symbols; gate passed |
| 15 | General processing | In progress — 6 / 22 symbols |
| 16 | Materials | Complete — 5 / 5 symbols; gate passed out of normal phase order |
| 17 | Geometry base | Complete — 1 / 1 symbol; gate passed out of normal phase order |
| 18 | Curves | Complete — 2 / 2 symbols; gate passed out of normal phase order |
| 19 | Geometry elements | Complete — 6 / 6 symbols; gate passed out of normal phase order |
| 20 | Concrete geometry elements | Complete — empty applicable source group; gate passed |
| 21 | Surfaces | Complete — 13 / 13 symbols |
| 22 | Volumes and boundary representation | Complete — 30 / 30 symbols; Java B-rep parity review and standard gate passed |
| 23 | Backgrounds | Complete — 4 / 4 symbols; standard gate passed |
| 24 | Cameras | Complete — 2 / 2 symbols; parity review and standard gate passed |
| 25 | Geometric processing | Complete — phases 25.2–25.7 closed with parity gates passed. The full Java CSG operator inventory (splitter, operator base, predicate processor, intersector, curve builder, vertex/vertex and vertex/face classifiers, set/non-intersecting classifiers, null-edges connector, finisher, structural fallbacks, set operator, modeler wrappers and fixtures) is ported and byte-identical to the Java reference driver on the MANT1986/MANT1988/APPE1967 dumps. The 24 Java CSG test sources plus the ten B-rep volume test sources are migrated. |
| 26 | Lights | Complete — `Light` with its nested `LightDirection` record and the `AmbientLight`, `DirectionalLight`, `PointLight`, and `SpotLight` subclasses are bit-identical to the Java reference driver over direction, distance-limit, attenuation, copy, equality, hash, and text output. `LightType` has no source mapping in the current Java base and was not invented. No Java test source exists for this package. |
| 27 | Tone-mapping checkpoint | Complete — the group is empty, the current Java base has no tone-mapping class, and no placeholder exists. The represented blocks (gamma transfer functions and the HDR buffers) were verified against Java drivers, which exposed and closed three byte-level defects in Phases 14 and 15. Eleven unlisted `media`/`solidTexture` production files were recorded as Phase 45 orphans. |
| 28 | Scene model | Complete — `SimpleBody`, `SimpleBodyGroup`, `SimpleScene`, and `SimpleSceneSnapshot` are bit-identical to the Java reference driver over transform caches, every world/object conversion, all ray-detail masks, the translation-only and sphere fast paths, group bounds, scene light-id assignment, and snapshot isolation and immutability. Closing the diff required literal corrections to `Matrix4x4d.exportToQuaternion`, `Matrix4x4d.importFromQuaternion`, and `Quaterniond.normalized` from Phase 3. No Java test source has a dependency closure limited to this phase. |
| 29 | Numerical-analysis checkpoint | Pending |
| 30 | I/O wrappers | Pending |
| 31 | I/O context checkpoint | Pending |
| 32 | Binary I/O checkpoint | Pending |
| 33 | Image I/O | Pending — partial advance: export path only (3 / 8 entries); see the Phase 33 record |
| 34 | XML I/O | Pending |
| 35 | VRML I/O checkpoint | Pending |
| 36 | Geometry I/O | Pending |
| 37 | SGL checkpoint | Pending |
| 38 | GUI model and controllers | Pending |
| 39 | Software shaders | Pending |
| 40 | CPU rendering | Pending — partial advance: `RenderingElement` and `render.raster.Rasterizer2D` (3 / 35 entries); see the Phase 40 record |
| 41 | GPU rendering architecture checkpoint | Pending |
| 42 | Animation | Pending |
| 43 | Application framework | Pending |
| 44 | GUI persistence | Pending |
| 45 | Orphan-inventory closure | Pending |
| 46 | Textual-parity and dependency-closure audit | Pending |

### Phase 1 Checkpoint — 2026-09-09

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
