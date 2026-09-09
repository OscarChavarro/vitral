# Vitral Java-to-TypeScript Port Plan

## Purpose

Build a complete, mechanically traceable TypeScript port of the current Java Vitral implementation. The immediate milestone is a fully migrated `vsdk.toolkit.common` package plus reliable user-facing clean and compile scripts that produce a consumable TypeScript/JavaScript library package. The longer plan follows the supplied dependency groups exactly.

No phase may use placeholders, empty method bodies, unconditional dummy returns, "not implemented" exceptions, fake renderers, or compile-only stubs. Existing TypeScript files are candidate implementations, not proof of parity: each one must be compared method by method with the current Java source.

## Current Status

Phases 1 through 14 are complete and their available gates were verified on 2026-09-09. Phase 15 is the active phase.

| Phase | Status | Completed | In progress | Remaining |
|---|---|---:|---|---|
| 1 — Java runtime compatibility | Complete | 86 / 86 inventory entries | — | — |
| 2 — Common entity foundation | Complete | 7 / 7 applicable inventory entries | — | `VSDKJ2ME` excluded by Web-target decision |
| 3 — Linear algebra | Complete | 16 / 16 inventory entries; 11 / 11 tests | — | — |
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
| 15 — General processing | In progress | 6 / 22 inventory entries | Core geometry, signal, image, and timing processing | 16 symbols and standard phase gate |
| 16 — Materials | Complete | 5 / 5 inventory entries | — | — |
| 17 — Geometry base | Complete | 1 / 1 inventory entries | — | — |
| 18 — Curves | Complete | 2 / 2 inventory entries | — | — |
| 19 — Geometry elements | Complete | 6 / 6 inventory entries | — | — |
| 20 — Concrete geometry elements | Complete | 0 / 0 applicable entries | Graph contains six obsolete duplicate package names | — |
| 21 — Surfaces | Complete | 13 / 13 inventory entries | Surface hierarchy, plane, contour, polygon, bicubic and functional patches, quad and triangle meshes, mesh group, strip mesh, and MD2 animation metadata | Standard phase gate |
| 22–45 | Pending | 0 | — | All planned inventory entries and decision gates |

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

Exit: satisfy the standard phase gate before starting Phase 26.

### Phase 26: Lights

Source group: `23_light.txt` (2 class entries).

Work: Port light models and attenuation/color behavior.

Class inventory:

```text
vsdk.toolkit.environment.light.Light
vsdk.toolkit.environment.light.LightType
```

Exit: satisfy the standard phase gate before starting Phase 27.

### Phase 27: Tone-mapping checkpoint

Source group: `23_tonemap.txt` (0 class entries).

Work: The authoritative group has no class entries. Verify that tone mapping remains represented elsewhere and create no placeholder.

Class inventory:

```text
(no class entries; the file contains only its group header)
```

Exit: satisfy the standard phase gate before starting Phase 28.

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

Exit: satisfy the standard phase gate before starting Phase 29.

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

- All 45 phases are closed in the prescribed order.
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
| 22 | Volumes and boundary representation | Pending |
| 23 | Backgrounds | Pending |
| 24 | Cameras | Pending |
| 25 | Geometric processing | Pending |
| 26 | Lights | Pending |
| 27 | Tone-mapping checkpoint | Pending |
| 28 | Scene model | Pending |
| 29 | Numerical-analysis checkpoint | Pending |
| 30 | I/O wrappers | Pending |
| 31 | I/O context checkpoint | Pending |
| 32 | Binary I/O checkpoint | Pending |
| 33 | Image I/O | Pending |
| 34 | XML I/O | Pending |
| 35 | VRML I/O checkpoint | Pending |
| 36 | Geometry I/O | Pending |
| 37 | SGL checkpoint | Pending |
| 38 | GUI model and controllers | Pending |
| 39 | Software shaders | Pending |
| 40 | CPU rendering | Pending |
| 41 | GPU rendering architecture checkpoint | Pending |
| 42 | Animation | Pending |
| 43 | Application framework | Pending |
| 44 | GUI persistence | Pending |
| 45 | Orphan-inventory closure | Pending |

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
