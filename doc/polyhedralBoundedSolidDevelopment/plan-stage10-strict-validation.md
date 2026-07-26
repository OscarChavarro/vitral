# Stage 10 — Default Strict Validation for CSG Boolean Operations

**Date:** 2026-07-26  
**Status:** Implementation complete; manual visual smoke check pending
**Primary goal:** Add a configurable `doStrictValidation` postcondition to
every public and internal CSG boolean entry point, defaulting to `true`, and turn the
`STEPER_MOTOR_GUIDE` failure discovered in
`TangibleInterfaceGizmoCreator` into a kernel regression fixture.

All Gradle commands in this plan run from `java/`.

---

## 1. Motivation

The `STEPER_MOTOR_GUIDE` model exposed a class of failures that can remain
visually hidden until a later boolean operation.

The model consists conceptually of:

- **A:** a 3 mm-thick arrow-shaped base. Its marker area is a 40 mm × 40 mm
  square and its current profile is
  `M 0 0 h 40 v 15 h 5 l 10 5 l -10 5 h -5 v 15 h -40 Z`.
- **B:** a lower shaft coupler with a 5.05 mm D-profile bore, a 4.6 mm flat
  axis, and a 4.2 mm bore depth.
- **C:** an upper bearing sleeve whose 9.02 mm inner diameter receives B, with
  a 1.6 mm radial wall. C is 8.06 mm high: 1 mm buried into A and 7.06 mm
  protruding above it.

The intended final set is:

```text
(B union C) union A
```

The original `B union C` image looked correct, but strict validation later
showed that it was not a trustworthy B-Rep:

1. When B's outer diameter and C's inner diameter were both exactly 9.02 mm,
   the boolean finisher retained a degenerate face loop with fewer than three
   edges.
2. Increasing B to 10.02 mm created the intended 0.5 mm radial overlap, but
   the shared lower Z plane still produced improper face intersections.
3. Offsetting that plane removed the immediate strict-loop failure, but the
   result had a topology summary inconsistent with the intended tube:
   one connected shell with adjusted Euler characteristic `-2`, rather than
   the expected `0`.
4. A later union with A converted the hidden defect into a visible collapsed
   D-shaped face that covered the lower bore.

The robust construction was geometrically equivalent but changed the operation
order:

1. Build and unite the exterior solid volumes.
2. Subtract the upper circular cavity.
3. Subtract the lower D-profile cavity with a small internal axial overlap.

The partial stepped tube then has one shell and adjusted Euler characteristic
`0`. The final part has one shell and adjusted Euler characteristic `2`,
representing a closed solid with a blind stepped pocket.

This incident demonstrates two separate needs:

- Boolean callers need fail-fast rejection of invalid B-Reps before a bad
  result contaminates the next operation.
- Performance-sensitive or legacy diagnostic callers need an explicit opt-out
  whose cost and behavior remain measurable.

---

## 2. Scope and Non-goals

### In scope

- Add `boolean doStrictValidation` to the complete `setOp` call chain.
- Preserve all existing overloads and make them delegate with
  `doStrictValidation = true`.
- Strictly validate every successful result path when the flag is `true`,
  including preflight, fallback, recovery, identity, non-intersection, and
  normal Mantyla-pipeline results.
- Improve strict-validation diagnostics with connected-shell and adjusted
  Euler summaries.
- Add the stepper motor guide as a deterministic base-module regression fixture.
- Harden boolean finalization against degenerate loops where the regression
  identifies a local, general-purpose fix.

### Non-goals

- Do not remove the explicit `doStrictValidation=false` opt-out.
- Do not change the result produced when `doStrictValidation` is `false`.
- Do not silently repair or replace an invalid boolean result under the strict
  flag.
- Do not require every valid result to have one shell or Euler characteristic
  `2`. Multiple shells and non-zero genus can be legitimate.
- Do not move application-specific AprilTag or UI code into `:base`.

---

## 3. API Design

### 3.1 Public modeler overload

Add the following overload to
`PolyhedralBoundedSolidModeler`:

```java
public static PolyhedralBoundedSolid setOp(
    PolyhedralBoundedSolid inSolidA,
    PolyhedralBoundedSolid inSolidB,
    int op,
    boolean withDebug,
    boolean maximizeResultFaces,
    boolean doStrictValidation)
```

Preserve source compatibility by delegating existing overloads as follows:

```java
setOp(a, b, op)
    -> setOp(a, b, op, false, true, true)

setOp(a, b, op, withDebug)
    -> setOp(a, b, op, withDebug, true, true)

setOp(a, b, op, withDebug, maximizeResultFaces)
    -> setOp(a, b, op, withDebug, maximizeResultFaces, true)
```

The same six-argument overload and delegation rules must exist in
`_PolyhedralBoundedSolidSetOperator`.

The boolean belongs at the end of the signature to avoid changing the meaning
of the existing `withDebug` and `maximizeResultFaces` arguments.

### 3.2 Semantics

`doStrictValidation` is a result postcondition and defaults to `true` through
the shorter overloads:

- `false`: retain current validation, performance, and return behavior.
- `true`: run the existing intermediate checks as usual, finish and normalize
  the result, then run strict validation before returning it.
- If strict validation fails, throw `IllegalStateException`. The exception
  must identify the operation, result path, operand/result cardinalities,
  bounds, connected-shell count, and adjusted Euler data.
- Never return a result whose strict postcondition failed.
- Do not mutate or weld the finished result merely to make strict validation
  pass.

Strict input validation is not part of this flag. Input handling remains under
`validateBooleanInputs`; strict input validation can be proposed separately if
real cases require it.

---

## 4. Centralize Result Validation

The boolean operator currently has many early returns and specialized result
paths. Adding a check only after the main pipeline would provide a false sense
of safety.

Introduce one private result gateway, conceptually:

```java
private static PolyhedralBoundedSolid completeSetOpResult(
    PolyhedralBoundedSolid result,
    int op,
    String resultPath,
    boolean maximizeResultFaces,
    boolean doStrictValidation)
```

Responsibilities:

1. Run the existing post-processing exactly once where applicable.
2. Run `validateIntermediate` under the existing behavior.
3. If requested, run `validateStrict`.
4. Attach the topology summary to any strict failure.
5. Return the result unchanged on success.

Audit every return from `_PolyhedralBoundedSolidSetOperator.setOp` and route
successful non-null results through the gateway:

- geometrically identical operands;
- empty/disjoint/containment and touching-only preflights;
- profile-difference fallback;
- offset-cylinder fallback;
- axis-aligned-cell fallback;
- orthogonal-profile fallback;
- connect recovery;
- normal intersection/classification/connect/finish pipeline.

Add a test-only path label or enum rather than inferring the path from geometry.
This makes strict failures actionable without enabling verbose boolean debug
output.

---

## 5. Strict Topology Summary

`validateStrict` already checks:

- planar faces;
- half-edge/topological integrity;
- strict loop geometry, including loops with fewer than three edges;
- improper face/face intersections.

The stepper investigation also needed global topology information. Move the
reusable calculation from the application experiment into the base validation
package.

### 5.1 Connected shells

Count connected components of faces through shared B-Rep edges. Report:

- shell count;
- face count per shell;
- whether every face was reached exactly once.

Do not reject `shellCount > 1` universally. A disjoint union may validly contain
multiple closed shells.

### 5.2 Adjusted Euler characteristic

For faces that may contain inner loops, use:

```text
chi = V - E + sum over faces (2 - boundaryLoopCount(face))
```

Equivalently, each planar connected face contributes
`1 - numberOfInnerLoops`.

Calculate both whole-solid and per-shell values. For a closed orientable
connected shell:

```text
chi = 2 - 2g
```

where `g` is the genus.

The topology summary should reject only universal contradictions, such as an
unreachable face or a per-shell Euler value incompatible with a closed
orientable 2-manifold. It must not reject a mathematically valid genus merely
because it is unexpected for a specific application.

The stepper regression itself must assert the semantic expectations:

- corrected `B union C`: one shell, `chi = 0`;
- final `(B union C) union A`: one shell, `chi = 2`.

### 5.3 Face orientation

Audit `_GeometricFaceOrientationStrategy`, which is currently separate from the
strategies used by `validateStrict`. Do not enable it blindly: its own
documentation describes it as heuristic. Add it only if the current strict
test corpus demonstrates no valid-shell false positives.

---

## 6. Stepper Motor Guide Regression Fixture

Create a base-test fixture independent of the tool module, for example:

```text
java/base/src/test/vsdk/toolkit/processing/
  polyhedralBoundedSolidOperators/
  StepperMotorGuideStrictValidationTest.java
```

Place reusable fixture construction in the same test package or in a focused
`StepperMotorGuideCsgFixture`. Do not add a dependency from `:base` to
`TangibleInterfaceGizmoCreator`.

Use millimetres converted through one explicit scale constant and preserve the
production dimensions:

| Feature | Measurement |
|---|---:|
| Base marker square | 40 mm × 40 mm |
| Base thickness | 3 mm |
| D bore diameter | 5.05 mm |
| D flat-axis width | 4.6 mm |
| D bore depth above base | 4.2 mm |
| C inner diameter | 9.02 mm |
| C radial wall | 1.6 mm |
| C total height | 8.06 mm |
| C buried depth | 1.00 mm |
| C protrusion | 7.06 mm |
| Boolean axial overlap used by corrected cutters | 0.10 mm |

The fixture must cover four cases:

1. **Coincident-cylinder regression.** Build legacy B with 9.02 mm outer
   diameter and C with 9.02 mm inner diameter. Demonstrate that the legacy
   boolean can produce a visually plausible result containing a loop with
   fewer than three edges. With `doStrictValidation = true`, the operation must
   fail at the operation that creates the invalid result, not at a later union.
2. **Default compatibility.** The equivalent call with no strict flag, or with
   the flag explicitly `false`, must preserve the pre-Stage-10 behavior and
   must not incur strict-validation counters.
3. **Corrected stepped tube.** Build the exterior cylinder, subtract the upper
   circular cavity first, and then subtract the overlapping lower D cavity.
   Every strict boolean must pass. Assert one shell and `chi = 0`.
4. **Final blind pocket.** Unite A with the exterior first, then perform the two
   cavity subtractions. Assert strict success, one shell, `chi = 2`, a closed
   D-shaped floor at the top of A, and no through-hole below that floor.

Do not rely only on rendered images. Add geometric assertions using Z slices or
point-containment probes:

- above the 4.2 mm transition, the cavity accepts the 9.02 mm circular profile;
- below the transition, material outside the 5.05/4.6 mm D profile remains;
- at the base top, the D pocket is closed;
- below the base top, the center is solid;
- the result contains no loop with fewer than three distinct edges.

Keep an explanatory comment linking the fixture to
`TangibleInterfaceCubeFixture.STEPER_MOTOR_GUIDE`, but keep the test
self-contained.

---

## 7. Boolean Finisher Hardening

Strict validation detects the bad result; the kernel should also avoid creating
it when a safe general fix is possible.

Investigate the stepper failure at the finisher boundary:

1. Trace the face and loop that becomes a one- or two-edge ring after coincident
   cylindrical boundaries are classified and connected.
2. Determine whether the defect is created by null-edge connection, face
   joining, face maximization, or post-finish compaction.
3. Add a local invariant before committing a loop:
   - at least three distinct vertices;
   - at least three non-zero-length edges;
   - non-zero projected area within the active numeric policy.
4. If the candidate loop is degenerate, remove it only when its removal is
   topologically justified by the local Euler operation. Otherwise fail the
   boolean with a diagnostic; do not guess a repair.
5. Re-run the legacy coincident-cylinder fixture. If the kernel can now produce
   a valid result, update the regression to assert strict success while keeping
   a separately constructed malformed-loop fixture to test rejection.

The corrected production modeling order remains valuable even after a kernel
fix: it avoids coincident boundaries and expresses the intended blind pocket
directly.

---

## 8. Execution Sequence

| Phase | Status | Outcome |
|---|---|---|
| S10.0 — Freeze evidence | Complete | Legacy defect captured at the first invalid `B union C`. |
| S10.1 — Topology summary | Complete | Shared shell/adjusted-Euler implementation and focused tests. |
| S10.2 — API plumbing | Complete | Six-argument public/internal overload; shorter calls validate strictly. |
| S10.3 — Result paths | Complete | All successful exits use one strict-validation gateway. |
| S10.4 — Stepper regression | Complete | Legacy rejection and corrected `chi=0`/`chi=2` cases covered. |
| S10.5 — Finisher hardening | Complete (diagnosis) | Unsafe repair rejected; fail-fast guard retained. |
| S10.6/S10.6A — Performance/docs | Complete | A/B overhead measured; Javadoc and benchmark log added. |
| Manual application inspection | Pending | Compile smoke passes; interactive cavity inspection remains. |

### S10.0 — Freeze evidence

- Add the stepper fixture and topology-summary assertions before changing the
  boolean implementation.
- Record which operation and result path first fails.
- Save cardinalities, shell summaries, and strict messages in test assertion
  descriptions, not as golden console output.

### S10.1 — Add topology summary support

- Implement connected-shell and adjusted-Euler calculation in the validation
  package.
- Unit-test a box (`chi=2`), through-tube (`chi=0`), two disjoint boxes
  (two shells, total `chi=4`), and a face containing inner loops.
- Reuse or consolidate the similar summary logic currently present in
  `BooleansFromReferenceObjectPairsTest`; do not maintain competing formulas.

### S10.2 — Add API plumbing

- Add the six-argument overload to `PolyhedralBoundedSolidModeler`.
- Add the matching overload to `_PolyhedralBoundedSolidSetOperator`.
- Delegate all existing overloads with `doStrictValidation=true`.
- Add API tests proving default overloads and explicit `true` are equivalent,
  while explicit `false` remains available for legacy compatibility.

### S10.3 — Cover every result path

- Introduce the centralized result gateway.
- Route every successful boolean exit through it.
- Add focused tests for at least one normal, preflight, fallback, and recovery
  result with strict validation enabled.

### S10.4 — Enable the stepper regression

- Run all four fixture cases from Section 6.
- Confirm the legacy defect is caught immediately.
- Confirm the corrected partial and final constructions pass.

### S10.5 — Harden the finisher

- Diagnose and fix the smallest general source of degenerate loops.
- Keep strict validation as a guard even if the legacy operation is repaired.

### S10.6 — Performance and documentation gate

- Benchmark representative booleans with strict validation on and off.
- Verify the explicit opt-out path has no strict face-pair scan and no
  topology-summary allocation.
- Add Javadoc describing cost and exception semantics.
- Update the CSG development notes with the new overload.

### S10.6A — A/B timing of the pre-existing unit-test corpus

- Freeze a Stage 9 wall-clock baseline before changing the boolean call chain,
  using `:base:test --no-build-cache --rerun-tasks` with the suite's existing
  serial execution policy.
- Select the pre-existing boolean unit tests whose current results already pass
  `validateStrict`; keep the tests that intentionally document
  pseudomanifolds in the `false` corpus only.
- Run that unchanged fixture/assertion corpus through the six-argument overload
  twice: once with `doStrictValidation=false`, and once with
  `doStrictValidation=true`.
- Warm up each mode, then run at least five alternating measured forks
  (`false`, `true`, `true`, `false`, ...) so JVM/Gradle warm-up and machine
  drift do not systematically favour one mode.
- Record wall-clock duration, median, p95, absolute delta, and percentage
  overhead. Keep compilation and JaCoCo generation outside the timed interval
  when comparing the flag itself.
- Also time the complete pre-existing `:base:test` suite with the default
  overloads to quantify the now-default strict path, while retaining the
  explicit-false measurement as the opt-out baseline.
- Store the command, JVM/Gradle version, host summary, raw samples, and result
  table under `doc/polyhedralBoundedSolidDevelopment/benchmarks/`; do not turn
  noisy wall-clock thresholds into required unit-test assertions.

### Execution log

- **2026-07-26 — S10.0/S10.4:** Added the self-contained stepper fixture with
  the planned 5.05 mm / 4.6 mm D profile, 9.02 mm sleeve cavity, 1.6 mm wall,
  8.06 mm sleeve height, 4.2 mm transition, and 0.1 mm cutter overlap. The
  coincident legacy `B union C` first fails strict validation in
  `path=normal-pipeline`, at face 4, with a loop containing fewer than three
  edges. The same call with strict validation disabled preserves the existing
  result and incurs zero strict-validation invocations.
- **2026-07-26 — S10.1:** Connected-shell calculation in
  `BooleansFromReferenceObjectPairsTest` now delegates to the shared topology
  summary instead of maintaining a second disjoint-set implementation.
- **2026-07-26 — S10.4:** The corrected partial stepped tube passes every
  strict boolean and has one shell with adjusted `chi=0`. The final guide has
  one shell with adjusted `chi=2`; material probes verify the upper circular
  cavity, lower D restriction, open pocket above the base, and solid material
  below its floor.
- **2026-07-26 — S10.5 diagnosis:** Instrumented Finish and face maximization
  temporarily. Finish, `lkfmrh`, `loopGlue`, cleanup, and triangulation all
  retained zero sub-three-edge loops. `maximizeFaces` created the defect in
  its `remove-dangling-edge` case, reducing the outer loop of face 4 to two
  edges and then one. Disabling final face maximization makes the same legacy
  boolean strictly valid. A proposed guard avoided the short loop but left a
  self-intersecting boundary, so it was rejected and not retained. Strict
  validation remains the safe fail-fast behavior; a separate malformed-loop
  fixture covers rejection.
- **2026-07-26 — S10.6A:** Reused the 20 pre-existing moon/cylinder
  difference cases for an alternating five-fork A/B comparison. All cases
  passed in both modes. Median wall time was 3.7247 s with strict validation
  disabled and 3.8147 s with it enabled: +0.0900 s, or +2.42%. The five-sample
  nearest-rank p95 increased from 3.7694 s to 3.9337 s (+4.36%). Raw samples
  and commands are recorded in
  `benchmarks/stage10-strict-validation-baseline.md`.
- **2026-07-26 — S10.6 initial policy:** The opt-out-default suite executed 420 tests
  with zero failures/errors (35 skipped) and completed in Gradle's reported
  1 min 28 s, versus the frozen 91.66 s external wall-clock baseline. No
  opt-out-path regression was visible. The public overload documents that
  strict mode adds global topology and all-face-pair work and can throw
  `IllegalStateException`. `_GeometricFaceOrientationStrategy` remains
  excluded: it deliberately uses a neighbour-normal heuristic with documented
  false negatives and is not a universal B-Rep postcondition.
- **2026-07-26 — default policy revision:** Based on the measured +2.42%
  median overhead and the fail-fast robustness benefit, all shorter public and
  internal overloads now delegate with `doStrictValidation=true`. Explicit
  `false` remains the compatibility/performance escape hatch. The first full
  suite run exposed 12 invocations in tests intentionally documenting
  intermediate-only, coincident-contact, or pseudomanifold legacy results;
  those fixtures now opt out explicitly. The final suite passed 420 tests.
  Its 2 min 44 s elapsed time versus 1 min 28 s with the prior default is an
  +86.4% whole-suite increase, showing that the +2.42% focused result does not
  generalize to every workload.

---

## 9. Verification Commands

Run after every implementation step:

```bash
./gradlew :base:compileJava :base:compileTestJava
./gradlew :base:test --tests \
  "vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.StepperMotorGuideStrictValidationTest"
./gradlew :base:test --tests \
  "vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngineTest"
./gradlew :base:test
```

Run the application-level smoke test after the base suite:

```bash
gradle :testsuite:Tools:TangibleInterfaceGizmoCreator:compileJava
```

Then launch `TangibleInterfaceGizmoCreator`, select
`STEPER_MOTOR_GUIDE`, and confirm the final cavity has:

- a wider circular upper level;
- a narrower lower D level;
- a visible D-shaped floor;
- no collapsed transition face;
- no additional disconnected shell.

---

## 10. Acceptance Criteria

Stage 10 is complete when:

- Every existing `setOp` overload preserves its API and defaults to
  `doStrictValidation=true`.
- The new strict overload is available through
  `PolyhedralBoundedSolidModeler`.
- Strict validation executes for every boolean result path when requested.
- A strict failure throws before an invalid result reaches a subsequent
  operation and includes actionable topology diagnostics.
- Connected-shell and adjusted-Euler summaries are implemented once in the
  validation package and covered by unit tests.
- The legacy stepper failure is either rejected at `B union C` or repaired by
  the kernel and proven valid.
- The corrected stepper partial has one shell and `chi=0`.
- The corrected final guide has one shell and `chi=2`, with a blind D-profile
  pocket of the specified dimensions.
- The complete `:base:test` suite passes.
- Default strict-validation overhead is documented; callers that explicitly
  opt out retain the measured lower-cost legacy path.
