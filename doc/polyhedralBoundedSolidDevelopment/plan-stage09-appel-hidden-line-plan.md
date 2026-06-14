# Stage 9 — Appel Hidden-Line Renderer: Correctness, Performance, and Robust Predicates

**Date:** 2026-06-14
**Author:** Investigation and implementation by Claude (Opus 4.8); written to be
continued by a coding agent.
**Working commit:** `6ab82d56` ("Java port: Efficiency update on quantitative
visibility for hidden line rendering").
**Primary files:**
- `java/base/src/main/vsdk/toolkit/render/hiddenLine/HiddenLineRenderer.java`
- `java/base/src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.java`
  (the quantitative-invisibility kernel)
- `java/base/src/main/vsdk/toolkit/environment/scene/SimpleBody.java`
  (body↔world transforms used by the kernel)
- `java/base/src/test/vsdk/toolkit/render/HiddenLineRendererTest.java` (guards)

All Gradle commands run from `java/`. References: `[APPE1967]` Appel, "The
notion of quantitative invisibility and the machine rendering of solids";
`[FOLE1992]` §15.3.2; `[MANT1988]` for the BREP data structure.

---

## 0. Background

`HiddenLineRenderer.executeAppelAlgorithm` implements Appel's hidden-line
algorithm over `PolyhedralBoundedSolid` BREPs. Per edge it:

1. **Builds an edge cache** (`buildCache`): classifies each solid edge as
   `CONTOUR` (one adjacent face front-facing, one back), `VISIBLE` (both front),
   or `HIDDEN` (both back), from a per-face front/back test.
2. **Splits the edge** (`processLineToBeDrawn`): finds where the edge's image
   crosses a contour line (sweep-triangle intersection), producing sub-segments
   of uniform visibility.
3. **Classifies each sub-segment** by its *quantitative invisibility* (QI): the
   number of front-facing faces strictly between the eye and a sample point on
   the sub-segment. QI == 0 ⇒ visible, QI > 0 ⇒ hidden.

The QI is computed by `PolyhedralBoundedSolid.computeQuantitativeInvisibility`
(the "kernel"), called through `SimpleBody.computeQuantitativeInvisibility`,
which transforms the eye and the sample point into **object space** (the BREP
faces are stored in object/local coordinates) before counting.

Test objects used throughout:
- `SPLIT_TEST_PART_1` (`SimpleTestGeometryLibrary.createTestObjectMANT1986_1`):
  a notched prism (a concave 7-vertex profile extruded in Y; 14 vertices,
  21 edges, 9 faces).
- `kurlanderBowl` (`etc/solids/kurlanderBowl.step`, model 28): a dense concave
  bowl with engraved motifs (V=2186, E=3396, F=1133).
- `featured object` (`createTestObjectAPPE1967_3`, models 26/27): the union of
  **12 axis-aligned boxes** forming a cube-edge frame plus a slot/arm
  (F=32, E=84, V=54). Because it is a clean box union, a trivial trustworthy
  ground truth exists: a point is inside iff it is inside any of the 12 boxes.

### 0.1 Offline diagnostic harness

Run the renderer headless and dump per-edge/per-segment data (gradle root is
`java/`):

```
cd java && gradle :testsuite:Jogl4Examples:PolyhedralBoundedSolidExample:runMain -q \
  -PrunMainClass=PolyhedralBoundedSolidExample \
  -PrunJvmArgs='--add-exports=java.desktop/sun.awt=ALL-UNNAMED|--add-opens=java.desktop/sun.awt=ALL-UNNAMED|-Dpoly.offline=true|-Dpoly.edgeIndex=-3|-DpolySolidModel=20|-Dpoly.appelDump=/tmp/appel.json|-Dpoly.output=/tmp/appel.png'
```

`-DpolySolidModel=` selects the object (20 = SPLIT_TEST_PART_1, 26 = featured
object, 28 = kurlanderBowl). `-Dpoly.appelDisplayMode=2` = "edges + visible",
`-Dpoly.surfaces=false` = wireframe. The JSON dump (gated on
`poly.edgeIndex=-3`) holds per-edge type, per-segment midpoint QI, and the
classification. A static diagnostic hook `HiddenLineRenderer.DEBUG_EDGE_INDEX`
(default -1, inert) logs the contour-crossing/split decisions for one edge.

---

## 1. Problem Statement

Reported defects, in order of investigation:

1. Visible lines drawn hidden and vice versa on `SPLIT_TEST_PART_1` and
   `kurlanderBowl`.
2. Partially occluded edges not clipped: edge v11–v12 of SPLIT_TEST_PART_1 drawn
   too long (`muyLargo`) or with its visible part missing (`noDetectado`).
3. The whole algorithm is **catastrophically slow** (~49 s/frame on
   kurlanderBowl), looking frozen under animation.
4. A spurious intermediate segment "inside edge 47–48" of the featured object.

---

## 2. Work Completed (this stage)

Every fix below is covered by a regression test in `HiddenLineRendererTest` and
the full base suite is green (**405 tests, 0 failures**).

### 2.1 QI over-count at concave edges — FIXED

`computeQuantitativeInvisibility` jittered the sample point **perpendicular** to
the line of sight (4 rays, majority vote). Appel sample points lie *on* a solid
edge, so the two incident faces pass through them. At a **concave** edge (the
notch floor of SPLIT_TEST_PART_1, z=0.30) the perpendicular jitter pushed the
sample across its own incident face, counting it as an occluder → a genuinely
visible edge reported hidden (only edge 14 failed, at every orientation where it
is front-facing).

**Fix:** before jittering, pull the sample a step toward the eye
(`pullBack = t0·1e-3`, larger than `delta = t0·1e-4`). Visible surface points
land in free space just off the surface (0 occluders); truly occluded points
stay inside the (thicker) occluding material so the front face is still counted.

**Verification:** an orientation-free crossing-count oracle validated against a
dense ray-march ground truth across all 21 edges × multiple angles. Jitter-based
oracles are blind to this bug (same failure mode as the kernel). Guard:
`given_splitTestPart1_atManyAngles_then_visibilityMatchesIndependentRayOracle`.

### 2.2 Split boundary discarded on dead-ΔQI failure — FIXED

In `processLineToBeDrawn` a contour crossing was only recorded as a split
boundary **after** the textbook ΔQI value was resolved. ΔQI is dead code (the
classifier resamples the midpoint QI), but the `continue` on `pos == LIMIT`
(projection on a face boundary) or a degenerate plane *discarded the
already-computed split*. On edge v11–v12 occluded by face 3-4-11-10 the split at
t = 0.725 (exactly the V→H transition) was thrown away → the edge was drawn fully
visible.

**Fix:** always add the split boundary once a valid in-unit-interval crossing is
found; ΔQI is computed best-effort, never gating the split. Coincident crossings
(within `VSDK.EPSILON`) are merged so none is lost. Guard:
`given_splitTestPart1_partialOcclusion_then_edgesClippedToGroundTruth` (dense
per-edge sampling vs an orientation-free ray-march of the rotated solid).

### 2.3 Face orientation (front/back labelling) — FIXED

`HiddenLineRenderer.getWorldContainingPlane` derived the face normal from the
cross product of the **first three loop vertices**, which flips to an inward
normal at a reflex corner (the notched cap), mislabelling front/back faces, edge
types (contour vs visible) and the occluding face of a contour.

**Fix:** take the orientation from the maintained outward normal
`face.getContainingPlane()` (Newell over the whole loop, reflex-safe) and
transform it to world via a new `SimpleBody.transformNormalToWorld`
(`= normalize(R · (n / scale))`, orientation preserving) plus a loop vertex as
the in-plane point. Guard:
`given_splitTestPart1_atManyAngles_then_edgeTypesMatchIndependentFrontBack`
(renderer edge type vs an independent world-Newell front/back test, Z and tilted
Rz·Rx orientations).

### 2.4 Performance — FIXED (~40×, 49 s → 1.2 s/frame on kurlanderBowl)

Root cause: `countStrictFrontOccluders` called `face.getContainingPlane()` (a
Newell fit over the loop + a per-face scale estimate) for **every face, per
jittered ray, per QI sample, per sub-segment** — ≈19 700 samples × 4 rays ×
1133 faces ≈ **89 M plane recomputes**, plus a `new RayHit()` per face per ray.

Fixes, all per-frame on the static solid:

| Step | Change | ms/frame |
|---|---|---:|
| baseline | — | 48 935 |
| 1 | **Plane + tolerance cache** on `PolyhedralBoundedSolid` (`beginVisibilityQueries`/`endVisibilityQueries` snapshot `queryPlaneCache[i]` and `queryNumericContext`; the kernel uses `cachedFacePlane(i)` and the 3-arg `face.testPointInside(p,tol,plane)`). The renderer brackets the edge loop with begin/end (try/finally) for every PolyhedralBoundedSolid body. | 12 816 |
| 2 | **Per-face AABB cull** (`queryFaceAabb`, 6 doubles/face) with an allocation-free slab test (`rayReachesFaceAabb`, pad = bigEpsilon, conservative) before the plane intersection. | 1 457 |
| 3 | **Optional dump**: `executeAppelAlgorithm` (render path) → `runAppelAlgorithm(..., collectDiagnostics=false)`, skipping the dump objects, the diagnostic seed QI, and the ΔQI projection; `executeAppelAlgorithmWithDiagnostics` passes `true`. | 1 217 |

Caveat (documented at the cache): the solid **must not be modified** while
`beginVisibilityQueries` is active. Verified by a microbenchmark (removed) and
the full suite; the kurlanderBowl image is unchanged.

### 2.5 QI counted the sample's own incident/boundary faces — FIXED

On the axis-aligned featured object whole visible edges were drawn hidden at some
rotations (e.g. body rot Z=45,X=60 edge 31). With the trustworthy 12-box ground
truth, the kernel was confirmed to **over-count**: at an axis-aligned silhouette
edge/vertex the perpendicular jitter pushed the line of sight across a face
**incident to the sample** (the sample's own surface) → phantom occluder. The
incident face's plane contains the sample (`pointDistance ≈ 0`) but the sample is
often on the face's **boundary** (`testPointInside == LIMIT`, not `INSIDE`).

**Fix:** `countStrictFrontOccluders` now receives the original surface sample and
skips any face incident to it —
`|plane.pointDistance(p)| < bigEps && testPointInside(p) != OUTSIDE` (INSIDE *or*
LIMIT). Removing the jitter entirely is **not** viable (it under-counts occluders
entered through an edge: featured scan 19→141, the featured oracle test fails).
On a 24×24 body-rotation scan vs the 12-box march this reduced stable
disagreements 19→16 and removed whole-edge artifacts. Guard:
`given_featuredObject_atRotation_then_visibleEdgeNotPhantomHidden`.

---

## 3. Pending / Open Problems

### 3.1 Incremental QI propagation is DISABLED (efficiency + fidelity)

The classifier resamples the kernel QI at every sub-segment midpoint, which is
why a frame costs O(sub-segments × rays × faces). The faithful `[APPE1967]`
algorithm seeds QI once per edge and **propagates** it by ±1 ΔQI at each contour
crossing — roughly one QI ray cast per edge instead of one per sub-segment
(≈6× fewer on kurlanderBowl). Restoring it was **attempted and reverted**: the
cheap per-crossing ΔQI sign is not robust on concave solids. Concrete blockers
(all proven with `DEBUG_EDGE_INDEX` and a per-face occlusion probe):

- The occluding face at a crossing is frequently **not** the contour edge's
  camera-"visible" face (the face label can be wrong, or the occluder is a
  **non-adjacent** face that took over the occlusion mid-interval without a
  contour crossing — a count-preserving face swap).
- Evaluating occlusion just after the crossing is silhouette-grazing.
- The image-space "side rule" sign (ΔQI=+1 if the edge end is on the same side of
  the contour image-line as the front face) is sign-robust in principle but the
  reference for "the face's side" is fragile for a **concave** face (its
  centroid can sit on the wrong side of the edge; a local in-plane inward step
  helps but still mis-signs some cases).

The robust ΔQI equals `kernelQI(afterMid) − kernelQI(beforeMid)`, which is the
midpoint resampling we already do (no saving). So propagation needs *both*
complete contour detection *and* a robust cheap ΔQI; it is gated on the robust QI
work below.

### 3.2 QI grazing on axis-aligned solids at convex silhouette corners — OPEN

This is the genuinely hard core. After §2.5 there remain ~16 stable
disagreements out of ~48 000 edge×orientation instances on the featured object.
Their mechanism (proven by tracing the 4 jittered counts): at a **convex
silhouette corner** the four perpendicular jitters each cross a *different*
genuinely-nearby face (not incident, at feature-size distance) and vote a
**unanimous-but-wrong** occluder of 1.

A principled rewrite was attempted and **reverted** (made it worse): replace
jitter+vote with a **crossing-count over intervals** —

- `collectRayBoundaryCrossings`: split the eye→p segment at every tolerant
  face-plane crossing (capture INSIDE *or* LIMIT, so an edge/vertex graze becomes
  an interval boundary).
- `isPointInsideSolid`: classify each interval inside/outside with an
  **independent generic auxiliary ray** (ray-cast parity, odd = inside).
- QI = number of outside→inside transitions before `p`.

This is orientation-free and conceptually robust, **but two degeneracies broke
it** (measured against the 12-box ground truth on the 24×24 scan):

1. **View direction coplanar with a face.** When the object-space line of sight
   lies *in* a face plane (e.g. body rot Z=90,X=45 maps the eye to y≈0 in object
   space), every interval midpoint falls **on** that face → `isPointInsideSolid`
   is degenerate → whole-edge errors. Scan jumped 16 → 84.
2. **A single fixed perturbation grazes elsewhere.** Tilting the line of sight by
   a constant generic ~1e-4 rad fixed the coplanar cluster but the **fixed tilt
   grazed for most other orientations** → scan exploded to **594** and broke
   SPLIT_TEST and the featured guard.

**Lesson:** a correct interval classification still needs a **per-case
non-degenerate perturbation**, not a constant. That is exactly what robust
predicates provide (see §5). The stable kernel (jitter + incident-exclusion) is
correct on the vast majority and only mis-handles these rare grazing corners on
pathological axis-aligned solids.

### 3.3 Edge-type styling vs visibility (minor)

`isFaceVisibleFromCameraTransformed` (perspective branch) returns front-facing if
*any* loop vertex sees the face front — a loose approximation that can mislabel
near-silhouette faces. Now uses the reliable outward normal (§2.3) but the
"any vertex" heuristic remains; revisit only if styling (thick contour vs thin
visible) is observed wrong.

---

## 4. Definition: Robust Geometric Predicates

A **geometric predicate** is a function that returns a **discrete combinatorial
answer** (a sign, an in/out, an ordering) from numeric coordinate inputs.
Examples used by the QI kernel:

- `orientation(a, b, c, d)` → sign of the signed volume (which side of plane
  `abc` is `d`)?
- `pointInFace(q, face)` → INSIDE / OUTSIDE / LIMIT.
- `rayCrossesFace(ray, face)` → does the ray pierce the face interior?
- `inside(point, solid)` → is the point inside the solid?

A predicate is **robust** when its discrete answer is **always correct and
mutually consistent**, with **no special "degenerate/ambiguous" output**, even
when the inputs are exactly collinear / coplanar / on a boundary. Floating-point
predicates fail two ways:

1. **Numeric error:** a sign computed as `≈0` is rounded the wrong way (an
   `orientation` near a coplanar configuration).
2. **Genuine degeneracy:** the point lies *exactly* on the face boundary, the ray
   lies *exactly* in a face plane — the predicate has no non-arbitrary answer,
   and naive code returns LIMIT / "ambiguous", which the caller then mishandles.

Two standard techniques make predicates robust:

- **Exact / adaptive-precision arithmetic** (e.g. Shewchuk's adaptive predicates)
  removes failure mode (1): the sign is computed exactly, error-free.
- **Symbolic perturbation — Simulation of Simplicity (SoS)** removes failure mode
  (2): every input is conceptually perturbed by a *distinct infinitesimal*
  `ε^i` so that **no degeneracy can occur** — every point is in general position.
  The perturbation is symbolic (never materialised as a finite number), so the
  answer is exact and, crucially, **globally consistent** (the same degenerate
  configuration is always resolved the same way). This is the property a fixed
  finite jitter/tilt lacks: a constant perturbation is non-degenerate for some
  configurations and grazing for others (exactly the §3.2 failure).

### 4.1 Why we want robust predicates here

Every QI failure that survived §2 is a **degeneracy**, not numeric noise:

- the sample point lies on a face boundary (its own silhouette edge) — §2.5;
- the line of sight is coplanar with a face — §3.2(1);
- the ray enters an occluder exactly through a shared edge (LIMIT on both faces) —
  the under-count that forced the jitter in the first place;
- a convex silhouette corner where the "nearest face" is ambiguous — §3.2 core.

Each was patched with a *finite* perturbation (pull-back, perpendicular jitter,
tilt) that helps one configuration and grazes another. A robust predicate layer
resolves **all** of them with a single consistent rule, which is the only way to
get a QI kernel that is correct for **every** orientation of an axis-aligned
solid — and the only stable foundation for restoring incremental propagation
(§3.1), whose ΔQI sign is itself a sign predicate.

---

## 5. Concrete Plan for the Robust QI Kernel

### 5.1 New class: name and location (DECIDED, created)

**Not next to the renderer.** The renderer is a *consumer* of the QI; it keeps
calling `SimpleBody.computeQuantitativeInvisibility` unchanged. The robustness
work lives in a **new public class** at the kernel layer:

> `java/base/src/main/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidPredicates.java`
> `public class PolyhedralBoundedSolidPredicates`

- **Package** `…volume.polyhedralBoundedSolid` (alongside the data structure and
  `PolyhedralBoundedSolidValidationEngine`, which is already a public
  geometric-analysis class living there), **not** the
  `geometricProcessing/polyhedralBoundedSolidOperators` package — that package
  holds *verbs* that transform solids (modeler, set ops, Euler ops). Predicates
  are *queries about* a solid, and the QI kernel already lives in
  `PolyhedralBoundedSolid` in this same package, so this is a natural extraction
  with minimal coupling.
- **Public**, because it is used from outside (at minimum `render/hiddenLine`),
  and reusable by the CSG classifier and validation engine, which have their own
  grazing problems. No leading underscore: everything in this ecosystem aims to
  be robust, so "robust" is not part of the name.
- Owns the robust primitives (`isPointInside`, the ray/boundary crossing count,
  the generic probe directions). It is self-contained today (calls
  `face.getContainingPlane()`); wiring it to the per-frame caches
  (`queryPlaneCache` / `queryFaceAabb`) for performance is a later step.

### 5.1b Progress on the new class (2026-06-14)

- **P1 `isPointInside(solid, point)` — DONE and verified.** Ray-cast parity with
  the SoS-style "adaptive non-grazing probe": cast along a generic direction; if
  any boundary hit is classified `LIMIT` (on a face edge/vertex), that probe
  grazes, so retry the next of seven pre-chosen non-axis-aligned directions.
  Verified against the 12-box ground truth on a 21³ grid: **3375/3375 correct**
  (test `isPointInside_matches12BoxGroundTruth`). This is the robust keystone.
- **P2 `quantitativeInvisibility(solid, eye, point)` — DONE and verified.**
  QI as the number of times the STRAIGHT line of sight (pulled back off the
  surface point) enters the solid INTERIOR. The segment is split at every
  boundary crossing (captured tolerantly) and each interval midpoint is
  classified `INTERIOR` / `OUTSIDE` / `ON_SURFACE`:
  - `ON_SURFACE` (midpoint on a face — the line of sight runs along the surface,
    e.g. coplanar with a face) is decided first and does **not** count;
  - otherwise the robust `isPointInside` decides INTERIOR vs OUTSIDE.

  QI = number of entries into an interior run. The two degeneracies are handled
  WITHOUT perturbing the line of sight (that was the whole problem with the
  jitter/tilt variants below): a tangent line of sight merely touches the
  boundary (neighbouring intervals are OUTSIDE → 0), and a coplanar one runs
  ON_SURFACE (→ 0). The earlier dead-end variants are kept only as a record of
  what does NOT work:

  | variant | apparent disagreements vs coarse march |
  |---|---:|
  | single tilted line of sight | 10732 |
  | + vote over 7 generic tilts | 5964 |
  | + minimum occluder-thickness threshold | 3026 |
  | **straight segment + ON_SURFACE detection (current)** | **0 real** |

  **Validation methodology (important).** A sampling ray-march cannot be a clean
  oracle at a near-tangent line of sight — it cannot tell a measure-zero graze
  from a real thin occluder (the coarse 3000-sample march MISSES ~0.2 mm grazes
  the exact QI correctly catches; a fine 300k-sample march OVER-catches a single
  sample at a true tangent). So the QI is validated against the **inside-run
  DEPTH**: an error is counted only when the depth is unambiguous — clearly
  substantial (> 5e-3, a real occluder ⇒ QI must be > 0) or clearly ~zero
  (< 5e-5, a tangent ⇒ QI must be 0); the in-between band is genuinely ambiguous
  and skipped. Across the full 15°/12-sample featured-object scan this gives
  **0 real errors** (vs the stable kernel's ~16). Guard:
  `PredicatesQiScanTest` (committed at a lighter 30°/8-sample grid, ~10 s).

  **Still NOT wired into the renderer.** Production stays on
  `PolyhedralBoundedSolid.computeQuantitativeInvisibility`; switching to the
  predicate QI is **P3** (below), gated on performance: the predicate QI calls
  `isPointInside` (its own ray cast) at each interval midpoint without the
  per-frame plane/AABB caches, so it is currently much slower per call.

### 5.2 Implementation steps

**P0 — Test scaffold first.** Promote the throwaway diagnostics into a committed,
ground-truth test:
- Featured object: the 12-box `inside()` ground truth + the 24×24 Rz·Rx scan,
  asserting **zero** stable per-edge disagreements (band-skipping true
  transitions). This is the acceptance criterion; today it reports 16.
- SPLIT_TEST_PART_1 and a dense kurlanderBowl spot-check must stay green.
Keep them in `HiddenLineRendererTest` (or a new `…RobustPredicatesTest`).

**P1 — Robust point-in-solid (`inside(q)`), the keystone.**
Implement `_PolyhedralBoundedSolidRobustPredicates.isInside(point)` as ray-cast
parity with **SoS-style tie handling**, not a finite jitter:
- Cast from `q` along a single direction; for each face compute the ray/plane
  intersection and an **exact-sign** point-in-polygon (2D orientation predicates
  in the face's dominant projection).
- When the ray hits a face **edge or vertex** (a degeneracy), resolve it with the
  symbolic rule rather than counting it 0 or 2 times: classify the crossing by
  the sign that SoS would give (consistently break the tie toward one side). The
  standard trick: order the symbolic ε so a hit "on an edge" is counted iff the
  perturbed point is on a chosen side; implement as a deterministic comparison of
  the involved vertex indices.
- This is exact-arithmetic-light: only `orientation` signs need care; coordinates
  here are doubles with feature size ≫ 1e-6, so an adaptive double predicate (or
  even a careful tolerance with consistent tie-break on vertex id) suffices.

**P2 — Robust QI via interval classification** (the §3.2 design, now backed by
P1's robust `inside`):
- `collectRayBoundaryCrossings(eye, d, reach)`: tolerant capture, as drafted.
- For each interval midpoint call `inside(mid)` from **P1** (robust at the
  coplanar-view degeneracy because the inside-test no longer depends on the line
  of sight; its own degeneracies are resolved by SoS, not by a fixed auxiliary
  direction).
- QI = number of outside→inside transitions before `reach`.
- Keep the pull-back so the sample's incident faces are excluded by the `reach`
  cutoff (no special-casing needed once `inside` is robust).
- Re-use `queryPlaneCache` / `queryFaceAabb` for performance; expect ≈ O(k·F) per
  QI with k = crossings (≈2 for visible, a few for hidden), comparable to today.

**P3 — Performance + wire into the renderer — DONE.**
1. Threaded the per-frame caches into the predicates via package-private
   accessors on `PolyhedralBoundedSolid`: `cachedFacePlane(i)` (now
   package-private), `queryToleranceContext()`, `queryRayReachesFace(...)` (ray
   AABB cull) and `queryPointNearFace(...)` (point AABB cull). The predicates use
   these in `collectBoundaryCrossings`, `countBoundaryCrossingsToInfinity` and
   `classifyOnSegment`.
2. `PolyhedralBoundedSolid.computeQuantitativeInvisibility(origin, p)` now
   delegates to `PolyhedralBoundedSolidPredicates.quantitativeInvisibility(this,
   origin, p)`. The old jitter kernel (`countStrictFrontOccluders`,
   `majorityFavoringVisible`) was removed.
3. Full base suite green (**407 tests, 0 failures**); all renderer guards pass,
   including the SPLIT_TEST and featured-object grazing guards (the production
   renderer now uses the robust QI). kurlanderBowl frame ≈ **1.6 s** (was ≈1.2 s
   with the jitter kernel): a 1.3× cost for correctness, to be recovered by P4
   (one seed QI per edge instead of one per sub-segment). The common visible case
   short-circuits to 0 (no boundary crossing).

**P4 — (Optional, after P3) restore incremental propagation** (§3.1): with a
robust QI and a robust `orientation` predicate, the ΔQI **sign** at a contour
crossing becomes a robust sign predicate (which side of the contour's front face
does the edge move to). Seed QI once per edge, propagate ΔQI, drop the
per-sub-segment resampling → the ≈6× fewer QI casts that the 1967 algorithm was
designed to deliver.

### 5.3 Risks / notes

- The featured object's box union *may* leave coincident/internal faces from CSG;
  confirm `F=32` is the clean count and that no interior face exists on a QI ray
  (the exact-ray probe found none — internal faces are **not** the cause; the
  cause is grazing). If a future object does have internal faces, robust
  `inside` (parity) is naturally immune (an internal face is crossed an even
  number of times around any region).
- Keep the change **inside the kernel**; the renderer, `SimpleBody`, and the
  public QI signature stay the same, so all existing guards keep their meaning.
- Do **not** ship any variant that is not net-better than the current stable
  kernel on the full 24×24 featured scan **and** all existing tests. The two
  reverted attempts (16→84, 16→594) are the cautionary baseline.

---

## 6. Status Summary

| Item | Status |
|---|---|
| QI over-count at concave edges (pull-back) | ✅ fixed, guarded |
| Split boundary discarded on dead ΔQI | ✅ fixed, guarded |
| Face front/back orientation (outward normal) | ✅ fixed, guarded |
| Performance (plane cache + AABB cull + optional dump) | ✅ 40× (49 s → 1.2 s) |
| QI counted sample's own incident/boundary faces | ✅ fixed, guarded (19→16) |
| `PolyhedralBoundedSolidPredicates` class created | ✅ public, in `volume/polyhedralBoundedSolid` |
| Robust `isPointInside` (P1) | ✅ done, verified 3375/3375 vs 12-box ground truth |
| Robust `quantitativeInvisibility` (P2) | ✅ done, verified 0 real errors vs depth-banded ground truth (beats stable kernel's ~16) |
| Thread caches + wire predicate QI into renderer (P3) | ✅ done — delegated, all guards green, kurlanderBowl ≈1.6 s |
| Incremental [APPE1967] ΔQI propagation (P4) | ⏳ NEXT — now unblocked; ~1 QI per edge instead of per sub-segment, recovers perf |

Full base suite at this stage: **407 tests, 0 failures.** The production renderer
now uses the robust predicate QI; the convex-corner grazing bugs on axis-aligned
solids are resolved in production. Remaining: P4 (incremental propagation) for
fidelity to [APPE1967] and to recover the ~1.3× frame-time cost.
