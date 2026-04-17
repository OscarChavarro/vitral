# CAD Kernel Audit

## 1) Scope and method
This audit evaluates the current B-Rep CAD kernel quality against [MANT1988], focused on:

- `base/src/main/vsdk/toolkit/environment/geometry/polyhedralBoundedSolid`
- `base/src/main/vsdk/toolkit/processing/polyhedralBoundedSolidOperators`
- `testsuite/VSDKExamples/PolyhedralBoundedSolidExample`

And contrasts the result with the previous content of this same document.

Important scope decision for this audit:
- Lack of curved-surface support is intentionally excluded from the gap analysis.

## 2) MANT1988 baseline used for comparison
From the reference chapters:

- Ch. 9 and Ch. 10: Euler operators and half-edge structure provide topological construction power, but geometric correctness is a separate concern.
- Ch. 14: splitting should be closed, general, and robust under numerical inaccuracies.
- Ch. 15: boolean set-op workflow is reduction -> classify -> connect -> finish; handling of coplanar and touching cases is central; 2-manifolds are not closed under set operations, so pseudomanifold-compatible handling is needed.

## 3) Current implementation assessment

## 3.1 B-Rep core (`PolyhedralBoundedSolid`)
Strengths:
- Strong conceptual alignment with MANT1988 operator vocabulary (`mvfs/kvfs`, `lmev/lkev`, `lmef/lkef`, `lkemr/lmekr`, `kfmrh/lmfkrh`, etc.).
- Data model reflects half-edge hierarchy from the book.

Observed weaknesses:
- `maximizeFaces()` uses recursive restart after local mutation; this is fragile under complex models and hard to reason about for worst-case behavior.
- `maximizeFaces()` still contains an explicit open status (`Case 2: Not tested!`) and an old TODO-level limitation about fully nested coplanar faces.
- Some operations are permissive and rely on warnings/fatal reports rather than transactional failure semantics.

Assessment: architecturally solid, operationally still brittle in hard topology normalization paths.

## 3.2 Splitter (`PolyhedralBoundedSolidSplitter`)
Strengths:
- High structural fidelity with Ch. 14 decomposition (`splitGenerate`, `splitClassify`, `splitConnect`, `splitFinish`).
- Numeric policy integration is present.

Observed weaknesses:
- The code explicitly documents local orientation adjustments and extra heuristics not formalized in the original algorithm.
- There are still comments indicating uncertainty in behavior assumptions (e.g., branch when no intersections are found: "Plane should be tested here before assuming this order!").
- Internal style remains mutation-heavy with complex control flow and list-side effects.

Assessment: functionally rich and close to textbook flow, but not yet hardened for all degenerate/edge distributions expected in industrial geometry pipelines.

## 3.3 Set operations (`PolyhedralBoundedSolidSetOperator`)
Strengths:
- Macro-pipeline strongly aligned with Ch. 15 (`setOpGenerate`, `setOpClassify`, `setOpConnect`, `setOpFinish`).
- Explicit no-intersection policy exists (disjoint/touching/containment).
- Coplanar rules are now table-driven for vertex-face and vertex-vertex classification, which is a positive hardening step.
- Uses numeric context (`PolyhedralBoundedSolidNumericPolicy`) instead of scattered tolerances.

Observed weaknesses:
- Implementation keeps extensive static mutable state (`son*` sets, debug flags, numeric context wiring), which is not thread-safe and complicates reentrancy.
- Inputs are mutated in-place during set-op flow (`compactIds`, `maximizeFaces`, id remapping, etc.), making API behavior side-effectful and risky for product use unless clearly isolated by cloning.
- Robustness still depends on heuristic geometric predicates in difficult coplanar/touching neighborhoods.

Assessment: very good research-grade realization of MANT1988 architecture, but not yet production-grade in determinism, side-effect isolation, and adversarial robustness.

## 3.4 Validation subsystem
Strengths:
- Validation is modularized (`validateIntermediate`, `validateStrict`) with strategy composition.
- Strict validators include loop self-intersection and face-face improper intersection checks.
- Numeric policy is centralized and propagated through validators.

Observed weaknesses:
- Core operator flows commonly gate on `validateIntermediate`; strict validation is not consistently part of post-op contracts.
- Topological repair helper behavior can modify model content (`remakeEmanatingHalfedgesReferences` removes vertices with null emanating halfedge), mixing validation and mutation concerns.

Assessment: good direction, but contract separation (validate vs repair) and strict-by-default safety profile are still incomplete.

## 3.5 Test and example reality
Strengths:
- Test suite now includes parameterized set-op scenarios (disjoint, touching, containment, MANT1988 fixtures).
- Validation and numeric policy have dedicated tests.

Observed weaknesses:
- `PolyhedralBoundedSolidSetOperatorAlgebraicPropertiesTest` currently encodes known algebraic drift by asserting that idempotence/absorption equivalences do not hold in selected corpus cases.
- Example/harness code still contains commented-out stress paths and commented-out validation lines in CSG flows.
- Visual example remains useful for debugging, but it is not a deterministic quality gate.

Assessment: coverage is improving and now honestly captures known failure classes, but correctness envelope is still below industrial confidence levels.

## 4) Comparison vs previous version of this document
Relative to the previous `vitralCadKernel.md`, this update keeps the same high-level verdict (strong conceptual alignment, robustness debt concentrated in hard boundary cases) but clarifies and tightens several points:

- Confirms that coplanar/touching handling moved from ad-hoc logic toward explicit decision tables.
- Adds explicit emphasis on non-thread-safe static mutable state in operators.
- Adds explicit emphasis on side-effectful API behavior (input solids mutated by set-op pipeline).
- Updates test interpretation: there is now deliberate codification of known algebraic drift in automated tests, not only commented visual scenarios.
- Separates validation maturity from production contract maturity: validators exist, but strict-mode enforcement is not yet systemic.

## 5) Production readiness evaluation (curved surfaces excluded)
Current maturity level:
- Advanced research prototype / early pre-production kernel.

Why not production yet:
- Determinism and robustness under adversarial coplanar/touching configurations are not fully guaranteed.
- Algebraic properties expected by industrial CAD boolean engines are known to drift in selected corpora.
- Thread safety and side-effect isolation are insufficient for concurrent, service-style, or transactional CAD backends.
- Quality gates still rely partly on manual/visual workflows and known-failure expectations.

## 6) What is missing for industrial CAD readiness
Priority P0 (must-have):
- Enforce non-mutating public boolean API contract (clone/isolate operands or provide explicit destructive variants).
- Remove or encapsulate static mutable operator state for thread-safe execution.
- Turn known algebraic drift corpus into pass criteria (not expected-failure criteria) for the target support domain.
- Define and enforce strict validation checkpoints after split/set-op in production path.

Priority P1 (high):
- Replace recursive restart patterns in topology normalization with bounded iterative passes and explicit convergence controls.
- Expand deterministic regression corpus around MANT1988 Ch. 14/15 corner cases (coplanar overlap, touching-only, containment, nested rings).
- Strengthen predicate robustness strategy (tolerance calibration suite + repeatability checks across scale ranges).

Priority P2 (important):
- Separate validation from repair semantics in API and internals.
- Add structured operation telemetry (candidate counts, phase timing, branch decisions) for reproducible diagnostics.
- Add large-model stress benchmarks and memory/time budgets as CI gates.

## 7) Final verdict
The kernel is one of the strongest MANT1988-faithful implementations in the project, with clear architectural correspondence to Euler/split/set-op theory and meaningful recent hardening on numeric policy and coplanar classification.

However, excluding curved surfaces as requested, it still does not qualify as production-grade industrial CAD kernel yet. The main blockers are robustness closure in hard boolean neighborhoods, deterministic algebraic behavior, thread-safe/state-safe execution model, and stricter automated quality contracts.
