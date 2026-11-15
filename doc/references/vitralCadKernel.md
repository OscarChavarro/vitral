# CAD Kernel Audit

**Updated:** 2026-11-14, at revision `4412e55a` (Java port, `java/base`).
This version replaces the April 2026 audit. The evidence behind every
statement marked with a defect id (B-, S-, E-, L-, C-) is in
`doc/polyhedralBoundedSolidDevelopment/Fable5.2Analysis.md`.

## 1) Scope and method

This audit evaluates the B-Rep kernel against [MANT1988]. Paths are relative
to `java/base/src/main/vsdk/toolkit/`:

- `environment/geometry/volume/polyhedralBoundedSolid` — solid, node classes,
  Euler operators, topology editing, validation, numeric policy, predicates.
- `environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators`
  — modeler façade, `slicing/`, `booleans/` (`intersection/`,
  `classification/`, `topology/`), `fallbacks/`, `construction/`, `fixtures/`.
- `common/dataStructures/CircularDoubleLinkedList` — the container under
  every face, loop, edge, vertex and half-edge list.
- Tests under `java/base/src/test/vsdk/toolkit/` (the directories
  `processing/polyhedralBoundedSolidOperators` and
  `environment/geometry/volume/polyhedralBoundedSolid`; the first one
  declares the package of `geometricProcessing`, so directory and package do
  not match).

Scope decision kept from the previous audit: lack of curved-surface support
is excluded from the gap analysis.

Method: source reading, the stage plans 1–10, the full test suite
(`gradle :base:test`: 712 tests, 0 failures, 48 skipped), and an independent
volume oracle run on box pairs and splitting planes that are not part of the
fixture corpus.

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
- Euler operators, topology editing and validation are separate classes
  since the June refactor.
- `maximizeFaces` is iterative and has a planarity guard; the open
  "Case 2: not tested" marker of the previous audit is gone.

Weaknesses:
- The list container has three defects: stale head after the list becomes
  empty (L-01), `remove` with an invalid index deletes the element under the
  stale window (L-02), `insertBefore` with a missing pivot inserts at the
  head (L-03).
- Six operator edge cases are broken or unchecked: `lkev` followed by `lmev`
  on a skeleton (E-01), `lkemr` when the outer loop collapses to one vertex
  (E-02), `lmekr` leaving a null emanating half-edge (E-03), `lmef(he, he)`
  (E-04), `lkev` leaving a dangling emanating half-edge (E-05), `lmef`
  across two loops (E-06).
- Operators report broken preconditions with warnings and may return after
  partial modification.
- `he.next()` is a linear scan of the loop; the book's intrusive links are
  O(1).

Assessment: sound on the main paths, not yet on the special cases the book
calls out (empty loops, inverse round trips).

### 3.2 Splitter (`slicing/_PolyhedralBoundedSolidSplitter`)

Strengths:
- Follows Program 14.1 (`splitGenerate`, `splitClassify`, `splitConnect`,
  `splitFinish`).
- Correct on generic planes, planes containing an edge, planes through two
  opposite edges, and planes coincident with a face (volume conserved,
  pieces strict-valid).
- A regression test exists for the MANT1986 fixture.

Weaknesses:
- A plane through a vertex of the solid returns two empty solids and
  empties the input, without any message (S-01).
- All state is still static (`soov`, `sone`, `sonf`, `ends`, …).
- When the plane misses the solid, the input object itself is returned.
- A slicing-only variant (§14.9, Problem 14.3) does not exist.

Assessment: close to the book and reliable away from vertices; not general
in the sense of §14.1.

### 3.3 Set operations (`booleans/`)

Strengths:
- Pipeline aligned with Ch. 15 and split into intersector, classifiers,
  connector and finisher.
- The intersection curve is an explicit object
  (`_PolyhedralBoundedSolidSetIntersectionCurveBuilder`) and orders the
  connect step.
- Per-operation list state lives in `_SetOperationContext`.
- Strict validation is a default postcondition of `setOp`, with a topology
  summary in the failure message.
- All reference fixtures and the 40-motif Kurlander bowl sweep pass.

Weaknesses:
- A section ring strictly inside a face is finished incorrectly when the
  result needs the disk inside the ring: a zero-area face with two
  coincident loops remains and strict validation throws (B-01). This is the
  generic failure for rotated boxes: 26 of 48 operations correct on
  randomly rotated box pairs.
- Coplanar faces whose edges cross, and containment with tangent faces,
  give wrong volumes or a fatal error (B-02).
- A solid with edges lying in a face of the other is taken as contained;
  the wrong result passes strict validation (B-03).
- Two solids sharing a face are returned as two shells; strict validation
  rejects that result (B-04).
- Axis-aligned coplanar cases are answered by the cell fallback; the same
  operands after a common rigid motion fail (B-05). `MANT1988_15_1`,
  `MANT1988_3` and `MANT1988_6_13` are also answered by fallbacks.
- Fallbacks are built before they are needed, and the cell fallback writes
  the B-Rep directly, without Euler operators.
- Operands are consumed (C-01).
- Static mutable state remains: debug flags, numeric context, id namespace,
  the connector's scratch and counters, finisher counters, global id
  counters in `_HalfEdge` and `_Edge`. Execution is not reentrant.
- No bounding-box rejection in generate.

Assessment: correct on the fixture families and on axis-aligned general
position; not yet correct for two arbitrary convex polyhedra.

### 3.4 Validation

Strengths:
- Two levels (`validateIntermediate`, `validateStrict`) with composed
  strategies; shells and adjusted Euler characteristic in
  `PolyhedralBoundedSolidTopologySummary`.
- Numeric policy is centralised.

Weaknesses:
- `validateIntermediate` does not check vertex back-pointers (E-05 passes).
- The empty solid passes `validateStrict`.
- Strict validation rejects the pseudomanifold results that §15.2 admits
  and that the touching-only preflight produces.
- `remakeEmanatingHalfedgesReferences` still removes vertices with a null
  emanating half-edge, mixing validation and repair.
- A failed input validation is logged and the operation continues.

### 3.5 Tests

Strengths:
- 48 kernel-related test classes; direct operator tests, invariant tests on
  connect and finish, curve builder tests, algebraic identity tests.
- An independent volume oracle (`PolyhedralBoundedSolidVolumeOracle`) with
  18 box-pair rows that pass and 20 rows kept as a disabled work queue.

Weaknesses:
- Most regression tests assert topology counts captured from the kernel's
  own output; they detect change, not error.
- Touching cases are tested with strict validation off and assert two
  shells.
- `Logger` fatal errors call `System.exit(1)` unless a test class disables
  it; only a few do.
- No fuzz or rigid-motion invariance gate yet.

## 4) Comparison with the April 2026 version of this document

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

Maturity: advanced research prototype.

Why not production yet:
- Wrong results on simple inputs outside the corpus, some of them silent.
- The splitter loses the solid on a common degenerate plane.
- Operands are destroyed and execution is not reentrant.
- Quality gates are snapshots of the kernel's own output.

## 6) What is missing

Priority P0:
- Fix the container and the Euler special cases; check operator
  preconditions and reject without side effects.
- Fix ring ownership in connect (B-01).
- Fix the splitter for planes through vertices (S-01).
- Make the volume oracle and a seeded fuzz a permanent gate.

Priority P1:
- Treat coplanar faces inside the pipeline with a two-dimensional boolean
  (B-02, B-04, B-05) and unify the preflight predicates (B-03); then retire
  the fallbacks.
- Non-destructive `setOp` through a structural copy.
- Remove the remaining static state; move the splitter to a context object.

Priority P2:
- Typed failures; validation separated from repair.
- Bounding-box rejection in generate; spatial index for strict validation.
- Intrusive half-edge links.

The staged plan is in `Fable5.2Analysis.md`, Section 7.

## 7) Verdict

The kernel is a faithful and much-hardened realisation of [MANT1988], with
one important addition of its own (the explicit intersection curve). It
passes its corpus. Measured outside the corpus it is reliable for
axis-aligned operands in general position and unreliable for rotated
operands, coplanar faces and planes through vertices.

## 8) MANT1988 problems backlog

| Problem | Evidence in code | Status | Still missing |
|---|---|---|---|
| 11.3 (`lkev`, `lkef`) | `PolyhedralBoundedSolidEulerOperators` | Implemented, with defects | E-01, E-05 |
| 11.4 (`lmekr`) | same | Implemented, with defects | E-03 |
| 11.5 (`lkimrh`, `lmikrh`) | aliases of `lkfmrh`/`lmfkrh` | Implemented | Argument checks |
| 12.2 (rotational sweep, endpoint on axis) | `rotationalSweepExtrudeWireAroundXAxis` | Implemented (baseline) | Strict-clean results at the poles |
| 12.3 (closed figure with edge on axis) | same | Partial | Formal constraints, corpus |
| 12.4 (`unsweep`) | — | Missing | The routine |
| 13.1 (face equation at origin) | Newell plane on the face | Implemented | — |
| 13.2 (`dropcoord`) | face projection | Implemented | — |
| 13.3 (`contfv`) | `testPointInside` | Implemented | — |
| 13.5 (ring moving) | `laringmv`, ring rescue | Implemented, with defects | B-01 |
| 13.6 (wide sector) | `checkWideness` (signed angle) | Implemented | Tolerance-boundary tests |
| 13.3 text (volume) | only in the test oracle | Missing in production | A kernel utility |
| 14.1 (`bisector`) | operator base | Implemented | Near-colinear case |
| 14.2 (`cleanup`) | operator base | Implemented | Separate rebuild from repair |
| 14.3 (slicing) | — | Missing | Section polygons as output |
| 15.1 (containment) | non-intersecting classifier, preflights | Implemented, with defects | B-02 (b), B-03 |
| 15.2 (`maximize_faces`) | `maximizeFaces` | Implemented | Test for a face inside another |
| 15.3 (no recursion) | worklist in the intersector | Implemented | — |
| 15.4 (vertex/face classifier) | `_SetVertexFaceClassifier` | Implemented, with defects | B-01 |
| 15.5 (`sectoroverlap`) | predicate processor | Implemented | B-02 |
| 15.6 (`revert`) | solid core | Implemented | — |
| 15.7 (both results at once) | — | Missing | Needs operand copy |
| 15.8 (2D set operations) | Weiler–Atherton clipper exists separately | Missing in the kernel | Use it for coplanar faces |
| 15.9 (`A \ B` as `A ∩ reverse(B)`) | `AlgebraicIdentityRegressionTest` | Partial | Oracle-based laws |
| 16.1 (`printproperties`) | `PolyhedralBoundedSolidTopologySummary` | Implemented | — |
| 16.2 (`scopy`) | serialisation-based clone | Partial | Structural copy |
| 16.4 (undo) | — | Missing | Operation log or copy-on-entry |

## 9) Robustness strategy

| Workstream | Current state | Target | Outcome |
|---|---|---|---|
| Container and Euler operators | Defects L-01–L-03, E-01–E-06 | Checked preconditions, intrusive half-edge links, round-trip tests | A trustworthy foundation |
| Ring ownership in connect | Unconditional `laringmv`, operation-specific control, ring rescue | Invariant: one two-loop null face per curve and solid; carrier face keeps its hole | B-01 closed, rescue removable |
| Coplanar faces | Sector-level reclassification, fallbacks for axis-aligned shapes | Face-pair `on` classification plus a 2D boolean | B-02, B-04, B-05 closed, fallbacks retired |
| No-intersection decisions | Four preflights with separate predicates | One predicate set with interior witness points | B-03 closed |
| Splitter | Static state, S-01 | Context object, shared classifier with the vertex/face path | General splitting |
| Operand contract | Destructive | Copy on entry, destructive variant named explicitly | Safe failure |
| Static state | Partly removed | None outside constants | Reentrant, parallel tests |
| Validation | Two levels, gaps | Back-pointer checks, pseudomanifold rule, no repair in validators | Fail-fast, diagnosable |
| Tests | Snapshots plus an oracle work queue | Seeded fuzz, rigid-motion invariance, result-path assertions | Errors detected, not only changes |
| Efficiency | All edges × all faces, all-pairs strict scan | Box rejection, spatial index | Larger models |
