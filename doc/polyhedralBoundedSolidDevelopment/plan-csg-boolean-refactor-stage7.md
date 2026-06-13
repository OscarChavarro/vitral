# Stage 7 — CSG Boolean Pipeline Refactor: Simplification and Organization

**Date:** 2026-06-12
**Author:** Audit by Claude (Fable 5); written to be executed by a coding agent (Opus)
**Baseline commit:** `6bf29caf` ("Java port: PolyhedralBoundedSolidExample kurlander bowl passing tests")
**Continues:** `doc/plan-csg-boolean-fix-stage6.md` (correctness work; closed)
**Sibling plan:** `doc/plan-csg-boolean-performance-stage8.md` (efficiency; shares
the same audit evidence — read its §1 for the JFR data referenced here)

---

## 1. Goal and Constraints

The boolean pipeline under
`java/base/src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators`
is functionally complete and passes the full testsuite. This stage does **not**
chase correctness bugs. It improves maintainability: remove dead code, break up
the god class by sub-responsibility, eliminate static mutable state, and
centralize instrumentation.

**Hard constraints for every step:**

- The existing testsuite is the regression contract. After every step:
  - `gradle :base:compileJava :base:compileTestJava` must be clean.
  - `gradle :base:test` must be fully green (run from `java/`).
- No public API changes in touched operator families unless a step explicitly
  says otherwise.
- This is a *move/delete* refactor, not a rewrite. Preserve [MANT1988]
  terminology, academic comments, and chapter references when moving code
  (repo policy, see CLAUDE.md).
- One step = one commit. Keep steps independently revertable.
- JaCoCo coverage of already-covered flows must not decrease
  (`base/build/reports/jacoco/test/html`).

---

## 2. Audit Findings (evidence)

### 2.1 Size inventory (operators package: 15,752 lines total)

| Class | Lines | Note |
|---|---:|---|
| `PolyhedralBoundedSolidSetOperator` | 3,899 | **god class**, see §2.2 |
| `_PolyhedralBoundedSolidSetNullEdgesConnector` | 1,593 | large but single-purpose |
| `_PolyhedralBoundedSolidSetClassifier` | 1,103 | orchestrates classification (live path) |
| `_PolyhedralBoundedSolidSetNonIntersectingClassifier` | 1,101 | preflights / special cases |
| `_PolyhedralBoundedSolidSetGeometricPredicateProcessor` | 946 | geometric predicates |
| `_PolyhedralBoundedSolidSetIntersectionCurveBuilder` | 837 | |
| `_PolyhedralBoundedSolidSplitter` | 821 | |
| `_PolyhedralBoundedSolidSetVertexVertexClassifier` | 665 | vertex/vertex geometry+sectors (live path) |
| others | < 700 each | reasonable |

Core package (`volume/polyhedralBoundedSolid`, 5,194 lines):
`PolyhedralBoundedSolidEulerOperators` (1,257), `…TopologyEditing` (1,212),
`…GeometricValidator` (904) — acceptable sizes, single responsibility, **not in
scope** for this stage.

### 2.2 The god class: `PolyhedralBoundedSolidSetOperator` (~110 methods)

Mixes six separable responsibilities:

1. **Pipeline orchestration** per [MANT1988] (generate → classify → connect →
   finish): `setOp`, `setOpGenerate`, `setOpClassify`, `setOpConnect`,
   `setOpFinish`, `postProcessResult`. This is the only responsibility that
   should remain in the class.
2. **A stale duplicate of the vertex/vertex classification machinery**
   (~1,100 lines, approx. lines 425–1542) — see §2.3. **Dead code.**
3. **Special-geometry fallback builders** (~1,700 lines, approx. lines
   1856–3577): four families that bypass the general pipeline for structurally
   detected operand shapes:
   - `buildProfileDifferenceFallback` (extruded YZ profiles),
   - `buildAxisAlignedCellBooleanFallback` + inner class
     `AxisAlignedCellBooleanBuilder`,
   - `buildOrthogonalProfileBooleanFallback` + inner classes
     `OrthogonalProfileOperandSpec`, `OrthogonalProfileBooleanFallbackSpec`,
     `ProfileCellBooleanBuilder`,
   - `buildOffsetCylinderDifferenceFallback` + inner classes
     `VerticalCylinderOperandSpec`, `OffsetCylinderDifferenceFallbackSpec`.
4. **ID-namespace support** (`updmaxnames`, `nextVertexId`) — partially
   duplicates `_PolyhedralBoundedSolidIdNamespace`.
5. **Intersection-vertex welding/pruning** (`weldIntersectionVertices`,
   `pruneStaleVertexFaceEntries`).
6. **Instrumentation**: bit flags `DEBUG_01…DEBUG_99` (field `debugFlags` is
   hard-coded `0` → 40+ guarded blocks are unreachable), `debugSolidExporter`
   callback, trace system properties (`vsdk.setop.traceCoplanarTangential`,
   `vsdk.setop.tracePipelineSummary`), `debugSolid(...)`.

### 2.3 Dead duplicate: the vertex/vertex block (key finding)

The **live** classification path is:

```
PolyhedralBoundedSolidSetOperator.setOpClassify          (line ~1543)
  └─> _PolyhedralBoundedSolidSetClassifier.runSetOpClassify
        └─> (private) vertexVertexClassify                (SetClassifier line ~1045)
              ├─> _PolyhedralBoundedSolidSetVertexVertexClassifier.classify(...)
              └─> (SetClassifier's own) vertexVertexInsertNullEdges(data, ...)
```

`_PolyhedralBoundedSolidSetOperatorVertexVertex` (24 lines) is just the data
record for one `sonvv` pair (`va`, `vb`) — [MANT1988].15.1 — not a classifier.

The god class **also** contains a complete private copy of the same machinery,
rooted at its own `vertexVertexClassify` (line ~1484):
`vertexVertexGetNeighborhood`, `vertexVertexReclassifyOnSectors`,
`vertexVertexReclassifyOnEdges`, `vertexVertexInsertNullEdges`,
`flipNullEdgeOrientationForOpenSide`, `sectoroverlap`, `sctrwitthin`,
`sctrwitthinProper`, `vertexVertexSectorIntersectionTest`, `addNoRepeat`,
`getOrientation`, `nulledge`, `strutnulledge`, `convexedg`, `sectorwide`,
`colinearVectors`, `coplanarSameOrientationForSectorPair`, plus the static
fields `nba`, `nbb`, `sectors`.

Verified facts:

- `PolyhedralBoundedSolidSetOperator.vertexVertexClassify` has **zero
  callers** anywhere in `base/src` (the only textual match is
  `_PolyhedralBoundedSolidSetClassifier.java:1100`, which calls the
  SetClassifier's *own* private method of the same name).
- The two copies have **diverged** (e.g. the god-class
  `vertexVertexReclassifyOnSectors` uses a local `sameOrientation`; the
  classifier's copy uses `nonopposite`) — the extracted version is the
  maintained one, the god-class version is stale.

So responsibility §2.2.2 is not "code to move"; it is **code to delete**,
compiler-guided, with a per-helper reachability check (some helpers, e.g.
`inside(he)` and `sectoroverlap`, are also referenced from live code paths and
must stay — see step R1).

### 2.4 Static mutable state (Mantyla globals)

The operator translates Mantyla's globals (`sonvv`, `sonva`, `sonvb`, `sonea`,
`soneb`, `sonfa`, `sonfb`, `nba`, `nbb`, `sectors`) into **static mutable
fields**, plus `idNamespace`, `numericContext`, and the per-call fallback specs
(`profileDifferenceFallback`, `offsetCylinderDifferenceFallbackSpec`,
`axisAlignedCellBooleanFallback`, `orthogonalProfileBooleanFallback`).
`_PolyhedralBoundedSolidSetClassifier` and
`_PolyhedralBoundedSolidSetVertexVertexClassifier` replicate the same pattern
(their own static `debugFlags`, `nba`, `nbb`, `sectors`, lists passed in via
`runSetOpClassify`).

Consequences: not re-entrant, not thread-safe, state can leak between
invocations, and tests must run serially (`maxParallelForks = 1` in
`base/build.gradle`). Fixing this unblocks parallel test execution (sibling
plan, step P7).

### 2.5 Instrumentation census

Log/print call counts (`reportMessage|System.out|System.err|printStackTrace`):
76 in `PolyhedralBoundedSolidSetOperator`, 39 in
`_PolyhedralBoundedSolidSetClassifier`, 23 in
`_PolyhedralBoundedSolidSetVertexVertexClassifier`, 22 in
`_PolyhedralBoundedSolidSplitter`. Most are gated by `debugFlags` (constant
`0` → dead) or by trace system properties (legitimately reachable).

Repo policy (CLAUDE.md "Visual Diagnostics Policy") requires keeping the
*diagnostic capability* — the goal is centralization and dead-branch removal,
not removal of diagnostics.

### 2.6 Half-delegations

The god class keeps one-line forwarding wrappers that only delegate to
`_PolyhedralBoundedSolidSetGeometricPredicateProcessor`: `compareToZero`,
`pointInFace`, `resolveCoplanarVertexVertexClass`,
`classifyCoplanarSectorRelation`. Call sites should call the processor
directly.

---

## 3. Execution Plan (step by step)

Run all Gradle commands from `java/`. After each step run the full gate:

```bash
./gradlew :base:compileJava :base:compileTestJava
./gradlew :base:test
```

If a step breaks a test, revert the step (do not patch semantics to make it
pass — that would change behavior, which is out of scope).

### Step R1 — Delete the dead vertex/vertex duplicate from the god class

Target: `PolyhedralBoundedSolidSetOperator.java`, approx. lines 425–1542.

1. Delete the root method `vertexVertexClassify` (line ~1484, `private static`,
   zero callers — re-verify with
   `grep -rn "vertexVertexClassify(" base/src --include="*.java"` before
   deleting; the only remaining hit must be inside
   `_PolyhedralBoundedSolidSetClassifier`).
2. Compile. Delete every private helper that the compiler now reports as
   unused, iteratively, **bottom-up**: `vertexVertexGetNeighborhood`,
   `vertexVertexReclassifyOnSectors`, `vertexVertexReclassifyOnEdges`,
   `vertexVertexInsertNullEdges`, `flipNullEdgeOrientationForOpenSide`,
   `addNoRepeat`, `getOrientation`, `nulledge`, `strutnulledge`, `convexedg`,
   `sectorwide`, `coplanarSameOrientationForSectorPair`, `colinearVectors`,
   `vertexVertexSectorIntersectionTest`, `sctrwitthin`, `sctrwitthinProper`,
   `sectoroverlap`. Java does not error on unused private methods, so use
   `grep -c "name("` per method (count of call sites inside the file) or an
   IDE/LSP "find usages"; delete only methods whose remaining references are
   themselves deleted code.
3. **Do not delete** without checking cross-references first:
   - `inside(he)` is `protected static` and may be used by subclasses /
     other operators — check `grep -rn "inside(" base/src` first.
   - `colinearVectorsWithDirection` is `public` — check external callers
     (`_PolyhedralBoundedSolidSetVertexVertexClassifier` likely calls it).
   - The static fields `nba`, `nbb`, `sectors` — delete only if no surviving
     method references them.
4. Delete the now-dead `DEBUG_04_VERTEXVERTEXCLASIFFIER` blocks that lived
   only inside deleted methods (the flag constant itself may still be used by
   `setOpClassify`'s `debugFlags` pass-through — keep the constant if so).
5. Gate. Expected outcome: god class shrinks by roughly 1,000–1,100 lines with
   zero behavior change.

Commit: `Stage 7 R1: remove stale duplicated vertex-vertex classification from SetOperator (dead code; live path is _PolyhedralBoundedSolidSetClassifier/_PolyhedralBoundedSolidSetVertexVertexClassifier)`

### Step R2 — Extract the fallback families into their own classes

Target: the ~1,700-line block described in §2.2.3. Create four
package-private classes in the same package (names follow the existing
`_Polyhedral…` convention):

- `_PolyhedralBoundedSolidProfileDifferenceFallback`
  (move `buildProfileDifferenceFallback`, `extractProfileAtX`,
  `clipProfileAboveZ`, `signedAreaOnYZ`, profile point helpers; the spec class
  `_PolyhedralBoundedSolidProfileDifferenceFallbackSpec` already exists as a
  top-level file).
- `_PolyhedralBoundedSolidAxisAlignedCellFallback`
  (move `buildAxisAlignedCellBooleanFallback`, `AxisAlignedCellBooleanBuilder`,
  `isAxisAlignedEdge`, `isAxisAlignedSolid`,
  `classifyPointForAxisAlignedFallback`, `axisAlignedCellSelected`,
  `addAxisAlignedBoundaryQuad`, `uniformCoordinates`,
  `uniqueVertexCoordinates`, `addUniqueCoordinate`, `boundsMatch`,
  `sameCoordinate`, `coordinate`, `isBetween` — check each for other callers
  before moving; shared helpers used by more than one family go to a small
  `_PolyhedralBoundedSolidFallbackGeometry` utility or stay put).
- `_PolyhedralBoundedSolidOrthogonalProfileFallback`
  (move `buildOrthogonalProfileBooleanFallback`, `createOrthogonalProfileSpec`,
  `createXExtrudedYZSpec`, `createYExtrudedXZSpec`, `profileCellSelected`,
  `addProfileBoundaryQuad`, `pointInsideYZProfile`, inner classes
  `OrthogonalProfileOperandSpec`, `OrthogonalProfileBooleanFallbackSpec`,
  `ProfileCellBooleanBuilder`).
- `_PolyhedralBoundedSolidOffsetCylinderFallback`
  (move `buildOffsetCylinderDifferenceFallback`, `describeVerticalCylinder`,
  `createFallbackCylinder`, `addUniqueXy`, inner classes
  `VerticalCylinderOperandSpec`, `OffsetCylinderDifferenceFallbackSpec`).

Mechanics:

1. Move methods verbatim (keep comments and [MANT1988] references). Change
   `private` → package-private (default) only where the god class must call
   the new entry point.
2. In the god class, replace the moved bodies with the single call to the new
   class. Keep the decision points in `setOp` unchanged (the sibling
   performance plan, step P1, will restructure *when* they run — do not do
   both changes in one commit).
3. Each family is its own commit; gate after each.

Expected outcome: god class drops to roughly 1,000–1,200 lines, containing
orchestration plus welding/ids/trace only.

### Step R3 — Remove forwarding wrappers and consolidate ID handling

1. Inline the four predicate wrappers (§2.6): update call sites to call
   `_PolyhedralBoundedSolidSetGeometricPredicateProcessor` directly; delete
   the wrappers. Use imports, not fully-qualified names (repo rule).
2. Move `updmaxnames` and `nextVertexId` into
   `_PolyhedralBoundedSolidIdNamespace` (or have them delegate to it) so the
   ID-renaming policy lives in one place. `updmaxnames` is `public` and is the
   [MANT1988].15.4 name — keep a delegating method with the same signature in
   the god class if external callers exist (check
   `grep -rn "updmaxnames" base/src testsuite`).
3. Gate; one commit.

### Step R4 — Centralize instrumentation, delete dead debug branches

1. Create a package-private `_SetOperationTrace` collaborator holding:
   the trace-property checks (`isCoplanarTangentialTraceEnabled`,
   `isPipelineSummaryTraceEnabled`), `traceCoplanarTangential`,
   `tracePipelineSummary`, the `DebugSolidExporter` hook and `debugSolid`,
   and the debug-flag constants.
2. Point `PolyhedralBoundedSolidSetOperator`,
   `_PolyhedralBoundedSolidSetClassifier` and
   `_PolyhedralBoundedSolidSetVertexVertexClassifier` at it (today each has
   its own copy of the property names and flag constants).
3. Delete `if ( (debugFlags & …) != 0 )` blocks that are unreachable **and**
   whose information is already covered by regression tests; keep blocks that
   the visual debugger workflow still uses (CLAUDE.md policy). When in doubt,
   keep the block but route it through `_SetOperationTrace`.
4. Gate; one commit.

### Step R5 — Introduce `SetOperationContext` (kill static mutable state)

Do this **after** R1–R4 (less code to thread the context through).

1. Define a package-private class `_SetOperationContext` carrying: the
   `sonvv/sonva/sonvb/sonea/soneb/sonfa/sonfb` lists, `nba/nbb/sectors` (the
   surviving copies in the classifier classes), the `idNamespace`, the
   `numericContext`, fallback specs/results, and the trace object from R4.
2. `setOp(...)` creates one context per invocation and passes it explicitly to
   `_PolyhedralBoundedSolidSetIntersector`, `_PolyhedralBoundedSolidSetClassifier`,
   `_PolyhedralBoundedSolidSetVertexVertexClassifier`,
   `_PolyhedralBoundedSolidSetNullEdgesConnector`,
   `_PolyhedralBoundedSolidSetFinisher`,
   `_PolyhedralBoundedSolidSetNonIntersectingClassifier`.
   Mechanical transformation: each `static` method that touches the state
   gains a `_SetOperationContext ctx` first parameter; static field reads
   `sonvv` become `ctx.sonvv`. Do it one collaborator class at a time, one
   commit each, gating after each.
3. Last commit removes the static fields from all operator classes and the
   `cleanup()` reset machinery in `_PolyhedralBoundedSolidOperator` that
   exists only to scrub static state between runs (verify with grep before
   removing).
4. Public entry points (`setOp` overloads, `PolyhedralBoundedSolidModeler`)
   keep their signatures — the context is internal.

Done criterion for R5: `grep -n "static"` over the operators package shows no
remaining *mutable* static fields (constants and pure static methods are
fine). This is the precondition for performance plan step P7 (parallel test
forks).

### Step R6 — Final sweep

1. Re-run the size inventory (`wc -l *.java | sort -rn`) and record it in §4.
2. Run the full testsuite plus
   `gradle :base:test :base:jacocoTestReport`; verify coverage of
   `polyhedralBoundedSolidOperators` did not drop versus baseline.
3. Update this document's §4 execution log.

---

## 4. Execution Log

- **2026-06-12 (audit):** Findings §2 verified against baseline `6bf29caf`:
  dead vertex/vertex duplicate confirmed by call-graph greps; live path
  documented in §2.3. Plan written; execution not started.

- **2026-06-12 (execution, branch `stage7-csg-boolean-refactor`):** Each step
  below is one commit, gated by a clean `:base:compileJava :base:compileTestJava`
  and a fully green `:base:test` (393 tests, 34 skipped) before commit.

  - **R1 — done.** Removed the stale duplicated vertex/vertex classification
    from `PolyhedralBoundedSolidSetOperator` (zero live callers). *Audit
    deviation:* `sectoroverlap` and `separateEdgeSequence` (+ its
    `SeparateEdgeSequenceResult` enum and dependency closure
    `recoverEdgeSequenceEndpointFromStrut`/`nulledge`/`strutnulledge`) were
    **kept** — they are exercised by reflection-based tests
    (`PolyhedralBoundedSolidSetOperatorCoplanarPredicateTest`,
    `VertexVertexEndpointRecoveryTest`) that the call-graph greps in §2.3 did
    not see. God class 3899 → 2949.
  - **R2 — done** (2 commits). Extracted the four fallback families plus a
    shared `_PolyhedralBoundedSolidFallbackGeometry` (12 cross-family
    primitives). New classes: `…ProfileDifferenceFallback`,
    `…AxisAlignedCellFallback`, `…OrthogonalProfileFallback`,
    `…OffsetCylinderFallback`. God class 2949 → 1316.
  - **R3 — done.** Dropped the dead `compareToZero`/`pointInFace`/
    `resolveCoplanarVertexVertexClass` wrappers (no callers after R1);
    `classifyCoplanarSectorRelation` kept (reflective test). Moved
    `updmaxnames` body and the null-namespace `nextVertexId` fallback into
    `_PolyhedralBoundedSolidIdNamespace`; god class keeps public `updmaxnames`
    ([MANT1988].15.4, external test caller) and `nextVertexId` as delegators.
  - **R4 — done (trace only).** Centralized the duplicated property-gated
    trace methods into `_SetOperationTrace`. *Audit correction:* §2.2.6/§2.5
    claimed `debugFlags` is hard-coded `0` → dead branches. **Wrong** —
    `setOp(…, withDebug=true, …)` reassigns `debugFlags` to the full flag set,
    so those blocks are the live, `withDebug`-gated visual-diagnostic dumps
    required by the Visual Diagnostics Policy. They were preserved, not
    deleted, along with the `DebugSolidExporter`/`debugSolid` hook.
  - **R5 — done** (6 commits: R5a, R5b, R5c, R5e, R5d, and the test rework in
    R5d). The per-operation Mantyla `son*` list state was removed from **all
    five** pipeline classes; no static `son*` list field remains in the
    operators package.
    - **R5a:** `nba`/`nbb`/`sectors` (vertex/vertex neighborhood scratch) made
      per-`classify()` instance state in
      `_PolyhedralBoundedSolidSetVertexVertexClassifier`.
    - **R5b:** new `_SetOperationContext` carries
      `sonvv/sonva/sonvb/sonea/soneb/sonfa/sonfb`; `setOp` creates one per call
      and threads it through generate→classify→connect→finish. The god class's
      seven static fields are gone. The test-only `separateEdgeSequence`
      overload no longer records into the lists (its live counterpart in
      `_PolyhedralBoundedSolidSetClassifier` does).
    - **R5c:** `_PolyhedralBoundedSolidSetClassifier` threads the context
      instead of copying the five lists into its own statics; those fields are
      removed.
    - **R5e:** `_PolyhedralBoundedSolidSetVertexFaceClassifier`'s `sonea`/
      `soneb` made per-call instance state (classify/insert/makeRing/
      vertexFaceClassify → instance); `VertexFaceClassifierCoplanarTest`
      (existence-only reflection) is unaffected.
    - **R5d:** `_PolyhedralBoundedSolidSetNullEdgesConnector`'s
      `sonea/soneb/sonfa/sonfb` and the `scanjoin` cursor `nextNullEdgeIndex`
      made per-call instance state; every method that touches them (connect,
      sortNullEdges, applyOrderPermutation, groupNullEdgesByRing,
      updateLastSnapshot, finalizeCoincidentLooseA/B, cutA/cutB, setOpConnect,
      `scanjoin`, `sgetnextnulledge`) became instance; the god class now does
      `new _PolyhedralBoundedSolidSetNullEdgesConnector().connect(...)`. This
      one required relaxing a test contract (explicitly requested):
      `SetOpConnectScanJoinTest` previously *asserted* `scanjoin`/
      `sgetnextnulledge` were `private static` and drove `sonea`/`soneb` via
      `field.get(null)` / `invoke(null, …)`. It was reworked to assert the
      primitives are private and **non-static** and to exercise the
      [MANT1988].15.7 programs 15.13/15.14 against a real connector instance;
      the old "static cursor reset between runs" case was replaced by a
      per-instance cursor-independence test that guards the same no-leak
      invariant under the re-entrant model. (The connector's
      `sonfaPairIndexByFaceId`/`…fb…` pair-index maps and the loose-end
      counters stay static — auxiliary state the finisher reads via the static
      accessors `getSonfaPairIndex` / `getLastLooseACount`; out of the `son*`
      list scope.)
    - `numericContext`/`idNamespace` intentionally left as managed static
      (per the agreed partial scope — list state only).
    - Note: the originally stated motivation (unblock parallel test forks) does
      not hold — Gradle `maxParallelForks` uses separate JVM processes where
      statics do not leak, and in-JVM JUnit parallelism is disabled
      (`junit.jupiter.execution.parallel.enabled=false`); the change stands on
      re-entrancy/hygiene grounds.
  - **R6 — done.** Size inventory (operators package, was 15,752):
    `PolyhedralBoundedSolidSetOperator` **3899 → 1246** (−68%);
    new `_SetOperationTrace` 49, `_PolyhedralBoundedSolidFallbackGeometry` 247,
    the four fallback classes 138/533/608/252. Full suite green at each gate;
    JaCoCo of `polyhedralBoundedSolidOperators` not decreased versus baseline
    (no live flow removed — only dead duplicates and pure code motion).
