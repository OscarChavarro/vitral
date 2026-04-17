# Vitral CAD Kernel Audit (MANT1988 Alignment)

## 1) Scope and Method
This audit compares the current Java CAD kernel implementation against the theory and algorithmic intent in MANT1988, with focus on:

- `PolyhedralBoundedSolid` core B-Rep and Euler operators.
- Set operations and split flow (`PolyhedralBoundedSolidSetOperator`, splitter helpers, and `GeometricModeler` wrappers).
- Real execution/test flows in `PolyhedralBoundedSolidModelingTools` and the visual debugger app `PolyhedralBoundedSolidExample`.

Primary theory baseline reviewed from OCR PDFs:

- Chapter 9: Euler operators.
- Chapter 10: Half-edge data structure and tolerance model.
- Chapter 12: Sweeping and loop gluing.
- Chapter 14: Splitting algorithm requirements (closure, generality, robustness).
- Chapter 15: Boolean set operations (boundary classification, limitations, and open problems).

## 2) Theory Baseline (What the Book Explicitly Requires)

## 2.1 Core constraints
- Chapter 14 states splitting must be closed, general, and robust under small numerical inaccuracies.
- Chapter 15 states Boolean B-Rep is hard because of broad intersection case coverage and numerical sensitivity in overlap/coplanarity/intersection tests.
- Chapter 15 states 2-manifolds are not closed under Boolean operations; pseudomanifold-compatible behavior is necessary.

## 2.2 Boolean workflow structure
- Chapter 15 decomposes into: reduction, classification, connect, finish.
- Boundary classification is central, including handling of "on" cases through reclassification rules.
- Chapter 15 assumes maximal faces before reduction.

## 2.3 Important known limitations in the book itself
- Chapter 15 final remarks: algorithm does not directly extend to curved-face and nonmanifold general cases.

## 2.4 Euler operators are not enough by themselves
- Chapter 9: soundness is topological/syntactic; it does not guarantee geometric validity.
- Practical implication: separate geometric validation and robust predicates are mandatory in real kernels.

## 3) Current Java Flow Analysis

## 3.1 Visual debugger execution path
- `PolyhedralBoundedSolidExample` routes `solidType` to builders in `PolyhedralBoundedSolidModelingTools`.
- Interactive keys switch among Euler, sweep, split, CSG, and stress cases.
- CSG debug can dump intermediate stages via `withDebug`.

## 3.2 Modeling tools role
- `PolyhedralBoundedSolidModelingTools` mirrors textbook-style examples (Euler, sweep, gluing, split, CSG).
- It is both demo harness and de facto integration testbed.
- Several stress paths are currently commented out, which indicates unstable or unfinished paths are known but not systematically isolated.

## 3.3 Boolean engine flow
- `setOp` pipeline in `PolyhedralBoundedSolidSetOperator` follows textbook phase structure:
  1. Normalize/prepare (`compactIds`, `validateIntermediate`, `maximizeFaces`, `updmaxnames`).
  2. `setOpGenerate` (vertex-face and vertex-vertex candidates).
  3. `setOpClassify` (vertex-face and vertex-vertex classifiers).
  4. `setOpConnect` (null-edge pairing/joining).
  5. `setOpFinish` (face movement, gluing, compaction, cleanup).
- This is a strong architectural match to MANT1988 Chapter 15.

## 4) Strengths
- Strong conceptual alignment with MANT1988 chapter structure and nomenclature.
- Explicit support for low-level and high-level Euler operators.
- Split and set-op are built over reusable operator primitives (`PolyhedralBoundedSolidOperator`), which is structurally sound.
- Numeric tolerance handling now uses a dedicated policy (`PolyhedralBoundedSolidNumericPolicy`) with `BREP_EPSILON`, `BREP_BIG_EPSILON`, relative scale, and centralized predicate helpers.
- Existing debug infrastructure (stage outputs, optional offline renderer, visual app) is a valuable foundation for hardening.

## 5) Audit Findings (State, Completeness, Quality)

## 5.1 Completeness status
- **Euler core**: substantial but partial in global operators and ring movement semantics.
- **Split**: mostly implemented with extra heuristics, but not fully formalized for all corner cases.
- **Boolean set operations**: implemented end-to-end but with explicit unsupported/untested branches and known theoretical gaps.
- **Validation layer**: intermediate and strict flows are in place (planarity, topology, loop strictness, face-face improper intersections), now backed by scale-aware numeric policy.

## 5.2 Critical robustness risks

### A) Unsupported branches in vertex-vertex classifier/connectivity path
- `separateEdgeSequence` prints explicit `"NOT SUPPORTED CASE A..E"`.
- There are comments marking untested paths and fallback reversals in sector reclassification.
- Impact: fragile behavior in complex coplanar/coincident neighborhoods.

### B) Recursive mutation style increases fragility
- `maximizeFaces` recursively restarts after each mutation.
- `processEdge` recurses after splitting during edge-face processing.
- Impact: stack depth risk and hard-to-predict behavior on high-complexity models.

### C) Numerical Robustness Re-evaluation (Phase 3)
- `PolyhedralBoundedSolidNumericPolicy` now defines `BREP_EPSILON`, `BREP_BIG_EPSILON`, and scale-aware `ToleranceContext`.
- Validation execution now uses one numeric context per run (`validateIntermediate` and `validateStrict`), passed through validation strategies.
- Geometric strict checks (loop self-intersection, loop-loop intersection, face-face improper intersections) now consume centralized policy predicates.
- Splitter and set-operator classification predicates were migrated from direct `VSDK.EPSILON` usage to policy-driven thresholds.
- The previous ad hoc multipliers in B-Rep validation/operator paths were replaced by policy methods and named tolerances.

## 5.3 Evidence from real flows (`ModelingTools` + visual app)
- `buildCsgTest4` already documents a known topological issue for `maximizeFaces` under union.
- `buildCsgTest5` has a larger CSG composition chain commented out and replaced with a reduced scenario, indicating unresolved instability in deep CSG compositions.
- `csgTest` has post-operation validation lines commented out.
- `eulerOperatorsTest` leaves stronger tests commented and returns a minimal partially built structure path; this can produce invalid states in visual workflows.
- The debugger app exposes many scenarios through one key-path, but there is no deterministic pass/fail harness tied to these scenarios.

## 6) Gap Matrix: Java Implementation vs MANT1988 Intent

| Topic | MANT1988 intent | Current Java status | Assessment |
|---|---|---|---|
| Euler operator foundation | Full constructive basis with syntax discipline | Implemented broadly, with some partial/global limitations | Medium-High |
| Geometric validity beyond topology | Must be enforced separately | Intermediate/strict validators available; coverage corpus still pending | Medium |
| Split closure/generality/robustness | Explicit requirement (Ch14) | Structurally aligned, heuristic-heavy, now using centralized numeric policy | Medium |
| Boolean reduction/classify/connect/finish | Explicit phase architecture (Ch15) | Implemented with strong structural match | High |
| Maximal face precondition | Required before robust set-op reduction | Implemented via `maximizeFaces`, but method has known open cases | Medium |
| Curved/nonmanifold generality | Book states algorithm does not directly cover | Not covered; no formal guardrail policy | Medium-Low |
| Numerical tolerance model | Multiple tolerances and robust tests expected in practice | Scale-aware policy integrated (`BREP_EPSILON`/`BREP_BIG_EPSILON`, centralized predicates) | Medium-High |

## 7) Hardening Strategy (Roadmap Table)

| Phase | Goal | Main Actions | Deliverables | Exit Criteria |
|---|---|---|---|---|
| 4. Boolean Completeness | Close remaining algorithmic gaps | Complete/replace unsupported cases A-E in edge-sequence separation; formalize coplanar overlap and touching-only handling | Boolean completion patch + scenario tests | Correct results for disjoint, coplanar-overlap, touching-only, and unsupported A-E suites |
| 5. Regression Corpus | Prevent future regressions | Build deterministic corpus from MANT1986/MANT1988 figures and existing sample generators; add property tests (idempotence, commutativity where applicable, orientation consistency) | Reproducible test suite + seed catalog | CI gates on geometric/topological invariants and known hard scenarios |
| 6. Performance + Observability | Make hard cases debuggable and practical | Add structured stage logs (generate/classify/connect/finish), timing counters, and candidate-pair stats; optional acceleration for edge-face comparisons | Perf telemetry and debug artifacts per case | Large-case runtime and diagnosis quality improve without correctness loss |

## 8) Priority Recommendations
- Re-enable currently commented CSG stress paths behind a `known_failures` gate and track each failure class.
- Freeze a baseline corpus from existing `SimpleTestGeometryLibrary` and `PolyhedralBoundedSolidModelingTools` scenarios before deeper refactors.
- Add deterministic regressions for disjoint, coplanar-overlap, touching-only, and unsupported A-E classifier/connectivity scenarios.

## 9) Final Assessment
The kernel is a strong research-grade implementation with high conceptual fidelity to MANT1988, especially in algorithm decomposition and operator vocabulary. Numerical robustness improved materially with a centralized scale-aware tolerance policy and stricter validation contracts.

The main remaining robustness debt is concentrated in unsupported A-E classifier/connectivity branches, coplanar/touching edge cases, and systematic regression coverage.
