# Coverage audit for [MANT1988] An Introduction to Solid Modeling

**Updated:** 2026-11-14, at revision `4412e55a` (Java port, `java/base`).

This document estimates how much of Mäntylä's book is represented by the
Vitral polyhedral bounded solid implementation. Paths are relative to
`java/base/src/`:

- `main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid`
- `main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators`
- `main/vsdk/toolkit/common/dataStructures/CircularDoubleLinkedList.java`
- `test/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid`
- `test/vsdk/toolkit/processing/polyhedralBoundedSolidOperators`
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
| Ch. 9.2.3, `MEV`, `MEF`, `KEMR` and inverses | Low- and high-level forms implemented. Special cases with empty loops are broken (E-01 to E-04). | 80% | Operator suite; `…EulerOperatorsEdgeCasesTest` (1 enabled, 6 disabled). |
| Ch. 9.2.4, `KFMRH`, `MFKRH` | Same-shell `lkfmrh`/`lmfkrh` and the `lkimrh`/`lmikrh` aliases. No shell abstraction. | 70% | `PolyhedralBoundedSolidKimrhMikrhTest`. |
| Ch. 9.3, Example | Box and holed-box constructions in `Box` and `SimpleTestGeometryLibrary`. | 65% | Fixture tests; no step-by-step assertion of Fig. 9.11. |
| Ch. 9.4, Properties | Euler characteristic and shells in `PolyhedralBoundedSolidTopologySummary`. Inverse round trips restore counts but not always back-pointers (E-03, E-05). | 40% | `PolyhedralBoundedSolidTopologySummaryTest`, inverse-pair tests. |
| Ch. 10.2, Data structure | Node hierarchy matches the book. Half-edge links are held in a list container instead of `nxt`/`prv`. | 80% | Validator tests, fixtures. |
| Ch. 10.3–10.4, Details and access | Neighbour access, mate, parent pointers. `next()` is a linear scan. | 70% | Indirect. |
| Ch. 11.2, Storage and lists | Java-managed; `CircularDoubleLinkedList` replaces `addlist`/`dellist`. Three container defects (L-01 to L-03). `delhe` differs from the book. | 50% | `CircularDoubleLinkedListTest` (3 enabled, 4 disabled). |
| Ch. 11.3, Low-level operators | All implemented. Preconditions are not enforced (E-06). | 82% | Operator suite and edge-case suite. |
| Ch. 11.5, High-level operators | `mev`, `smev`, `mef`, `smef`, `kemr`, `kfmrh`. | 82% | Wrapper tests on successful paths. |
| Ch. 12.2, Arcs and circles | `addArcToExistingFace`, `createCircularLamina`. | 70% | Cylinders, moon fixtures, sweep tests. |
| Ch. 12.3, Translational sweep | `translationalSweepExtrudeFace` and planar variant. | 70% | Fixtures; few direct tests. |
| Ch. 12.3/12.5, Rotational sweep | `rotationalSweepExtrudeWireAroundXAxis`. Closed profiles partial. | 65% | `CurveModelerRotationalSweepTest`. |
| Ch. 12.4, Gluing and `loopglue` | `loopGlue` with a search for matching start vertices and guards for degenerate loops; `merge`. | 70% | Boolean finish tests; no dedicated `loopGlue` matrix. |
| Ch. 13.1, Face equations | Newell plane, planarity validation. | 80% | Geometric validator tests. |
| Ch. 13.2, Containment and intersection | Point-in-face, edge/face intersection, tolerance contexts, robust predicates class. | 65% | Predicate tests, numeric policy tests. |
| Ch. 13.3, Integral properties | Not in production. Volume exists only in the test oracle. | 10% | `PolyhedralBoundedSolidVolumeOracle` (test helper). |
| Ch. 14.1–14.3, Splitting outline | `_PolyhedralBoundedSolidSplitter.split` follows Program 14.1. | 75% | `…SplitterMantylaRegressionTest`, `…SplitterDegeneratePlaneTest`. |
| Ch. 14.4, `splitgenerate` | Implemented. | 80% | Split tests. |
| Ch. 14.5, Neighbourhood classification | Implemented with orientation adaptations. A plane through a vertex loses the solid (S-01). | 55% | 4 plane kinds pass; 2 disabled rows for S-01. |
| Ch. 14.6, Null-edge insertion | `insertNullEdges`. | 60% | Indirect. |
| Ch. 14.7, Joining | `splitConnect`, `canJoin`, `join`, `cut` with lexicographic sort. | 65% | Indirect. |
| Ch. 14.8, Finish | `splitFinish`. The input solid is consumed. | 60% | Volume conservation on passing planes. |
| Ch. 14.9, Slicing | Not implemented as a separate operator. | 15% | None. |
| Ch. 15.1–15.4, Statement and classification | Five-phase algorithm for `UNION`, `INTERSECTION`, `SUBTRACT`. Criterion 3′ results are produced but rejected by strict validation (B-04). | 65% | Set-operator tests, strict set-op tests, oracle rows. |
| Ch. 15.5, `setopgenerate` | `_PolyhedralBoundedSolidSetIntersector` with a worklist, weld of coincident vertices and split of self-touching loops. Edges lying in a face are not handled (B-03). | 75% | `IntersectorWeldTest`, `IntersectorParametricOrderingTest`. |
| Ch. 15.6.1, Vertex/face classification | `_PolyhedralBoundedSolidSetVertexFaceClassifier`. Ring inside a face goes wrong downstream (B-01). | 60% | `VertexFaceClassifierCoplanarTest`, oracle rows. |
| Ch. 15.6.2, Vertex/vertex classification | Sector search, reclassification of "on" sectors and edges, null-edge insertion. Coplanar faces with crossing edges fail (B-02). | 60% | `SectoroverlapTraceDiagnosticTest`, coplanar predicate tests, `VertexVertexEndpointRecoveryTest`. |
| Ch. 15.7, `setopconnect` | `_PolyhedralBoundedSolidSetNullEdgesConnector`, ordered by the intersection curve builder, with ring rescue. Ring ownership is the open point (B-01). | 65% | `SetOpConnectScanJoinTest`, `SetOpConnectNoLooseInvariantTest`, `IntersectionCurveBuilderTest`. |
| Ch. 15.8, `setopfinish` | `_PolyhedralBoundedSolidSetFinisher`: pair sanitising, `lmfkrh`, `revert`, `movefac`, `lkfmrh`, `loopGlue`, triangulation of non-planar faces. | 70% | `SetOpFinishInvariantsTest`, reference pairs. |
| Ch. 15.4, "no intersections found" | Preflights for touching, identity, containment and partial coplanar area. Tangent containment and straddling solids are misjudged (B-02 b, B-03); face contact is not glued (B-04). | 55% | `PolyhedralBoundedSolidPreflightTest`, set-operator tests, oracle rows. |
| Ch. 15 problems | 15.1, 15.2, 15.3, 15.4, 15.5, 15.6 implemented; 15.9 partly tested; 15.7 and 15.8 missing. | 65% | See the backlog in `vitralCadKernel.md`. |
| Ch. 16, Undoing | Inverse operators and `revert`; no undo log; clone by serialisation. Operands are consumed (C-01). | 20% | Inverse-operator tests. |
| Ch. 17, User Interface | JOGL4 debugger, HUD, CSG sample navigation, offline renderer. | 30% | Manual. |
| Ch. 18, Extensions | Outside this module. | 10% | None. |
| Appendix A, Homogeneous coordinates | `Vector3Dd`, `Matrix4x4d`, transforms. | 35% | Indirect. |
| Appendix B, Point-set topology | Regularisation as validation rules; touching unions are not regularised (B-04). | 30% | Validator tests. |

Not in the book and present in the kernel: the intersection curve builder,
scaled tolerance contexts, strict validation as a postcondition, the
topology summary, four shape-specific fallbacks, and quantitative
invisibility for the Appel hidden-line renderer ([APPE1967]).

## Operator and phase test map

| Area | Main implementation points | Tests | Confidence |
| --- | --- | --- | --- |
| List container | `CircularDoubleLinkedList` | `CircularDoubleLinkedListTest` | Low until L-01 to L-03 are fixed. |
| Euler operators | `PolyhedralBoundedSolidEulerOperators` | `…EulerOperatorsTest`, `…EulerOperatorsEdgeCasesTest`, `…KimrhMikrhTest` | High on main paths; low on empty-loop cases. |
| Half-edge invariants | Node classes, `_PolyhedralBoundedSolidTopologicalValidator`, `PolyhedralBoundedSolidValidationEngine` | `…TopologicalValidatorTest`, `…ValidationEngineTest`, `…TopologySummaryTest` | Medium; back-pointers are not checked. |
| Geometric predicates | `PolyhedralBoundedSolidGeometricValidator`, `…NumericPolicy`, `…Predicates`, `_…SetGeometricPredicateProcessor` | `…GeometricValidatorTest`, `…NumericPolicyTest`, `…PredicatesTest`, `…CoplanarPredicateTest` | Medium. |
| Split | `_PolyhedralBoundedSolidSplitter` | `…SplitterMantylaRegressionTest`, `…SplitterDegeneratePlaneTest` | Medium away from vertices; none through vertices. |
| Boolean generate | `_PolyhedralBoundedSolidSetIntersector` | `IntersectorWeldTest`, `IntersectorParametricOrderingTest` | Medium. |
| Boolean classify | `_…SetVertexFaceClassifier`, `_…SetVertexVertexClassifier`, `_…SetClassifier`, `_…SetNonIntersectingClassifier` | `VertexFaceClassifierCoplanarTest`, `SectoroverlapTraceDiagnosticTest`, `VertexVertexEndpointRecoveryTest`, `…PreflightTest` | Medium-low for coplanar and tangent cases. |
| Boolean connect | `_…SetNullEdgesConnector`, `_…SetIntersectionCurveBuilder` | `SetOpConnectScanJoinTest`, `SetOpConnectNoLooseInvariantTest`, `IntersectionCurveBuilderTest` | Medium; low for rings inside a face. |
| Boolean finish | `_…SetFinisher` | `SetOpFinishInvariantsTest`, `…StrictSetOpTest` | Medium. |
| Reference corpus | `SimpleTestGeometryLibrary`, `CsgSampleCorpus` | `BooleansFromReferenceObjectPairsTest`, `AlgebraicIdentityRegressionTest`, `Mant1988Section15_2UnionDiagnostic` | High for these fixtures; three families pass through fallbacks. |
| Kurlander bowl | `CsgKurlanderBowlFixture` | `KurlanderBowlStarInvariantTest`, `KurlanderBowlMotifSweepRegressionTest`, `KurlanderMotif4OperationMatrixTest`, `CsgKurlanderBowl…RegressionTest`, `CsgMoonCylinderDifferenceDegeneracyTest`, `Stage6FaceSubdivisionDiagnosticTest` | High for A−B on 40 motifs; baselines are kernel snapshots. |
| Applied models | `StepperMotorGuideCsgFixture` | `StepperMotorGuideStrictValidationTest` | High for this model. |
| Independent oracle | `PolyhedralBoundedSolidVolumeOracle` (test) | `BooleanVolumeOracleTest`: 18 rows enabled, 20 disabled | The only correctness check that does not depend on kernel output. |

## Known alignment gaps

- Fixtures `CROSS_PAIR`, `MANT1988_6_13`, `MANT1988_15_1`, `MANT1988_3` and
  `MANT1988_15_2_HOLED`, listed as failing in the previous version, now pass
  in all four operations. `MANT1988_15_1` (B−A), `MANT1988_3` and
  `MANT1988_6_13` pass through shape-specific fallbacks, not through the
  Chapter 15 pipeline.
- Ring strictly inside a face (B-01): `A ∩ B` and one difference keep a
  zero-area face. This is the generic failure for rotated operands.
- Coplanar faces outside axis alignment (B-02, B-05), edges lying in a face
  (B-03) and touching solids (B-04).
- Split by a plane through a vertex (S-01).
- Empty-loop cases of the Euler operators and the list container (E-, L-).
- Integral properties (§13.3), slicing (§14.9), simultaneous results
  (Problem 15.7), two-dimensional set operations (Problem 15.8) and undo
  (Ch. 16) are not implemented.
