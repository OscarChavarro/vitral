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
- `common/dataStructures/CircularDoubleLinkedList` — the container under
  every face, loop, edge, vertex and half-edge list.
- Tests under `java/base/src/test/vsdk/toolkit/` (the directories
  `environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators`
  and `environment/geometry/volume/polyhedralBoundedSolid`; directories now
  match packages).

Scope decision kept from the previous audit: lack of curved-surface support
is excluded from the gap analysis.

Method: source reading, the stage plans 1–10, the full test suite
(`gradle :base:test`: 1109 tests, 0 failures, 38 skipped; 712 tests in the
previous version), the slow gates (`gradle :base:slowTest`: oracle fuzz and
rigid-motion invariance), and an independent volume oracle run on box pairs
and splitting planes that are not part of the fixture corpus.

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
  252 / 256 correct.

Weaknesses:
- A section pinched at a reflex vertex (two section regions meeting at one
  vertex inside a face) comes out as a single self-touching loop: right
  volume, strict-invalid (4 fuzz cases).
- The splitter and the vertex/face classifier remain two implementations
  (Problem 15.4).
- A slicing-only variant (§14.9, Problem 14.3) does not exist.

Assessment: general in the sense of §14.1 except for the pinched-section
case.

### 3.3 Set operations (`booleans/`)

Strengths:
- Pipeline aligned with Ch. 15 and split into intersector, classifiers,
  connector and finisher. The intersection curve is an explicit object.
- Ring ownership (B-01), coplanar faces with crossing edges and tangent
  containment (B-02), edges lying in a face (B-03) are fixed. Face contact is
  glued by the pipeline (B-04).
- All 560 cases of the oracle fuzz are correct, including rotated operands,
  translation to 1000 and coplanar faces after a 45° rotation.
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
- `STACKED_BLOCKS` ∩, A−B and B−A are still answered by the
  partial-coplanar preflight, not by the pipeline.
- Two blocks sharing a face give a zero-volume lamina for ∩, not the
  regularised empty set. This is policy (2026-11-16): a lamina is valid and
  easy to display; a collapsed lamina (area below EPSILON) is rejected.
- Static mutable state remains in the connector (scratch and counters), the
  debug flags and the id counters. The public operations are serialized on
  one lock: thread-safe, not concurrent.
- A failed input validation is still only a warning.

Assessment: correct on every box-pair family measured, including rotated
and coplanar ones, with every answer from the Chapter 15 pipeline or its
preflights. The shape-specific fallbacks were retired on 2026-11-16 (B-05
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
  kernel's own output; they detect change, not error. The oracle covers
  boxes only: non-convex operands and chains of booleans have no
  independent check yet.

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
- Static state in the connector, validation mixed with repair, input
  validation as a warning, the pinched split section, the
  `STACKED_BLOCKS` preflight. (B-05 closed: fallbacks retired; `CROSS_PAIR`
  A−B rejected as non-manifold; the lamina kept by policy.)

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
- The independent oracle covers box pairs only.
- Static state limits concurrency to one operation at a time.
- The kernel has not been measured on models with thousands of faces.

## 6) What is missing

Priority P0:
- Done 2026-11-16: fallbacks retired, `CROSS_PAIR`, `MANT1988_3` and
  `MANT1988_6_13` report `normal-pipeline`, rigid-motion gate 36 / 36;
  laminae allowed, collapsed laminae rejected.
- Answer `STACKED_BLOCKS` from the pipeline instead of the partial-coplanar
  preflight.

Priority P1:
- Split pinched sections into separate loops (reuse
  `splitSelfTouchingLoops`).
- An oracle for non-convex operands and chains of booleans.
- Remove the static state of the connector; then lift the lock.

Priority P2:
- Validation separated from repair; failed input validation as an error.
- Spatial index for larger models; measure on thousands of faces.
- Volume and other integral properties (§13.3) in production code.

The record of the plan and its open items is Section 10 of
`Fable5.2Analysis.md`.

## 7) Verdict

The kernel is a faithful and hardened realisation of [MANT1988], with one
important addition of its own (the explicit intersection curve). Measured
outside its corpus with an independent oracle, it is correct on every
box-pair family tried, including rotated and coplanar operands, and its
splitter handles planes through vertices. The shape-specific fallbacks are
gone; the open work is concurrency, the remaining preflight and an oracle
beyond box pairs.

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
| 13.5 (ring moving) | `laringmv`, ring rescue | Implemented | Check whether the ring rescue still fires |
| 13.6 (wide sector) | `checkWideness` (signed angle) | Implemented | Tolerance-boundary tests |
| 13.3 text (volume) | only in the test oracle | Missing in production | A kernel utility |
| 14.1 (`bisector`) | operator base | Implemented | Near-colinear case |
| 14.2 (`cleanup`) | operator base | Implemented | Separate rebuild from repair |
| 14.3 (slicing) | — | Missing | Section polygons as output |
| 15.1 (containment) | non-intersecting classifier, preflights | Implemented | — |
| 15.2 (`maximize_faces`) | `maximizeFaces` | Implemented | Test for a face inside another |
| 15.3 (no recursion) | worklist in the intersector | Implemented | — |
| 15.4 (vertex/face classifier) | `_SetVertexFaceClassifier` | Implemented | Sharing code with the splitter |
| 15.5 (`sectoroverlap`) | predicate processor | Implemented | — |
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
| Ring ownership in connect | B-01 closed | Remove the ring rescue if it no longer fires | Simpler connect |
| Coplanar faces | B-02, B-04 and B-05 closed; fallbacks retired, rigid-motion gate 36 / 36 | Answer `STACKED_BLOCKS` without the preflight | Every answer from the pipeline |
| No-intersection decisions | One predicate set with interior witness points; B-03 closed; laminae allowed by policy | Reject only collapsed laminae (done) | Laminae as displayable results |
| Splitter | Context object, S-01 closed, 252 / 256 fuzz | Split pinched sections; share the classifier | General splitting |
| Operand contract | Copy on entry; `setOpDestructive`; typed failures | — | Safe failure (done) |
| Static state | Connector scratch, debug flags, id counters; public operations behind a lock; tests parallel across JVMs | None outside constants | Concurrent operations |
| Validation | Back-pointer checks; non-manifold unions rejected | No repair in validators; input validation as an error | Fail-fast, diagnosable |
| Tests | Oracle fuzz, rigid-motion and split gates, contract tests, result paths | Oracle for non-convex operands and chains | Errors detected, not only changes |
| Efficiency | Box rejection in generate and strict validation; planes once per pass | Spatial index; measurements on large models | Larger models |
