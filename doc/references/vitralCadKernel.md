# CAD Kernel Audit

**Updated:** 2026-11-16, after the execution of the Fable 5.2 plan on the
Java port (`java/base`). The previous version (2026-11-14, revision
`4412e55a`) described the kernel before that plan; Section 4 compares the
two. The execution record is Section 10 of `Fable5.2Analysis.md`. The evidence behind every
statement marked with a defect id (B-, S-, E-, L-, C-) is in
`doc/polyhedralBoundedSolidDevelopment/Fable5.2Analysis.md`.

## 1) Scope and method

This audit evaluates the B-Rep kernel against [MANT1988]. Paths are relative
to `java/base/src/main/vsdk/toolkit/`:

- `environment/geometry/volume/polyhedralBoundedSolid` — solid, node classes,
  Euler operators, topology editing, validation, numeric policy, predicates.
- `environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators`
  — modeler façade, `slicing/`, `booleans/` (`intersection/`,
  `classification/`, `topology/`), `fallbacks/` (only the id namespace
  remains), `construction/`, `fixtures/`.
- `common/dataStructures/CircularArrayList` — the container under every
  face, loop, edge, vertex and half-edge list (array-backed since stage
  11; reading it writes nothing).
- Tests under `java/base/src/test/vsdk/toolkit/` (the directories
  `environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators`
  and `environment/geometry/volume/polyhedralBoundedSolid`; directories now
  match packages).

Scope decision kept from the previous audit: lack of curved-surface support
is excluded from the gap analysis.

Method: source reading, the stage plans 1–11, the full test suite
(`gradle :base:test`: 1149 tests, 0 failures, 0 skipped; 712 tests in the
April 2026 version), the slow gates (`gradle :base:slowTest`: box-pair oracle fuzz,
rigid-motion invariance, the whole Kurlander bowl), and two independent
volume oracles, for box pairs and splitting planes and for non-convex
operands and chains of booleans, that are not part of the fixture corpus.

## 2) MANT1988 baseline used for comparison

- Ch. 9–11: Euler operators and the half-edge structure give topological
  construction power; geometric correctness is a separate concern.
- Ch. 14: splitting must be closed, general and robust under numerical
  inaccuracy.
- Ch. 15: reduction → classify → connect → finish; coplanar and touching
  cases are central; 2-manifolds are not closed under set operations, so the
  result may be a pseudomanifold (criterion 3′).

## 3) Current implementation assessment

### 3.1 B-Rep core

Strengths:
- Operator vocabulary and data model follow the book (`mvfs/kvfs`,
  `lmev/lkev`, `lmef/lkef`, `lkemr/lmekr`, `lkfmrh/lmfkrh`, `lringmv`).
- Euler operators, topology editing and validation are separate classes.
- The list container defects (L-01 to L-03) and the operator special cases
  (E-01 to E-06) are fixed. The operators check those preconditions on entry
  and reject without side effects.
- `he.next()` and `he.previous()` are O(1): a half-edge keeps a
  back-pointer to its list node.
- `PolyhedralBoundedSolidStructuralCopy` gives a linear structural copy
  (`scopy`) that keeps ids, list order and loop starts.

Weaknesses:
- Global id counters in `_HalfEdge` and `_Edge`.
- `remakeEmanatingHalfedgesReferences` still mixes validation and repair.

Assessment: sound on the main paths and on the special cases the book calls
out.

### 3.2 Splitter (`slicing/_PolyhedralBoundedSolidSplitter`)

Strengths:
- Follows Program 14.1 (`splitGenerate`, `splitClassify`, `splitConnect`,
  `splitFinish`), with its state in a per-call context.
- Planes through vertices work (S-01 fixed). One sector convention is
  documented and used by the classification and the null-edge insertion.
- The input is never modified or returned; each piece goes to the list of
  its side of the plane, and a plane that does not cut sends a copy to the
  side the solid lies on.
- A seeded fuzz splits every corpus solid by planes through its vertices:
  256 / 256 correct. A section pinched at a reflex vertex is split into
  simple loops, and pieces that touch come out as separate solids.
- The splitter and the vertex/face classifier build their neighbourhood
  with one implementation (`_PolyhedralBoundedSolidVertexNeighborhood`,
  Problem 15.4); only the reclassification rules differ.

Weaknesses:
- A slicing-only variant (§14.9, Problem 14.3) does not exist.

Assessment: general in the sense of §14.1.

### 3.3 Set operations (`booleans/`)

Strengths:
- Pipeline aligned with Ch. 15 and split into intersector, classifiers,
  connector and finisher. The intersection curve is an explicit object.
- Ring ownership (B-01), coplanar faces with crossing edges and tangent
  containment (B-02), edges lying in a face (B-03) are fixed. Face contact is
  glued by the pipeline (B-04).
- All 560 cases of the oracle fuzz are correct, including rotated operands,
  translation to 1000 and coplanar faces after a 45° rotation.
- Tolerances follow the size of the model down to 10⁻⁹: the non-convex
  oracle gives the same answers at 10⁻³, 1 and 10³.
- The whole Kurlander bowl (82 booleans, genus 40) is one strict-valid
  shell.
- Contract: `setOp` leaves its operands intact (`setOpDestructive` keeps the
  book's behaviour). Failures are typed under
  `PolyhedralBoundedSolidOperationException`. An empty result that the
  operands disprove throws `SolidOperationFailedException` instead of being
  returned silently. `getLastSetOpResultPath()` names the path that
  answered.
- Generate rejects edge/face pairs by bounding boxes; read-only passes
  compute each face plane once.

Weaknesses:
- A union touching only along an edge or at a vertex is rejected
  (`NonManifoldBooleanResultException`), by decision, instead of returning
  the pseudomanifold of criterion 3′.
- Two blocks sharing a face give a zero-volume lamina for ∩, not the
  regularised empty set. This is policy (2026-11-16): a lamina is valid and
  easy to display; a collapsed lamina (area below EPSILON) is rejected.
- Inside the tolerance band the second operand is moved: a face of B
  parallel to a face of A and closer than two tolerances is put on its
  plane by translating B (`_PolyhedralBoundedSolidSetOperandAligner`).
  This is what makes 422 of the 432 operations of
  `NearDegenerateBooleanFuzzGateTest` answered (the other 10 are unions
  in edge contact, rejected by decision), and it means A ∪ B and B ∪ A
  can differ by that displacement. Faces tilted by less than the
  tolerance, not parallel, are not covered.
- Global state remains in the numeric context, the id namespace, the
  debug flags and the id counters; the connector itself is per instance.
  The public operations are serialized on one lock: thread-safe, not
  concurrent.

Assessment: correct on every family measured. Box pairs 560 / 560.
Non-convex operands (L, U, step and frame shapes; star prisms), rotated,
at three scales, with coplanar bottoms and in chains of three operations:
every check of `BooleanCompositeOracleFuzzGateTest` and
`BooleanVolumeIdentityFuzzGateTest` is answered right since stage 11
(`plan-stage11-robustness-hardening.md`). Every answer comes from the
Chapter 15 pipeline or from the three general decisions taken before it
(contact, identity, containment). The shape-specific fallbacks were retired on 2026-11-16 (B-05
closed, rigid-motion gate 36 / 36).

### 3.4 Validation

Strengths:
- Two levels (`validateIntermediate`, `validateStrict`) with composed
  strategies; shells and adjusted Euler characteristic in
  `PolyhedralBoundedSolidTopologySummary`.
- `validateIntermediate` checks the emanating half-edge of every vertex.
- Strict validation computes face planes and boxes once and skips face
  pairs whose boxes are apart.
- Numeric policy is centralised.

Weaknesses:
- The empty solid passes `validateStrict`; `isEmptyResult` and the
  lost-solid check of `setOp` cover the case for callers.
- `remakeEmanatingHalfedgesReferences` still removes vertices with a null
  emanating half-edge.

### 3.5 Tests

Strengths:
- An independent volume oracle (`PolyhedralBoundedSolidVolumeOracle`) with
  no disabled rows left, a seeded oracle fuzz over six box-pair families
  (560 cases, ratchets per family), a rigid-motion invariance gate, a split
  fuzz, and contract tests (`SetOpContractTest`).
- Fatal kernel errors fail a test instead of killing the Gradle worker
  (`vsdk.withSystemExit=false` for the test tasks).
- The test task runs in up to 8 forked JVMs: about 34 s for the suite.

Weaknesses:
- Most regression tests still assert topology counts captured from the
  kernel's own output; they detect change, not error. The oracles and the
  volume identities cover boxes, shapes made of boxes and prisms; solids
  with curved-like faces of many sides are checked by the identities
  only.

## 4) Comparison with earlier versions

### With the 2026-11-14 version

Closed by the Fable 5.2 plan:
- L-01 to L-03, E-01 to E-06, B-01, B-02, B-03, S-01, C-01.
- B-04: face contact glued; edge or vertex contact rejected as
  non-manifold.
- Fatal errors no longer kill the test worker; typed failures; explicit
  empty result.
- Splitter state in a context; input of `split` and `setOp` preserved.
- Bounding-box rejection in generate and strict validation; planes
  computed once per read-only pass.
- Oracle fuzz, rigid-motion and split fuzz gates.

Still open:
- The remaining global state. Validation mixed with repair and the
  `STACKED_BLOCKS` preflight were closed in stage 11. (B-05 closed: fallbacks retired; `CROSS_PAIR`
  A−B rejected as non-manifold; the lamina kept by policy.)
- Second pass (2026-11-16, Fable5.2Analysis.md 10.4): the pinched split
  section, input validation as an error, the connector's static state and
  the shared neighbourhood are closed; the non-convex oracle is in place
  and shows the open connect defects.
- Stage 11 (2026-11-16, `plan-stage11-robustness-hardening.md`): those
  connect defects are closed. Root causes: `lmekr` lost the outer loop of
  a face; connect redistributed rings on solid A only for intersections;
  `scanjoin` joined ends that were not neighbours on the curve; finish
  dropped shells that no cut reaches; contact nodes kept the curves from
  being ordered. The ring rescue is removed. Validation is read-only, the
  interior-overlap test no longer samples, the container is an array.

### Earlier: comparison of the 2026-11-14 version with the April 2026 version

Closed since then:
- Table-driven coplanar predicates, touching-only policy, iterative
  `maximizeFaces`.
- `son*` lists and neighbourhood scratch moved out of static fields (stage
  7).
- Algebraic drift tests replaced by `AlgebraicIdentityRegressionTest`.
- Strict validation checkpoint on every result path (stage 10).
- Side-channel outputs `lastIntersected*` removed.
- Performance: suite from about 14 minutes to about 1 minute for the same
  tests (stage 8).

Still open:
- Operand mutation, remaining static state, validation mixed with repair,
  typed failures, telemetry beyond the text trace.

New in this version:
- The defect families B-01 to B-05, S-01, E-01 to E-06, L-01 to L-03.
- The finding that part of the green fixture matrix depends on fallbacks
  and on axis alignment.

## 5) Production readiness (curved surfaces excluded)

Maturity: robust research prototype, close to usable for polyhedral models
built from boxes, prisms and sweeps.

Why not production yet:
- Global state limits concurrency to one operation at a time.
- The kernel has not been measured on models with thousands of faces.

## 6) What is missing

Priority P0:
- Done 2026-11-16: fallbacks retired, `CROSS_PAIR`, `MANT1988_3` and
  `MANT1988_6_13` report `normal-pipeline`, rigid-motion gate 36 / 36;
  laminae allowed, collapsed laminae rejected.
- Done in stage 11: `STACKED_BLOCKS` answered by generate and classify;
  connect on non-convex faces; the ring rescue retired; validation
  separated from repair.

- Done after stage 11: one decision for "these two faces coincide"
  inside the tolerance band, taken before the operation; the ring side
  test and three related costs (two 514-face cylinders from about 5.8 s
  to about 2 s).

Priority P1:
- Thread the numeric context and the id namespace through the
  operations; then lift the lock.

Priority P2:
- Spatial index for larger models; measure on thousands of faces.
- Volume and other integral properties (§13.3) in production code.

The record of the plan and its open items is Section 10 of
`Fable5.2Analysis.md` and `plan-stage11-robustness-hardening.md`;
efficiency is in `performance.md`.

## 7) Verdict

The kernel is a faithful and hardened realisation of [MANT1988], with one
important addition of its own (the explicit intersection curve). Measured
outside its corpus with an independent oracle, it is correct on every
box-pair family tried, including rotated and coplanar operands, and its
splitter handles planes through vertices. The shape-specific fallbacks are
gone, and so are the ring rescue and the special preflight. Non-convex
operands, coplanar faces and chains of operations are answered right on
every family of the oracles, and so are contacts inside the tolerance
band. The open work is concurrency and efficiency.

## 8) MANT1988 problems backlog

| Problem | Evidence in code | Status | Still missing |
|---|---|---|---|
| 11.3 (`lkev`, `lkef`) | `PolyhedralBoundedSolidEulerOperators` | Implemented | — |
| 11.4 (`lmekr`) | same | Implemented | — |
| 11.5 (`lkimrh`, `lmikrh`) | aliases of `lkfmrh`/`lmfkrh` | Implemented | Argument checks |
| 12.2 (rotational sweep, endpoint on axis) | `rotationalSweepExtrudeWireAroundXAxis` | Implemented (baseline) | Strict-clean results at the poles |
| 12.3 (closed figure with edge on axis) | same | Partial | Formal constraints, corpus |
| 12.4 (`unsweep`) | — | Missing | The routine |
| 13.1 (face equation at origin) | Newell plane on the face | Implemented | — |
| 13.2 (`dropcoord`) | face projection | Implemented | — |
| 13.3 (`contfv`) | `testPointInside` | Implemented | — |
| 13.5 (ring moving) | `laringmv`, `clearRingSide` in `join` | Implemented | — (rings are redistributed on both solids for every operation; the ring rescue is removed) |
| 13.6 (wide sector) | `checkWideness` (signed angle) | Implemented | Tolerance-boundary tests |
| 13.3 text (volume) | only in the test oracle | Missing in production | A kernel utility |
| 14.1 (`bisector`) | operator base | Implemented | Near-colinear case |
| 14.2 (`cleanup`) | operator base | Implemented | Separate rebuild from repair |
| 14.3 (slicing) | — | Missing | Section polygons as output |
| 15.1 (containment) | non-intersecting classifier, preflights | Implemented | — |
| 15.2 (`maximize_faces`) | `maximizeFaces` | Implemented | Test for a face inside another |
| 15.3 (no recursion) | worklist in the intersector | Implemented | — |
| 15.4 (vertex/face classifier) | `_SetVertexFaceClassifier`, `_PolyhedralBoundedSolidVertexNeighborhood` | Implemented | — (neighbourhood shared with the splitter) |
| 15.5 (`sectoroverlap`) | predicate processor | Implemented | — (both interval ends tested since 2026-11-16) |
| 15.6 (`revert`) | solid core | Implemented | — |
| 15.7 (both results at once) | — | Missing | Operand copy now exists |
| 15.8 (2D set operations) | Weiler–Atherton clipper exists separately | Missing in the kernel | Use it for coplanar faces |
| 15.9 (`A \ B` as `A ∩ reverse(B)`) | `AlgebraicIdentityRegressionTest` | Partial | Oracle-based laws |
| 16.1 (`printproperties`) | `PolyhedralBoundedSolidTopologySummary` | Implemented | — |
| 16.2 (`scopy`) | `PolyhedralBoundedSolidStructuralCopy` | Implemented | — |
| 16.4 (undo) | copy on entry in `setOp` and `split` | Partial | Operation log |

## 9) Robustness strategy

| Workstream | Current state | Target | Outcome |
|---|---|---|---|
| Container and Euler operators | Fixed; preconditions checked; O(1) half-edge neighbours | Intrusive links in the other ports | A trustworthy foundation |
| Ring ownership in connect | Closed: `lmekr` keeps the outer loop first, rings are redistributed on both solids for every operation, joins follow the curve; no rescue | — | Simpler connect (done) |
| Coplanar faces | B-02, B-04 and B-05 closed; fallbacks and the coplanar preflight retired; contact nodes removed before connect; coplanar non-convex family 72 / 72; B aligned with A inside the tolerance band (422 of 432 near-degenerate operations answered) | Faces tilted by less than the tolerance | No rejection near coincidence |
| No-intersection decisions | One predicate set with interior witness points; B-03 closed; laminae allowed by policy | Reject only collapsed laminae (done) | Laminae as displayable results |
| Splitter | Context object, S-01 closed, pinched sections split, neighbourhood shared with the vertex/face classifier, 256 / 256 fuzz | — | General splitting (done) |
| Operand contract | Copy on entry; `setOpDestructive`; typed failures | — | Safe failure (done) |
| Static state | Connector per instance; numeric context, id namespace, debug flags, id counters still global; public operations behind a lock; tests parallel across JVMs | None outside constants | Concurrent operations |
| Validation | Read-only; repair is `PolyhedralBoundedSolidRepair`, on request; non-manifold results and unclosed intersection curves rejected; invalid operands throw | — | Fail-fast, diagnosable (done) |
| Tests | Box and non-convex oracle fuzz (scales, chains, coplanar), volume identities on prisms, near-degenerate contacts, split with general planes, Euler operator sequences, determinism, read-only validation, reentrancy; every ratchet of the oracles at its total | — (every ratchet at its total, or at the count left by a decision) | Errors detected, not only changes |
| Efficiency | Box rejection in generate and strict validation; planes once per pass | Spatial index; measurements on large models | Larger models |
