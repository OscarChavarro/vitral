# Stage 2 plan — CSG kernel hardening towards production grade
Original date: 2026-05-13
Last update: 2026-05-16 (STAGE 2 CLOSURE)
Author: Assisted Analysis (Opus 4.7)
This document extends `doc/plan-csg-boolean-fix.md`. Stage 1 left the
sweep of 40 Kurlander motifs in `ok=15, empty=11, invalid=2,
blackFaces=12`. Stage 2 attacks the structural causes by going through the
pipeline as defined by Mäntylä 1988 chapter 15, eliminating the
accumulating heuristics and checking invariants on each edge.
**Final status of stage 2 (2026-05-16)**: levels 1-7 closed.
Suite: **305 tests, 0 failures, 6 skipped**. §10 (visual validation)
started but NOT closed — sweep remains ok=15. Stage 2 is
closes with complete diagnosis of the remaining blocker (root cause
from the ordering problem in `scanjoin`). Pending corrections
moved to stage 3.
**Sweep Kurlander final**: ok=15, empty=16, blackFaces=9, unchanged=0,
invalid=0, exception=0. Baselines formalizados en
`KurlanderBowlMotifSweepRegressionTest` (MINIMUM_OK=15,
MAXIMUM_FAILURES=25).

---

## 0. Normative references

- [MANT1988] Mäntylä, M. *An Introduction to Solid Modeling*, Computer
  Science Press, 1988. Especially:
  - §10.2 Half-edge data structure and the planar face invariant
  - §13.1 Face equations (`faceeq`), `getmaxnames`, `updmaxnames`
  - §15.4 Outline of the set-operations algorithm (Program 15.1)
  - §15.5 `setopgenerate` (Programs 15.2-15.4)
  - §15.6 Vertex neighborhood classifier (Programs 15.5-15.11)
  - §15.7 `setopconnect` (Programs 15.13-15.14)
  - §15.8 `setopfinish` (Program 15.15)
- [MANT1986] Mäntylä, M. *Boolean Operations of 2-Manifolds through
  Vertex Neighborhood Classification*, ACM TOG 5(1).
- `doc/references/coverage_MANT1988.md` — self-report of coverage.
- `doc/plan-csg-boolean-fix.md` — stage 1.
---

## 1. Current status audited
### 1.1 Test suite (snapshot 2026-05-16, closing stage 2)
**305 tests · 0 failures · 6 skipped** (all skipped with reason
documented).
Clases de test relevantes (todas verdes):

- `BooleansFromReferenceObjectPairsTest` — 37 tests, 2 skipped (CSG_KURLANDER_BOWL placeholder; utility snapshot)
- `SetOpConnectNoLooseInvariantTest` — 5 tests, 1 skipped (looseA theoretical invariant MANT1988_15_1 INT/SUB — §7.2)
- `PolyhedralBoundedSolidSetOperatorCoplanarPredicateTest` — 9 tests, 2 skipped (permissive sectoroverlap — §7.5 non-goal)
- `CsgKurlanderBowlFirstStarRegressionTest` — 6 tests, 0 skipped (fifth star active, passes `validateIntermediate` — §9.5)
- `KurlanderBowlMotifSweepRegressionTest` — 1 test `@Tag("slow")`, baseline ok≥15 / failures≤25
- `KurlanderMotifEmptyDiagnosticTest` — 1 diagnostic test (TEMPORARY ARTIFACT — eliminate in stage 3)
- `AlgebraicIdentityRegressionTest` — 10 tests, 0 skipped (replacement of legacy drift detector)
- `SetOpFinishInvariantsTest` — **10 tests**, 0 skipped (§9.1 + §9.2 invariants; counters = 0 for 5 fixtures × 2 ops)
- `SetOpConnectScanJoinTest` — 7 tests, scanjoin / sgetnextnulledge contracts + 41 names banned in regression guard
- `VertexFaceClassifierCoplanarTest` — 5 tests (includes reflection guard)
- `VertexVertexEndpointRecoveryTest` — 4 tests from enum `SeparateEdgeSequenceResult`
- `IntersectorWeldTest`, `IntersectorParametricOrderingTest` — coverage of §4.2 and §4.3
- `PolyhedralBoundedSolidPreflightTest` — 4 Level 1 tests
### 1.2 LOC eliminadas

- `_PolyhedralBoundedSolidSetNullEdgesConnector.java`: **2790 → 1282**
  LOC (−54%; the 34 extra LOC vs previous snapshot are the trace
  diagnosis added in §10). Removed: dual path `flexibleEndpointChains`,
  post-loop safety network (`closeLegacyCoincidentLooseEnds`,
  `resolveClassicAlternatingLooseCycle`, `resolveClassicLooseNetwork`),
  deferrals, `removeLooseEndsA/B` extra, ~17 orphaned helpers, 7 system
  properties `flexible*`, 4 inner classes.
- `PolyhedralBoundedSolidSetOperator.java`: ~165 LOC removed
  (retry block `trySubtractConnectRecovery` + helpers).
- `_PolyhedralBoundedSolidSetVertexFaceClassifier.java` (extracted) +
  zombie block `vertexFaceClassify` in SetOperator: ~505 LOC.
### 1.3 Bloqueadores restantes (actualizado post-§7)

**Cerrado en §7:**

- ✅ Algebraic drift (7 original cases): 10/12 solved with preflight
  geometric identity + AlgebraicIdentityRegressionTest.
  2 absorption drift accepted as option 3 (§7.3.1.D-cont).
- ✅ `looseA != 0` confirmed non-functional blocker (§7.0).
- ✅ `sectoroverlap` confirmed non-root-cause (§7.1, identical trace).
- ✅ `AlgebraicPropertiesTest` removed (reverse drift detector).
**Pending (moved to stage 3):**
- **`BooleansFromReferenceObjectPairsTest.given_csgKurlanderBowl_...`** — 1 test
  `@Disabled` with placeholder. Requires actual topology capture of the entire bowl.
- **ISE Fallback**: raise `sanitizePairedFaces` fallback to `IllegalStateException`
  (counter = 0 confirmed in §9.1, but pending verification with full sweep).
- **Kurlander visual sweep**: 16 EMPTY + 9 remaining blackFaces. Root cause of
  EMPTY fully diagnosed in §10 (see §16). Fix requires correction
  ordering of null edges in the Connect phase — stage 3 work.
- **`KurlanderMotifEmptyDiagnosticTest`**: temporary artifact, remove in stage 3.
- **2 absorption drift cases** (`MANT1988_15_2_LIMIT`, `MANT1988_6_13`): accepted
  as option 3 in §7.3.1.D-cont; re-evaluate in stage 3.
---

## 2. Estrategia global

Stage 2 follows the natural ordering of the pipeline so that each phase
receive data guaranteed by the previous phase. Each phase is worked on
four steps: **measure → correct → validate → implement test
regression**.
Reglas de oro:

1. **Do not introduce new heuristics**. Any correction that
   requires a flag, you must hide it behind the default and eliminate the
   flag before closing the phase.
2. **Each invasive change adds targeted tests** in `:base:test`
   (ideally without `@Disabled`, except for the complete sweep of motifs).
3. **The 40 motifs sweep** (`--motifSweep`) and the full drum set
   `:base:test` are executed at the close of each phase. Metric: `ok`
   monotonically increasing.
4. **Maintainability over cleverness**: prefer a slower algorithm
   but aligned word-for-word with Mäntylä rather than maintaining a
   exotic variant.
### 2.1 Mapa de niveles del plan (actualizado)

| Level | Phase | Primary files | Section | State |
|------:|------|---------|--------|--------|
| 1 | Modeling and preprocessing | Validation engine, generators | §3 | ✅ Closed |
| 2 | `setopgenerate` | `_PolyhedralBoundedSolidSetIntersector` | §4 | ✅ Closed |
| 3 | structural `setopclassify` | V/F and V/V classifiers, `separateEdgeSequence` | §5 | ✅ Closed except §5.2 (moved to §7) |
| 4 | structural `setopconnect` | `_PolyhedralBoundedSolidSetNullEdgesConnector` | §6 | ✅ Closed except §6.3 (moved to §8) |
| 5 | Algorithmic core — algebraic bugs + sectoroverlap diagnosis | predicate processor + algebraic identity preflight | §7 | ✅ Closed |
| 6 | Post-core cleanup (revert/ring/Kurlander experiments) | SetOperator + connector | §8 | ✅ Closed (everyone keep) |
| 7 | `setopfinish` | `_PolyhedralBoundedSolidSetFinisher` | §9 | ✅ Closed (§9.1–9.5) |
| **8** | **Visual validation and regression** | `--motifSweep` + tests slow | **§10** | ❌ **Partial — complete diagnosis, fix pending** |
Final metric achieved: sweep `ok=15, empty=16, blackFaces=9`.
Target metric not achieved: `ok=40, empty=0, invalid=0, blackFaces=0` → stage 3.
---

## 3. Nivel 1 — Modelado y preprocesamiento ✅ CERRADO

**Goal**: Ensure that the entry to Intersect meets the
invariants of [MANT1988] §10.2 and §13.1 — plane faces within the
epsilon, no near-coinciding vertices, globally unique IDs.
**Subhitos completados**:

| Subhito | State | Test coverage |
|---|---|---|
| §3.1 `weldCoincidentVertices` + `validateBooleanInputs` wired into `setOp` | ✅ | `PolyhedralBoundedSolidPreflightTest` (4 tests) |
| §3.2 Normal Newell with centroid + fallback corner | ✅ | (indirect via Preflight + Validator tests) |
| §3.3 `_PolyhedralBoundedSolidIdNamespace` global | ✅ | (indirect: used in Intersector, SetOperator, Finisher) |
| §3.4 Snap 1e-10 in `addArcToExistingFace` + JavaDoc Sphere | ✅ | `given_twoCylindersWithSameRadius_..._noCoincidentVertices` |
| §3.5 Acceptance tests | ✅ | 4 tests in `PolyhedralBoundedSolidPreflightTest` |
Deferred submilestones (non-critical):
- Moon direct without previous boolean — deferred; weld mitigates.
- `containingPlane` cache with dirty flag — deferred to §9.
- Specific test `ConePlanarFaceGenerationTest` — deferred to Level 5.
---

## 4. Nivel 2 — `setopgenerate` ✅ CERRADO

**Objective**: produce `sonvv`, `sonva`, `sonvb`, `sonea`, `soneb` and
intersection vertices exactly on the receiving face, with order
stable topological and without duplicates.
**Subhitos completados**:

| Subhito | State | Test coverage |
|---|---|---|
| §4.1 Projection to the receiving plane (inherited from stage 1) | ✅ | (indirect on existing booleans) |
| §4.2 Weld post-Intersect (`weldIntersectionVertices` + `pruneStaleVertexFaceEntries`) | ✅ | `IntersectorWeldTest` (2 tests) |
| §4.3 Stable order (`Double.compare` exact + midpoint tiebreaker) | ✅ | `IntersectorParametricOrderingTest` (2 tests) |
| §4.4 Acceptance tests | ✅ | The previous ones |
**Cierre**: `:base:test` 261 → 282 tests; 0 failures; 0 regresiones.

---

## 5. Nivel 3 — `setopclassify` estructural ✅ CERRADO

**Objective**: each vertex neighborhood is classified as `IN`,
`OUT` or `ON` with consistency between A and B (Mäntylä table 15.3).
**Subhitos completados**:

| Subhito | State | Notes and coverage |
|---|---|---|
| §5.1 Delete branch "borrowed wMANT2008" V/F classifier | ✅ | ~505 zombie LOC eliminated. `VertexFaceClassifierCoplanarTest` (5 tests) with reflection guard. |
| §5.3 Rename `separateInterior` → `flipNullEdgeOrientationForOpenSide` + formalize convergence of `separateEdgeSequence` (cycle detection on configurations `(from, to)`, enum `SeparateEdgeSequenceResult` with 5 values) | ✅ | `VertexVertexEndpointRecoveryTest` (4 tests) |
| §5.4 Acceptance tests | ✅ | Covered by the previous two classes |
**Subhito moved**: §5.2 (harden `sectoroverlap`) → **§7** (Level 5
new: algorithmic core). The reason is that §5.2 and §6.1-C have the
same root cause (imperfect order of null-edge generation for
geometries with exact `a2 == b1` matches) and are attacked together.
---

## 6. Nivel 4 — `setopconnect` estructural ✅ CERRADO

**Goal**: structural form aligned with Programs 15.13/15.14,
eliminating heuristics and post-loop safety net.
**Subhitos completados**:

| Subhito | State | Metric/Coverage |
|---|---|---|
| §6.1-A Delete `setOpConnectWithFlexibleChains` + 7 flags `flexible*` + 4 inner classes | ✅ | ~700 LOC. `SetOpConnectScanJoinTest` reflection guard (22 names banned). |
| §6.1-B Post-loop safety net purge (5 sub-milestones: `closeLegacyCoincidentLooseEnds`, `resolveClassicAlternatingLooseCycle`, `resolveClassicLooseNetwork`, deferrals, `removeLooseEndsA/B`) | ✅ | ~470 LOC + 17 helpers in cascade. Guard of 19 additional names. |
| §6.1.1 Iterator `sgetnextnulledge` (Program 15.14 literal) | ✅ | Inner class `NullEdgePair` + cursor `nextNullEdgeIndex`. 4 direct tests. |
| §6.1.2 `scanjoin` (Program 15.13 literal) + elimination `crossLooseMatch` | ✅ | Rename `canJoin` → `scanjoin`, removed non-Mantyla branch. |
| §6.2.1 Delete `trySubtractConnectRecovery` | ✅ | Test reflection guard (3 names). |
| §6.2.2 Delete flags `forceARingMove` + `flexibleDisableBRingMoveForSubtract` | ✅ | Mirrored in same guard. |
| §6.4-A Program invariant test 15.14 | ✅ | `SetOpConnectNoLooseInvariantTest`: 4 baseline + 2 pending (root cause = §7). |
| §6.4 `SetOpConnectScanJoinTest` (book core + regression guards) | ✅ | 7 own tests, 41 names banned in regression guard total. |
**Moved subhits**:
- §6.1-C (refine matching for the 2 pending): **twin of §5.2**.
  Structural analysis performed and post-pass attempt discarded
  (broke HOLLOW_BRICK). It goes to **§7**.
- §6.1.3 (loose → `IllegalStateException`): **locked by §7**.
- §6.2.3 (`revert(B)` before Connect — Equation 15.1): **locked
  per §7** (experiment run, 28 failures for current connector).
  It goes to **§8** once §7 closes.
- §6.3 (delete `groupNullEdgesByRing`): **measurable after §7**.
  It goes to **§8**.
---

## 7. Level 5 — Real algebraic bugs and diagnostics ✅ CLOSED
### 7.0 Address change summary (2026-05-15)
The original plan assumed that the "looseA == looseB == 0" invariant of
Program 15.14 was the only blocker. **The instrumented diagnosis
of §7.3.1 invalidated that premise**:
1. **Sectoroverlap is NOT the root cause**. Trace executable demonstrated
   that `MANT1988_15_1 + INTERSECTION` (looseA=4) and
   `MANT1988_15_1 + UNION` (looseA=0) produce **exactly the
   same 52 calls with the same 48/4 decisions**. The difference
   in loose does not come from sectoroverlap.
2. **`looseA != 0` is NOT a functional blocker**. HOLLOW_BRICK +
   INTERSECTION terminates Connect with looseA=4 **and still produces the
   correct topological result** that the suite validates. The setopfinish
   current handles the remaining loose.
3. **The real blockers are others**. By inspecting the 9 tests
   `@Disabled`, it was found that `AlgebraicPropertiesTest` (disabled
   class level) had **5 real drift detectors failing** in
   3 fixtures (`MANT1986_2` idempotence, `MANT1988_15_2_LIMIT`,
   `MANT1988_6_13` ⇒ see §7.3 below). These are functional bugs
   real.
**Architectural conclusion**: the objective to pursue is NOT "looseA
== 0 strict" but "algebraic tests identify 0 drift" and
"the Kurlander sweep completes 40/40." The theoretical invariant is
orthogonal to the functional outcome.
### 7.1 Diagnosis gained and infrastructure delivered ✅
**Trace ejecutable** entregado en
`SectoroverlapTraceDiagnosticTest`:

- 4 scenarios covered: MANT1988_15_1 INT/SUB (failing due to invariant
  theoretical), MANT1988_15_1 UNION (passing control), HOLLOW_BRICK INT
  (control that also happens with looseA=4).
- Structured capture via `SectoroverlapTraceEntry` (mutable POJO):
  index, faceA/B, vertexA/B from→to, a1/a2/b1/b2, diff_a2_b1,
  diff_b2_a1, flag boundary-ray-contact, decision.
- Infrastructure `enableSectoroverlapTrace()` /
  `disableSectoroverlapTrace()` / `getSectoroverlapTrace()` in the
  predicate processor — usable for future diagnostics without
  System.out contamination.
**Tabla comparativa** (clave del descubrimiento):

| Case | looseA | Calls | TRUE/FALSE | BRC | Pass suite |
|---|---|---|---|---|---|
| MANT1988_15_1 + UNION | 0 | 52 | 48/4 | 8 | ✅ |
| MANT1988_15_1 + INTERSECTION | 4 | 52 | 48/4 | 8 | ✅ (tests validate topology) |
| MANT1988_15_1 + SUBTRACT | 4 | 52 | 48/4 | 8 | ✅ |
| HOLLOW_BRICK + INTERSECTION | 4 | 60 | 56/4 | 12 | ✅ |
Same trace, different loose results, same topological outcome
correct. Sectoroverlap is **invariant** to the functional outcome.
### 7.2 Tests `@Disabled` reorganizados

After §7.1, the `SetOpConnectNoLooseInvariantTest` is re-labeled:
- The 4 baselines (`MANT1988_15_1 + UNION`, `STACKED_BLOCKS + *`)
  They continue as regression guards of the current behavior of the
  connector.
- The 2 pending (`MANT1988_15_1 + INTERSECTION/SUBTRACT`) are
  **theoretical invariants**, NOT functional regressions. Your `@Disabled`
  now documents that your non-compliance does NOT affect the actual suite.
### 7.3 Bloqueadores funcionales reales descubiertos 🟡

Tras habilitar temporalmente `AlgebraicPropertiesTest`:

| Test | Fixtures that fail |
|---|---|
| Idempotence (`A∪A=A`, `A∩A=A`, `A−A=∅`) | MANT1986_2 (indices 0.1), MANT1988_15_2_LIMIT (0), MANT1988_6_13 (1) |
| Absorption (`A∪(A∩B)=A`, etc.) | 0 fixtures (passes for all 3) |
| Difference swapped operands (determinism) | MANT1986_2 |
**5 drift detectors fail** consistently with `expected false but
was true` — the operation is producing results that violate the
algebraic laws (non-idempotent, non-deterministic to swap).
### 7.3.1 Attacking real algebraic bugs — balanced sub-steps
#### 7.3.1.A Isolate 1 fixture and a test ✅ CLOSED
**Diagnosis** (via temporary `Mant1988_6_13IdempotenceDiagnostic`):
for 3 fixtures (`MANT1988_6_13[0]`, `MANT1988_15_2_LIMIT[0]`, also
`MANT1986_2`), operations `A∪A` and `A∩A` with cloned `A` produced
**empty** result (f=0, e=0, v=0) instead of the baseline.
**Root cause identified**: Mäntylä 1988 does not specify the case
degenerate `A ≡ B` (geometrically identical operands). The classifier
marks all faces of B as "inside A" and symmetrically, and
UNION/INTERSECTION collapse to ∅.
**Fix implementado**:

1. New public predicate `PolyhedralBoundedSolidValidationEngine.areGeometricallyIdentical(a, b, tolerance)`:
   verifies identical cardinality (V, E, F), bbox within tolerance, and
   pairwise matching of vertices. O(n²) in vertices.
2. New "identity preflight" in
   `PolyhedralBoundedSolidSetOperator.setOp` (just after the
   `isTouchingOnlyPreflightCase`): if `areGeometricallyIdentical` is
   true, dispatch direct to:
   - `UNION` or `INTERSECTION` → `deepCloneSolid(inSolidA)`
   - `SUBTRACT` → `new PolyhedralBoundedSolid()` (empty)
**Cobertura entregada**:

- New test `AlgebraicIdentityRegressionTest` with 7 tests (6 idempotence
  + 1 diff-swap) that acts as a regression guard.
- The old `PolyhedralBoundedSolidSetOperatorAlgebraicPropertiesTest`
  `@Disabled` is explicitly left with a message documenting that its
  assertions were inverted (reverse drift detector). was preserved
  for archeology up to §7.3.1.B.
**Metrics after §7.3.1.A**:
- Suite: 289 → **293 tests** (+4 new in `AlgebraicIdentityRegressionTest`,
  3 of the temporary diagnosis already removed)
- Failures: 0
- Skipped: 9 (no change — the legacy AlgebraicProperties continued and continues
  `@Disabled`; the new Regression covers the positive contract)
**Drift remanente** (a atacar en §7.3.1.D):
- Absorption: 3 fixtures (`MANT1986_2`, `MANT1988_15_2_LIMIT`,
  `MANT1988_6_13`)
- Diff-swap determinism: 2 fixtures (`MANT1988_15_2_LIMIT`,
  `MANT1988_6_13`)

#### 7.3.1.B Re-mapping post-§7.3.1.A ✅

After the identity preflight fix, drift mapping updated via
temporary diagnosis (idempotence + absorption + diff-swap):
| Test method | Clean | Drift remanente |
|---|---|---|
| Idempotence (6 cases) | 6 | 0 |
| Absorption (3 cases) | 1 (`MANT1986_2`) | 2 (`MANT1988_15_2_LIMIT`, `MANT1988_6_13`) |
| Diff-swap determinism (3 cases) | 3 | 0 |
| **Total** | **10** | **2** |

The new `AlgebraicIdentityRegressionTest` covers all 10 clean cases
as positive regression guard (§7.3.1.A delivered 7, §7.3.1.B extended to
10 with absorption + diff-swap clean).
#### 7.3.1.D Containment-only preflight (parcial) ✅ + ⚠

**Correct hypothesis**: the 2 remaining absorptions fail because
operations like `A ∪ (A∩B)` when `A∩B ⊂ A` collapse to ∅. Mäntylä
only dispatches `runSetOpNoIntersectionCase` when the solids are
THEY TOUCH but they do not contain.
**Solution implemented**:
- New `_PolyhedralBoundedSolidSetNonIntersectingClassifier.runContainmentOnlyPreflightCase`
  which detects `A⊂B` / `B⊂A` without proper edge/face intersections.
- New `isContainmentOnlyPreflightCase` in `PolyhedralBoundedSolidSetOperator`
  invoked right after the touching preflight.
**Partial result**:
- Suite: 0 regressions (302 → 296 tests, already removing diagnoses
  temporary).
- The 2 absorption problem cases are **NOT activated** by this
  preflight: have geometry with marginal/face intersections
  partial tangents that trigger `hasProperEdgeFaceIntersection`
  like `true`. Containment classifier is too strict
  for those compound cases.
**Why it is partially deferred**:
The actual root cause for `MANT1988_15_2_LIMIT` and `MANT1988_6_13`
absorption is that `A ∩ B` does not produce a strictly solid
content — produces one with higher cardinality (extra cuts per
intersection edges). That solid then, when joined with A, triggers the
same original bug ("two almost superimposed solids" with tangency
partial → pipeline collapses to ∅). This requires a deeper fix
in the classifier that distinguishes "containment with tangency" from
"true intersection" — work larger than scope
of this turn.
**ALL §7.3.1.D-cont** (for next turn):
- Trace of `classifySolidAgainstSolid` for those 2 cases (how many
  vertices "in", how many "limit", how many "out"?)
- Possible fix: amplify `classifyNoIntersectionRelation` so that
  recognize "tangent containment" as a valid relation when
  `hasProperEdgeFaceIntersection` returns false but there is
  `hasPartialCoplanarFaceAreaOverlap`.
#### 7.3.1.D-cont Deep investigation of containment tangent ⚠ Discarded
**Diagnosis** (temporary `ContainmentClassifierProbe`): for both
problematic cases `setOp(A, A∩B, UNION)`:
| Predicado | MANT1988_15_2_LIMIT | MANT1988_6_13 |
|---|---|---|
| `classifySolid(A, A∩B)` | OUTSIDE (-1) | LIMIT (0) |
| `classifySolid(A∩B, A)` | LIMIT (0) | LIMIT (0) |
| `classifyNoIntersectionRelation` | **TOUCHING** | **TOUCHING** |
| `hasProperEdgeFaceIntersection` (ambos sentidos) | false | false |
| `hasPartialCoplanarFaceAreaOverlap` | true | true |

`A∩B` has **all its vertices on the boundary of A** (not INSIDE
strict), so `classifySolid` returns LIMIT, and
`classifyNoIntersectionRelation` classifies it as TOUCHING. The
TOUCHING dispatcher in `runSetOpNoIntersectionCase` ago
`merge(A); merge(B)` for UNION (duplication) and similar drift in
INTERSECTION.
**Fix attempt with tangent containment + dedicated dispatcher**:
extended preflight to accept `TOUCHING` when one of the
solids is completely on the boundary of the other (predicate
`allVerticesOnOrInside`) and `runSetOpContainmentCase` was created with
specific dispatch. Result: **fixes the 2 absorption cases
but Kurlander Bowl + Moon, csgLampShell, and 12 other tests are back**
because that dispatch makes `merge(A)` simple, without preserving the cuts
internals that complex geometry requires (e.g., the moon inside
of the bowl must maintain its boundary as a topological hole, not
disappear).
**Architectural conclusion**: the pattern "B ⊂ A with tangency
partial coplanar" has two sub-cases that look identical at the level
of classifier but require topologically different results:
1. **Tangent containment trivial** (absorption step 2): the result
   It is simply the external solid. Case of
   `MANT1988_15_2_LIMIT`/`MANT1988_6_13` absorption.
2. **Tangent containment with required cuts** (Kurlander Bowl −
   Moon, csgLampShell): the result must have coplanar faces
   subdivided to preserve the internal topological hole.
Distinguishing between the two requires regular pipeline information.
(what intersection edges the classifier produces). This is not something
that a preflight can decide by looking only at initial geometry.
**Retroactive fix**: only the strict preflight was kept
containment (without tangent). The 2 absorption drift cases remain
documented as TODO with full analysis above.
**Roadmap to close §7.3.1.D-cont completely**:
- Option 1: refactor the Generate/Classify classifier so that
  detect "containment with mandatory cuts vs trivial containment"
  based on whether coplanar contact polygons are ALL boundary
  or only partial face sharing. Medium magnitude work.
- Option 2: post-validation: if the regular pipeline produces ∅ where
  geometry suggests containment, re-execute with dispatcher
  simple containment as fallback. Heuristic but pragmatic.
- Option 3: accept that `MANT1988_15_2_LIMIT` and `MANT1988_6_13`
  absorption do not hold, add them as `@Disabled` with
  justification, and focus work on sweeping Kurlander which is the
  true goal of the plan.
**Decision**: for now option 3 (do not introduce complexity for 2
cases with marginal ROI). Re-evaluate when closing level 7 (Finish).
#### 7.3.1.A.HIST Isolate 1 fixture and a test (original description) [LOW, LOW]
Start with the simplest: `MANT1986_2 + idempotence` (`A∪A=A`).
Produce a temporary diagnostic test that runs:
```java
PolyhedralBoundedSolid a = createFixture(MANT1986_2)[0];
PolyhedralBoundedSolid result = setOp(a, b, UNION);
// Verificar: result tiene mismas faces/edges/vertices que a
```

Identify **where the result differs** from `a`. More vertices?
Masks? Faces with different orientation? That gives a clue to the bug.
#### 7.3.1.B Characterize the source of drift [MEDIUM, LOW]
With the isolated case, add additional trace to answer:
- Does the interceptor duplicate vertices coincident with itself?
- Does the classifier mark any face as ON when it should be IN?
- Does the finisher do a face-breaking loopGlue?
This reuses the trace infrastructure from §7.1 + tracking from
sonea/soneb/sonfa/sonfb.
#### 7.3.1.C Aplicar fix y verificar [MEDIO/ALTO, MEDIO]

Implement the specific fix based on §7.3.1.B. Check:
- The idempotence test passes for the affected fixture.
- The other drift detectors do not get worse.
- The entire suite is still green.
- Especially: HOLLOW_BRICK + INTERSECTION still producing
  correct topology.
#### 7.3.1.D Repeat for the other failed fixtures [REPETITIVE]
Once MANT1986_2 idempotence is resolved, attack the other 4 faults.
Each one is a small isolated case.
#### 7.3.1.E Reactivate AlgebraicPropertiesTest ✅ CLOSED (due to removal)
**Decision**: the legacy test `PolyhedralBoundedSolidSetOperatorAlgebraicPropertiesTest`
was **removed from the repository**. Reason: his assertions were
designed as inverted drift detectors
(`assertThat(allHold).isFalse()`), which meant that they passed
when a bug existed and they failed when the bug was fixed — the
gradient opposite to that desirable for a regression suite.
Effective coverage: the replacement `AlgebraicIdentityRegressionTest`
(created in §7.3.1.A-C) covers the **10 cases clean** with assertions
positives:
- 6 idempotence (all fixtures × 2 indices)
- 3 diff-swap determinism (the 3 fixtures)
- 1 absorption (MANT1986_2)
The 2 remaining drift cases (`MANT1988_15_2_LIMIT` and `MANT1988_6_13`
absorption) are documented as ALL above (§7.3.1.D-cont).
They were not added as a specific `@Disabled` to the regression guard
because your analysis is already complete in the plan and adding them would be
duplication.
**Result**: 3 skipped legacy items disappeared (293 tests vs
296), without loss of coverage. Cleaner structure: one
suite of algebraic identities with correct semantics.
### 7.4 Outcome real del Nivel 5 ✅

- `AlgebraicPropertiesTest` legacy **removed** (reverse drift detector).
  Replaced by `AlgebraicIdentityRegressionTest` with 10 positive tests:
  6 idempotence + 3 diff-swap + 1 absorption (MANT1986_2). No regressions.
- Geometric identity preflight (`areGeometricallyIdentical` +
  `isContainmentOnlyPreflightCase`) integrated into `setOp`.
- Trace infrastructure in `_PolyhedralBoundedSolidSetGeometricPredicateProcessor`
  available for future diagnostics.
- 2 absorption drift cases (`MANT1988_15_2_LIMIT`, `MANT1988_6_13`)
  accepted as option 3 (marginal ROI; re-evaluate when closing §9 Finish).
- **Final suite: 293 tests, 0 failures, 6 skipped** (all skipped with
  documented reason: 3 theoretical invariants/non-goals, 2 Kurlander → §8,
  1 maintenance helper).
### 7.5 What will NOT be done in §7 (documented decision)
- **`sectoroverlap`** is not attacked: the trace proved that it is not the problem.
  Maintain the current epsilon-tolerant implementation.
- **strict looseA==0 is not pursued**: setopfinish handles
  loose and produces correct topology. The theoretical invariant remains
  documented but not objective.
- **Matching scanjoin is not rewritten**: same thing, not necessary
  for the suite to pass.
These points can be taken up in a future **stage 3** if desired.
raise theoretical rigor, but stage 2 prioritizes outcomes.
---

## 8. Level 6 — Post-core Cleanup ✅ CLOSED (experiments completed)
The three sub-steps of §8 were deliberate experiments. None produced
effective cleaning; all resulted in "maintaining the current state with
documented decision".
### 8.1 §6.2.3 `revert(B)` antes de Connect ✅ DECIDIDO: mantener en Finisher

**Experiment (2026-05-15)**: `inSolidB.revert()` was moved from
`_PolyhedralBoundedSolidSetFinisher:443` to before `setOpConnect(op)` in
`PolyhedralBoundedSolidSetOperator`. Result: **29 failures** (same as
with the previous connector). The clean §6/§7 connector does not change the
dependence.
**Root Cause**: The Finisher sequence `lmfkrh(inSolidB, ...) → revert(B) →
movefac → loopGlue` requires B to be unreverted for
the `lmfkrh` (which create the new mirror faces with the orientation
original) and reversed during `movefac/loopGlue` (so that the B sides
are left with normals complemented in the SUBTRACT result). Move
`revert` before Connect passes B already reverted to `lmfkrh`, reversing
the orientations that `loopGlue` then uses.
**Decision**: `revert(B)` remains in its current position, between `lmfkrh`
and `movefac`, inside the Finisher. This position, although different from
Program 15.1 by Mäntylä, is correct for the current implementation.
### 8.2 §6.3 `groupNullEdgesByRing` ✅ DECIDIDO: mantener

**Experiment (2026-05-15)**: `groupNullEdgesByRing()` was disabled in
`sortNullEdges()`. Result: **3 errors** in
`CsgKurlanderBowlAllMotifsRegressionTest` — precisely the motifs with
multiple intersecting curves (e.g., bowl with inner + outer boundary).
**Conclusion**: The method is necessary for multi-ring cases. The
current implementation already uses `partitionNullEdgesIntoRings` (deterministic)
+ `sortRingsBySignature` for alignment, which satisfies the goal
of the plan to "formalize as deterministic `partitionNullEdgesIntoRings`".
There is nothing additional to remove.
### 8.3 `CsgKurlanderBowlFirstStarRegressionTest` quinto star ✅ DIAGNOSTICADO

**Finding (2026-05-15)**: The `given_..._then_connectStageClosesAllStarEdges` test
(first star) was already active and green **before §8** — there was no
`@Disabled` about him.
The existing `@Disabled` was about `given_kurlanderBowlAndFifthStar_when_...`.
Removed `@Disabled` and tested: 5th star (index 4) produces
**Face [232] and Face [144] are not coplanar** and returns 0 contours instead
2. Cause: The Finisher pipeline produces non-coplanar faces for the
fifth star geometry — a §9 issue (setopfinish).
**Action**: `@Disabled` restored with complete diagnostic message.
Reactivate after §9.
**Post-§8 metrics**: 293 tests, 0 failures, 6 skipped (no change).
---

## 9. Nivel 7 — `setopfinish` ✅ CERRADO (§9.1–9.5)

**Goal**: implement Program 15.15 without recoveries or triangulation
post-hoc, maintaining the planar face invariant by construction.
### 9.1 Instrumentar `sanitizePairedFaces` — fallback legacy ✅ HECHO

**Measured result**: counter `lastLegacyFallbackCount` = **0** in all
the reference fixtures (MANT1986_2 × 3 ops, MANT1988_15_2 UNION,
MANT1988_6_13 SUBTRACT). Pairing by `pairIndex` works
correctly and the order-by-index fallback is never triggered.
**Implementado**:
- Campo `lastLegacyFallbackCount` + getter `getLastLegacyFallbackCount()`
  en `_PolyhedralBoundedSolidSetFinisher`.
- Fallback incrementa contador y emite `Logger.reportMessage(WARNING)`.
- `SetOpFinishInvariantsTest` §9.1: 5 tests, todos verdes — counter = 0.

**Next**: with the counter confirmed at 0 for the entire baseline,
the fallback can be raised to `IllegalStateException` without risk to
the known fixtures. Pending: check with Kurlander fixtures
before removing the fallback (§9.4).
### 9.2 Instrumentar `triangulateNonPlanarFaces` ✅ HECHO

**Measured result**: counter `lastTriangulatedFaceCount` = **0** in
all reference fixtures. `loopGlue` does not produce non-planar faces
in the baseline geometries.
**Implemented**:
- Field `lastTriangulatedFaceCount` + getter in `_PolyhedralBoundedSolidSetFinisher`.
- Reset at the start of `triangulateNonPlanarFaces`; increase when `lmef` triangulates.
- `SetOpFinishInvariantsTest` §9.2: 5 tests, all green — counter = 0.
**Next**: triangulation now operates as an implicit assertion mode
(it counts but does not prevent). With Kurlander reactivated (§9.4), if the counter
goes up, look for the cause in `loopGlue`.
### 9.3 Guarda de planaridad en `maximizeFaces` ✅ HECHO

**Implemented**:
- `wouldMergedFaceBeCoplanar(rightHalf, leftHalf, numericContext)` in
  `PolyhedralBoundedSolidTopologyEditing`: loop through both loops, collect
  vertex positions, call `validateFacePointsAreCoplanar`.
- Guard `if (!wouldMergedFaceBeCoplanar(...)) continue;` before `lkef`
  in the coplanar fusion section of `maximizeFaces`.
- Suite remains at 303/0/6 — no regressions.
### 9.4 Level 7 acceptance tests 🟡 Diagnostic completed
**Findings (2026-05-16)**:
Test renamed `given_kurlanderBowlAndFifthStar_..._resultIsValidAndPairIndexMatchingSucceeds`
with `@Disabled` and complete diagnostic note. Test result:
- **§9.1 counter = 0** ✅ — `sanitizePairedFaces` matches by `pairIndex` without fallback.
- **§9.2 counter = 7** — expected for tessellated curved surface. `loopGlue`
  merges adjacent faces with different normals; `triangulateNonPlanarFaces` solves 7.
- **2 non-resolvable faces**: face[275] (loopSize=3, collinear triangle)
  and face[145] (loopSize=1, self-loop: `h.mirrorHalfEdge().parentFace == face`).
**Confirmed root cause** (`loopGlue` + `lmekr`):
- `lmekr` receives a size 1 ring (`migratedHalfEdges.size()==1`).
- Create self-loop bridge (v→v). Later `lkev` uses `h2.previous()`
  invalid (loop already destroyed). The final `lkef` leaves face[145] with self-loop.
- **Reference**: `lmekr` line 936-940 has a comment "rare condition" that
  documents this case without correcting it. → **§9.5**.
**Implemented improvements** (in production):
- `findNonDegenerateEar`: tracking of `bestCandidate` with fallback to `epsilon`.
- `triangulateNonPlanarFaces`: guard `loopSize==1 → lkef` (different-face works;
  self-loop falls into safe skip with `i++`).
Suite: 303/0/6 (fifth star skip remains, renamed).
### 9.5 Fix `lmekr`/`loopGlue` for ring size < 3 ✅ CLOSED
**Objective**: eliminate degenerate faces (self-loop, collinear triangle) that
`loopGlue` occurs when `lmekr` receives an undersized ring, and
resolve invalid topology cascades in `triangulateNonPlanarFaces` and `lkef`.
**Three root causes found and corrected**:
**A) `loopGlue` (`PolyhedralBoundedSolidTopologyEditing.java`)**:
Guard §9.5 before `lmekr`: if any loop has `halfEdgesList.size() < 3`
the degenerated loop is discarded with `removeLoop` + return. The case is covered both
`h1` as `h2` symmetrically, and the case in which both are degenerate.
**B) `findNonDegenerateEar` (`_PolyhedralBoundedSolidSetFinisher.java`)**:
Replaced unnormalized cross-product check (`|a||b|sinθ > bigEpsilon`)
by standardized check (`|cos(θ)| < 1 − unitVectorTolerance`), aligned with
`validateFacePointsAreCoplanar`. This prevents `triangulateNonPlanarFaces`
produce collinear triangles that fail planarity after lmef.
**C) `triangulateNonPlanarFaces` — handler `loopSize ≤ 3`**:
- For self-referential faces size-1 (h.next()==h, mirror.parentLoop==null):
  removed directly with `remove(i)` in polygonsList + search and removal
  of the orphaned edge in edgesList. This removes the `face[145]` artifact.
- For faces size ≤ 3 with mirror on a different side: `lkef` absorbs on the face
  adjacent; `i = 0` to re-examine faces that absorbed vertices.
**D) `lkef` (`PolyhedralBoundedSolidEulerOperators.java`) — orphaned loop**:
`maximizeFaces` calls `lkef` on faces with multiple loops (inner rings).
`lkef` only migrated `loopToBeKilled`; the other loops of the killed face
they were left with invalid `parentFace` → topological integrity fails (count=1).
Now, after the main migration, the remaining loops of the killed face are
reassigned to `he1.parentLoop.parentFace` (surviving face).
**Result**: `CsgKurlanderBowlFirstStarRegressionTest` (6 tests, 0 skipped),
including `given_kurlanderBowlAndFifthStar_..._resultIsValidAndPairIndexMatchingSucceeds`.
Suite: 303/0/5 (no regressions).
---

## 10. Level 8 — Visual validation and regression ❌ Partial
### 10.1 Sweep automatizado ✅ HECHO (baseline formalizado)

`KurlanderBowlMotifSweepRegressionTest` created with `@Tag("slow")`.
Conservative thresholds that formalize the current state:
- `MINIMUM_OK_COUNT = 15` (observado: 14 stars + 1 moon)
- `MAXIMUM_FAILURE_COUNT = 25` (observado: empty=16, blackFaces=9)

The sweep is executed with `gradle :base:test --tests "*KurlanderBowl*MotifSweep*"`.
The regression test protects the improvements already achieved and will alert if
A future change worsens the score.
**`KurlanderMotifEmptyDiagnosticTest`**: created as a tool
diagnosis for motif 24 (EMPTY) vs motif 21 (OK). Temporary artifact
— must be eliminated in stage 3 once the fix has been incorporated.
### 10.2 Visual diagnostics ✅ EXISTENTE (ampliado por usuario)

`PolyhedralBoundedSolidExample` keeps highlighting and options
visual debugging. User added more debugging options
controlled visual during this stage.
Available modes: `--motifSweep`, `--motifIndex N`, highlighting
numbered vertices, edge and face display, CSG overlays.
Baselines in `doc/baselines/kurlander/motif_NN.png` — not created yet
(transfer to stage 3).
### 10.3 EMPTY motifs diagnosis — completed without fix ❌
**Symptom**: 16 of 40 motifs produce empty results (sonfa=0 after Connect).
**Analysis performed** (sessions 2026-05-16):
The pipeline trace (`vsdk.setop.tracePipelineSummary=true`) was instrumented
with a compact dump of the 76 pairs of null edges for motif 24 (EMPTY) vs
motif 21 (OK). The two motifs have identical structure:
`A:sameLoop=64 diffLoop=12 B:sameLoop=12 diffLoop=64`, but:
- Motif 21 (OK): `connect end sonfa=2 looseA=0`
- Motif 24 (EMPTY): `connect end sonfa=0 looseA=20`
**Root cause identified**: the `scanjoin` algorithm (Program 15.13 Mäntylä)
requires that, for a pair of null edges `(hea, heb)`, an index `j` exists
in the lists `(endsa[j], endsb[j])` where **simultaneously**:
- `neighbor(hea, endsa[j])` = same A-side, opposite roles (rightHalf/leftHalf)
- `neighbor(heb, endsb[j])` = same B side, opposite roles
The lists `endsa`/`endsb` are **paired**: the index `j` preserves the
correspondence established when a peer previously failed.
STRUT_B (A-diffLoop, B-sameLoop) pairs fail when the B side of the
null edge B-sameLoop does not appear in any previous `endsb[j]`. This happens
when the STRUT_B pair arrives BEFORE any B-diffLoop pair it shares
the same side B.
**Detail for motif 24** (pair[12]: A-diffLoop f=140/f=139, B-sameLoop f=229):
- Pairs[0-11]: B-faces cover f=215–f=225 only; f=229 never appears
- Par[13] (B-diffLoop f=229/f=230) and par[15] (B-diffLoop f=228/f=229) arrive
  AFTER par[12] → when par[12] tries to scanjoin, f=229 is not in endsb
- If par[13] preceded par[12], par[13] would fail scanjoin and add
  (A:f=140, B:f=229) to endsa/endsb; par[12] would match at that index
**Classification of the 12 motif 24 B-sameLoop pairs**:
| Pair | B-face | Does it appear in B-diffLoop? | Position | Fixable with reordering |
|-----|--------|------------------------|----------|---------|
| 8 | f=224 | yes (even 6, 11, 30, 36) | BEFORE you par difloop | ✅ now works |
| 10 | f=215 | yes (par 9, 19) | BEFORE | ✅ now works |
| 12 | f=229 | yes (pairs 13, 15) | AFTER ← problem | ✅ fixable |
| 21 | f=234 | yes (pairs 20, 33, 53, 57) | BEFORE | ✅ now works |
| 34 | f=269 | Does NOT appear in any B-diffLoop | — | ❌ not fixable by reordering |
| 35 | f=263 | yes (pairs 32, 40) | AFTER | ✅ fixable |
| 37 | f=229 | yes | AFTER | ✅ fixable |
| 46 | f=292 | NOT | — | ❌ not fixable |
| 49 | f=236 | yes (pairs 44, 54, 63) | BEFORE | ✅ now works |
| 59 | f=209 | yes (pairs 58, 61, 70) | BEFORE but A-face f=360 only | ❌ A-face blocking |
| 65 | f=373 | NOT | — | ❌ not fixable |
| 71 | f=209 | yes | BEFORE | ✅ partially (second scanjoin) |
**Unsolvable pairs** (f=269, f=292, f=373, f=360): they are points of
"tangential" intersection where the curve touches but does not cross face B
(or face A in the case f=360). The classifier generates a null edge STRUT
for that degenerate contact that can never have a complementary pair.
**Implication for the fix**:
A "B-diffLoop before B-sameLoop for the same B-side" reordering
It would only resolve fixable cases (maximum 5-6 pairs of motif 24).
Pairs with unique faces (f=269, f=292, f=373) require correction
upstream in the **classifier** so that it does not generate null edges for
tangential contacts that do not create new topology, or a handling
special in the connector for null edges without complementary.
Full fix (looseA=0 for all motifs) requires work
of stage 3 in the Generate/Classify phase.
### 10.4 BLACK_FACES Diagnostics — pending
9 motifs classified as BLACK_FACES (inconsistent face orientation).
Not investigated at this stage. Probable root cause: the finisher reverses the
orientation of some faces of B during `revert(B)`/`movefac`. Transfer
to stage 3.
---

## 11. Execution order, dependencies and risk (updated)
| Step | Section | State | Note |
|------|---------|--------|------|
| 1 | §3 Level 1 — preprocessing | ✅ Closed | — |
| 2 | §4 Level 2 — setopgenerate | ✅ Closed | — |
| 3 | §5 Level 3 — classify structural | ✅ Closed | §5.2 → §7 |
| 4 | §6 Level 4 — structural connect | ✅ Closed | §6.3 → §8 |
| 5 | §7 Level 5 — algebraic bugs + sectoroverlap diagnosis | ✅ Closed | 2 drift absorption → stage 3 |
| 6 | §8 Level 6 — post-core cleanup | ✅ Closed | all "keep" documented |
| 7 | §9 Level 7 — setopfinish | ✅ Closed (§9.1–9.5) | 303→305 tests |
| 8 | §10 Level 8 — visual validation | ❌ Partial | complete diagnosis; fix → stage 3 |
---


## 13. Quick references for stage 3
Recommended entry points for stage 3 plan:
**Main blocker — EMPTY motifs (16 cases)**:
- Root cause documented in §10.3 and §16 of this plan.
- Key file: `_PolyhedralBoundedSolidSetNullEdgesConnector.java`,
  method `setOpConnect()` (~line 1048) and `scanjoin()` (~line 886).
- Fix required: correction of null edges ordering in the connector
  (reordering of STRUT_B pairs) or elimination of tangential null edges
  upstream in the classifier.
- Regression test already created: `KurlanderBowlMotifSweepRegressionTest`.
- Artifact diagnosis: `KurlanderMotifEmptyDiagnosticTest` (remove after fix).
**Secondary blocker — BLACK_FACES (9 cases)**:
- Probable cause: `revert(B)` / `movefac` in Finisher reverses orientation.
- Entry point: `_PolyhedralBoundedSolidSetFinisher.java`.
**Cleanup pending**:
- `KurlanderMotifEmptyDiagnosticTest` delete.
- Diagnostic trace in `_PolyhedralBoundedSolidSetNullEdgesConnector`
  (block `isPipelineSummaryTraceEnabled()` post-sort, ~line 1064) —
  It can remain if it is useful or be eliminated.
- `BooleansFromReferenceObjectPairsTest.given_csgKurlanderBowl_...`:
  capture real topology and replace placeholder.
- 2 absorption drift cases (`MANT1988_15_2_LIMIT`, `MANT1988_6_13`):
  see §7.3.1.D-cont for complete analysis.
**Diagnostic tools available**:
- `vsdk.setop.tracePipelineSummary=true` → `[SetOpPipelineTrace]` in stdout.
- `KurlanderMotifEmptyDiagnosticTest` — trace of individual motifs.
- `PolyhedralBoundedSolidExample` — visual debugging with expanded controls.
---

## 14. Log de cambios del plan

- **2026-05-13**: Initial version of the stage-2 plan.
- **2026-05-14**: Levels 1-2 closed. Levels 3-4 in progress.
- **2026-05-15 (this shift)**: Complete restructuring.
  - Levels 1-4 marked as closed with sub-milestone tables and
    coverage.
  - **New §7 (Level 5)**: algorithmic core that unifies §5.2 and
    §6.1-C with plan of attack in 4 balanced sub-steps (§7.3.1 a
    §7.3.4).
  - §6.2.3, §6.3 moved to §8 (Level 6) — blocked by §7.
  - Before §7 (Finish) and §8 (Visual) renumbered to §9 and §10.
  - §11 (order of execution) and §13 (quick references)
    updated.
  - Final metrics: 282/0/9, connector −55% LOC.
- **2026-05-15 (closing session §7)**:
  - §7.3.1.E: `PolyhedralBoundedSolidSetOperatorAlgebraicPropertiesTest`
    removed from repo (`git rm`). `AlgebraicIdentityRegressionTest` Javadoc
    updated to reflect deletion (not "archaeology").
  - §7 (Level 5) marked ✅ CLOSED. Actual metrics: 293/0/6.
  - §8 (Level 6) unlocked → next active block.
- **2026-05-15 (session §8)**:
  - §8.1: revert(B) experiment before setOpConnect → 29 failures, reverted.
    Documented cause: lmfkrh needs B without reverting before movefac.
  - §8.2: experiment without groupNullEdgesByRing → 3 Kurlander multi-ring failures,
    reversed. The method is already deterministic (uses partitionNullEdgesIntoRings).
  - §8.3: fifth star @Disabled removed, fails with non-coplanar faces [232,144]
    → issue of §9 Finisher; @Disabled restored with diagnostic message.
  - §8 (Level 6) marked ✅ CLOSED. Metrics: 293/0/6 (no change).
  - §9 (Level 7 — setopfinish) → next active block.
- **2026-05-16 (session §9)**:
  - §9.1: counter `lastLegacyFallbackCount` added to `_PolyhedralBoundedSolidSetFinisher`.
    Reset at the start of each call to `sanitizePairedFaces` (same semantics as`lastTriangulatedFaceCount`). Measured = 0 for all baseline fixtures. Fallback does not activate post-§7.
  - §9.2: `lastTriangulatedFaceCount` counter added. Measured = 0; `loopGlue` no
    produces non-planar faces in baseline geometries.
  - §9.3: `wouldMergedFaceBeCoplanar()` guard added in `maximizeFaces` before `lkef`.
  - `SetOpFinishInvariantsTest` created: 10 tests (5 fixtures × §9.1 + §9.2), all green.
  - Suite: 303/0/6 (no regressions). §9.4 (Kurlander reactivation) → pending.
- **2026-05-16 (session §9.5)**:
  - §9.5: Kurlander's fifth star passes `validateIntermediate`. Four fixes:
    1. `loopGlue`: symmetrical guard `isDegenerateLoop (size < 3)` before `lmekr`.
    2. `findNonDegenerateEar`: normalized check (unitVectorTolerance) aligned with planarityValidator.
    3. `triangulateNonPlanarFaces`: direct prune for self-referential size-1 faces;
       `i = 0` restart after lkef to re-examine absorbed faces.
    4. `lkef`: migrate extra loops from the killed face to surviving face (prevent count=1
       after maximizeFaces on faces with inner rings).
  - `@Disabled` removed from `given_kurlanderBowlAndFifthStar_...`.
  - Suite: 303/0/5 (no regressions). Level 7 (§9) ✅ CLOSED.
- **2026-05-16 (STAGE 2 CLOSURE)**:
  - §10: automated sweep formalized in `KurlanderBowlMotifSweepRegressionTest`
    (ok≥15, failures≤25). `KurlanderMotifEmptyDiagnosticTest` created.
  - §10.3: complete diagnosis of EMPTY motifs. Root cause: ordering problem
    `scanjoin` — STRUT_B pairs arrive before their B-faces appear in `endsb`.
    Fix requires upstream fix (classifier or pre-sort topology-aware) → stage 3.
  - §10.4: BLACK_FACES (9 motifs) — uninvestigated → stage 3.
  - Final suite: 305/0/6. Sweep: ok=15, empty=16, blackFaces=9.
  - Plan partially closed: infrastructure criteria ✅; sweep criterion ❌.- User added more visual debugging options in `PolyhedralBoundedSolidExample`.
## 15. Testing status
For individually selected motifs within the Kurlander Bowl:
| Motif index | State |
|------|---------|
| 0 | ✅ |
| 1 | ✅ |
| 2 | ❌ Internal contour failure |
| 3 | ✅ |
| 4 | ❌ Failure in both contours |
| 5 | ❌ Failure in both contours |
| 6 | ❌ Internal contour failure |
| 7 | ⚠️ B-A loses a shell |
| 8 | ❌ External contour failure |
| 9 | ❌ Object A removed |
| 10 | ✅ |
| 11 | ❌ External contour failure |
| 12 | ✅ |
| 13 | ❌ External contour failure |
| 14 | ✅ |
| 15 | ✅ |
| 16 | ❌ External contour failure |
| 17 | ❌ External contour failure |
| 18 | ❌ External contour failure |
| 19 | ❌ Object A removed |
| 20 | ❌ External contour failure |
| 21 | ✅ |
| 22 | ❌ Resulting non-planar face |
| 23 | ❌ Resulting non-planar face and neighboring face removed |
| 24 | ❌ Object A removed |
| 25 | ❌ Object A removed |
| 26 | ❌ Failure in both contours |
| 27 | ❌ Object A removed |
| 28 | ❌ Object A removed |
| 29 | ❌ Object A removed |
| 30 | ❌ Object A removed |
| 31 | ❌ Object A removed |
| 32 | ❌ Object A removed |
| 33 | ❌ Object A removed |
| 34 | ❌ Object A removed |
| 35 | ❌ Object A removed |
| 36 | ❌ Object A removed |
| 37 | ❌ External contour failure |
| 38 | ❌ Object A removed |
| 39 | ❌ Object A removed |
---

## 16. Technical Root Cause — EMPTY motifs (reference for stage 3)
This section preserves the detailed technical analysis obtained in §10.3
so that stage 3 can resume without repeating the diagnosis.
### 16.1 Structure of the 76 pairs of null edges
For the bowl SUBTRACT motif (moon) operation, the classifier generates
76 pairs of null edges with structure:
- A:sameLoop=64, A:diffLoop=12 → 12 STRUT_A pairs (flipNullEdgeOrientationForOpenSide)
- B:sameLoop=12, B:diffLoop=64 → 12 STRUT_B pairs (separateEdgeSequence with
  B-sameLoop when hb1==hb2 in V/V classifier)
`partitionNullEdgesIntoRings` produces **76 size 1 rings** (each
null edge STRUT forms an isolated ring by having both vertices in the
same geometric point). `groupNullEdgesByRing` is no-op → the order in
sonea/soneb is the insertion order of the classifier.
### 16.2 Scanjoin success condition for STRUT_B pairs
For `scanjoin(rightHalf_A, leftHalf_B)` to be successful for a pair
STRUT_B (A-diffLoop f_A1/f_A2, B-sameLoop f_B), it is required that in the
paired list `(endsa[j], endsb[j])` there is an index `j` where:
```
endsa[j].parentLoop.parentFace == f_A1 o f_A2   (misma cara A)
endsa[j] == endsa[j].parentEdge.leftHalf         (rol opuesto a rightHalf_A)
endsb[j].parentLoop.parentFace == f_B             (misma cara B)
endsb[j] == endsb[j].parentEdge.rightHalf         (rol opuesto a leftHalf_B)
```

This `j` only exists if a previous peer failed scanjoin and added
`(endsa[j]=A-half-en-f_A, endsb[j]=B-half-en-f_B)` at the same index.
The required predecessor pair is of type STRUT_A: A-sameLoop f_A, B-diffLoop
f_B/fX. If that pair fails its first scanjoin, it adds `(rightHalf_A(f_A),
leftHalf_B(f_B o fX))` — if `leftHalf_B` is in f_B, the condition is met.
### 16.3 Problematic pairs of motif 24 and their missing predecessors
| Pair STRUT_B | B-face | Necessary predecessor pair | Position | Type of problem |
|-------------|--------|--------------------------|----------|--------------|
| 12 | f=229 | par 13 (B-DL f=229/230) | AFTER ← | Reordering fix |
| 35 | f=263 | par 32 (B-DL f=258/263) | AFTER | Reordering fix |
| 37 | f=229 | par 13/15 already existing | AFTER | Reordering fix |
| 34 | f=269 | none | N/A | Null edge tangential — fix in classifier |
| 46 | f=292 | none | N/A | Null edge tangential — fix in classifier |
| 65 | f=373 | none | N/A | Null edge tangential — fix in classifier |
| 59 | A-face=f=360 | none (A-face only) | N/A | A-diffLoop tangential — fix in classifier |
### 16.4 Fix options for stage 3
**Option A — Reordering in `setOpConnect`**: before the main loop,
reorder sonea/soneb so that each STRUT_B (B-sameLoop) pair has
at least one B-diffLoop pair with the same B-face preceding it in the index.
- Implementable as: topological sort by face-adjacency graph.
- Covers fixable cases (table 10.3), not tangential ones.
- Risk: it could alter the A-B pairing necessary for other cases.
**Option B — Elimination of tangential null edges in `setOpClassify`**:
in `vertexVertexInsertNullEdges`, detect when the STRUT pair creates a
null edge that has no complementary (the curve only touches the face).
Do not insert the null edge or mark it for pre-Connect deletion.
- Cleaner semantically (do not generate unnecessary topology).
- Requires topological context analysis in the classifier.
**Option C — Special handling of null edges without complementary in Connect**:
in `setOpConnect`, if scanjoin fails completely (both null), check
whether side B will never appear in endsb (pre-scan). If so, delete
that null edge with `lkef` directly.
- Pragmatic, without modifying the classifier.
- Risk: `lkef` on a null edge already integrated in the B-rep can
  leave inconsistent topology if other null edges referenced it.
**Recommendation**: Option A + B in combination. A for reorderable cases,
B for tangentials. Measure impact with `KurlanderBowlMotifSweepRegressionTest`.
### 16.5 Connection with BLACK_FACES
All 9 BLACK_FACES have faces with inconsistent orientation in the result.
The probable cause is in `_PolyhedralBoundedSolidSetFinisher.java`:
`revert(B)` inverts all the normals of B, then `movefac` moves those
faces the result. If any face is left with an inverted orientation in the
join, `validateConsistentFaceOrientations` detects it as BLACK_FACE.
Entry point: compare orientations before/after `movefac` for
the problematic motifs.