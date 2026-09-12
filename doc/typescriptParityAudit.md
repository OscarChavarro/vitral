# TypeScript literal-parity audit

Date: 2026-09-11 (updated 2026-09-12 with the Phase 25.7 closure)

Java source of truth: `java/base/src/main` at `deb03bf1`.

## Verdict

The TypeScript tree at the end of the recorded Phase 25.6 gate is **not** a
1:1 port of the Java source. A green TypeScript build and the then-current 106
tests did not constitute a parity gate: production classes and complete
private algorithm graphs were absent, and several existing implementations
used replacement algorithms.

Phase 25 must not be described as complete. The plan itself records Phase
25.7 as unstarted. The current Java CSG/operator source under
`environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators`
contains 32 files and 15,608 lines; none had a TypeScript counterpart when
this audit began.

## Audit method

- Compared the Phase 1--25 plan inventory with the current Java and TypeScript
  production trees.
- Compiled `java/base` and compared compiled Java public members (`javap`) to
  TypeScript AST members for every same-path class.
- Compared private method inventories for every same-path class.
- Reviewed low source-size ratios and every public-member mismatch against the
  Java method bodies.
- Searched production and tests for placeholders, dummy returns, unsupported
  branches, skipped tests, and explicit alternative/simplified algorithms.
- Compared the 58 Java base test sources with the TypeScript test tree.

## Confirmed missing production blocks

This audit was extended to phases 1--26 on 2026-09-11. Phase 26's five Java
light classes were absent from the TypeScript source tree and are now present.

### Phase 26

Closed on 2026-09-12 with a Java-versus-TypeScript reference-driver diff of 428
identical lines, comparing raw IEEE-754 bits rather than formatted decimals.
The audit corrected `Light`'s base class, which was `FundamentalEntity` instead
of Java's `Entity`, and added the `equals`, `hashCode`, and `toString`
contracts that the `Light.LightDirection` record declaration generates in Java.

Reaching text parity for that record exposed two defects in base helpers used
across the port:

- `VSDK.formatDouble` delegated to `toFixed`, which rounds half away from zero,
  drops the sign of a negative zero, and switches to exponent notation above
  1e21. Java's `DecimalFormat` takes the digits from `Double.toString`, breaks
  half-way cases against the exact binary value, falls back to half-to-even,
  keeps the sign, and prints `NaN`, `∞`, and `-∞`. It is now computed with
  exact integer arithmetic, behind a fast path for the values where `toFixed`
  provably agrees, and verified against Java over a 39-value table at zero to
  four decimals and over 20000 pseudo-random doubles at zero to six decimals.
- `Double.toString` delegated to `String(value)`, which drops the trailing
  `.0`, uses a lower-case `e`, and keeps plain notation up to 1e21. Java's
  layout rules are now implemented and verified against the same table.

One residual difference is recorded rather than emulated: JDK 17 still uses the
pre-19 `FloatingDecimal`, which emits more digits than the shortest
representation that round-trips for a small set of values, `Double.MIN_VALUE`
(`4.9E-324` in Java, `5.0E-324` here) among them. Both helpers take their
digits from that converter, so both inherit the difference: 11 of the 20000
fuzzed values differ, against 3143 for the previous `toFixed` implementation.
Ten of the eleven have a magnitude above 2^53. Closing the remaining gap
requires porting `FloatingDecimal` itself.

No Java test source exists for the light package, so none was eligible for
migration and none was invented.

### Phase 27

Closed on 2026-09-12. The group is empty, the current Java base declares no
tone-mapping class, and no placeholder was ever created in TypeScript. The
represented blocks are the gamma transfer functions of `ImageProcessing`
(Phase 15) and the HDR buffers `RGBAImageHDRUncompressed` and `RGBAPixelHDR`
(Phase 14). Verifying them against Java reference drivers exposed three
byte-level defects, all now fixed:

- `ImageProcessing.gammaCorrection` called the TypeScript `putPixel`, which
  represents Java's clamping `putPixel(int)` overload, where Java binds
  `putPixel(byte)` because `VSDK.unsigned8BitInteger2signedByte` returns a
  `byte`. Every indexed pixel whose corrected value exceeded 127 was clamped to
  zero. It now calls `putPixelByte`.
- `RGBImageUncompressed.putPixel` and `RGBAImageUncompressed.putPixel`
  re-applied the unsigned-to-signed conversion to their numeric arguments. Java
  declares only `byte` overloads there and stores the value as given, so every
  channel above 127 became zero and the `(byte) -1` alpha became `0`. They now
  store the argument unchanged, and the three-argument RGBA form fills alpha
  with `-1` as Java does.

Both fixes are confirmed by driver diffs against Java: 208 identical lines over
eight gamma values on indexed and RGB images, and 63 identical lines over the
four `NormalMap` exports and the three `ZBuffer` exports. That second dump
contains 676 negative channel values, every one of which the TypeScript port
returned as zero before the fix.

This phase also found eleven current-Java production files that no phase
inventory lists and that have no TypeScript counterpart: the nine files of
`vsdk.toolkit.media.solidTexture`, plus `IndexedColorImageHDRUncompressed` and
`RGBAColorPalette`. The Analyzed State exhaustiveness check covered only
`vsdk.toolkit.common`, so they are orphans of the 580-entry graph and are
recorded as the first named input to Phase 45. A residual style divergence is
left open for Phase 15: Java declares two `gammaCorrection` overloads, while
TypeScript merges them into one union-typed method with equivalent behavior.

### Phase 15

The recorded status of 6/22 was real, despite later phases having been marked
complete. This audit added the complete Java algorithm graph for:

- `SolverPolynomialQuarticBairstow`
- `DeterminantStrategy`, `InverseStrategy`, and `LinearAlgebraBackend`
- `MatrixAlgorithmsSupport`
- `GaussCpuStrategy`, `LuCpuStrategy`, and `NaiveCofactorCpuStrategy`
- `StrategySelector` and `LinearAlgebraEngine`, including their nested public
  contracts

The native `LzwWrapper` and `SpharmonicKitWrapper` still require explicit Web
runtime designs. Their actual implementations live in `pkgs/LempelZivWelch`
and `pkgs/SpharmonicKit27`; adding empty TypeScript native facades would violate
the port rules.

### Phase 25.7

Closed on 2026-09-12. The complete Java CSG inventory under
`environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators` is
now ported 1:1: the splitter, the shared operator, the geometric predicate
processor, the intersector and intersection-curve builder, the vertex/vertex
and vertex/face classifiers, `_PolyhedralBoundedSolidSetClassifier`,
`_PolyhedralBoundedSolidSetNonIntersectingClassifier` (with `_PreflightCache`),
`_PolyhedralBoundedSolidSetNullEdgesConnector` (with `ConnectResult` and
`NullEdgePair`), `_PolyhedralBoundedSolidSetFinisher`, the offset-cylinder
fallback with its two spec records, `_PolyhedralBoundedSolidSetOperator` (with
`DebugSolidExporter` and `SeparateEdgeSequenceResult`), the
`PolyhedralBoundedSolidModeler.setOp` wrappers, `SimpleTestGeometryLibrary`,
`CsgKurlanderBowlFixture` and `vsdk.toolkit.processing.CurveModeler`.

Parity evidence: a Java reference driver and a Node driver print the full
`PolyhedralBoundedSolid.toString()` dump (half-edge tables with ids) for
MANT1986_2, MANT1988_15_1 and MANT1988_3 under UNION/INTERSECTION/SUBTRACT plus
APPE1967_1; the 1,388-line outputs are byte-identical (`diff` empty). The
migrated Java tests reproduce the Java hardcoded baselines exactly: the 33-case
reference matrix of `BooleansFromReferenceObjectPairsTest`, the 11 per-motif
`TopologicalSummary` baselines of `KurlanderMotif4OperationMatrixTest`, the
40/40 OK Kurlander sweep, and the curve-report counts of
`IntersectionCurveBuilderTest`.

## Confirmed incomplete or replacement implementations

The following are source-reviewed findings, not line-count guesses:

- `Matrix4x4f` now exposes the previously missing Java projection, cofactor,
  quaternion, Euler-angle, array export, equality, hash, and diagnostic API;
  numeric statement-level comparison remains pending.
- `ParametricBiCubicPatch` now exposes `printGeometryMatrices` (Java's
  `doExtraInformation` is an intentional empty implementation in Java and is
  inherited from the TypeScript geometry contract); its private
  geometry-matrix construction still needs a statement-by-statement parity
  proof.
- `TriangleMesh.slice` is now present with the Java case ordering and cut
  helpers, with a deterministic one-inside/two-outside regression.
- `Sphere`, `Cone`, `Arrow`, `Box`, `Torus` and `VoxelVolume` now follow the
  Java Euler/modeler construction sequences, and the TypeScript-only
  `PolyhedralBoundedSolidBuilder` was deleted.
- `PolyhedralBoundedSolidTopologyEditing` was re-ported from the Java
  implementation (coincident-loop reduction, dangling-edge repair, coplanarity
  safeguards, restart logic, `maximizeFaces`, `loopGlue`, `compactIds` and
  `weldCoincidentVertices`).
- `PolyhedralBoundedSolidGeometricValidator`, `PolyhedralBoundedSolid`,
  `PolyhedralBoundedSolidPredicates`, and `PolyhedralBoundedSolidEulerOperators`
  omit sizeable private decision graphs or replace their flow. Existing small
  tests do not exercise the Java CSG degeneracies that consume those paths.
- `_PolyhedralBoundedSolidFallbackGeometry` and the two profile/axis-aligned
  fallback builders that consume it have Java's method inventory and behavior
  (they participate in the byte-identical boolean parity dumps), but their
  bodies are still compressed: they use TypeScript arrays and arrow-function
  predicates where Java uses `ArrayList<Double>`/`ArrayList<Vector3Dd>` and
  explicit loops. Container-level re-porting is pending; it must be done
  together with the three call sites, and it needs a fallback-firing regression
  first, because these builders only execute when Connect leaves loose ends.
- `RGBAImageCompressed` used a different fatal exception path and did not
  reset all state after invalid compressed input.
- The source tree contains legacy TypeScript seed classes with no current Java
  counterpart. They are not public exports, but they must be removed or
  explicitly classified before an exhaustive source-tree parity claim.

## Corrections made during this audit

- Restored the literal Bairstow solver and made `Torus` use Java's normalized
  ray, root arrays, positive-root selection, and hit mutation order. Removed
  the replacement derivative/bisection root solver.
- Added all three Java matrix strategies, backend selection, engine behavior,
  and the translated Java strategy-consistency test.
- Restored Java's distinct direct and indexed distance-field algorithms and
  the separate enlarge/shrink branches in `ImageProcessing.resize`.
- Restored `Matrix4x4d` projections, Euler composition/extraction, cofactors,
  Java inversion path, exports, exact equality/hash, and diagnostics.
- Restored exact equality/hash behavior for double vectors, `MatrixNxM`,
  `ColorRgb`, and `Ray`; restored Ray's unit-direction tolerance.
- Restored missing renderer-configuration comparison, vertex-color access,
  null shading default, and diagnostic output.
- Restored missing compressed/RGB image clone/access contracts and Java's
  invalid-input state reset.
- Restored `Sphere`'s public position/normal/tangent/binormal frame methods and
  the missing `Cone`/`Arrow` detail-intersection entry points.
- Added the five Phase 26 light classes (`Light`, `AmbientLight`,
  `DirectionalLight`, `PointLight`, and `SpotLight`) with the Java direction,
  distance-limit, attenuation, and copy behavior.
- Ported `TriangleMesh.slice` and its `simpleTriangleCut`,
  `halfTriangleCut`, `doubleTriangleCut`, append, and intersection helpers.
- Ported the Phase 25.7 `_PolyhedralBoundedSolidIdNamespace` allocator and
  `_SetOperationTrace` diagnostic contract; the operator-dependent records and
  boolean pipeline remain pending.
- Ported the complete Java method inventory of
  `_PolyhedralBoundedSolidOperator`, including its ring-parity shadow
  diagnostics, projection helpers and both `join` forms, together with
  `_SetOperationContext`, and the vertex/vertex, vertex/face, and null-edge
  coincidence records. `_SetOperationContext` now preserves Java's deferred
  list initialization, and the null-edge helper names match Java exactly. The
  intersector/classifier/topology pipeline remains pending.
- Ported `_PolyhedralBoundedSolidFallbackGeometry` coordinate, profile,
  clipping, and tolerance helpers. Structural fallback builders remain
  dependent on the subsequent operator/modeler layers.
- Started `PolyhedralBoundedSolidModeler` with literal transformation, arc,
  circular-lamina, and translational sweep procedures; rotational sweep and
  set-operation procedures remain to be ported before this class is complete.
- Added the Java rotational wire sweep around the X axis, including endpoint
  axis-collapse and face maximization branches.
- Added literal parametric-curve B-Rep construction with Java's segment
  sampling, weld tolerance, break-marker loop closure, ring merge, and Euler
  operations.
- Ported `_PolyhedralBoundedSolidFaceValidator` surface-degeneracy, area,
  close-edge, and segment-distance checks.
- Ported the exact sector/sector and sector/face classification record classes,
  including all constants, fields, relabeling rules and diagnostics.
- Ported `_PolyhedralBoundedSolidProfileDifferenceFallback` completely, from
  structural recognition through Euler reconstruction and ID compaction.
- Corrected and completed the splitter class inventory: the Java-named sector
  and null-edge classes are retained, every Java helper is present, and the
  `insertNullEdges`, null-edge ordering, connect and statistics paths match the
  Java control flow. The splitter now builds under strict TypeScript checking
  and has a valid two-sided solid regression.
- Rechecked the Phase 23 background classes and Phase 24 `CameraSnapshot` API;
  their Java public contracts and current behavior are aligned. The remaining
  Phase 23--24 gate is now only the consolidated regression run.

## Findings resolved on 2026-09-12 (Phase 25.7 closure)

- `java.lang.Math.round` used `ceil(a - 0.5)` for negatives, so `round(-2.5)`
  returned `-3` where Java returns `-2`. It now follows Java's
  "ties round towards positive infinity" contract, answers `0` for NaN and
  saturates at the `long` range.
- `InfinitePlane` exposed the one-argument Java `doIntersectionFirstHit(Ray)`
  under the invented name `intersectRay`, and `doIntersectionWithNegative`
  returned the forward hit instead of Java's negated-`t` result for the
  parallel case. Both now match Java; `Camera` and the axis-aligned cell
  fallback were updated to the Java name.
- `_PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector` and
  `...OnFace` were compressed rewrites (map lookups and ternary chains); they
  are now literal ports of the Java `switch` cascades, including the
  `applyRules` INTERSECTION/SUBTRACT split and the exact `toString` layout.

## Documented runtime boundaries (Phase 25.7)

- No JVM system-property table: `Boolean.getBoolean` answers `false` and the
  new `System.getProperty` port (plus the base-package
  `_PlatformProperties.platformGetProperty` adapter) answers `null`, so
  `vsdk.setop.*` traces and `vsdk.setop.connect.keepInsertionOrder` keep their
  Java defaults.
- No Java serialization: `_PolyhedralBoundedSolidSetOperator.deepCloneSolid`
  (and the equivalent helper in `StepperMotorGuideCsgFixture`) rebuild the
  half-edge graph node by node through identity maps, preserving ids,
  positions and list order.
- No `System.identityHashCode`: the `separateEdgeSequence` cycle detector uses
  a `WeakMap`-backed identity table.
- No filesystem in the base package: `debugSolid` prints the Java banner and
  delegates to the installed `DebugSolidExporter`; it does not write
  `<pattern>.txt`.
- `System.err` mirroring in `CsgKurlanderBowlFixture.printProgressMessage` is
  emitted once through the single console adapter.
- Tests: `private` and declared return types are erased at run time, so the
  Java reflection-based API audits assert existence and prototype (instance)
  placement instead of `Modifier.isPrivate` and return types; vitest's 5 s
  default timeout is raised explicitly for the Kurlander/stepper fixtures,
  which JUnit runs without a time budget.
- Tests: vitest's worker/main birpc channel has a fixed 60 s timeout and can
  only answer while the worker's event loop is free, so the long synchronous
  bowl sweeps yield a macrotask between iterations
  (`_HarnessEventLoopYield.yieldToEventLoop`). Without it the run reports
  `[vitest-worker]: Timeout calling "onTaskUpdate"` and exits non-zero even
  with every assertion passing. The budget is per spec file, not per test, so
  the yield is inserted in every file whose total run time approaches 60 s:
  `KurlanderMotif4OperationMatrixTest`, `KurlanderBowlMotifSweepRegressionTest`,
  `KurlanderBowlStarInvariantTest`, `Stage6FaceSubdivisionDiagnosticTest` and
  `PredicatesQiScanTest`. JUnit has no equivalent constraint.

## Java test sources intentionally not migrated

- `Mant1988Section15_2UnionDiagnostic` is a `main()` console diagnostic with
  zero `@Test` methods that reflects on `setOpGenerate`/`setOpClassify`/
  `setOpConnect` static members deleted by Stage 7 R5. Running it against the
  current Java kernel fails with
  `NoSuchMethodException: ..._PolyhedralBoundedSolidSetOperator.setOpGenerate(...)`,
  so there is no behavior to reproduce.

## Gate status

Phase 25.7 production and test migration is complete and verified against the
Java reference driver. The literal-parity gate for the earlier phases remains
**open** for the items still listed above (statement-level proofs for
`Matrix4x4f`, `ParametricBiCubicPatch`, the remaining primitive branches, and
the legacy TypeScript seed classes). No test-count-only gate may override the
source/API audit.
