# Stage 9 — CSG Boolean Pipeline Performance

**Date:** 2026-06-12
**Author:** Audit by Claude (Fable 5); written to be executed by a coding agent (Opus)
**Baseline commit:** `6bf29caf` ("ava port: PolyhedralBoundedSolidExample kurlander bowl passing tests")
**Sibling plan:** `doc/plan-csg-boolean-refactor-stage7.md` (maintainability;
shares the same audit). Steps here are independent of the refactor except
where explicitly noted (P7 requires refactor step R5).

---

## 1. Problem Statement and Evidence

The base testsuite takes ~14 minutes, dominated by boolean operations that run
~100× slower than expected for solids of this size (hundreds of faces) on a
2021 CPU. Evidence below comes from JUnit timing XMLs and two JFR profiling
runs performed during the audit.

### 1.1 Where the 14 minutes go (JUnit XMLs, `base/build/test-results/test`)

| Test class | tests | time (s) |
|---|---:|---:|
| `KurlanderMotif4OperationMatrixTest` | 53 | 281.7 |
| `KurlanderBowlMotifSweepRegressionTest` | **1** | 281.3 |
| `Stage6FaceSubdivisionDiagnosticTest` | 3 | 153.1 |
| `KurlanderBowlStarInvariantTest` | 20 | 72.4 |
| `IntersectionCurveBuilderTest` | 9 | 26.4 |
| `CsgKurlanderBowlAllMotifsRegressionTest` | 5 | 11.8 |
| everything else (~40 classes) | — | < 20 total |

~96% of the wall clock is 5 test classes, all running Kurlander-bowl booleans.
The 40-motif sweep is a single test method at ~7 s per boolean operation.

### 1.2 Profiling infrastructure (already in place)

`base/build.gradle` has an opt-in JFR hook added during the audit:

```bash
./gradlew :base:cleanTest :base:test -Pjfr --tests "<TestClass>" -x jacocoTestReport
# writes base/build/profiling/test.jfr (settings=profile, dumponexit=true)
```

Analysis recipe used below (aggregate leaf frames and full-stack presence):

```bash
jfr print --events jdk.ExecutionSample test.jfr \
  | awk '/stackTrace = \[/{getline; sub(/^ */,""); sub(/\(.*/,""); print}' \
  | sort | uniq -c | sort -rn | head -15        # leaf frames (CPU burn)
jfr print --events jdk.ExecutionSample test.jfr \
  | grep -E "^\s+vsdk" | sed 's/^ *//;s/(.*//' \
  | sort | uniq -c | sort -rn | head -15        # presence in stacks
```

Note: the JaCoCo agent stays attached even with `-x jacocoTestReport`; it shows
up during class loading but does not dominate steady state.

### 1.3 JFR results — large run (`Stage6FaceSubdivisionDiagnosticTest`, 159 s, 12,855 samples)

Leaf frames (a 10 s run on `CsgKurlanderBowlFirstStarRegressionTest` gave the
same distribution — this is not a small-test artifact):

| Leaf frame | samples | % |
|---|---:|---:|
| `CircularDoubleLinkedList.nextOf` | 4,434 | 34.5% |
| `CircularDoubleLinkedList.get` | 3,686 | 28.7% |
| `PolyhedralBoundedSolidNumericPolicy.estimateFaceScale` | 2,115 | 16.5% |
| `_PolyhedralBoundedSolidFace.testPointInsideDetailed` | 752 | 5.9% |
| `_PolyhedralBoundedSolidFace.calculatePlaneByNewell` | 513 | 4.0% |
| `_PolyhedralBoundedSolidFace.testPointInside` | 462 | 3.6% |

Full-stack presence:
`_PolyhedralBoundedSolidSetNonIntersectingClassifier.classifyPointAgainstSolid`
appears in **70.9%** of all samples; `hasConfirmedInteriorOverlap` in 46.3%;
`_PolyhedralBoundedSolidFace.getContainingPlane` in 45.3%. High allocation
pressure was also observed (~171 GCs in the 10 s run).

### 1.4 Root causes (three multiplying layers, all verified in code)

**Layer 1 — Architecture (~71% of time): unconditional preflights.**
`PolyhedralBoundedSolidSetOperator.setOp` (around lines 3673–3699) runs, on
*every* call and before the Mantyla pipeline:

1. `prepareProfileDifferenceFallbackSpec` and
   `prepareOffsetCylinderDifferenceFallbackSpec`;
2. `buildAxisAlignedCellBooleanFallback` and
   `buildOrthogonalProfileBooleanFallback` — these **construct complete
   fallback solids up-front**, used or not;
3. `_PolyhedralBoundedSolidSetNonIntersectingClassifier.runPartialCoplanarFaceAreaCase`,
   which calls `hasConfirmedInteriorOverlap`: builds a 3D sampling grid from
   the unique vertex coordinates of both solids per axis (`xs × ys × zs` can
   reach 10⁴–10⁶ points for the bowl) and classifies **each grid point against
   both solids**, each classification scanning all faces;
4. `isTouchingOnlyPreflightCase`, which **repeats** `classifySolidAgainstSolid`
   ×2, `hasConfirmedInteriorOverlap`, and `hasProperEdgeFaceIntersection` ×2
   with no memoization relative to step 3.

In the testsuite's common case (bowl − motif: the solids DO intersect), all
preflights run, fail, and only then does the real pipeline start.

**Layer 2 — Geometric recomputation (≈25% direct, and it multiplies layer 1).**
`_PolyhedralBoundedSolidFace.getContainingPlane()`
(`nodes/_PolyhedralBoundedSolidFace.java:216`) does not cache: every call
rebuilds the tolerance context (`PolyhedralBoundedSolidNumericPolicy.forFace`
→ `estimateFaceScale`, which walks all loops) and recomputes the Newell plane
(another full loop walk). The in-file comment says "Faces no longer store that
plane" — caching was removed deliberately in commit `ac836a49`; reintroducing
it needs explicit invalidation design. Worse,
`classifyPointAgainstSolid` (`_PolyhedralBoundedSolidSetNonIntersectingClassifier.java:244-249`)
calls `getContainingPlane()` **twice per face per point**.

**Layer 3 — Data structure (63% of leaf samples).**
`CircularDoubleLinkedList` (`vsdk/toolkit/common/dataStructures/`) implements
`get(index)`, `nextOf`, `previousOf`, `locateWindowAtElem`, `remove(pos)` as
linear scans from `head`. The dominant code pattern
`for (i = 0; i < list.size(); i++) { list.get(i); }` is therefore O(n²) per
loop — there are **554** such indexed accesses in the two CSG packages, many
nested. Critically, `_PolyhedralBoundedSolidHalfEdge.next()` is implemented as
`parentLoop.halfEdgesList.nextOf(this)`
(`nodes/_PolyhedralBoundedSolidHalfEdge.java:76`, `previous()` idem line 67):
**every single step of a loop traversal is an O(k) search**, so walking a face
boundary is O(k²). In Mantyla's original C this was a direct pointer
(`he->nxt`). Newell plane computation, `estimateFaceScale`, validators and
`toString` all sit on top of this.

---

## 2. Execution Plan (step by step)

Run all Gradle commands from `java/`. Gate after every step:

```bash
./gradlew :base:compileJava :base:compileTestJava
./gradlew :base:test
```

All current tests green = the step is acceptable. Additionally, **measure**
after every step (see P0.3) and record the numbers in §3. Expected end state:
testsuite in low single-digit minutes; P1+P2 alone should deliver most of it.

### Step P0 — Guardrails and baseline (no kernel changes)

1. Keep the `-Pjfr` hook in `base/build.gradle` (already committed during the
   audit).
2. Tagging: `KurlanderBowlMotifSweepRegressionTest`,
   `Stage6FaceSubdivisionDiagnosticTest` and `KurlanderMotif4OperationMatrixTest`
   already carry `@Tag("slow")`; **add it to `KurlanderBowlStarInvariantTest`**
   (72 s, currently untagged). Add to `base/build.gradle`:

   ```groovy
   tasks.named('test') {
       if (project.hasProperty('excludeSlow')) {
           useJUnitPlatform { excludeTags 'slow' }
       }
   }
   ```

   (Mind the existing `useJUnitPlatform()` call — merge, don't duplicate.)
   Default behavior (CI, plain `gradle :base:test`) must not change.
3. Record the baseline: run the full suite once, save the per-class timing
   table (recipe: parse `base/build/test-results/test/TEST-*.xml` headers).
   Re-run and re-record after each step below. A step that does not move the
   needle should still be kept if it is a prerequisite for a later step;
   otherwise reconsider.

### Step P1 — Gate and memoize the preflights (targets the 71%)

All changes inside `PolyhedralBoundedSolidSetOperator.setOp` and
`_PolyhedralBoundedSolidSetNonIntersectingClassifier`. Output semantics must
be identical: same returns for the same inputs — the motif/moon/algebraic
identity tests are the contract.

1. **Memoize within one `setOp` call.** `runPartialCoplanarFaceAreaCase` and
   `isTouchingOnlyPreflightCase` each independently compute
   `classifySolidAgainstSolid` (×2), `hasConfirmedInteriorOverlap` and
   `hasProperEdgeFaceIntersection` (×2). Introduce a small package-private
   result holder (e.g. `_PreflightClassificationCache` with lazily-computed
   fields) created at the top of `setOp` and passed to both; each expensive
   predicate computes once per (A,B,op) invocation. This alone halves
   preflight cost with zero risk.
2. **Short-circuit `hasConfirmedInteriorOverlap` before building the grid.**
   Current code builds the full `xs × ys × zs` cartesian grid and tests every
   point against both solids until one is INSIDE/INSIDE. Add cheap early
   passes, in order, returning `true` on first hit:
   a. the center of the AABB overlap volume;
   b. vertices of A lying strictly inside B's AABB (and vice versa),
      classified against the other solid only;
   c. only then fall back to the existing grid — but iterate it
      center-outward or cap it with a sample budget (e.g. uniformly subsample
      to ≤ 1,000 points before the full grid). Because the function is an
      existence test ("is there a shared interior point"), any early-found
      witness gives the same answer; the budget-capped grid must keep the
      exhaustive grid as final fallback to preserve exact semantics for the
      degenerate fixtures.
3. **Build fallbacks lazily.** `buildAxisAlignedCellBooleanFallback` and
   `buildOrthogonalProfileBooleanFallback` construct full solids up-front in
   every `setOp`; they are consumed only at specific failure/special-case
   sites (find them with `grep -n "axisAlignedCellBooleanFallback\|orthogonalProfileBooleanFallback" PolyhedralBoundedSolidSetOperator.java`).
   Replace the eager fields with lazy suppliers (compute-on-first-use,
   memoized for the rest of the call). Their *detection* predicates
   (`shouldUseAxisAlignedCellBooleanFallback`, spec preparation) may need to
   stay eager if the pipeline branches on them early — check each call site;
   keep detection cheap and construction lazy.
4. Gate + measure. Expect the biggest single improvement of the whole plan on
   `KurlanderBowlMotifSweepRegressionTest`.

One commit per sub-step (P1.1, P1.2, P1.3).

### Step P2 — Stop recomputing face planes and tolerance contexts

1. **Micro-fix first (separate commit, zero risk):**
   `classifyPointAgainstSolid` calls `face.getContainingPlane()` twice per
   face (`_PolyhedralBoundedSolidSetNonIntersectingClassifier.java:246,249`).
   Capture in a local. Audit the same file (and
   `hasProperEdgeFaceIntersection`) for repeated calls in the same scope.
2. **Cache the containing plane per face.** In `_PolyhedralBoundedSolidFace`:
   - add `private InfinitePlane cachedContainingPlane;` and
     `private long topologyStamp;` (or a boolean dirty flag);
   - `getContainingPlane()` returns the cache when clean, else recomputes via
     the existing Newell/corner path and stores;
   - invalidation: caching was removed deliberately in `ac836a49`, so the
     invalidation points must be explicit and complete. Faces change when (a)
     their loop topology changes — every Euler operator and every method in
     `PolyhedralBoundedSolidTopologyEditing` / the splitter that adds, removes
     or re-links half-edges of the face — and (b) when vertex positions move.
     The safe mechanical approach: add `face.invalidateCachedPlane()` and call
     it from every site that mutates `boundariesList`, any
     `loop.halfEdgesList`, or `_PolyhedralBoundedSolidVertex.position` for a
     vertex of the face. For vertex moves, the simplest sound rule is to
     invalidate via the vertex's emanating half-edges' parent faces (vertex →
     half-edge → parentLoop → parentFace).
   - If exhaustive invalidation proves too risky, fall back to **scoped
     caching**: an explicit `FaceGeometryCache` (map face → plane+tolerance)
     created and used only inside read-only analysis passes
     (`classifyPointAgainstSolid`, `hasConfirmedInteriorOverlap`,
     `hasProperEdgeFaceIntersection`, validators), discarded before any
     topology mutation. This captures most of the win (the preflights are the
     dominant consumers) with no invalidation hazard.
3. **Cache the tolerance context with the plane.**
   `PolyhedralBoundedSolidNumericPolicy.forFace` → `estimateFaceScale` (16.5%
   of CPU) has the same call pattern; cache `ToleranceContext` alongside the
   plane with the same invalidation (or inside the same scoped cache).
4. Gate + measure after each sub-step; separate commits. The geometric
   validators (`PolyhedralBoundedSolidGeometricValidator`,
   strict-loops/face-intersections strategies) are the most sensitive
   consumers — if any validation-related test fails, the invalidation set is
   incomplete: find the mutation site you missed, do not loosen the test.

### Step P3 — O(1) half-edge `next()`/`previous()`

Targets the 34.5% `nextOf` leaf plus the quadratic factor inside every loop
walk (Newell, faceScale, validators, splitter, connector).

1. Add `next`/`previous` reference fields to `_PolyhedralBoundedSolidHalfEdge`
   maintained as part of loop structure, replacing the
   `parentLoop.halfEdgesList.nextOf(this)` lookups in `next()`/`previous()`
   (`nodes/_PolyhedralBoundedSolidHalfEdge.java:67,76`).
2. Find every mutation point of `loop.halfEdgesList`:
   `grep -rn "halfEdgesList\." base/src/main --include="*.java"` — expect the
   Euler operators (`PolyhedralBoundedSolidEulerOperators`), topology editing,
   the splitter and the null-edges connector. Each `add/insertBefore/remove`
   on the list must also splice the direct pointers. Keep the list itself for
   now (iteration order, size, membership) — this step only de-linearizes
   `next()`/`previous()`.
3. Add a cheap structural assertion to the existing topological validator
   (`_PolyhedralBoundedSolidTopologicalValidator`): walking `next` pointers
   from `boundaryStartHalfEdge` visits exactly `halfEdgesList.size()` elements
   and returns to start; equivalently for `previous`. This converts any missed
   splice into an immediate validation failure instead of a wrong-geometry
   mystery.
4. Gate + measure. Risk: medium (touches topology invariants); the validator
   assertion plus the full testsuite is the safety net. If a fixture fails,
   bisect by enabling the assertion and finding the unspliced mutation site.

Alternative if step 2 turns out to have too many mutation sites: store a
back-reference from each element to its list node
(`_CircularDoubleLinkedListNode`) so `nextOf` becomes O(1) generically; this
is less invasive but benefits only half-edges whose node reference is kept
consistent — same splice discipline, so prefer the direct-pointer design.

### Step P4 — Kill O(n²) indexed iteration over `CircularDoubleLinkedList`

Targets the 28.7% `get` leaf.

1. Make `CircularDoubleLinkedList<E>` implement `Iterable<E>` with an O(1)-step
   iterator (snapshot `head`, walk `next`, count `size` steps). Document that
   the iterator does not support concurrent structural modification; loops
   that mutate while iterating must keep using indices or collect first.
2. Migrate hot loops to `for (E e : list)`, guided by the JFR stacks, in this
   order: `estimateFaceScale`, `calculatePlaneByNewell` path,
   `classifyPointAgainstSolid` / `classifySolidAgainstSolid` /
   `hasProperEdgeFaceIntersection`, `PolyhedralBoundedSolid.doIntersection` /
   `calculateMinMaxPositions` / `testPointInsideForRayIntersection`, then the
   validators. **Skip loops that structurally modify the list during
   iteration** (e.g. `merge`'s drain loops are fine as `while size>0 get(0)` —
   `get(0)` is O(1)).
3. As a safety net for non-migrated code, memoize the last accessed
   (index, node) pair in `get(int)` so sequential `get(i)`/`get(i+1)` walks
   become O(1) amortized; invalidate the memo on any structural change
   (`add`, `insertBefore`, `remove*`, `push`, `reverse`, `swapElements`).
   This single change accelerates all 554 indexed loops at once without
   touching them; keep it even after migrating the hot ones.
4. Gate + measure. The 554 call sites do **not** all need migration —
   after P3 plus the memo in P4.3, re-profile and migrate only what still
   shows up.

### Step P5 — O(1) id lookups

`PolyhedralBoundedSolid.findVertex(id)` / `findFace(id)` are linear scans used
inside loops. Add `HashMap<Integer, …>` indices maintained at the mutation
points (vertex/face add and remove — the Euler operators and `merge`;
`compactIds` and `updmaxnames` rewrite ids, so they must rebuild or update the
maps). Verify with JFR whether this still matters after P1–P4; if `findVertex`
no longer appears in stacks, file this step as not-needed and skip it.

### Step P6 — Allocation pressure (conditional)

After P1–P4, re-profile with `jfr print --events jdk.ObjectAllocationSample`.
The audit run showed ~17 GCs/s driven by per-iteration allocation of
`Ray`/`RayHit`/`Vector3Dd`/`ToleranceContext`/`ArrayList`. Most of it
disappears with P1 (fewer classifications) and P2 (no per-call tolerance
contexts). Only act here if allocation still dominates: typical fixes are
reusing a scratch `Vector3Dd` in inner loops of geometric predicates and
hoisting list allocations out of per-face loops. Do not introduce object
pools — measure first.

### Step P7 — Parallel test execution (requires refactor stage 7, step R5)

Precondition: no mutable static state in the operators package
(`doc/plan-csg-boolean-refactor-stage7.md` step R5 done). Then in
`base/build.gradle`:

```groovy
maxParallelForks = Math.max(1, Runtime.runtime.availableProcessors().intdiv(2))
```

Expected 3–4× wall-clock on the suite (the five slow classes parallelize
well). Verify no test relies on shared files or fixed ports (none known).
Also evaluate precomputing/sharing fixture operands in
`KurlanderMotif4OperationMatrixTest` (53 tests × fresh
`CsgKurlanderBowlFixture.createBowlAndFirstStarOperands` each — operands are
mutated by `setOp`, so sharing requires deep-cloning a cached prototype, which
is only a win if fixture construction is significant; measure first).

---

## 3. Measurement Log

Record after each step (suite wall-clock from `gradle :base:test`, per-class
times for the five slow classes, and the JFR top-5 leaf frames when a profile
was taken).

| Step | Suite time | Sweep (281 s baseline) | Matrix (282 s) | Stage6 (153 s) | Notes |
|---|---|---|---|---|---|
| baseline `6bf29caf` | ~14 min | 281.3 s | 281.7 s | 153.1 s | JFR: nextOf 34.5%, get 28.7%, estimateFaceScale 16.5% |

## 4. Execution Log

- **2026-06-12 (audit):** Timing attribution (§1.1), JFR hook added to
  `base/build.gradle`, two JFR runs analyzed (§1.3), root-cause chain verified
  in code (§1.4). Plan written; execution not started.
