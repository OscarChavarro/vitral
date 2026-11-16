# Stage 11 — Robustness and stability hardening

**Date:** 2026-11-16 · **Port:** Java only (the other ports follow later)
· **Baseline:** commit `0ab960a9`.

Decision: robustness and stability are closed before any work on
efficiency (`performance.md`). This plan covers the six items chosen for
that. Efficiency gains that fall out of them are welcome but are not the
goal.

## Rules

- No decision of the kernel changes without a test that shows the old
  behaviour was wrong.
- After every phase: `gradle :base:test` with 0 failures and 0 skips,
  the slow gates green, and the ratchets of
  `BooleanCompositeOracleFuzzGateTest` equal or higher.
- The Kurlander bowl keeps F=1133, E=3396, V=2186 and volume
  0.38391604531097 unless a phase states why it changes.
- New tests are written to fail first when they describe a defect, and
  are never disabled.

## Phases

| Phase | Item | Outcome wanted |
|---|---|---|
| A | Array-backed container | `CircularArrayList` replaces `CircularDoubleLinkedList` in the kernel. Reading never writes, so concurrent readers do not compete |
| B | Tests for the uncovered areas | Volume identities, split with an oracle, Euler operators with inverses, determinism, near-degenerate inputs |
| C | Validation does not repair | Validators only read. Repair is a separate, explicit flow |
| D | Interior overlap without sampling | The "do the interiors overlap" decision is exact for polyhedra, not a grid of probes |
| E | `STACKED_BLOCKS` through the pipeline | Intersection and difference of solids that share part of a face are answered by the Chapter 15 pipeline; the partial-coplanar preflight is removed |
| F | Connect on non-convex faces | The composite oracle ratchets rise; the ring rescue is removed when it no longer fires |

### Phase A — Array-backed container

Problem: `CircularDoubleLinkedList.get(int)` writes an access memo, and
`next()`/`previous()` move a shared cursor. Two threads that only read
the same list race. Nested index loops also cost O(n) per read.

1. `CircularArrayList<E>` in `common/dataStructures`, with the same
   public operations and the same documented preconditions (L-01 to
   L-03). Elements in one array; `get` is a plain read.
2. Elements may remember their position (`ArrayLinkable`): owner and
   index. `nextOf`, `previousOf`, `contains` and `locateWindowAtElem`
   are O(1) for them and fall back to a scan otherwise, so correctness
   never depends on the handle.
3. Faces, loops, edges, vertices and half-edges implement the handle.
4. The cursor (`next`, `previous`, `getWindow`) stays for single-thread
   editing code and is documented as not safe for concurrent readers.
5. Port the container test; add tests for the handle after every kind
   of edit and for a reader that interleaves two index loops.
6. Remove `CircularDoubleLinkedList`, its node class and `Linkable` once
   nothing uses them.

Exit: suite and gates unchanged; same topology summaries everywhere
(the order of elements is the same as before).

### Phase B — Tests for the uncovered areas

| Test | Checks |
|---|---|
| `BooleanVolumeIdentityFuzzTest` | \|A∪B\| = \|A\|+\|B\|−\|A∩B\|, \|A−B\| = \|A\|−\|A∩B\|, \|B−A\| = \|B\|−\|A∩B\| on prisms of N sides (convex and star-shaped), rotated; needs no oracle |
| `SplitterVolumeOracleFuzzTest` | vol(above)+vol(below) = vol(solid), every piece strict-valid, each piece on its side, for random planes on non-convex solids and on solids with through holes |
| `EulerOperatorInverseTest` | Each operator followed by its inverse restores the topology summary; the Euler–Poincaré formula holds after every step of random sequences |
| `SetOpDeterminismTest` | The same operation twice gives the same summary, volume and vertex positions |
| `NearDegenerateBooleanTest` | Coplanar and touching configurations displaced by 0.1, 1, 10 and 100 tolerances: a valid result with the volume the oracle gives, or a typed rejection; never a wrong solid |

Failures found here become entries of this plan, not disabled tests.

### Phase C — Validation does not repair

1. List every write done by a `validate*` method (known:
   `remakeEmanatingHalfedgesReferences` inside
   `validateTopologicalIntegrity`).
2. Validators become read-only. A test runs every validator on a deep
   copy and compares the solid before and after, field by field.
3. Repairs move to `PolyhedralBoundedSolidRepair` (explicit calls, each
   reporting what it changed).
4. Call sites that relied on the hidden repair call the repair first, in
   the open; then check whether the repair is needed at all (if a kernel
   stage leaves stale references, that is a defect of the stage).

### Phase D — Interior overlap without sampling

Today: 27 probes in the common bounding box, then a grid from the vertex
coordinates. A thin overlap between grid lines is missed.

For two valid polyhedra the interiors overlap exactly when one of these
holds:

1. an edge of one crosses the interior of a face of the other (already
   computed: `hasProperEdgeFaceIntersection`);
2. a point just inside one solid, next to one of its faces, is inside
   the other (covers containment and coplanar faces with the same
   orientation);
3. the boundaries do not cross and one solid contains a vertex of the
   other.

The witness of case 2 is the face witness moved against the normal by a
small multiple of the tolerance.

Steps: tests first (thin overlaps the grid misses, built on purpose);
then the new predicate; then compare old and new on every operand pair
of the fuzz families and explain each difference.

### Phase E — `STACKED_BLOCKS` through the pipeline

1. Record what the pipeline answers today with the preflight off, for ∩,
   A−B and B−A of `STACKED_BLOCKS` and of the coplanar fuzz families.
2. Fix what fails (expected: coplanar faces of opposite orientation with
   partial overlap in classify, and the empty/lamina result in finish).
3. Keep the lamina policy: a lamina is a valid result, a collapsed one
   is rejected.
4. Remove `runPartialCoplanarFaceAreaCase` and its helpers.

### Phase F — Connect on non-convex faces

Start: rotated 47/72, chains 23/36, coplanar bottoms 46/72.

1. Catalogue the failures by cause with the pipeline trace (known
   causes: loose pairs joined across the wrong chord; strut rings on a
   dividing chord).
2. Reduce each cause to the smallest pair of operands that shows it and
   add it as a regression test.
3. Fix in the connect stage, following [MANT1988] 15.7 and the
   intersection curve ordering. No shape-specific branch.
4. Raise the ratchets after each fix. Remove the ring rescue when its
   counter is zero on every family.

## Status

| Phase | State |
|---|---|
| A | done |
| B | done |
| C | done |
| D | done |
| E | done |
| F | done |

## Results

### Phase A (done)

- `CircularArrayList` and `ArrayLinkable` replace
  `CircularDoubleLinkedList`, its node class and `Linkable`, which were
  removed. Faces, loops, edges, vertices and half-edges carry the handle,
  so `contains`, `nextOf` and `previousOf` are O(1) for all of them.
- Same order of elements, same preconditions: every topology summary and
  every ratchet is unchanged.
- `CircularArrayListTest`: the eight cases of the old container, plus the
  handles after every kind of edit, the cursor, and eight threads reading
  one list at once.
- Side effect: the Kurlander bowl goes from 17.6 s to 5.2 s, since `get`
  no longer walks the list. The two 514-face cylinders stay at 5 s; their
  cost is in connect (`performance.md` P-04).

### Phase B (done)

| Test | Result |
|---|---|
| `BooleanVolumeIdentityFuzzGateTest` | Regular prisms 16 / 16. Star and regular 13 / 16, two stars 11 / 16. No broken identity: the 8 failures are typed rejections (coincident vertices in the result), the non-convex connect defect of Phase F |
| `SplitterGeneralPlaneOracleFuzzTest` | 240 / 240 on star prisms, prisms of up to 32 sides and L, U, step and frame shapes, with planes in general position |
| `EulerOperatorSequenceFuzzTest` | 40 random `lmev`/`lmef` steps and their undo on every corpus solid and a star prism: valid, same Euler–Poincaré characteristic and same volume after every step |
| `SetOpDeterminismTest` | The 36 corpus operations and 4 on prisms repeat bit for bit |
| `NearDegenerateBooleanFuzzGateTest` | 432 operations on six kinds of contact displaced by −100 to 100 tolerances, as built and rotated: 363 answered, 69 rejected, 0 wrong |

Defects found by Phase B:

1. **Silent loss of a piece (fixed).** A box crossing another with one
   face 0.1 tolerances off coplanar, rotated: B − A returned one of its
   two pieces, strict-valid, no error. Generate found 7 of the 8 crossing
   points, the intersection curve had an open chain, connect left loose
   null edges and finish went on with the one closed cycle. `setOp` now
   throws `SolidOperationFailedException` when connect leaves loose ends
   on a curve that is not closed ([MANT1988] Program 15.14).
2. **Loose ends on a closed curve (closed in Phase F).** At first only
   loose ends on an open curve were rejected, because three results with
   loose ends on closed curves seemed right. After the `lmekr` fix of
   Phase F none is: every operation that ends connect with a loose end is
   rejected, as [MANT1988] Program 15.14 requires.
3. **69 rejections in the tolerance band (open, Phases E and F).** Most
   are at 0.1 and 1 tolerances from coplanar: the solids are neither
   clearly touching nor clearly crossing.
4. **The half-space oracle is wrong for rotated boxes that only touch**
   (it returns a negative intersection volume). The near-degenerate fuzz
   takes its expected volumes from the pair before the rotation.
5. **`CHAINS` has one wrong volume** in the composite oracle, already
   present before this stage and not gated as a hard failure. Fixed in
   Phase F (whole shells), and gated since.

### Phase C (done)

- `_TopologicalIntegrityStrategy` no longer rebuilds the vertex
  back-pointers. No test and no kernel stage depended on that repair.
- `validateBooleanInputs` no longer welds coincident vertices; it reports
  them. The set operator welds its own copies of the operands, in the
  open, in the preparation step where it already merges coplanar faces.
- `PolyhedralBoundedSolidRepair.remakeEmanatingHalfEdgeReferences`
  returns a report (vertices relinked, vertices removed). The cleanup of
  `PolyhedralBoundedSolidTopologyEditing` calls it.
- `ValidationDoesNotModifyTest` compares the whole structure before and
  after every validator, on valid solids, on a boolean result, on a
  broken back-pointer and on coincident vertices.
- Still written by validation: the validity flag of the solid
  (`setValidationState`). It is the result of the validation, not a
  repair, but it is a write on a shared solid; it goes with the edit
  counter of `performance.md` P-06.

### Phase D (done)

- `InteriorOverlapPredicateTest` first: box pairs with coplanar faces and
  overlaps down to 10⁻³ of their size, as built and under 12 rigid
  motions, and 60 pairs of prisms with coplanar ends. The sampling test
  answered every box pair right and missed 2 of the 60 prism pairs, one of
  them with an overlap area of 0.005.
- `_PolyhedralBoundedSolidInteriorOverlap` decides from the boundaries.
  Each face of one solid is cut by what the other solid leaves on its
  plane (crossing segments and edges lying in it); a point beside every
  piece of every cut visits every cell of the face; a cell inside the
  other solid, or lying on it, is confirmed by a point just under the
  face that is inside both. No grid, no probes.
- The grid code is removed from
  `_PolyhedralBoundedSolidSetNonIntersectingClassifier` (about 190
  lines).
- The decision has a stated band: an overlap thinner than about 5
  tolerances is a contact. A first version without the band sent pairs
  that enter each other by one tolerance to the pipeline, which rejected
  them (three more rejections in the near-degenerate gate); with the band
  the gate is back to its counts.
- All 60 prism pairs and all box pairs are answered right. Suite 1145
  tests, 0 failures; slow gates 8 of 8.

### Phase E (done)

- Measured first, with the partial-coplanar preflight switched off: the
  pipeline already answered `STACKED_BLOCKS`. Generate and classify find
  no null edge, and the "no intersections found" case of [MANT1988] 15.4
  returns the operands for the two differences. Only the intersection
  differed: empty instead of the lamina.
- The lamina policy moved into that case
  (`appendContactLaminae` in `runSetOpNoIntersectionCase`), and
  `runPartialCoplanarFaceAreaCase` is removed. `STACKED_BLOCKS` ∩, A−B
  and B−A now report `no-intersection-after-classify`, with the same
  results as before (`SetOpResultPathTest`).
- Two things the removed preflight was doing without saying so, found by
  the near-degenerate gate when it went away (9 answers lost at first):
  1. It answered contacts inside the tolerance band from operands that
     generate had not edited. The "no intersections found" case now also
     answers from copies of the operands taken before generate. Taken
     from the edited operands, the results carried extra vertices: strict
     validation rejected them, and the lamina of `STACKED_BLOCKS` came
     out with a last-bit error in one coordinate.
  2. It caught face contacts before the touching preflight, which
     refused them. The touching preflight now takes solids that share
     part of a face and nothing else; a union along a face still goes to
     the pipeline to be glued.
- Near-degenerate gate: 367 of 432 answered (363 before), 0 wrong.
- Result paths left: `normal-pipeline`, `no-intersection-after-classify`,
  `touching-only-preflight`, `containment-only-preflight` and
  `geometric-identity-preflight`. The three preflights are the general
  decisions of [MANT1988] 15.4 and problem 15.1 taken before generate;
  none is written for one shape.

### Phase F (done)

| Family | Before the stage | Now |
|---|---:|---:|
| Non-convex, axis-aligned | 72 / 72, 3 of them rejections counted as correct | 72 / 72, all answered |
| Non-convex, rotated (also at 10⁻³ and 10³) | 47 / 72 | 72 / 72 |
| Chains of three operations | 23 / 36, one wrong volume | 36 / 36 |
| Non-convex, coplanar bottoms | 46 / 72 | 72 / 72 |
| Star prism and regular prism | 13 / 16 | 16 / 16 |
| Two star prisms | 11 / 16 | 16 / 16 |
| Near-degenerate contacts | 363 of 432 answered, 2 wrong before the gate | 371 answered, 61 rejected, 0 wrong |

Suite: 1149 tests, 0 failures, 0 skipped. Slow gates: 8 of 8. Kurlander
bowl: same solid (F=1133, volume 0.38391604531097).

Causes found and fixed, in the order they were found:

1. **`scanjoin` joined any loose end with the same face and role.** A face
   that is not convex can be crossed several times by one face of the
   other solid; the crossings lie on one line and all pass that test, so
   an end was joined across the notch of the face. Only the end left by a
   neighbour along the intersection curve is accepted now.
2. **`lmekr` lost the outer loop** (an Euler operator defect). It removes
   the loop of its second half-edge. When that was the outer loop of the
   face, the merged loop stayed in the place of the ring and another ring
   of the face became "the outer loop". Test:
   `PolyhedralBoundedSolidEulerOperatorsTest`, two rings.
3. **Whole shells were dropped.** Finish moves to the result the parts
   connected to a cut. A shell that no cut reaches (one piece of a
   two-piece solid, or the wall of a cavity) was left behind without an
   error. Each such shell is now classified against the other solid and
   kept on the same terms as the cut parts. Test: `SetOpWholeShellTest`.
4. **Sides of a cut taken by convention only.** When the faces on both
   sides of a cut say the opposite of the convention, with no face
   disagreeing, finish exchanges the two.
5. **Rings were redistributed on solid A only for intersections.** Connect
   called `join` on A with ring moving switched off unless the operation
   was an intersection. After a division of a face of A, a ring that lay
   in the new half stayed in the old one. This was the root of two things
   at once:
   - the ring rescue: 266 of its 284 firings over the fuzz families moved
     a ring out of a face that did not contain it into the one that did;
   - the "ring in the wrong piece" failures: a hole already cut by another
     curve is not a pending strut, so the rescue never moved it back.

   Rings are now redistributed on both solids for every operation. The
   rescue fires 0 times on the fuzz families and on the 40 Kurlander
   motifs (52 before), and is removed, with its ratchet. `join` no longer
   has a ring-moving switch.
6. **Contact nodes kept the curves from being ordered.** A vertex of one
   solid lying in a coplanar face of the other gives a null edge with no
   neighbour on the intersection curve; an edge lying in such a face gives
   an open chain of two. One of them made connect give up the curve order
   for the whole operation and fall back to the order of emission, which
   joins a face crossed twice along one line across its hole. These nodes
   are removed before connect (`dropIsolatedNodes`). An open chain is
   removed only when every node of it is a vertex in a coplanar face; any
   other open chain is a curve with a crossing missing, and the operation
   is rejected.
7. **Loose ends after connect are always an error** ([MANT1988] Program
   15.14). With the fixes above no correct answer has them.

Tried and withdrawn, because no family changed: keeping rings out of
faces without area; redistributing rings after the first division of
`join`.

## After the stage: the tolerance band and the slow cylinders

### The tolerance band (done)

Near-degenerate gate: 422 of 432 operations answered, 0 wrong. The 10
left are the unions of solids in edge contact, rejected by decision
(B-04). Before: 371 answered.

1. **One decision on whether two faces coincide.**
   `_PolyhedralBoundedSolidSetOperandAligner` runs in the preparation of
   the operands, on the copies the operation owns. A face of B parallel
   to a face of A, next to it and closer than two tolerances is put on
   its plane by translating B as a rigid body. After that every stage
   sees exact coincidence, which it handles; before, generate, the
   classifiers and the contact decision each decided on their own and
   disagreed inside the band (open curves, lost solids, results that
   failed strict validation).
   - A translation, not a move of single vertices: every face of B stays
     planar.
   - Two gaps along two directions are both closed. Two different gaps
     along one direction cannot be: nothing moves and the operands go on
     as they are.
   - The displacement is at most two tolerances per direction. Only B is
     moved, so A ∪ B and B ∪ A can differ by it.
   - The "result cannot be empty" guard of `setOp` looks for its
     witnesses in operands aligned the same way.
2. **Tangent containment and the difference.** B inside A with a face on
   a face of A: A − B is A with a pocket open to the outside. The
   containment decision answered it with a cavity, which strict
   validation rejected, at exact contact too. Those pairs now go to the
   pipeline, which builds the pocket.

Tests: `SetOperandAlignerTest`; every row of
`NearDegenerateBooleanFuzzGateTest` at 36 of 36 except the two of
EDGE_CONTACT (31).

### The slow cylinders (done)

| Workload | Before | Now |
|---|---:|---:|
| Two 514-face cylinders, union / intersection / difference | 6.2 / 5.4 / 5.8 s | 2.3 / 1.9 / 1.9 s |
| Two 258-face cylinders | 1.15 / 1.0 / 1.1 s | 0.41 / 0.32 / 0.35 s |
| Kurlander bowl, 82 booleans | 5.7 s | 4.9 s |

Same results to the last digit of the volumes; every fuzz family
unchanged. Four changes, none of which alters a decision:

1. The ring side test prepares the two outer loops, their plane and the
   tolerance once per division, not once per ring
   (`_RingSideTest`, `performance.md` P-04).
2. Its clearance is computed on plain numbers, without allocating a
   vector per segment.
3. Finish classifies each face next to a cut once, not once per edge it
   shares with the cut, and computes the face planes of the two solids
   once.
4. The planarity check looked at every pair of points of a face to keep
   the plane of the last one that gives a plane: cubic in the size of
   the face. It now starts from the last pair and stops at the first
   that gives a plane, which is the same plane.

## Open

1. **Efficiency** (`performance.md`): the next costs on the cylinders
   are the index updates of `CircularArrayList` when a long loop is
   edited and the point classifications of finish.
2. **Concurrency**: the global state behind the lock.
3. **The other ports.** Everything here is in the Java port only.
