# PolyhedralBoundedSolid kernel: performance

**Date:** 2026-11-16 · **Port measured:** Java, commit `29619797`
("PolyhedralBoundedSolid revisited, part 4") · **Machine:** 72 cores,
196 GB, OpenJDK 17.0.8.

This document answers three questions:

1. Where does the time of a boolean operation go?
2. Can the kernel be made faster, single-threaded or with several
   threads, without lowering the robustness reached in
   `Fable5.2Analysis.md` Section 10?
3. What does it take to remove the public attributes of the kernel
   classes?

Every improvement found is listed with an identifier (P = single thread,
MT = threads, E = encapsulation, T = tests). Nothing in this document is
implemented except P-01.

---

## 1. Summary

- The pipeline of [MANT1988] Chapter 15 (generate, classify, connect,
  finish) is not what makes booleans slow. On the Kurlander bowl it takes
  under 5 % of the time. The time goes to checks around it: one preflight,
  the validators and the cleanup.
- One change of two lines of ordering (P-01, already committed in part 4)
  took the whole bowl, 82 booleans, from 138 s to 17.6 s with the same
  result.
- What remains is dominated by quadratic walks over linked lists and by
  face planes that are recomputed on every call. Five local fixes
  (P-02 to P-06) address the routines that hold most of the remaining
  samples. They change no decision of the algorithm.
- Time grows almost with the cube of the face count: 5 s for a union of
  two 514-face cylinders. The cause there is one routine of connect
  (P-04), not the size of the problem.
- Threads: today no two operations can run at once (one global lock).
  Three kinds of global state keep the lock in place. Removing them
  (MT-1) lets independent booleans run in parallel, which helps CSG trees
  but not chains. Inside one boolean only the read-only work can be
  split among threads (MT-2), and after the single-thread fixes that
  work is mostly validation. The stages that edit topology must stay
  sequential.
- Recommendation: do P-02 to P-06 first, then MT-1, then E, then MT-2.
  The single-thread fixes are worth more than the threads and are
  safer.

---

## 2. Method

- Sampling profiler: Java Flight Recorder, `settings=profile`, main thread
  only. Percentages are shares of samples; "inclusive" counts a method
  and everything it calls.
- Workload A, few intersections on a growing solid: the whole Kurlander
  bowl, `CsgKurlanderBowlFixture.create()`. 82 booleans; the result has
  1133 faces and genus 40.
- Workload B, many intersections: two prisms of N sides (cylinders of
  radius 1 and height 1), the second one rotated 0.37 rad about a skew
  axis and translated, so that every side face of one crosses the other.
  Union, intersection and difference, best of three runs after warm-up.
- Every run keeps the default strict validation, and every result was
  strict-valid. For workload B the volumes satisfy
  |A∪B| = |A| + |B| − |A∩B| and |A−B| = |A| − |A∩B| to 10⁻¹⁴ up to
  N = 256.
- Not measured: `split` and the Euler operators alone, memory beyond the
  peak resident size (1.4 GB for the bowl with a 16 GB heap limit), and
  the other ports.

---

## 3. Measurements

### 3.1 Workload A: Kurlander bowl

| Version | Wall time | Result |
|---|---:|---|
| Part 3 (`124b4d04`) | 138 s | F=1133, E=3396, V=2186, volume 0.38391604531097 |
| Part 4 (`29619797`, with P-01) | 17.6 s | identical |

Where the 17.6 s go (inclusive shares):

| Part of `setOp` | Share |
|---|---:|
| Result completion: post-process, non-manifold check, strict validation | 48 % |
| Finish stage (almost all of it `cleanup`) | 25 % |
| Operand validation (`validateBooleanInputs`) | 16 % |
| Preflights | 3 % |
| Connect | 2 % |
| Generate | 1 % |

The same samples by kind of work, across those parts:

| Work | Share |
|---|---:|
| Intermediate validation (all callers) | 28 % |
| Strict validation | 16 % |
| Operand validation | 16 % |
| `cleanup` | 23 % |
| `findNonManifoldContact` | 14 % |

By routine (self time):

| Routine | Share | Called from |
|---|---:|---|
| `CircularDoubleLinkedList.get(int)` | 48 % | 19 % edge counting in `validateTopologicalIntegrity`; 13 % `searchForEdge`/`searchForVertex` in `cleanup`; 8 % `findNonManifoldContact`; the rest in validators |
| `validateTopologicalIntegrity` | 14 % | its own inner loop over edges |
| `findNonManifoldContact` | 6 % | all vertex pairs, all edge pairs |
| `validateFacePointsAreCoplanar` | 6 % | planarity |

### 3.2 Workload B: two N-sided cylinders

| N | Faces per operand | Union | Intersection | A − B |
|---:|---:|---:|---:|---:|
| 16 | 18 | 7.9 ms | 5.3 ms | 6.6 ms |
| 32 | 34 | 22.9 ms | 6.6 ms | 8.5 ms |
| 64 | 66 | 51.7 ms | 22.2 ms | 27.6 ms |
| 128 | 130 | 152 ms | 100 ms | 121 ms |
| 256 | 258 | 792 ms | 585 ms | 672 ms |
| 512 | 514 | 5075 ms | 4300 ms | 4667 ms |

Doubling N multiplies the time by 5 to 7 at the top of the table: the
growth is between N² and N³. Turning strict validation off saves 5 to
10 %.

Profile at N = 512 (inclusive shares):

| Part | Share |
|---|---:|
| Connect, almost all in `join` → `clearRingSide` | 60 % |
| — `getContainingPlane` (Newell plane recomputed) | 39 % |
| Validation (intermediate, strict, inputs) | 18 % |
| Building the two cylinders (not part of the boolean) | 7 % |
| Finish and post-process | 10 % |
| Preflights | 4 % |
| Generate | 3 % |

`clearRingSide` is the ring placement added in part 3
(`Fable5.2Analysis.md` 10.4, item 3). It is correct, but it recomputes
the plane and the tolerance context of both faces for every ring of
every join. The end caps of the cylinders have hundreds of vertices and
collect one ring per crossing.

---

## 4. Single-thread improvements

Each entry gives the evidence, the change and why it does not touch
robustness. "Same decisions" means the kernel takes exactly the same
branches, so every gate must give identical counts and topology
summaries.

### Done

**P-01 — Cheap predicates first in the preflights.** Done in part 4.
`runPartialCoplanarFaceAreaCase` and `runTouchingOnlyPreflightCase` asked
`hasInteriorOverlap()` first. That predicate samples a grid built from
the vertex coordinates of both solids and classifies each sample against
both solids with three rays each. The other predicates of the same
condition (edge/face crossings, coplanar contact) are cheaper and, for
operands that really intersect, already decide. All are pure functions
of the unchanged operands and are memoized, so the order does not change
the answer. Bowl: 138 s → 17.6 s.

### First priority: quadratic walks

**P-02 — Membership and counting without list scans.**
*Evidence:* 32 % of workload A.
- `validateTopologicalIntegrity` finds the index of each half-edge's
  edge by scanning the edge list: O(H·E).
- `cleanup` calls `searchForEdge` and `searchForVertex`, which scan the
  whole list for every half-edge: O(H·E + H·V).

*Change:* count in an identity map (or a scratch counter on the edge);
replace the two searches by `contains`, which is already O(1) for
elements that know their node (`Linkable`).
*Robustness:* same decisions. *Effort:* small.

**P-03 — `findNonManifoldContact` with a spatial hash.**
*Evidence:* 14 % of workload A. It compares all vertex pairs and all
edge pairs, reading both through `get(i)`.
*Change:* hash the vertices on a grid of cell size equal to the
tolerance and compare each vertex with the 27 neighbouring cells; key
the edges by their pair of end vertices.
*Robustness:* the comparison inside a cell is the same distance test.
The grid must look at neighbouring cells so that two points on either
side of a cell border are still compared. Keep the O(n²) version as a
test oracle (T-03).
*Effort:* small.

**P-04 — Ring placement in `join` without recomputing planes.**
*Evidence:* 59 % of workload B at N = 512.
*Change:* compute once per `join` the plane, the tolerance and the 2D
projection of the outer loops of the two faces, and reuse them for all
rings. Reject rings by bounding box before the parity test.
*Robustness:* same decisions; the values are the ones computed today,
computed once. No topology changes between two rings unless a ring
moves, and a moved ring does not change an outer loop.
*Effort:* small.

**P-05 — Snapshot arrays instead of `get(i)` in read-only passes.**
*Evidence:* `get(int)` is 48 % of the self time of workload A and 21 %
of workload B. The list keeps one access memo, so a single ascending
loop is linear. Two nested loops over the same list (pairs of vertices,
pairs of faces) reset the memo on every inner pass and each read costs
O(n). There are 665 `.get(` call sites in the kernel packages.
*Change:* read-only passes (validators, preflights, non-manifold check)
copy the list to an array once, as `PolyhedralBoundedSolidFaceGeometry`
already does for planes. Passes that edit the list keep using it.
*Robustness:* same decisions. It also removes a hazard for threads: see
MT-2. *Effort:* medium, mechanical; do the profiled sites first.

**P-06 — Validate once.**
*Evidence:* validation is 60 % of workload A after P-01: operand
validation 16 %, two intermediate passes 28 %, strict 16 %. In a chain
of booleans the operand of one operation is the strict-validated result
of the previous one, validated again.
*Change:*
- Run the intermediate validation once after the last edit of
  `postProcessResult`, not before and after `maximizeFaces`.
- Mark a solid as validated at a given edit count; skip operand
  validation when the mark is current. This needs the edit counter of
  P-07.

*Robustness:* no check is dropped for a solid that changed. The risk is
an edit that does not advance the counter, which is why this depends on
E-2 (no public attributes).
*Effort:* small for the first part, medium for the second.

### Second priority: repeated geometry

**P-07 — Cached face planes with an edit counter.**
*Evidence:* `getContainingPlane` is 39 % inclusive in workload B, and
`NumericPolicy.forFace` 12 %. Faces deliberately store no plane, because
stored planes went stale; every call runs Newell over the loop.
*Change:* a counter per solid, advanced by every Euler operator and
every vertex move; each face keeps its plane with the counter value it
was computed at. A scoped cache per read-only pass is the conservative
variant and is already used by generate and the preflights.
*Robustness:* correct only if every edit advances the counter. With
public attributes any class can relink a loop without doing so. Do it
after E-2, or only in the scoped form.
*Effort:* medium.

**P-08 — Interior-overlap witness without the vertex grid.**
*Evidence:* this was the 87 % of the bowl before P-01. It still runs
when the operands touch without crossing, and its cost is the product of
the distinct vertex coordinates on three axes times the faces of both
solids.
*Change:* candidate witnesses from the geometry: a point just inside
each face of A (the face witness moved against the normal by a multiple
of the tolerance) tested against B, and the reverse. O(F) candidates.
*Robustness:* this changes a decision procedure, unlike the entries
above. The grid is itself a sampling heuristic and can miss a thin
overlap. Compare both on the fuzz families before replacing (T-04).
*Effort:* medium.

**P-09 — Point-in-solid classification.** `classifyPointAgainstSolid`
casts three rays against every face and allocates two `Ray` objects per
face. Use the face boxes to reject, allocate nothing in the loop, and
cast the second and third ray only when the first is ambiguous.
*Robustness:* the vote of three rays is a decision; keeping it when the
first ray passes within tolerance of an edge preserves the behaviour in
the ambiguous cases only. Needs T-04. *Effort:* small.

**P-10 — Strings built for traces that are off.** `summarizeHalfEdge`,
`summarizeNullEdge` and `Vector3Dd.toString` appear in the connect
profile (about 1 %) with tracing disabled. Build the text only when the
flag is set. *Effort:* trivial.

### Third priority: larger models

**P-11 — Spatial index in generate.** Generate tests every edge of one
solid against every face of the other, with box rejection. It is 1 to
3 % today. With the fixes above it will be the next quadratic term near
10⁴ faces. The toolkit already has a bounding volume hierarchy
(`BinnedSahHierarchy`). *Effort:* medium.

**P-12 — Solid construction.** Building the two 512-sided cylinders took
7 % of the samples of workload B. The high-level operators find faces
and vertices by identifier with a list scan, so a sweep of N sides is
O(N²). An identifier index per solid removes it. *Effort:* medium.

**P-13 — N-ary difference.** The bowl subtracts 80 disjoint motifs one
by one. Each step copies, validates and cleans a solid that keeps
growing, so the total cost grows with the square of the number of
motifs. An operation that takes a list of disjoint tools could validate
the target once and run the pipeline per tool on the faces near it.
*Robustness:* this is a new operation with its own contract, not an
optimization of an existing one. *Effort:* large.

**P-14 — `HalfEdge.next()` through the list.** 12 % of the self time of
workload B is `CircularDoubleLinkedList.nodeOf`, reached from `next()`.
The lookup is O(1) but checks ownership every time. Intrusive next and
previous links in the half-edge remove it. `Fable5.2Analysis.md` lists
this as pending for the other ports. *Effort:* medium.

### Expected effect

These are estimates from the sample shares, not measurements.

| After | Bowl (17.6 s now) | Two 514-face cylinders (5 s now) |
|---|---|---|
| P-02 + P-03 | about 10 s | no visible change |
| + P-04 | no visible change | about 2 s |
| + P-05 + P-06 | 4 to 6 s | 1 to 1.5 s |
| + P-07 | 3 to 5 s | under 1 s |

---

## 5. Threads

### 5.1 State today

- `PolyhedralBoundedSolidModeler` takes one lock (`KERNEL_LOCK`) around
  `setOp`, `setOpDestructive` and `split`. Calls from several threads are
  safe and run one at a time.
- Diagnostics are per thread (`ThreadLocal`): result path, connector
  report, sector-overlap trace, triangulated face count.
- The test suite gets its parallelism from separate JVMs.

Global state that keeps the lock necessary:

| State | Where | Use |
|---|---|---|
| `numericContext` | static in `_PolyhedralBoundedSolidOperator`; 220 references, set at 14 places | The tolerances of the operation in progress |
| `idNamespace` | static in `_PolyhedralBoundedSolidOperator` | Identifier ranges of the operation in progress |
| `currentId` | static in `_PolyhedralBoundedSolidEdge` and `_PolyhedralBoundedSolidHalfEdge` | Identifier counters |
| `debugFlags` | static in the set operator, the three classifiers and the connector | Configuration |
| `debugSolidExporter` | static in the set operator | Configuration |
| `intersectionTrace` | static list in the intersector | Diagnostics |
| `PolyhedralBoundedSolidStatistics` | static counters | Diagnostics |

One more hazard is not a static field: `CircularDoubleLinkedList.get`
writes an access memo. Two threads that only read the same list still
race on that memo.

### 5.2 MT-1: independent booleans in parallel

Goal: remove the lock, so that operations on different solids run at
once.

1. `numericContext` and `idNamespace` per operation. The shortest step
   is a `ThreadLocal` behind the existing accessor; hot loops read it
   once into a local variable. The cleaner one is to pass
   `_SetOperationContext`, which already exists, to the classes that
   read them.
2. Identifier counters: atomic, or one counter per solid. Identifiers
   only need to be unique inside a solid; `compactIds` already rewrites
   them.
3. `debugFlags` and the exporter: set before use and read-only during
   operations, or moved into the context.
4. `intersectionTrace` and the statistics: per thread or atomic.
5. Replace the lock by nothing. Keep `setOp` copying its operands, so
   two threads can share an input solid. Sharing needs P-05 for the
   structural copy as well, because copying reads the lists.

*What it gives:* a CSG tree evaluates its sibling subtrees in parallel.
For a balanced tree the gain approaches the number of cores at the
leaves. For a chain such as the bowl (each step needs the previous
result) the gain is none; the 80 motifs can be built and placed in
parallel, which is about 3 % of the time.

*Robustness:* results must be identical to the serial ones. Gate: T-01.

*Effort:* medium. The 220 references to `numericContext` are mechanical.

### 5.3 MT-2: threads inside one boolean

Only work that reads and does not edit can be split. Stage by stage:

| Stage | Share now (A / B) | Parallel? | How |
|---|---:|---|---|
| Copy of the operands | under 1 % | Yes, A and B on two threads | After MT-1 |
| `maximizeFaces` and validation of the operands | 16 % / 2 % | Yes, A and B on two threads | After MT-1 |
| Preflights | 3 % / 4 % | Yes | A-in-B and B-in-A on two threads; points classified in parallel |
| Generate | 1 % / 3 % | Partly | Search for edge/face hits in parallel, apply the subdivisions in a fixed order. Today search and subdivision alternate through a worklist, so this is a restructuring |
| Classify | small | No | Inserts null edges in both solids; the order decides the topology |
| Connect | 2 % / 60 % | No | Each join changes the faces the next one reads. P-04 is the fix for its cost |
| Finish, cleanup | 25 % / 10 % | No | Edits the result. P-02 is the fix |
| Non-manifold check | 14 % / under 1 % | Yes | After P-03 it is no longer worth it |
| Validation of the result | 44 % / 16 % | Yes | Per face (planarity, loops) and per pair of faces (intersections), over snapshot arrays |

Conditions:

- P-05 first. Concurrent readers of a `CircularDoubleLinkedList` race on
  its access memo; over arrays they do not.
- Deterministic output. Parallel passes must report the same first
  failure and the same message as the serial pass: collect per index,
  then scan in order.
- No reordered floating-point sums. Volumes and areas accumulated in a
  different order differ in the last bits, and tolerances compare them.

*What it gives:* with validation at roughly half the time after the
single-thread fixes, eight threads on validation alone give at most
about 1.8 times overall. The editing stages bound the rest.

*Effort:* medium for validation, large for generate.

### 5.4 MT-3: separate processes

Already used by the tests (`-PtestForks=N`). It needs no kernel change
and suits batch jobs: one process per model. It costs a JVM start and
the copy of the solids between processes.

### 5.5 Order

MT-1 before MT-2: it is the smaller change, it removes the global state
that MT-2 also needs gone, and it is what a CSG tree evaluator needs.
Neither replaces Section 4: one thread with P-02 to P-06 is expected to
gain more on both workloads than eight threads without them.

---

## 6. Classes without public attributes

### 6.1 Inventory

43 public instance attributes in 13 classes.

| Class | Public attributes |
|---|---|
| `_PolyhedralBoundedSolidHalfEdge` | `parentLoop`, `parentEdge`, `startingVertex`, `id` |
| `_PolyhedralBoundedSolidEdge` | `rightHalf`, `leftHalf`, `id`, `debugColor` |
| `_PolyhedralBoundedSolidVertex` | `position`, `id`, `emanatingHalfEdge`, `debugColor` |
| `_PolyhedralBoundedSolidLoop` | `parentFace`, `boundaryStartHalfEdge`, `halfEdgesList` |
| `_PolyhedralBoundedSolidFace` | `id`, `parentSolid`, `boundariesList` |
| `_PolyhedralBoundedSolidSectorClassification` | `sector`, `classification`, `isWide`, `position`, `situation` |
| `…SetOperatorSectorClassificationOnFace` | `referencePlane`, `reverse`, `coplanarRelation` |
| `…SetOperatorSectorClassificationOnSector` | `secta`, `sectb`, `s1a`, `s2a`, `s1b`, `s2b`, `intersect`, `hea`, `heb`, `wa`, `wb` |
| `…SetOperatorSectorClassificationOnVertex` | `he`, `wide` |
| `…SetOperatorVertexFace` | `v`, `f` |
| `…SetOperatorVertexVertex` | `va`, `vb` |
| `…SetOperatorNullEdge` | `e` |
| Entry class inside `_PolyhedralBoundedSolidSplitter` | `e` |

The node attributes are read or written at about 2 300 places
(`startingVertex` 596, `boundariesList` 434, `parentLoop` 291,
`boundaryStartHalfEdge` 198, `parentFace` 185, `rightHalf` 183,
`leftHalf` 165, `parentEdge` 122, `emanatingHalfEdge` 49,
`parentSolid` 40), in 49 source files and 20 test files of `base`, plus
7 files in three applications (`SceneEditorApplication`,
`PolyhedralBoundedSolidExample`, `TangibleInterfaceGizmoCreator`).

### 6.2 Why it matters beyond style

- P-06 and P-07 need to know when a solid changed. With public
  attributes any class can relink a half-edge without telling the solid.
- The back-pointer invariants (`validateBackPointers`) are checked after
  the fact. Setters can keep them at the moment of the edit.
- Two attributes expose a mutable list (`boundariesList`,
  `halfEdgesList`). Code outside the Euler operators can add or remove
  loops directly.

### 6.3 Plan

**E-1 — Classification entries** (the eight small classes, about 30
attributes). Private attributes with `getX` / `setX`. They live inside
one operation and have no invariants. Internal to the kernel packages;
no application uses them. *Effort:* small.

**E-2 — Node links** (`HalfEdge`, `Edge`, `Vertex`, `Loop`, `Face`).
1. Add getters and setters next to the public attributes.
2. Convert the readers, one attribute per change, with the suite run
   after each: the compiler finds every site once the attribute is
   private.
3. Convert the writers. Writers outside
   `PolyhedralBoundedSolidEulerOperators` and the node classes are the
   interesting ones: list them, and move each into an operator or
   justify it.
4. Make the attributes private. Setters advance the edit counter of the
   solid (P-07).

*Effort:* large but mechanical.

**E-3 — Lists.** `boundariesList` and `halfEdgesList` become private,
with read access by index and iteration, and edits only through methods
of the node (`addRing`, `removeLoop`, `makeOuter`).

**E-4 — `position` and `id`.** `Vertex.position` gets a setter that
advances the edit counter, since moving a vertex changes the planes of
its faces. `id` gets a getter; only `compactIds` and the constructors
set it.

Notes:

- Cost: none. The JIT inlines trivial accessors; confirm on the two
  workloads after E-2.
- Other ports: the names must carry over as written (`getX` / `setX`,
  no records, no properties), since the C++, TypeScript and Turbo C
  ports follow the Java one literally. Turbo C++ 3.0 has no inlining
  across translation units unless the accessors are defined in the
  class body.
- E-1 and E-2 touch most kernel files. Do them in a change of their own,
  with no behaviour change in the same commit.

---

## 7. Tests to add

| Id | Test | Protects |
|---|---|---|
| T-01 | Concurrency gate: the box-pair and composite fuzz families run from 8 threads at once; every result equals the serial result (counts, topology summary, volume) | MT-1 |
| T-02 | Work-counter ratchet: counters of plane computations, point classifications and list steps for the bowl and for N = 128 cylinders, with a maximum that only goes down. Counters are deterministic; wall time is not | Every P entry |
| T-03 | `findNonManifoldContact`: hashed version against the all-pairs version on the corpus, the fuzz results and solids with planted contacts across cell borders | P-03 |
| T-04 | Preflight equivalence: old and new interior-overlap and point classification on every operand pair of the fuzz families, including touching and thin-overlap pairs | P-08, P-09 |
| T-05 | Scaling gate (slow): N = 64, 128, 256 cylinders; the ratio between consecutive sizes stays under a bound | Regressions to cubic time |
| T-06 | Stale-plane test: after each Euler operator and each vertex move, the cached plane equals a fresh one | P-07 |
| T-07 | Volume identities on the N-sided cylinders: \|A∪B\| = \|A\| + \|B\| − \|A∩B\|, \|A−B\| = \|A\| − \|A∩B\|. They hold today to 10⁻¹⁴ up to N = 256 and need no oracle | Results on curved-like operands, before and after every change |

Protocol for every P entry marked "same decisions": the suite, the slow
gates and the composite ratchets must give identical counts, and the
bowl must give F=1133, E=3396, V=2186 and volume 0.38391604531097.

---

## 8. Suggested order

| Step | Entries | Why here |
|---|---|---|
| 1 | T-02, T-05, T-07 | A baseline before changing anything |
| 2 | P-02, P-03, P-04, P-10 | Small, local, same decisions; most of the gain |
| 3 | P-05, first part of P-06 | Mechanical; prepares the threads |
| 4 | MT-1 with T-01 | Removes the lock |
| 5 | E-1, then E-2 to E-4 | Makes the edit counter trustworthy |
| 6 | P-07, second part of P-06, T-06 | Needs step 5 |
| 7 | MT-2 on validation | Needs steps 3 and 4 |
| 8 | P-08, P-09 with T-04 | Change a decision procedure; last and behind a comparison |
| 9 | P-11 to P-14 | When models reach thousands of faces |
