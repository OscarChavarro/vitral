# Coverage audit for [MANT1988] An Introduction to Solid Modeling

**Updated:** 2026-11-16, after the execution of the Fable 5.2 plan on the
Java port (`java/base`; base revision `21139a43` plus the working tree of
that plan). The previous version (2026-11-14, `4412e55a`) described the
kernel before it.

This document estimates how much of Mäntylä's book is represented by the
Vitral polyhedral bounded solid implementation. Paths are relative to
`java/base/src/`:

- `main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid`
- `main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators`
- `main/vsdk/toolkit/common/dataStructures/CircularArrayList.java`
- `test/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid`
- `test/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators`
- `testsuite/Jogl4Examples/PolyhedralBoundedSolidExample`

The percentages are engineering estimates, not page counts. They combine
code correspondence, fixture coverage and executable tests. A row is scored
lower when a confirmed defect exists, even if the code mirrors the book.
Defect ids (B-, S-, E-, L-, C-) refer to
`doc/polyhedralBoundedSolidDevelopment/Fable5.2Analysis.md`.

| [MANT1988] section | Implemented coverage | Estimate | Tests |
| --- | --- | ---: | --- |
| Ch. 6, Boundary Models | Polyhedral B-Rep with face, loop, half-edge, edge and vertex nodes. Validity (§6.3) as two validation levels. | 50% | Validators, rendering tests, boolean regressions. |
| Ch. 9.1, Plane Models | Present as the basis of the operators; no separate subsystem. | 20% | Indirect. |
| Ch. 9.2.2, `MVFS` / `KVFS` | Implemented. | 90% | `PolyhedralBoundedSolidEulerOperatorsTest`. |
| Ch. 9.2.3, `MEV`, `MEF`, `KEMR` and inverses | Low- and high-level forms implemented. The empty-loop special cases (E-01 to E-04) are fixed. | 90% | Operator suite; `…EulerOperatorsEdgeCasesTest` (all enabled). |
| Ch. 9.2.4, `KFMRH`, `MFKRH` | Same-shell `lkfmrh`/`lmfkrh` and the `lkimrh`/`lmikrh` aliases. No shell abstraction. | 70% | `PolyhedralBoundedSolidKimrhMikrhTest`. |
| Ch. 9.3, Example | Box and holed-box constructions in `Box` and `SimpleTestGeometryLibrary`. | 65% | Fixture tests; no step-by-step assertion of Fig. 9.11. |
| Ch. 9.4, Properties | Euler characteristic and shells in `PolyhedralBoundedSolidTopologySummary`. Inverse round trips restore counts and back-pointers (E-03, E-05 fixed). | 60% | `PolyhedralBoundedSolidTopologySummaryTest`, inverse-pair tests. |
| Ch. 10.2, Data structure | Node hierarchy matches the book. Half-edge links are held in a list container instead of `nxt`/`prv`. | 80% | Validator tests, fixtures. |
| Ch. 10.3–10.4, Details and access | Neighbour access, mate, parent pointers. `next()` and `previous()` are O(1) through the index each half-edge keeps of its place in the loop. | 80% | Indirect. |
| Ch. 11.2, Storage and lists | Java-managed; `CircularArrayList` replaces `addlist`/`dellist`: an array, with `get` in O(1), and reading it writes nothing (stage 11). Container defects L-01 to L-03 fixed; a missing pivot or an invalid index is a hard error. `delhe` differs from the book. `lmekr` keeps the merged loop as the outer loop when it removes the outer one (stage 11). | 80% | `CircularArrayListTest`, `EulerOperatorSequenceFuzzTest`, `PolyhedralBoundedSolidEulerOperatorsTest`. |
| Ch. 11.3, Low-level operators | All implemented. The preconditions behind E-01 to E-06 are checked on entry; the operator rejects without side effects. | 88% | Operator suite and edge-case suite. |
| Ch. 11.5, High-level operators | `mev`, `smev`, `mef`, `smef`, `kemr`, `kfmrh`. | 82% | Wrapper tests on successful paths. |
| Ch. 12.2, Arcs and circles | `addArcToExistingFace`, `createCircularLamina`. | 70% | Cylinders, moon fixtures, sweep tests. |
| Ch. 12.3, Translational sweep | `translationalSweepExtrudeFace` and planar variant. | 70% | Fixtures; few direct tests. |
| Ch. 12.3/12.5, Rotational sweep | `rotationalSweepExtrudeWireAroundXAxis`. Closed profiles partial. | 65% | `CurveModelerRotationalSweepTest`. |
| Ch. 12.4, Gluing and `loopglue` | `loopGlue` with a search for matching start vertices and guards for degenerate loops; `merge`. | 70% | Boolean finish tests; no dedicated `loopGlue` matrix. |
| Ch. 13.1, Face equations | Newell plane, planarity validation. | 80% | Geometric validator tests. |
| Ch. 13.2, Containment and intersection | Point-in-face, edge/face intersection, tolerance contexts, robust predicates class. Read-only passes reuse face planes and boxes (`PolyhedralBoundedSolidFaceGeometry`). | 70% | Predicate tests, numeric policy tests, `PolyhedralBoundedSolidFaceGeometryTest`. |
| Ch. 13.3, Integral properties | Not in production. Volume exists only in the test oracle. | 10% | `PolyhedralBoundedSolidVolumeOracle` (test helper). |
| Ch. 14.1–14.3, Splitting outline | `_PolyhedralBoundedSolidSplitter.split` follows Program 14.1, with its state in a per-call context. | 85% | `…SplitterMantylaRegressionTest`, `…SplitterDegeneratePlaneTest`, `SplitterOracleFuzzTest`. |
| Ch. 14.4, `splitgenerate` | Implemented. | 85% | Split tests and split fuzz. |
| Ch. 14.5, Neighbourhood classification | Implemented with one documented sector convention. Planes through vertices work (S-01 fixed). ON edges between same-class sectors are reclassified only at reflex edges. The neighbourhood is built by `_PolyhedralBoundedSolidVertexNeighborhood`, shared with the vertex/face classifier (Problem 15.4). | 85% | 8 cutting and 6 non-cutting planes; split fuzz 256 / 256. |
| Ch. 14.6, Null-edge insertion | `insertNullEdges`, with `head` advanced between runs (Program 14.7) and struts oriented in connect. | 80% | Split fuzz. |
| Ch. 14.7, Joining | `splitConnect`, `canJoin`, `join`, `cut` with lexicographic sort. A section pinched at a reflex vertex is split into simple loops by `resolvePinchedLoops`. | 80% | Split fuzz, `…SplitterDegeneratePlaneTest`. |
| Ch. 14.8, Finish | `splitFinish`. Runs on a copy, so the input is unchanged; pieces go to the list of their side of the plane. | 80% | Volume, side and strict checks on every plane. |
| Ch. 14.9, Slicing | Not implemented as a separate operator. | 15% | None. |
| Ch. 15.1–15.4, Statement and classification | Five-phase algorithm for `UNION`, `INTERSECTION`, `SUBTRACT`. Face contact is glued; a union touching only along edges or at vertices is rejected as non-manifold (B-04, by decision). | 80% | Set-operator tests, strict set-op tests, oracle rows, oracle fuzz gates. |
| Ch. 15.5, `setopgenerate` | `_PolyhedralBoundedSolidSetIntersector` with a worklist, weld of coincident vertices, split of self-touching loops and edge/face box rejection. Edges lying in a face are handled (B-03 fixed). | 85% | `IntersectorWeldTest`, `IntersectorParametricOrderingTest`, oracle fuzz. |
| Ch. 15.6.1, Vertex/face classification | `_PolyhedralBoundedSolidSetVertexFaceClassifier`, on the neighbourhood shared with the splitter; a reflex sector is split by its bisector. A ring inside a face is finished correctly (B-01 fixed). | 80% | `VertexFaceClassifierCoplanarTest`, oracle rows, rotated fuzz families. |
| Ch. 15.6.2, Vertex/vertex classification | Sector search, reclassification of "on" sectors and edges, null-edge insertion. Coplanar faces with crossing edges work (B-02 fixed). `sectoroverlap` tests both ends of the angular intervals. | 75% | `SectoroverlapTraceDiagnosticTest`, coplanar predicate tests, `VertexVertexEndpointRecoveryTest`, coplanar fuzz family. |
| Ch. 15.7, `setopconnect` | `_PolyhedralBoundedSolidSetNullEdgesConnector`, ordered by the intersection curve builder, with its state per instance. Ring ownership fixed (B-01); rings follow the half that clearly contains them after both divisions of `join`. Stage 11: `scanjoin` joins only neighbours along the curve; rings are redistributed on both solids for every operation (the ring rescue is removed); contact nodes are removed before connect; an operation that ends with loose ends is rejected. Non-convex faces are answered on every oracle family. | 90% | `SetOpConnectScanJoinTest`, `SetOpConnectNoLooseInvariantTest` (no pending case), `IntersectionCurveBuilderTest`, `SetOpReentrancyTest`, `BooleanCompositeOracleFuzzGateTest`. |
| Ch. 15.8, `setopfinish` | `_PolyhedralBoundedSolidSetFinisher`: pair sanitising, `lmfkrh`, `revert`, `movefac`, `lkfmrh`, `loopGlue`, triangulation of non-planar faces. Stage 11: shells that no cut reaches are classified against the other solid and kept (the book's finish loses them); the two sides of a cut are checked against the other solid. | 85% | `SetOpFinishInvariantsTest`, `SetOpWholeShellTest`, reference pairs, chains of the composite oracle. |
| Ch. 15.4, "no intersections found" | Decisions for contact, identity and containment, taken before generate, and the same resolution after classify when no null edge is found; both answer from operands that generate has not edited. Whether the interiors overlap is decided from the boundaries, without sampling (`_PolyhedralBoundedSolidInteriorOverlap`). Two blocks sharing a face give a lamina for ∩, by policy. The partial-coplanar preflight is retired. | 85% | `InteriorOverlapPredicateTest`, `SetOpResultPathTest`, `PolyhedralBoundedSolidPreflightTest`, set-operator tests, oracle rows. |
| Ch. 15 problems | 15.1, 15.2, 15.3, 15.4, 15.5, 15.6 implemented; 15.9 partly tested; 15.7 and 15.8 missing. | 65% | See the backlog in `vitralCadKernel.md`. |
| Ch. 16, Undoing | Inverse operators and `revert`; no undo log. Structural copy (`scopy`, Problem 16.2) in `PolyhedralBoundedSolidStructuralCopy`; `setOp` and `split` leave their operands intact (C-01 fixed). | 35% | Inverse-operator tests, `PolyhedralBoundedSolidStructuralCopyTest`, `SetOpContractTest`. |
| Ch. 17, User Interface | JOGL4 debugger, HUD, CSG sample navigation, offline renderer. | 30% | Manual. |
| Ch. 18, Extensions | Outside this module. | 10% | None. |
| Appendix A, Homogeneous coordinates | `Vector3Dd`, `Matrix4x4d`, transforms. | 35% | Indirect. |
| Appendix B, Point-set topology | Regularisation as validation rules. A face-touching union is glued; edge- or vertex-touching unions are rejected instead of returned as pseudomanifolds (B-04 decision). | 40% | Validator tests, `BooleanVolumeOracleTest`. |

Not in the book and present in the kernel: the intersection curve builder,
scaled tolerance contexts, strict validation as a postcondition, the
topology summary, typed operation failures, the result path report, the
non-manifold result rejection, and
quantitative invisibility for the Appel hidden-line renderer ([APPE1967]).

## Operator and phase test map

| Area | Main implementation points | Tests | Confidence |
| --- | --- | --- | --- |
| List container | `CircularArrayList` | `CircularArrayListTest` | High; L-01 to L-03 fixed; handles after every edit; concurrent readers. |
| Euler operators | `PolyhedralBoundedSolidEulerOperators` | `…EulerOperatorsTest`, `…EulerOperatorsEdgeCasesTest`, `…KimrhMikrhTest` | High, including the empty-loop cases. |
| Half-edge invariants | Node classes, `_PolyhedralBoundedSolidTopologicalValidator`, `PolyhedralBoundedSolidValidationEngine` | `…TopologicalValidatorTest`, `…ValidationEngineTest`, `…TopologySummaryTest` | Medium-high; vertex back-pointers are checked. |
| Geometric predicates | `PolyhedralBoundedSolidGeometricValidator`, `…NumericPolicy`, `…Predicates`, `_…SetGeometricPredicateProcessor` | `…GeometricValidatorTest`, `…NumericPolicyTest`, `…PredicatesTest`, `…CoplanarPredicateTest` | Medium. |
| Split | `_PolyhedralBoundedSolidSplitter` | `…SplitterMantylaRegressionTest`, `…SplitterDegeneratePlaneTest`, `SplitterOracleFuzzTest` | High, including planes through vertices; 4 of 256 fuzz planes give a pinched, strict-invalid section. |
| Boolean generate | `_PolyhedralBoundedSolidSetIntersector` | `IntersectorWeldTest`, `IntersectorParametricOrderingTest` | Medium-high. |
| Boolean classify | `_…SetVertexFaceClassifier`, `_…SetVertexVertexClassifier`, `_…SetClassifier`, `_…SetNonIntersectingClassifier` | `VertexFaceClassifierCoplanarTest`, `SectoroverlapTraceDiagnosticTest`, `VertexVertexEndpointRecoveryTest`, `…PreflightTest` | Medium-high; coplanar and tangent box cases pass the oracle fuzz. |
| Boolean connect | `_…SetNullEdgesConnector`, `_…SetIntersectionCurveBuilder` | `SetOpConnectScanJoinTest`, `SetOpConnectNoLooseInvariantTest`, `IntersectionCurveBuilderTest` | Medium-high; rings inside a face fixed. |
| Boolean finish | `_…SetFinisher` | `SetOpFinishInvariantsTest`, `…StrictSetOpTest` | Medium-high. |
| Public contract | `PolyhedralBoundedSolidModeler`, `PolyhedralBoundedSolidStructuralCopy`, typed exceptions | `SetOpContractTest`, `PolyhedralBoundedSolidStructuralCopyTest` | High: operands intact, typed failures, no silent empty result. |
| Reference corpus | `SimpleTestGeometryLibrary`, `CsgSampleCorpus` | `BooleansFromReferenceObjectPairsTest`, `AlgebraicIdentityRegressionTest`, `Mant1988Section15_2UnionDiagnostic`, `RigidMotionInvarianceGateTest` | High for these fixtures; 35 of 36 operations are answered by the pipeline (`STACKED_BLOCKS` ∩ and differences through its "no intersections found" case; `CROSS_PAIR` A−B rejected as non-manifold), and all 36 are invariant under a rigid motion. |
| Kurlander bowl | `CsgKurlanderBowlFixture` | `KurlanderBowlStarInvariantTest`, `KurlanderBowlMotifSweepRegressionTest`, `KurlanderMotif4OperationMatrixTest`, `CsgKurlanderBowl…RegressionTest`, `CsgMoonCylinderDifferenceDegeneracyTest`, `Stage6FaceSubdivisionDiagnosticTest` | High: the 4 operations on all 40 motifs, and the whole bowl (82 booleans, genus 40, `CsgKurlanderBowlFullModelTest`, slow); baselines are kernel snapshots. |
| Applied models | `StepperMotorGuideCsgFixture` | `StepperMotorGuideStrictValidationTest` | High for this model. |
| Independent oracle | `PolyhedralBoundedSolidVolumeOracle`, `CompositeShapeOracle` (tests) | `BooleanVolumeOracleTest` (no disabled rows), `BooleanOracleFuzzGateTest` (560 cases, slow), `BooleanCompositeOracleFuzzGateTest` (non-convex, scales, chains) | The only correctness checks that do not depend on kernel output. All box cases and all non-convex cases correct (rotated at three scales, chains, coplanar bottoms: every ratchet at its total). |

## Known alignment gaps

- The shape-specific fallbacks and the partial-coplanar preflight were
  retired (2026-11-16). The result path of each operation is in the impact
  matrix of `optimizationsOverMANT1988.md`.
- `CROSS_PAIR` A−B has a non-manifold answer (two pieces touching along an
  edge) and is rejected with `NonManifoldBooleanResultException`, both
  as built and rotated.
- `STACKED_BLOCKS` ∩ returns a zero-volume lamina, not the regularised
  empty set. This is policy (2026-11-16): laminae are valid results because
  they are topologically sound and easy to display; only a collapsed
  lamina, with area below EPSILON, is rejected (`LaminaPolicyTest`).
- Contacts inside the tolerance band (faces 0.1 to 1 tolerances apart)
  are answered by moving B onto the planes of A before the operation.
  This is not in [MANT1988], which assumes exact arithmetic. Faces tilted
  by less than the tolerance are not covered.
- Global state (numeric context, id namespace, debug flags, id counters);
  the public operations are serialized on a lock.
- Integral properties (§13.3), slicing (§14.9), simultaneous results
  (Problem 15.7), two-dimensional set operations (Problem 15.8) and undo
  (Ch. 16) are not implemented.
