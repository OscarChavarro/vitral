# Fable 5.2 Analysis — PolyhedralBoundedSolid CAD Kernel

**Date:** 2026-11-14
**Revision analysed:** `4412e55a` (main), Java port (`java/base`)
**Scope:** `PolyhedralBoundedSolid`, its Euler operators, the splitter and the
boolean set operations; the reference [MANT1988]; the 2026 git history; the
development notes in `doc/` and `doc/polyhedralBoundedSolidDevelopment/`.
**Constraint honoured:** no production code was changed. Only tests were
added (Section 8).

All Gradle commands in this document run from `java/`.

---

## 1. Summary

The 2026 work turned a 7.6 k-line literal transcription of the book (9 source
files, no kernel unit tests, booleans failing on the book's own figures) into
a 22.9 k-line kernel with 48 test classes that passes every reference fixture
and the 40-motif Kurlander bowl sweep. That is a real achievement, and the
stage plans document it honestly.

The corpus, however, is what has been made to pass. When the kernel is
measured against an oracle that does not depend on the corpus, on pairs of
plain boxes that are *not* fixtures, it fails often:

| Box-pair family (4 operations each) | Correct and strict-valid | What goes wrong |
|---|---:|---|
| Axis-aligned, general position | 48 / 48 | — |
| B rotated about z | 46 / 48 | defect B-01 |
| B rotated about a random axis | 26 / 48 | defect B-01 |
| Same, translated to (1000, 1000, 1000) | 26 / 48 | same cases fail: translation is not a factor |
| Integer-grid coordinates (coplanar faces, shared edges) | 314 / 320 | 62 of those answered by the cell fallback, 6 fail on B-04 |
| Coplanar top and bottom, B rotated 45° about z | 19 / 48 | 19 silently wrong volumes, 5 fatal errors, 5 strict rejections (B-02) |

The main findings, in order of importance:

1. **A section ring strictly inside a face is not finished correctly
   (B-01).** This is the "peg in a block" case, and also the generic case
   for rotated boxes (a corner poking through a face). `A ∩ B` and one
   difference return the right volume but keep a zero-area face with two
   coincident loops, so the default strict postcondition throws.
2. **Coplanar faces only work when both operands are axis-aligned (B-02,
   B-05).** There the axis-aligned cell fallback answers instead of the
   Chapter 15 pipeline. Rotate both operands by the same rigid motion and
   the same boolean returns a wrong volume or dies with a fatal error.
3. **Some wrong answers are silent (B-02, B-03).** They pass strict
   validation: `A \ B` returning `A` unchanged, `A ∪ B` returning `A`,
   `B \ A` returning the empty solid.
4. **The splitter returns two empty solids, and empties its input, whenever
   the plane passes through a vertex of the solid (S-01).** The corner cut
   `x + y + z = 1` of a cube is the smallest reproduction.
5. **The list container under every loop has three defects (L-01 to L-03),**
   and five Euler-operator special cases are broken or unchecked (E-01 to
   E-06). They are mostly latent today, but they are the layer everything
   else stands on.
6. **Two face-touching cubes do not unite (B-04).** The union returns two
   shells; existing tests codify that as correct and call `setOp` with
   strict validation off.

Four new test classes (39 test cases) capture all of this (Section 8). The suite stays
green: reproductions of defects are `@Disabled` with a defect id, following
the policy in `optimizationsOverMANT1988.md` ("disabled tests are a work
queue").

Section 7 proposes a plan. Its core is: make the oracle fuzz a permanent
gate, fix the foundation layer first, then B-01 (one root cause, largest
effect), then replace the fallbacks by a proper treatment of coplanar faces.

Section 9 is the dialogue with Mäntylä: chapter by chapter, what the text
would have needed to say for a reader to reach a complete kernel.

---

## 2. Method

1. Read the audit documents (`vitralCadKernel.md`, `coverage_MANT1988.md`,
   `optimizationsOverMANT1988.md`) and the ten stage plans.
2. Read the 2026 git history for the kernel (about 120 commits between
   2026-04-16 and 2026-07-27, plus the architecture moves up to 2026-11-09).
3. Read the production code: Euler operators, node classes, the list
   container, the set operator driver, the finisher, `loopGlue`, the
   fallbacks, the numeric policy. The classifier and connector internals
   (about 4 k lines) were read selectively, guided by traces.
4. Read chapters 14 and 15 of [MANT1988] in full, and the problems, programs
   and relevant sections of chapters 9 to 13 and 16.
5. Ran the baseline: `gradle :base:test` → **673 tests, 0 failures, 35
   skipped, 5 min 08 s** on this machine.
6. Wrote standalone probes compiled against `base/build/classes` (no
   production change) and an instrumented private copy of the finisher, to
   test hypotheses that came from reading the code.
7. Built an independent oracle (Section 2.1) and fuzzed box pairs and
   splitting planes.
8. Converted every confirmed finding into a JUnit test.

### 2.1 The oracle

The existing regression tests assert topology snapshots (faces, edges,
vertices, bounding box) captured from the kernel's own output. Such a
baseline can encode a bug: stage 6 documents one case where it did. The
tests cannot say whether a *new* configuration is right.

The oracle added here uses two facts that do not depend on the half-edge
pipeline:

- the volume of a B-Rep by the divergence theorem ([MANT1988] §13.3,
  generalised to faces with rings);
- for convex operands, the exact intersection volume `I` by clipping
  half-spaces, which gives `vol(A ∪ B) = vol(A) + vol(B) − I`,
  `vol(A ∩ B) = I`, `vol(A \ B) = vol(A) − I`.

A result is accepted only if it passes `validateStrict` **and** its volume
matches. Both are needed: a zero-area lamina does not change the volume
(B-01), and a wrong solid can be perfectly valid (B-02, B-03).

The oracle lives in
`base/src/test/.../polyhedralBoundedSolidOperators/PolyhedralBoundedSolidVolumeOracle.java`.

---

## 3. Evolution of the implementation

| Period | State |
|---|---|
| 2007–2021 (`871b4447`) | 9 kernel files, 7,632 lines: `PolyhedralBoundedSolid`, five node classes, `PolyhedralBoundedSolidOperator`, `...Splitter`, `...SetOperator`. A transcription of Programs 10.x–15.x with the exercises answered. No kernel unit tests (5 test files in the repository). Booleans failed on several of the book's figures. |
| 2026-04-16 → 04-23 | Validation levels, numeric policy, containment (Problem 15.1), touching-only policy, `checkWideness`, coplanar overlap algebra, iterative `maximizeFaces`. Block-and-wedge union fixed in connect (ring movement on side A). Three fixture families recovered by **fallbacks** rather than by the pipeline. |
| 2026-05-14 → 05-17 (stages 1–3) | Kurlander bowl exposes connect as heuristic. Flags and workarounds removed, `scanjoin` aligned with the book, algebraic identity tests, 40 × 4 diagnostic matrix. Stars pass, moons fail. |
| 2026-05-30 → 06-13 (stages 4–8) | Intersection curve builder: curve order becomes the ordering authority in connect; curve-neighbour ring rescue closes the sweep (40/40). Infinite-plane vertex snap fixed. God class split, `_SetOperationContext`, fallbacks extracted. Performance: suite 14 min → 1 min 08 s. |
| 2026-06-13 → 06-14 (stage 9) | Appel hidden-line work: robust predicates class, quantitative invisibility. |
| 2026-07-26 → 07-27 (stage 10) | `doStrictValidation` postcondition, default `true`; stepper-motor guide fixture; topology summary; splitter fix. |
| 2026-09 → 2026-11 | Package moves (`geometricProcessing`), ports. No kernel behaviour change. |

Three observations about this history matter for what follows.

**The corpus drove the fixes.** Each stage closed when a named set of
fixtures passed. The fixtures are the book's figures plus one demanding
model (the bowl). Stage 5 itself says of the stars: "luck made reproducible,
not a validated invariant". The same is true, one level up, of the whole
corpus: it samples a small part of the configuration space, and the kernel
is tuned to that part.

**Three fixes are replacements, not repairs.** `MANT1988_15_1`,
`MANT1988_3` and `MANT1988_6_13` pass because a shape-specific builder
(profile difference, axis-aligned cells, orthogonal profiles) produces the
answer when connect leaves loose ends. The impact matrix in
`optimizationsOverMANT1988.md` shows them green; it does not show that the
Chapter 15 pipeline still fails on them.

**The documents have drifted.** `vitralCadKernel.md` still lists static
`son*` state and missing thread safety that stage 7 partly fixed, and
`coverage_MANT1988.md` still lists `CROSS_PAIR` and `MANT1988_6_13` as
failing. Both name packages that no longer exist. Test sources sit in
`src/test/vsdk/toolkit/processing/...` but declare the package
`vsdk.toolkit.environment.geometry.geometricProcessing...`.

---

## 4. Current architecture, briefly

```
PolyhedralBoundedSolid                    solid node, ray queries, merge, revert
  nodes/_Face,_Loop,_HalfEdge,_Edge,_Vertex
  CircularDoubleLinkedList                container for faces/edges/vertices/loops/half-edges
PolyhedralBoundedSolidEulerOperators      mvfs … lmfkrh, high-level wrappers
PolyhedralBoundedSolidTopologyEditing     loopGlue, compactIds, maximizeFaces, …
PolyhedralBoundedSolidValidationEngine    validateIntermediate / validateStrict
PolyhedralBoundedSolidNumericPolicy       ToleranceContext (scale = bbox diagonal)

slicing/_Splitter                         Ch. 14, still all-static state
booleans/_SetOperator                     driver: preflights → generate → classify → connect → finish
  intersection/_SetIntersector            Ch. 15.5
  intersection/_IntersectionCurveBuilder  not in the book (stage 5)
  classification/…                        Ch. 15.6 + non-intersecting classifier (1,341 lines)
  topology/_NullEdgesConnector            Ch. 15.7 (1,593 lines; the book's is ~40)
  topology/_SetFinisher                   Ch. 15.8
fallbacks/, construction/*Fallback*       4 shape-specific builders (1,803 lines)
```

The driver's control flow is worth stating, because it explains several
findings:

1. normalise operands (`compactIds`, `maximizeFaces`), validate inputs
   (a failure is only logged);
2. **build the fallbacks eagerly** (profile difference, offset cylinder,
   axis-aligned cells, orthogonal profiles);
3. preflights that bypass the pipeline: partial coplanar area, touching
   only, geometric identity, containment only;
4. generate → split self-touching loops → classify;
5. if classify emitted no null edge: "no intersection" case;
6. connect; if loose ends remain and a fallback exists, **the fallback is
   the result**;
7. finish; on exception or degenerate result, a fallback is the result;
8. post-process, strict validation.

Seven distinct result paths exist. Only path 4–7 without fallback is
Mäntylä's algorithm.

---

## 5. Defects found

Every defect below was reproduced by a probe and is captured by a test.
"Silent" means no exception, no warning, and a strict-valid result.

### 5.1 Booleans

#### B-01 — Section ring strictly inside a face

*Configuration.* A block and a peg whose end is inside the block: the four
peg edges pierce the interior of one block face. The section polygon is a
ring inside that face ([MANT1988] Fig. 15.6). Also a cube corner poking
through a slab face (triangular ring), and a peg crossing the whole block
(two rings).

*Behaviour.* With the carrier face on A: `A ∪ B` and `A \ B` are correct.
`A ∩ B` and `B \ A` have the correct volume but one extra face made of two
coincident loops of opposite orientation, plus duplicated vertices
(peg case: F=7, E=16, V=12 instead of F=6, E=12, V=8). Strict validation
reports "Face has intersecting loops", so the default call throws.

*Evidence.* An instrumented copy of the finisher on the corner case shows
that, before gluing, the component moved from the carrier solid is not a
lamina of two faces but three faces: the real cap, an annular zero-area
face (outer loop on one copy of the ring vertices, inner ring on the other
copy) and the null face. The carrier face left behind has **no hole**: the
ring that the struts formed inside it was moved out of it. `finish` glues
the null-face pair correctly, and the annular face remains. Calling
`loopGlue` once more on that face yields the correct solid (F=7, E=15, V=10
for the corner case, strict-valid, same volume).

*Likely cause.* Ring ownership during `join`. Stage 5 already recorded that
`laringmv` moves every ring unconditionally, that the second `lmef` in
`join` does not redistribute rings, and that the block-and-wedge fix added
"ring-movement control" for UNION on side A only. The operations that pass
here are exactly those that need the carrier face *with* its hole; those
that fail need the disk *inside* the ring. This is consistent with a rule
calibrated for one of the two halves.

*Why the corpus missed it.* No fixture has a ring strictly inside a face
whose inside disk is needed, except through shapes where a fallback or the
ring rescue applies.

*Weight.* It explains 22 of 22 failures in the random-rotation family and
both failures in the z-rotation family.

#### B-02 — Coplanar faces whose edges cross, or tangent containment

*Configuration (a).* Slab 2×2×1 and cube 1×1×1 with the same z range; the
cube is rotated 30° about z and sticks out of the slab.

*Behaviour.* `A ∪ B` and `B \ A` correct. `A \ B` returns A unchanged
(volume 4.0 instead of 3.5), **silent**. `A ∩ B` raises a fatal
`IndexOutOfBounds` from `CircularDoubleLinkedList.get` inside the finisher
(`boundariesList.get(1)` on a face with one loop).

*Configuration (b).* Small box inside the slab, top and bottom faces on the
slab's faces.

*Behaviour.* Classify records 8 vertex/face contacts and emits **no** null
edge, so the driver takes the "no intersection after classify" branch and
treats the solids as disjoint: `A ∪ B` returns both shells with volume
`vol(A) + vol(B)`; `A \ B` returns A, silent; `A ∩ B` is the same fatal
error.

*Reading.* This is the case the book warns about in §15.1 (Fig. 15.1,
"coplanar overlapping faces such as the bottom face") and addresses with
Eq. 15.3. The reclassification is implemented for vertex/vertex sectors;
what is missing is a consistent treatment when *all* contacts of a face are
"on" contacts.

#### B-03 — Edges of B lying in a face of A

*Configuration.* Cube 2×2×2; a unit cube rotated 45° about z and centred on
the face `x = 1`, so that two of its vertical edges lie in that face and
half of it is outside.

*Behaviour.* The containment-only preflight accepts B as contained in A.
`A ∪ B` returns A (8.0 instead of 8.5), `A ∩ B` returns B (1.0 instead of
0.5), `B \ A` returns the empty solid (0 instead of 0.5): all **silent**.
`A \ B` is rejected by strict validation.

*Reading.* The preflight decides containment from vertex classification
with "on the boundary" counted as "inside". With vertices on the boundary
and no proper edge/face crossing, a solid that straddles the boundary is
indistinguishable from one that is tangent from inside.

#### B-04 — Touching solids do not unite, and strict validation rejects the kernel's own answer

*Configuration.* Two unit cubes sharing a full face; two boxes touching
along an edge segment.

*Behaviour.* The touching-only preflight returns both shells unchanged.
Volume is right. Strict validation says "Faces intersect improperly", so the
default `setOp` throws for the simplest union there is.

*Existing tests.* `PolyhedralBoundedSolidSetOperatorTest` and
`VertexFaceClassifierCoplanarTest` call these cases with
`doStrictValidation=false` and assert two shells with Euler sum 4, or only
`size() >= 0`. The regularised union of two face-sharing cubes is one box.
The book's Eq. 15.2 gives it: `AonB−` faces drop out and the outer parts are
glued.

*Decision needed.* For face contact the answer should be one shell. For
edge or vertex contact the book's criterion 3′ (§15.2) admits the two-shell
pseudomanifold, so strict validation must accept that pattern or the
preflight must say that it does not produce strict-valid results.

#### B-05 — The axis-aligned fallback hides the coplanar defect

*Configuration.* B-02 (a) without the rotation: slab and cube axis-aligned,
coplanar top and bottom. Then the same pair after one rigid rotation applied
to both operands.

*Behaviour.* Axis-aligned: all four operations correct; the trace shows
"axis-aligned cell fallback replacing …" for three of them. Rigidly rotated:
`A ∩ B` fatal, `A \ B` returns A (silent).

*Reading.* A boolean must commute with a rigid motion of both operands.
The kernel does not have this property. In the integer-grid fuzz, 62 of 320
results came from the fallback.

*Side effects of the fallback design.*
- It is built before the preflights, for every pair of axis-aligned
  operands, whether or not it is used (up to 15³ cells, two ray casts each).
- It writes faces, loops and half-edges directly, bypassing the Euler
  operators. `attachEdge` stores the first two half-edges of an edge and
  ignores further ones, so a result in which two cells meet only along an
  edge gets a half-edge whose edge does not point back to it.
- Its point classification uses one fixed ray and ignores hits on a face
  boundary, so parity can be wrong for a sample whose ray grazes an edge.
- `buildUniformSampledCellBooleanFallback` (a 12³ voxel approximation) is
  dead code.

#### C-01 — `setOp` consumes its operands

After a successful union of two boxes, operand A holds 0.65 of its original
2.69 volume units: the faces needed by the result were moved out. This is
the book's design (§15.8), and `vitralCadKernel.md` already lists it as P0.
It is restated here because it interacts with everything above: after a
strict-validation exception the caller has neither a result nor its
operands.

### 5.2 Splitter

#### S-01 — A plane through a vertex returns nothing

*Configuration.* Cube `[-1,1]³`, plane `x + y + z = 1` (through three
vertices, cutting off a corner tetrahedron). Also any plane through exactly
one vertex that cuts the cube.

*Behaviour.* `split` returns one solid above and one below, both with zero
faces. The input solid is also empty afterwards. No exception, no warning.
`validateStrict` returns `true` for the empty solids.

*Controls that pass.* Generic oblique plane, mid plane, plane containing one
edge, diagonal plane through two opposite edges, plane coincident with a
face, plane touching a vertex from outside, plane missing the solid.

*Reading.* A vertex on the plane with a non-trivial neighbourhood is
precisely what §14.5 is about. Planes through edges work, so the ON-sector
and ON-edge rules are in place; the failing pattern is an ON-vertex whose
edges are all strictly above or below, combined with vertices from
`splitgenerate` elsewhere.

*Other observations.* The splitter still keeps all its state in static
fields (`soov`, `sone`, `sonf`, `ends`, …); stage 7 did not reach it. When
the plane misses the solid, the list "above" receives the *same object* as
the input, not a copy.

### 5.3 Euler operators

| Id | Operator | Behaviour | Expected ([MANT1988]) |
|---|---|---|---|
| E-01 | `lkev` then `lmev` | `mvfs → smev → lkev → smev` gives a 2-half-edge loop in which one half-edge is its own `next()` | §11.2.2: `delhe`/`addhe` keep the edge-less half-edge of an empty loop consistent |
| E-02 | `lkemr(he1, he2)` | With the arguments swapped so that the outer component collapses to one vertex: fatal "Case B Should not happen!" | §11.3.4: both resulting loops may be empty; the Javadoc itself says "swap the arguments" |
| E-03 | `lkemr` → `lmekr` | The vertex of a one-vertex ring gets `emanatingHalfEdge = null`; `lmekr` does not restore it | Problem 11.4: inverse of `lkemr` |
| E-04 | `lmef(he, he)` | Returns a face whose loop is empty; both new half-edges land in the old loop | Program 11.7: a "circular" face with one edge |
| E-05 | `lkev` on a strut of a box | The surviving vertex keeps an emanating half-edge that was removed from its loop. `validateIntermediate` still returns `true` | vertex back-pointer must stay valid |
| E-06 | `lmef(he1, he2)` | Half-edges from different loops: a whole loop is migrated, a face is returned, the solid is invalid | §11.3.3: precondition `he1.wloop == he2.wloop` |

E-03 has a second edge: `remakeEmanatingHalfedgesReferences` removes
vertices whose emanating half-edge is null, so a live vertex can be deleted
by a "repair" pass.

Other points read in the code and not turned into tests:
- `lmekr` empties the killed ring with `for (i…) remove(i)`, which removes
  every other element. Harmless only because the ring is discarded.
- `lkfmrh` does not check `face1 != face2` or that both faces belong to the
  solid.
- `lkev` calls `mirrorHalfEdge().next()` without guarding the edge-less
  half-edge.

### 5.4 List container

| Id | Behaviour |
|---|---|
| L-01 | `removeElemAtWindow` leaves `head` on the removed node when the list becomes empty. The next `add` links behind the stale node: `get(0)` returns the removed element, and neighbour queries see a phantom node. `lkev` performs exactly this sequence; E-01 is its consequence. |
| L-02 | `remove(index)` with an out-of-range index does not move the window and then deletes whatever element the window was left on. |
| L-03 | `insertBefore(x, pivot)` with a pivot that is not in the list inserts at the head. `addhe` depends on this call. |

### 5.5 Process and contract hazards

- **`Logger` fatal errors call `System.exit(1)` by default.** One kernel
  test class switches this off. Any other test that reaches a `FATAL_ERROR`
  path kills the Gradle worker instead of failing. The kernel has 13 such
  call sites.
- **Static mutable state remains** after stage 7: `debugFlags` (four
  classes), `numericContext`, `idNamespace`, the whole connector scratch
  (`endsa`, `endsb`, `lastLoose*`, `curveNeighborPositions`, …), the finisher
  counters, the curve builder's `lastTraversalNeighborPositions`, and global
  id counters in `_HalfEdge` and `_Edge`. The driver reads
  `getLastLooseACount()` to choose a fallback. Two booleans on two threads
  cannot run.
- **An empty solid passes `validateStrict`.** That is defensible for
  `A \ A`, but it means "the operation produced nothing" is never an error
  (B-03 `B \ A`, S-01).
- **Input validation failure is a warning.** `validateBooleanInputs`
  failing does not stop the operation.
- **`deepCloneSolid` uses Java serialisation** and catches
  `StackOverflowError`. Serialising a linked structure recursively is
  depth-limited by the stack; a large solid will fail to clone, and the
  identity preflight then returns an empty solid for `A ∪ A`.
- **`he.next()` is a linear scan** of the loop (`nextOf`). Stage 8 measured
  it as the top leaf frame and reverted two map-based attempts. The book's
  intrusive `nxt`/`prv` pointers are O(1) and would also remove L-01 to L-03
  for half-edges.

---

## 6. Assessment

**What is solid.** The half-edge model and the successful paths of the
local Euler operators. The numeric policy as a single place for tolerances.
The validation engine and the strict postcondition (it is what made B-01
visible). The intersection curve builder: making the curve an explicit
object is the most important idea added to the book's algorithm, and its
report (`cleanlyClosed`, isolated nodes, pinches) is a good diagnostic. The
trace infrastructure. The discipline of the stage logs.

**What is fragile.**

- *Connect.* 1,593 lines against the book's forty. It orders by curve,
  votes on orientation, interleaves cycles round-robin and rescues rings by
  re-parenting. Stage 5 states that "no purely geometric redistribution rule
  is decidable mid-connect". B-01 is on this fault line.
- *The decision layer in front of the pipeline.* Four preflights and four
  fallbacks, each with its own notion of inside, touching and contained.
  B-02 (b), B-03 and B-04 are all wrong decisions made *before* Chapter 15
  runs.
- *Tests as snapshots.* About 2,000 lines of Kurlander tests assert numbers the
  kernel produced. They detect change, not error.

**Efficiency.** After stage 8 the suite is fast enough. Structurally the
algorithm is still all edges × all faces with no bounding-box rejection
(the book's own bibliographic note says a practical version "should
definitely" have it), strict validation is an all-pairs face scan (+86 % on
the suite), and loop traversal is quadratic in loop length. None of this
matters for the present models; all of it will for a model with thousands of
faces.

**Maturity.** The verdict of `vitralCadKernel.md` stands: an advanced
research prototype. The numbers in Section 1 refine it: reliable for
axis-aligned general position and for the fixture families; not yet
reliable for two arbitrary convex polyhedra.

---

## 7. Plan

Ordered so that each step makes the next one safer. Every step ends with
`gradle :base:test` green and the oracle rows it claims re-enabled.

### Stage A — Make the oracle a gate (tests only)

1. Keep `BooleanVolumeOracleTest` rows as the work queue; enable a row when
   its defect is fixed, never by weakening the assertion.
2. Add a `@Tag("slow")` fuzz test built on `PolyhedralBoundedSolidVolumeOracle`:
   the six families of Section 1 with a fixed seed, asserting a minimum
   count of correct results per family (start at today's numbers, raise
   only). This is the same pattern as `MINIMUM_OK_COUNT` in the Kurlander
   sweep.
3. Add a rigid-motion invariance test over the *existing* fixtures: every
   reference pair, rotated by one fixed generic rotation, must give the same
   topology summary and volume as the unrotated pair. This measures how much
   of the green matrix depends on axis alignment.
4. Add an assertion on the result path: expose `resultPath` (it already
   exists as a local string) and let tests assert `normal-pipeline`. A
   fixture that passes through a fallback should say so in the matrix.
5. Set `VSDK.setWithSystemExit(false)` once for the whole test task (a JUnit
   extension or `systemProperty`), not per class.

### Stage B — Foundation (small, local, high leverage)

1. Fix L-01, L-02, L-03. Make a missing pivot and an invalid index hard
   errors.
2. Fix E-01 to E-06. For each operator, check its book precondition on
   entry and reject without side effects.
3. Extend `validateIntermediate`: every vertex's emanating half-edge is
   non-null (unless the vertex is the lone vertex of an empty loop), starts
   at the vertex and is a member of its loop; every loop's
   `boundaryStartHalfEdge` is a member of the loop.
4. Decide whether to keep the container for half-edges. Recommendation:
   intrusive `next`/`previous` fields on `_HalfEdge`, as in the book. It
   removes the linear `next()`, removes the class of bugs in L-01 to L-03
   from the hottest structure, and makes the Java code line up with
   Programs 11.3–11.8 again, which helps the C++, TypeScript and Turbo C
   ports. Stage 8 called this "the invasive 17-site change"; with the
   operator edge-case tests in place it is a contained change.

### Stage C — B-01

1. Write the invariant first, as a test on the connect output: after
   connect, for every closed curve, each solid has exactly one null face,
   with exactly two loops, and the carrier face of an interior ring has that
   ring as an inner loop. The corner-poke case violates it today.
2. Fix ring ownership in `join` so that the invariant holds for both halves,
   not by operation. The stage 5 shadow study is the starting material.
3. Expected effect: random-rotation family from 26/48 to 48/48.
4. Then test whether the ring rescue is still needed. If the invariant
   holds, it should never fire; assert that on the Kurlander sweep and
   remove it if so.

### Stage D — Coplanar faces and the preflights

1. Define one predicate set for "A and B have no proper intersection" and
   use it in all preflights. A solid is contained only if no vertex is
   outside **and** a witness interior point of every face is not outside
   (this separates B-03 from tangent containment).
2. Handle faces that lie on each other inside the pipeline, following
   Eq. 15.2: classify face pairs as `AonB+` / `AonB−` up front, and compute
   the overlap of two coplanar faces with a 2D boolean. The repository
   already has a Weiler–Atherton clipper with union (commits `4f11d0ee`,
   `63da7895`); this is what the book means in §15.1 by "a two-dimensional
   set operation of two polygons".
3. Fix B-04 for face contact with the same machinery (glue along `AonB−`).
4. Then retire the fallbacks one at a time. The criterion: the fixture that
   needed it reports `normal-pipeline` and still matches its baseline. Until
   then, build them lazily, only when connect has failed.

### Stage E — Splitter

1. Fix S-01. The test names the two reproductions.
2. Move the splitter's static state into a context object, as stage 7 did
   for the set operator.
3. Return copies, never the input object.
4. Decide whether the splitter and the vertex/face classifier can share code
   (Problem 15.4 says they can). Two implementations of the same
   neighbourhood classification is twice the surface for bugs like S-01.

### Stage F — Contracts

1. Non-destructive `setOp` by default: copy the operands with a structural
   copy (Problem 16.2, `scopy`), not serialisation. Keep the destructive
   form under an explicit name.
2. Typed failures instead of `IllegalStateException` and `VSDKFatalException`.
3. Remove the remaining static state; then run the test task with parallel
   forks (stage 8 step P7).
4. An explicit result for "empty": callers should be able to tell
   `A \ A = ∅` from "the operation lost the solid".

### Stage G — Efficiency, when models grow

1. Bounding-box rejection in generate (edge box against face box, then solid
   box against solid box).
2. A spatial index for strict validation's face-pair scan.
3. Cache face planes on the face with invalidation by the Euler operators.

### Documentation

Update `coverage_MANT1988.md` and `vitralCadKernel.md`, or mark them as
superseded. Add the result path to the impact matrix in
`optimizationsOverMANT1988.md`. Move test sources to directories that match
their packages.

---

## 8. Tests added

No production code was touched. `gradle :base:test --tests …` on the four
new classes: 39 tests, 0 failures, 13 skipped (the `@Disabled` methods; the
boolean work queue counts once but holds 20 rows).

| File | Enabled | Disabled (defect) |
|---|---|---|
| `common/dataStructures/CircularDoubleLinkedListTest` | 3 basic behaviours | 4 (L-01 ×2, L-02, L-03) |
| `…/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperatorsEdgeCasesTest` | 1 (`lkemr` lone-vertex ring) | 6 (E-01 … E-06) |
| `…/polyhedralBoundedSolidOperators/BooleanVolumeOracleTest` | 18 rows that match the oracle today | 20 rows (B-01 ×6, B-05 ×2, B-02 ×6, B-03 ×4, B-04 ×2) and C-01 |
| `…/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidSplitterDegeneratePlaneTest` | 4 planes | 2 planes (S-01) |
| `…/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidVolumeOracle` | helper | — |

Each disabled test was first run enabled and failed for the stated reason.
To see a defect, remove its `@Disabled` and run, for example:

```bash
gradle :base:test --tests '*BooleanVolumeOracleTest' -x :base:jacocoTestReport
```

The fuzz harness that produced the table in Section 1 was a scratch program
and is not in the repository; Stage A step 2 is to rebuild it as a
slow-tagged test on the oracle helper.

### Further tests worth writing

Not written because they need a decision or more construction code:

- Non-convex operands against a sampled oracle (point-in-solid by an
  independent ray count): L-shapes, a block with a through hole (genus 1).
- Chains: `(A ∪ B) \ C`, with results of one boolean as operands of the
  next. For boxes the expected volume is exact by inclusion–exclusion. This
  is the stepper-motor incident class of stage 10.
- Algebraic laws with the oracle: commutativity of ∪ and ∩ by volume and
  topology summary, `vol(A \ B) + vol(A ∩ B) = vol(A)`, De Morgan inside a
  bounding box.
- Scale: the same pair at 10⁻³ and 10³. The tolerance scale is the bounding
  box diagonal; translation was checked (no effect at 10³), scale was not.
- Vertex-on-edge and vertex-on-vertex contacts in non-axis-aligned position
  (a square pyramid apex touching a face; two boxes sharing one vertex).
- `maximizeFaces` on a face lying completely inside another (hint of
  Problem 15.2) and on the result of every oracle case.
- Reentrancy: a boolean that throws, followed by an unrelated boolean in the
  same JVM, must give the same result as the unrelated boolean alone.
- `loopGlue` on loops of 3, 4 and n vertices with every rotation of the
  start half-edge, and on loops where two vertices coincide.

---

## 9. Dialogue with Mäntylä (diálogo con Mantilla)

The book gives a complete *outline* of a boundary modeller and a complete
*implementation* of its lower half. From chapter 14 on, more and more is
"left as an exercise", "not elaborated here" or "space does not permit". The
2026 work is, to a large extent, what those sentences cost. This section
goes through the text and says, at each point, what a note in the margin
should have said. It is written as suggestions to the author, with the
evidence from this project.

### Chapter 6 — Boundary Models

**§6.3 Validity.** The chapter lists the conditions of a valid boundary
model and says that checking them is expensive. *Suggestion:* give the
checks as a procedure with two levels, one for use between Euler operators
(structure only) and one for finished solids (geometry too), and say that
the intermediate states of chapters 14 and 15 satisfy only the first. We
had to discover this: `validateIntermediate` and `validateStrict` were
introduced on 2026-04-17, and the strict level only became a postcondition
in stage 10, after a visually correct but invalid solid had contaminated a
later union.

**Figure 6.13.** It became our fixture `MANT1988_6_13`. It still does not
pass through the general pipeline.

### Chapter 9 — Euler Operators

**§9.2.3, KEMR.** The text says that the special cases in which one or both
resulting loops are empty are included. *Suggestion:* draw them. A figure
with the three cases (ring is a lone vertex, outer loop is a lone vertex,
both) would have fixed E-02, which fails on the second.

**Table 9.1 and Figure 9.11.** The table of operators and the step-by-step
construction of a block are the best test specification in the book.
*Suggestion:* print the `[v e f h r s]` tuple after every step of
Fig. 9.11. Then the figure is an executable test, and Problem 9.2 has a
checkable answer.

**§9.4, soundness and completeness.** The theorems say that every operator
has an inverse. *Suggestion:* add the practical corollary as a requirement
on the code: applying an operator and its inverse must restore the data
structure *including back-pointers*, and the structure must remain usable
afterwards. Our inverse tests checked the counts and passed; E-01, E-03 and
E-05 are all cases where the counts are restored and the structure is not.

**Problem 9.5** (soundness of KEMR/MEKR): the conditions it names (edge
identification, cyclic identification, orientability) are exactly what a
validator should check. A sentence saying so would connect the theory to
the code.

### Chapter 10 — Half-Edge Data Structure

**§10.2–10.3, Program 10.1.** The half-edge has `nxt` and `prv`. We replaced
them by a general list container, and three of the defects in this report
(L-01 to L-03) and the main performance hotspot of stage 8 come from that
choice. *Suggestion:* state why the links are intrusive. A loop is not a
collection that contains half-edges; the half-edges *are* the loop, and
`lmef`, `lkemr` and `lmekr` work by splicing. A reader who ports the book to
a language with standard containers should be warned that the splicing
operators do not translate into container operations.

**The empty loop.** The half-edge with a null edge pointer that stands for a
one-vertex loop is introduced almost in passing. It is the source of most
special cases in chapter 11. *Suggestion:* give it a name, a figure, and a
list of the invariants that are suspended for it (no mate, `nxt == self`,
`vedge` of its vertex points to it).

**`vedge`.** The text says it points to "one of" the half-edges emanating
from the vertex. *Suggestion:* add the rule every operator must follow: if
you remove the half-edge that `vedge` points to, redirect it. E-03 and E-05
break this rule.

**Problems after Program 10.4.** The listing procedures are the natural
place to introduce a dump format. We built ours late; a comparable text dump
of a solid from the start would have made every later diagnosis cheaper.

### Chapter 11 — Implementation of Euler Operators

**§11.2.2, `addhe` and `delhe` (Programs 11.3, 11.4).** These two procedures
contain the empty-loop logic. Our `delhe` carries the comment "quite
different from the original … this could lead to subtle problems". It did.
*Suggestion:* state their contract as a table: for `delhe`, three cases
(half-edge without edge; last half-edge of its loop; ordinary), with the
state of the loop after each.

**Program 11.7, `lmef`.** The text says `he1` and `he2` may be equal.
*Suggestion:* show that case in a figure, and state the precondition
`he1->wloop == he2->wloop` as a check in the code. E-04 and E-06.

**Program 11.8, `lkemr`.** See §9.2.3 above. *Suggestion:* add to the text
which vertex of a collapsed loop keeps the half-edge, and what happens to
`vedge`.

**Problems 11.3–11.5** (`lkev`, `lkef`, `lmekr`, `lkimrh`, `lmikrh`). Half
of the low-level operators are exercises. *Suggestion:* at least give, for
each, the three or four cases an answer must handle and a round-trip test:
"apply `lmev` then `lkev` on every vertex of the block of Fig. 9.11, in
every order of arguments, and compare the listing". Our answer to 11.3 was
"borrowed" and is correct on the main path only.

**§11.5, high-level operators.** They search by identifier and report
failure by return value. *Suggestion:* say what state the solid is in after
a failed operator. The answer should be "unchanged". Ours log a warning and
return, sometimes after partial modification.

### Chapter 12 — Basic Modeling Algorithms

**§12.4.2, Program 12.9, `loopglue`.** This procedure becomes the last step
of every boolean. The text presents it for two cylinders. *Suggestions:*

1. State the precondition: the two loops have the same number of vertices,
   pairwise coincident, in opposite orientation, and the first half-edges of
   both loops are at coincident vertices. The last condition is not
   guaranteed after chapter 15; we added a search for a matching start
   (`findMatchingLoopVertices`).
2. State what to do with loops of fewer than three vertices. We added guards
   in stage 2 (§9.5 of that plan).
3. Say that after `loopglue` the face has disappeared, so the caller must
   not use it.

**Problem 12.2 and 12.3** (rotational sweep with points on the axis). They
need "an algorithm that reduces a face to a single vertex". That algorithm
is also what a boolean needs to remove degenerate faces. A cross-reference
would help.

**Problem 12.4, `unsweep`.** Referenced again by Problem 15.8. Not
implemented here.

### Chapter 13 — Geometric Algorithms

**§13.1, Program 13.1, `faceeq`; Problem 13.1.** The problem says accuracy
is "considerably enhanced" by translating the face to the origin.
*Suggestion:* put that in the program, not in a problem. Every predicate
downstream depends on it.

**§13.2, `comp` and `EPS`.** The section says the tolerance must be chosen
carefully and suggests measuring the fuzziness of the model. *Suggestions:*

1. Say that the tolerance must scale with the model, and give the scaling.
   We introduced `PolyhedralBoundedSolidNumericPolicy` with a scale from the
   bounding box on 2026-04-17.
2. Say that one tolerance is not enough. We use two (`epsilon` and
   `bigEpsilon`) and separate ones for unit vectors, angles and parameters.
3. **Warn that distance to a plane is not distance to a face.** Stage 6 was
   a vertex snapped onto the *infinite* plane of a face far away from it,
   which bent four quads on the other side of the model. The rule "never
   move a vertex because of a plane unless the vertex projects into the
   face" belongs in this section.
4. Say what must be true *after* a tolerant decision: if two vertices are
   declared coincident, later predicates must agree. Our post-generate weld
   exists for this.

**Programs 13.3–13.6, `contfv`, `contlv`, `int2ee` and the globals `hithe`,
`hitvertex`.** The containment test returns its result in external
variables. *Suggestion:* return a record. `vitralCadKernel.md` lists the
side-channel outputs (`lastIntersected*`) as a thread-safety problem.

**Problem 13.5 (ring-moving), Problem 13.6 (wide sector).** These two
exercises are load-bearing for chapters 14 and 15: `laringmv` decides which
face owns a ring after `lmef`, and `checkwideness` decides the shape of
every neighbourhood. *Suggestion:* move both into the text. For 13.5, state
the criterion (a ring belongs to the face whose outer loop contains it) and
then state its limit: **in the middle of the joining step the loops are not
simple polygons, so containment is not decidable there.** This single
sentence is the summary of stage 5, items 5 and 6, and the probable cause of
B-01.

**§13.3, integral properties.** Presented as an application. *Suggestion:*
present the volume as the first test of any modelling operation. A splitting
algorithm is tested by `vol(above) + vol(below) = vol(S)`; a set operation
by inclusion–exclusion. We had the formula for years and did not use it this
way until this analysis; it found B-01 to B-05 and S-01 in an afternoon.

### Chapter 14 — A Splitting Algorithm

**§14.1.** The algorithm "must work correctly for all possible geometric
configurations" and be robust. *Suggestion:* follow the claim with the list
of configurations, as a table: plane through no vertex; through one vertex
with edges on both sides; through one vertex with all edges on one side;
through an edge; through a face; through several of these at once. S-01 is
row two of that table.

**§14.4, Program 14.2, `splitgenerate`.** The text notes that each coplanar
vertex is met several times. *Suggestion:* note also that the loop runs over
the edge list while `lmev` appends to it, and say whether the new edges must
be visited.

**§14.5, Figures 14.7 and 14.8, the reclassification rules.** The four
arrangements of rule (b) are listed in the text. *Suggestion:* a truth
table with all combinations of (previous, ON, next) and the result, and for
each row a small figure. Our `reclassifyOnEdges` carries comments about
orientation adaptations that are not in the book; a table would have made
them checkable.

**"checkwideness and bisector … left as exercises".** Problem 14.1 and 13.6.
`checkWideness` was completed on 2026-04-18 with a signed angle about the
face normal. *Suggestion:* give the predicate. An unsigned angle cannot tell
a 90° sector from a 270° one; the sign needs the face normal, and the text
does not say so.

**§14.6, Figure 14.9, Program 14.7, `insertnulledges`.** The orientation
convention (null edges point from below to above) is stated here and used
silently in §14.7 and §14.8. *Suggestion:* state it as an invariant with a
check, because everything after depends on it.

**§14.7, Figure 14.10, Programs 14.8–14.10.** The text shows that
topological neighbourhood is not enough and sorts the null edges
lexicographically. *Suggestions:*

1. Say what the sort assumes: that the next null edge in the sorted order
   that is a topological neighbour is the correct partner. That holds on a
   plane. The text should then warn, here, that chapter 15 will lose the
   plane.
2. `join` (Program 14.10): "Care must be taken to make sure that potential
   internal loops end up in correct faces after a MEF. The procedure
   `laringmv` fills this need." This is the most understated sentence in the
   book. See §13 above.
3. `cut` detects the end of a section polygon when the same loop is on both
   sides of the null edge. *Suggestion:* add what the faces look like at
   that moment (one null face with two loops) as an invariant.

**§14.8, Programs 14.11–14.12, `classify`, `movefac`.** *Suggestion:* say
that the input solid is destroyed, and what is returned when the plane
misses the solid. Our splitter returns the input object itself in that case.

**Problem 14.2 (`cleanup`), Problem 14.3 (slicing).** `cleanup` rebuilds the
edge and vertex lists; ours also deletes vertices it considers orphaned.
*Suggestion:* specify `cleanup` as "rebuild lists, change nothing else".

### Chapter 15 — Boolean Set Operations

**§15.1, Figure 15.1.** The chapter opens with two extruded profiles whose
intersection has a coplanar bottom face, and says that handling it needs a
two-dimensional set operation, "a task that even alone is quite
challenging". Then it announces "a different path". *Suggestion:* come back
to Fig. 15.1 at the end of the chapter and run the algorithm on it. In our
implementation that figure (`MANT1988_15_1`) is answered by a fallback, and
its relatives (B-02, B-05) fail. We believe the different path does not
avoid the two-dimensional problem; it moves it into `sectoroverlap` and the
"on" reclassification, where it is solved vertex by vertex without the
global consistency a polygon boolean would give.

**§15.2, criteria 1–3 and 3′, Figures 15.2 and 15.3.** The pseudomanifold
discussion is clear. *Suggestions:*

1. Say which of the two representations of Fig. 15.3 the algorithm
   produces, and for which operation.
2. Give criterion 3′ as a validator. Our strict validator rejects results
   the kernel produces under 3′ (B-04, edge contact).
3. State the input precondition as a check too. We validate inputs and only
   warn.

**§15.3, Equations 15.1–15.3, Figures 15.4 and 15.5.** This is the
theoretical centre and it is sound. *Suggestions:*

1. Eq. 15.3 is printed as a table whose rows are hard to read; print it as
   three explicit rows (∪, ∩, \) by four columns, and add the row for
   `B \ A`.
2. Work Fig. 15.5 through the table: list the eight components of that
   example and show which survive for each operation.
3. Add the cases where the whole contact is "on": two solids sharing a face
   (B-04), a solid inside another with a face on its boundary (B-02 b). For
   these the vertex classifier emits no null edge at all, the program prints
   "no intersections found", and the equations are never applied.

**§15.4, Program 15.1.** `if (nedga == 0) { printf("no intersections
found"); return 0; }` *Suggestion:* this line needs a section. "No null
edges" has at least five meanings: disjoint; A inside B; B inside A; touching
at faces, edges or vertices; identical. Problem 15.1 covers two of them. We
wrote 1,341 lines (`_SetNonIntersectingClassifier`) and four preflights for
this one `if`, and B-02 (b), B-03 and B-04 show that they are not yet right.

**§15.5, steps 1–8 and Programs 15.2–15.4.**

1. "It is very helpful to make the further assumption that all faces are
   maximal." *Suggestion:* this is a precondition of the whole chapter, and
   `maximize_faces` is Problem 15.2 with the hint that a face may lie inside
   another. Put the procedure in the text, and say that it must run on the
   result too if results are to be operands.
2. "The case of e being coplanar with f can be ignored: all relevant
   intersections … will be located when comparing e against the
   (noncoplanar) neighbor faces of f." *Suggestion:* prove it or bound it.
   It assumes that the neighbour faces of `f` cross `e`. In B-03 the edge
   lies in the face and ends inside it; nothing is located.
3. Program 15.3 recurses through `processedge` after subdividing; Problem
   15.3 asks to remove the recursion by sorting the intersections along the
   edge. *Suggestion:* make the sorted version the program. The order of
   subdivision along an edge matters (`IntersectorParametricOrderingTest`).
4. `addsovf` and `addsovv` "should check against duplicate inputs".
   *Suggestion:* say what "duplicate" means when two vertices are
   coincident within tolerance but distinct, and add the weld step: after
   generate, vertices at the same point on the same solid must be one
   vertex. Without it, moon motifs had pinched loops (stage 4).
5. Program 15.4 as printed calls `addsovv(v, f, BvsA)` in the `cont == 1`
   branch; it should be `addsovf`.
6. *Suggestion:* add a step 9: split any loop that touches itself at a
   vertex. We added `splitSelfTouchingLoops`.

**§15.6.1, Figure 15.6, vertex/face classification.** "For space reasons, we
shall not elaborate the vertex-face classifier any further." Problem 15.4.
*Suggestion:* elaborate it. This classifier creates the null edges "as an
internal loop into the face intersected", and those internal loops are the
objects whose ownership goes wrong in B-01. Three things should be on the
page:

1. the null edge is a two-vertex ring of the pierced face;
2. when several such rings in one face are joined, they form a closed
   polygon inside the face, and the face must end with that polygon as a
   hole and the disk as a separate lamina;
3. which of the two is "in" and which is "out", for each operand and
   operation.

**§15.6.2, Programs 15.6–15.9, Figures 15.7 and 15.8.**

1. `nbrpreproc` (Program 15.8) splits wide sectors with `inside(he, bisec)`
   when the sector is degenerate. `inside` is not defined in the chapter.
2. `sectortest`: "If the cross product vanishes, the sectors are coplanar
   and considered to intersect if they overlap. … `sectoroverlap` … is not
   elaborated on here." Problem 15.5. *Suggestion:* elaborate it. We
   rewrote it three times (2026-04-17, 04-18, 05-15); stage 1 names an
   over-permissive `sectoroverlap` as the secondary root cause. The
   distinction between overlapping in an area, touching along a ray and
   touching at the apex must be explicit, and the result must be consistent
   for the two sectors on either side of a shared edge.
3. `sctrwithin` returns `t1 < 0 && t2 < 0` after two null-vector special
   cases. *Suggestion:* a figure for each special case.

**Reclassification, Figures 15.9–15.12, Program 15.10.**

1. The worked example of Fig. 15.9 is very good. *Suggestion:* give the
   same three tables (after the test, after the "on" sectors, after the
   neighbours) for ∩ and \.
2. Program 15.10 as printed has lost lines: the `if (nonopposite)` branch
   appears after its `else` body. Print it again.
3. "Space does not permit the inclusion of the procedure `srecledges`."
   *Suggestion:* include it. Figure 15.12 (four sectors around a common
   line, "mixed order") describes the test in a sentence; the implementation
   needs a robust angular sort around the edge, and the degenerate
   sub-cases (two of the four sectors coplanar) are the block-and-wedge of
   Fig. 15.2.

**Insertion of null edges, Figure 15.13, Programs 15.11–15.12.**

1. Program 15.11 pairs consecutive intersecting entries of `sectors`.
   *Suggestion:* state the requirement on the *order* of the array. The
   procedure "rearranges the sectors according to the side codes in order
   to give a consistent orientation to the null edges"; that rearrangement
   is not in the listing.
2. The "strut" case (one sector with several intersections) is described in
   five lines. *Suggestion:* a figure and the orientation rule for the
   strut. Stage 5's `STRUT_B` path and the flip
   (`flipNullEdgeOrientationForOpenSide`) are our reconstruction.
3. *Suggestion:* say when **not** to emit a null edge: a contact that only
   grazes (all sectors on one side after reclassification). An unmatched
   null edge can never be joined and stays as a loose end.

**§15.7, Programs 15.13–15.14, joining.** This is where the text and a
working implementation are furthest apart.

1. "The intersection polygons are in general not planar figures. Therefore,
   we cannot expect that the very same algorithm would work." The remedy
   given is that null edges come in pairs and both solids must join them in
   the same order. `ssortnulledges` is called and not defined.
   *Suggestion:* define the order. On a plane the lexicographic sort works;
   on a space curve it does not, and insertion order works only when the
   classifier happens to emit along the curve. **The order that works is
   the order along the intersection curve**, which can be computed before
   joining: two null-edge pairs are consecutive if they share a face of A
   *and* a face of B, and pairs on the same two faces are ordered along the
   line `nA × nB`. This is our `_SetIntersectionCurveBuilder`. It should be
   a section of this chapter: "15.7.1 The intersection curve".
2. `scanjoin` pushes one half of A's null edge with the opposite half of
   B's. *Suggestion:* state the consequence: a pair can close only against
   a predecessor whose pushed half is on the left in the shared face of A
   and on the right in the shared face of B. So the *direction* of traversal
   of each curve matters, and with several curves they must be advanced
   together, because completing one ring re-parents the struts of another
   (stage 5, phase 2).
3. `isloosea`, `islooseb`, `cuta`, `cutb` "are simple modifications … and
   we will not elaborate on them". *Suggestion:* at least give the loose-end
   invariant: at the end of the joining step both arrays are empty. We
   added it as a check (`SetOpConnectNoLooseInvariantTest`); before that, a
   failed join produced an empty or half-built result with no message.
4. The arrays `endsa[30]`, `endsb[30]` are fixed-size. A sentence on the
   bound would be honest.
5. *Suggestion:* state the postcondition of the joining step as the
   precondition of the next: for every closed intersection curve, each
   solid has one null face with exactly two loops. Program 15.15 indexes
   `floops->nextl` without checking; our fatal `IndexOutOfBounds` in B-02
   is that line.

**§15.8, Program 15.15, result generation.**

1. "On the basis of the consistent orientation of null edges, the new faces
   … are known to belong to the 'in'-components." *Suggestion:* this is the
   one place where an error in orientation anywhere upstream becomes a wrong
   solid, silently. Add a check: one point-in-solid test per moved
   component. The text is proud of "avoiding point-in-polyhedron tests
   completely"; one test per component at the end is cheap and would have
   turned our BLACK_FACES and EMPTY results into diagnosable errors.
2. The text says that for the difference a temporary solid is created and
   reversed; Program 15.15 calls `revert(b)` on the whole of B.
   *Suggestion:* make text and program agree, and note that after `revert`
   the face equations must be negated (Problem 15.6 mentions it in a hint).
3. `movefac` is called for face `i` of both lists in one loop, and a face
   may already have been moved with its component. *Suggestion:* say so.
4. "The parts of A and B not required in the final result will remain in
   the original solids." *Suggestion:* say plainly that the operands are
   destroyed, and show the copy-first wrapper. Problem 15.7 then follows
   naturally.
5. *Suggestion:* add the clean-up that a real result needs: remove null
   faces with fewer than three edges, re-maximise faces, triangulate or
   reject non-planar faces. We do all three after `loopglue`.

**§15.9, final remarks.** "The algorithm is slow … a practical algorithm
should definitely employ … enclosing boxes." *Suggestion:* show the box test
in Program 15.2; it is three lines. Our generate step still has none.

**Problems.**

- *15.1* (containment). The hint asks for vertex-in-solid. *Suggestion:* add
  that a vertex on the boundary decides nothing, and that the test must be
  made with a point known to be off the boundary (B-03).
- *15.2* (`maximize_faces`). See §15.5. Implemented; the nested-face case
  of the hint has no dedicated test.
- *15.3* (no recursion). Done here; should be the text's version.
- *15.4* (vertex/face classifier). See §15.6.1.
- *15.5* (`sectoroverlap`). See §15.6.2.
- *15.6* (`revert`). Done.
- *15.7* (both results at once). Not done. It becomes easy once operands
  are copied and components are identified explicitly.
- *15.8* (2D set operations from the 3D algorithm). *Suggestion:* reverse
  it. The 3D algorithm needs the 2D one for coplanar faces; present a 2D
  boolean on loops first (it is also the simplest setting in which to teach
  null edges and joining) and use it in §15.3.
- *15.9* (is `A \ B = A ∩ reverse(B)`?). A good question with no executable
  answer in the book; `AlgebraicIdentityRegressionTest` is ours.

**A missing section: testing.** The chapter has no test cases beyond its
figures. *Suggestion:* a final section with (a) a list of twenty
configurations of two blocks, by kind of contact, as a table: crossing
through edges only; ring inside a face; corner through a face; edge on a
face; vertex on a face, edge, vertex; faces coplanar and overlapping,
coincident, opposite; containment, tangent containment; (b) the volume
identity as the acceptance test; (c) the requirement that results be valid
operands. Our first oracle fuzz on exactly such a list found five families
of failure in code that passed every figure of the book.

### Chapter 16 — Undoing

**§16.7.1, error rollback.** The text observes that a set operation that
fails midway can undo its changes with the log. *Suggestion:* move this
forward to chapter 15 as a requirement: a boolean either returns a valid
result or leaves its operands unchanged. We have neither undo nor copies;
after a failure the operands are gone (C-01).

**Problem 16.1, `printproperties`.** Components and genus of a solid.
*Suggestion:* use it in the validator. Our `TopologySummary` (stage 10:
shells, adjusted Euler characteristic) is the answer, and it caught the
stepper-motor defect.

**Problem 16.2, `scopy`.** The right tool for non-destructive booleans; our
deep copy uses serialisation.

**Program 16.10, removal of edges.** Related to `maximize_faces`; a
cross-reference from Problem 15.2 would help.

### Chapter 17 — A User Interface, and Chapter 18 — Extensions

Outside the scope of this analysis. One remark on chapter 17: the debugger
and trace tools built in 2026 (operand partials, pipeline summary trace,
face focus) were essential to every stage. *Suggestion:* a section on
debugging views for intermediate states: draw null edges with their
orientation, loose ends, and the faces of one loop.

### Appendix B — Elements of Point-Set Topology

Regularised operations are defined here. *Suggestion:* add one line each
for the consequences used in chapter 15: two solids sharing a face have a
connected regularised union (B-04); `A ∩* B` of solids that only touch is
empty.

### What the text would need, in one list

1. Intrusive links, the empty-loop half-edge and `vedge` maintenance stated
   as invariants (Ch. 10–11).
2. Preconditions checked and failed operators without side effects
   (Ch. 11).
3. A validator at two levels, used as pre- and postcondition (Ch. 6, 14,
   15).
4. Tolerances that scale, more than one of them, and the rule about planes
   and faces (Ch. 13).
5. `checkwideness`, `laringmv`, `sectoroverlap`, `srecledges` and the
   vertex/face classifier in the text, not in the exercises (Ch. 13–15).
6. The "no intersection" case as a full case analysis (Ch. 15.4).
7. The intersection curve as an explicit object that orders the joining
   step (Ch. 15.7).
8. Invariants between phases: no loose ends after joining; one two-loop
   null face per curve and solid; one inside test per moved component
   (Ch. 15.7–15.8).
9. Coplanar faces through a two-dimensional boolean (Ch. 15.1, 15.3).
10. A test section with a configuration table and the volume identity
    (Ch. 13.3, 14, 15).

Items 1–8 are what 2026 added, in whole or in part. Items 9 and 10 are what
this analysis says is still missing.

---

## Appendix — Reproductions

All with `OrientedBox(sx, sy, sz, transform)` from the oracle helper; boxes
are centred at the origin before the transform.

| Id | A | B | Operation | Observed | Expected volume |
|---|---|---|---|---|---:|
| B-01 | 2×2×2 | 0.5×0.5×1.5 at (0.1, 0.2, 1.0) | A ∩ B | strict fail, F=7 E=16 V=12 | 0.1875 |
| B-01 | 4×4×1 at (0.05, 0.03, 1.2) | unit cube, diagonal along z, then 0.37 rad about z | A ∩ B | strict fail, F=5 E=9 V=7 | 0.003963 |
| B-02 | 2×2×1 | unit cube at (1.0, 0.1, 0), 30° about z | A \ B | 4.0, silent | 3.5 |
| B-02 | same | same | A ∩ B | fatal IndexOutOfBounds | 0.5 |
| B-02 | 2×2×1 | 0.5×0.5×1 at (0.1, 0.1, 0), 30° about z | A ∪ B | 4.25, strict fail | 4.0 |
| B-03 | 2×2×2 | unit cube at (1.0, 0, 0.2), 45° about z | A ∪ B | 8.0, silent | 8.5 |
| B-03 | same | same | B \ A | empty, silent | 0.5 |
| B-04 | unit cube | unit cube at (1, 0, 0) | A ∪ B | two shells, strict fail | 2.0 |
| B-05 | 2×2×1 | unit cube at (1.0, 0.1, 0); both rotated 0.5 rad about (0.3, 1.0, 0.2) | A \ B | 4.0, silent | 3.5 |
| S-01 | 2×2×2 | plane normal (1,1,1) through (1,1,−1) | split | two empty solids, input emptied | 8.0 total |
