# Stage 6 — Spurious Face Subdivisions on Moon Motifs 23/28/33/38

**Date:** 2026-06-12
**Author:** Diagnosis and fix by Claude (Fable 5)
**Baseline commit:** `d6f2ced5` ("PolyhedralBoundedSolid preparation for next fix")
**Continues:** `doc/plan-csg-boolean-fix-stage5.md` (read its §9 execution log first; the
40/40 sweep gate from stage 5 still holds at this baseline)

---

## 1. Original Task Statement (translated from the stage-6 kickoff note)

After stage 5 closed the 40-motif sweep (`ok=40 empty=0 invalid=0 blackFaces=0`),
visual inspection of the Kurlander bowl single-motif results showed a *new* class
of defect, unrelated to the previous connect-ordering failures: for moon motifs
with indices **23, 28, 33 and 38**, some bowl faces are subdivided that must not
be — quadrilaterals broken into triangles, including faces on the **back of the
solid, diametrically opposite** to the moon imprint (screenshots `bug1.png` …
`bug4.png` at the repository root). The task: analyze the boolean pipeline
rigorously, find the reason for the new errors, fix it if ready, and record the
findings in this file.

## 2. Symptom

- `bug1.png`/`bug2.png` (motif 23) and `bug4.png` (motif 33): thin triangle fans
  crossing quads of the bowl wall far away from the crescent hole.
- `bug3.png` (motif 28): same defect seen from the back, plus dense vertex
  clutter near the imprint.
- The defect is *cosmetic-plus*: the results still validate (the stage-5 sweep
  classifies all 40 motifs OK) but the meshes carry spurious non-planar-face
  triangulations.

## 3. Reproduction and Measurement (kernel-level, no renderer needed)

`Stage6FaceSubdivisionDiagnosticTest` (new, `@Tag("slow")`, in
`base/src/test/vsdk/toolkit/processing/polyhedralBoundedSolidOperators/`) runs
`bowl − moon` for motifs 21 (healthy control), 23, 28, 33, 38 and prints the
result face-size histogram, the finisher's `lastTriangulatedFaceCount`, and the
centroid (cylindrical coordinates) of every triangle face.

Measured at baseline `d6f2ced5` (before the fix):

| Motif | Result faces | Faces triangulated in `finish()` | Triangles |
|-------|--------------|----------------------------------|-----------|
| 21 (OK) | 229 | 0 | 32 (all in the bowl bottom cap — legitimate) |
| 23/28/33/38 (all identical) | 242–243 | **14** | **51–53** |

Triangle centroid dump for motif 23 (moon placed at azimuth −135°, z = 0.9)
located the spurious triangles in two patches near the bowl rim:

- azimuth ≈ −120°…−152°, z ≈ 1.0…1.56 — above the moon imprint;
- azimuth ≈ +29°…+60°, z ≈ 1.13…1.56 — **diametrically opposite** the moon.

All four failing motifs share `positionIndex = 3` in the fixture tables:
`MOON_Z_VALUES[3] = 9.0`, `MOON_AZIMUTH_OFFSETS[3] = -45.0` — the same latitude
and the same azimuth phase relative to the bowl mesh, one motif per quadrant.
That is why exactly these four fail: the defect is a geometric coincidence of
this placement, repeated by 90° symmetry.

## 4. Root Cause

Vertex-displacement bisection: comparing the back-region vertices of the
original bowl operand against the same vertices in the result showed exactly one
moved vertex —

```
bowl:   <0.653281, 0.653281, 1.382683>   (outer sphere, az=45°, z=1.38268)
result: <0.653296, 0.653267, 1.382690>   (moved ~2.16e-5)
```

az = 45° is the **exact antipode** of the moon azimuth (−135°), and the
displacement direction is **perpendicular to the moon's cylinder axis** — the
fingerprint of a snap onto one of the moon cylinder's side-face planes.

The mover is the vertex-snap preamble of the edge/face test in
`_PolyhedralBoundedSolidSetIntersector.doSetOpGenerate(...)`:

```java
// Snap vertices in the (epsilon, bigEpsilon] gap onto the face plane.
if ( isZeroBig(d1) && !isZero(d1) ) {
    v1.position = f.getContainingPlane().projectPoint(v1.position);
    d1 = 0.0;
}
```

`doSetOpGenerate` runs for **every edge of A × every face of B** (and vice
versa). The distances `d1`/`d2` are measured against the **infinite containing
plane** of `f`, before any bounded-containment check. A face plane extended to
infinity can pass within `bigEpsilon` of vertices arbitrarily far from the face
itself; the snap then drags those unrelated vertices off their own faces. Four
previously planar bowl quads sharing the antipodal vertex become slightly
non-planar, `finish()`'s `triangulateNonPlanarFaces` ear-clips them, and the
ear-clipping cascade produces the triangle fans of the screenshots. The
moon-side rim patch is the same mechanism via other side planes of the same
cylinder (closer to the motif, more planes pass nearby).

This violates the locality of the Mäntylä chapter-15 Generate phase: [MANT1988]
§15.3 evaluates edge/face crossings against the *face*, not against its
unbounded plane; the snap was a stage-1…4 robustness addition that lacked the
containment guard.

## 5. Fix (generic, no per-motif logic)

In `doSetOpGenerate`, the snap now only applies when the projected vertex
actually lies over the bounded face (`pointInFaceDetailed(...) != OUTSIDE`,
which already uses the `bigEpsilon` containment band):

```java
if ( isZeroBig(d1) && !isZero(d1) ) {
    Vector3Dd snapped1 = f.getContainingPlane().projectPoint(v1.position);
    if ( pointInFaceDetailed(f, snapped1).status() != Geometry.OUTSIDE ) {
        v1.position = snapped1;
        d1 = 0.0;
    }
}
```

Safety argument: if the projection falls OUTSIDE the bounded face, any
edge-plane crossing point near that vertex is also outside the face, so the
subsequent `pointInFaceDetailed(f, p)` containment check would reject the
intersection anyway — the snap could never have contributed a valid
intersection there. The case the snap was originally added for (a borderline
vertex whose edge genuinely crosses inside `f`) projects INSIDE or LIMIT and
keeps the legacy behavior. The fix is therefore strictly a narrowing to the
intended domain, with no new tolerances, flags, or fixture-specific branches.

## 6. Verification

After the fix (same diagnostic):

- Motifs 23/28/33/38: result faces 242→**229**, `finish()` triangulated faces
  14→**0**, histogram identical in shape to the healthy motif 21 result
  (34 triangles, all in the bottom cap; 179 quads).
- The antipodal bowl vertex returns to its exact original position
  `<0.653281, 0.653281, 1.382683>`.
- Motif 21 (control): bit-identical output before/after the fix.

Regression gate (commands per CLAUDE.md):

- `gradle :base:test` — full suite green after the intentional baseline
  re-capture described below (the pre-existing @Disabled/@Tag("slow") set
  stays skipped).
- `gradle :base:test -PincludeSlowTests --tests "*KurlanderBowlMotifSweepRegressionTest*"`
  — sweep stays at `MINIMUM_OK_COUNT=40 / MAXIMUM_FAILURE_COUNT=0`.
- `KurlanderBowlStarInvariantTest` 20/20. All star-motif baselines in
  `KurlanderMotif4OperationMatrixTest` unchanged (stars never entered the
  snap band).
- Visual: offline render of `CSG_DIRECT` motif 23 (`--offline --screenshot`)
  shows clean quads; compare with `bug1.png`.

### 6.1 Intentional baseline changes (moon motifs 21/23 in the 4-op matrix)

The stage-5 `TopologicalSummary` baselines for moons 21 and 23 were captured
with the snap defect present, so they encode the buggy outputs. They were
re-captured with `zzTempPrintMoonBaselines` (added in `d6f2ced5` for exactly
this purpose) and spliced into `expectedMotif21*()`/`expectedMotif23*()`.
What changed and why it is an improvement:

- **Motif 23 A−B:** 242 faces / 511 edges → **229 / 498** — the 13 spurious
  ear-clip faces are gone; the structure is now identical to the healthy
  moons.
- **Motif 21 B−A (moon − bowl):** identical topology counts, but the result's
  bounding box `maxY` moves from −0.54 to **−0.49**. −0.49 is the true
  position of the moon's near cap after the `d6f2ced5` placement change
  (radial start 0.54 − proximity shift 0.05); the cap lies inside the bowl
  cavity and must survive in B−A at its true extent. The pre-fix kernel
  deformed that region; the post-fix value matches the analytic operand
  geometry.
- After the fix, motifs 21 and 23 (different latitude/azimuth placements)
  produce **identical topology summaries for all four operations** — the
  expected congruence for congruent motif placements that only differ by a
  rigid transform relative to the bowl's symmetry.

## 7. Residual Risk and Follow-ups

- The snap guard adds one `pointInFaceDetailed` call per borderline vertex
  (only when `d ∈ (epsilon, bigEpsilon]`), negligible in practice.
- The same "infinite plane vs bounded face" pattern is worth auditing in the
  second Generate snap (the intersection-line snap a few lines below, which
  adjusts the *new* point `p` — already bounded by the later containment check)
  and in `doVertexOnFace` (gated by `compareToZero(d) == 0` at epsilon level —
  much tighter, and followed by an explicit containment check; no action
  needed).
- `Stage6FaceSubdivisionDiagnosticTest` is kept as a slow-tagged diagnostic.
  Its face-histogram probe is the regression detector for this class of defect
  (spurious non-planar triangulation shows up as `triangulated > 0` for any
  bowl−motif case).

## 8. Execution Log

| Date | Step | Result |
|------|------|--------|
| 2026-06-12 | Baseline | Full `:base:test` green at `d6f2ced5`; sweep 40/40. Defect reproduced kernel-only: motifs 23/28/33/38 → 14 faces triangulated in finish, triangle patches near rim at moon azimuth and its antipode. |
| 2026-06-12 | Bisection | Bowl operand has zero faces with planarity residual > 1e-6; result has one displaced vertex at the moon-azimuth antipode (az=45°, z=1.38268, Δ≈2.16e-5 ⊥ moon axis). Mover identified: unconditional vertex snap in `doSetOpGenerate`. |
| 2026-06-12 | Fix | Containment-guarded snap (§5). Motifs 23/28/33/38 now structurally identical to healthy moons; control motif 21 A−B unchanged bit-for-bit. |
| 2026-06-12 | Baselines | First full-suite run after the fix exposed 2 stale moon baselines in `KurlanderMotif4OperationMatrixTest` (motif 23 A−B with the 13 spurious faces; motif 21 B−A with the deformed cap extent — see §6.1). Re-captured via `zzTempPrintMoonBaselines`; 5 of 8 moon literals changed, all toward the analytically correct geometry. Stage-6 diagnostic test upgraded into a regression net (asserts zero non-planar faces, zero ear-clips, untouched antipodal region). |
| 2026-06-12 | Gate | Full `:base:test` green; sweep 40/40 (`-PincludeSlowTests`); star invariant 20/20; 4-op matrix green with re-captured moon baselines; visual render of motif 23 clean. |
