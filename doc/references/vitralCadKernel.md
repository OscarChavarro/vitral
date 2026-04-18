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
- `maximizeFaces()` now uses iterative restart after local mutation (improved over recursion), but still relies on mutation-heavy restart semantics and open-case heuristics.
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
- Harden topology-normalization convergence guarantees (bounded iterative passes, explicit progress metrics, fail-fast on non-convergence).
- Expand deterministic regression corpus around MANT1988 Ch. 14/15 corner cases (coplanar overlap, touching-only, containment, nested rings).
- Strengthen predicate robustness strategy (tolerance calibration suite + repeatability checks across scale ranges).

Priority P2 (important):
- Separate validation from repair semantics in API and internals.
- Add structured operation telemetry (candidate counts, phase timing, branch decisions) for reproducible diagnostics.
- Add large-model stress benchmarks and memory/time budgets as CI gates.

## 7) Final verdict
The kernel is one of the strongest MANT1988-faithful implementations in the project, with clear architectural correspondence to Euler/split/set-op theory and meaningful recent hardening on numeric policy and coplanar classification.

However, excluding curved surfaces as requested, it still does not qualify as production-grade industrial CAD kernel yet. The main blockers are robustness closure in hard boolean neighborhoods, deterministic algebraic behavior, thread-safe/state-safe execution model, and stricter automated quality contracts.

## 8) MANT1988 Problems Backlog (Non-Theoretical, PolyhedralBoundedSolid-Relevant)
This section lists implementation-oriented MANT1988 exercises that are directly relevant to the current B-Rep kernel and can be used as an execution roadmap.

| MANT1988 problem | Current evidence in codebase | Status | What is still missing | Suggested implementation entry points |
|---|---|---|---|---|
| 11.3 (`lkev`, `lkef`) | Implemented in `PolyhedralBoundedSolid` | Implemented | Add stronger inverse-pair property tests (`lmev<->lkev`, `lmef<->lkef`) on random valid solids | `PolyhedralBoundedSolid.java`, new property tests in `base/src/test/...` |
| 11.4 (`lmekr`) | Implemented in `PolyhedralBoundedSolid` | Implemented | Add ring-orientation and face-boundary-order invariants after operation | `PolyhedralBoundedSolid.java`, strict validation extensions |
| 11.5 (`lkimrh`, `lmikrh` family) | Implemented in `PolyhedralBoundedSolid` via explicit `lkimrh/lmikrh` aliases backed by `lkfmrh/lmfkrh` | Implemented | Add dedicated regression tests for alias-equivalence and inverse pairing in complex shells | `PolyhedralBoundedSolid.java`, `base/src/test/...` |
| 12.2 (rotational sweep, endpoint on axis) | Core operator `rotationalSweepExtrudeWireAroundXAxis` added in `GeometricModeler` with axis-endpoint handling path and dedicated tests | Implemented (baseline) | Harden topology regularization so resulting solids are strict-validator clean in all pole-degenerate cases | `GeometricModeler.java`, `GeometricModelerRotationalSweepTest.java` |
| 12.3 (closed figure with edge on axis) | Partially addressed in modified sweep sample | Partial | Formalize constraints, remove ad-hoc branching, add deterministic corpus | `GeometricModeler.java`, dedicated sweep tests |
| 12.4 (`upsweep`) | Conceptually present via sweep/glue flows; explicit book-level routine not isolated | Partial | Implement explicit `upsweep` API + pre/post contracts | `GeometricModeler.java`, operator façade |
| 13.1 (`dropcoord`) | Implemented in face projection logic | Implemented | Expose reusable geometric primitive (currently embedded) and test degenerate normals | `_PolyhedralBoundedSolidFace.java`, `ComputationalGeometry.java` |
| 13.3 (`contfv`) | Functionally covered by `testPointInside` | Implemented | Improve thread-safety of side-channel outputs (`lastIntersected*`) | `_PolyhedralBoundedSolidFace.java` |
| 13.5 (ring-moving utility) | Implemented (`laringmv` path in operator base) | Implemented | Add formal preconditions and failure semantics when target loop assignment is impossible | `PolyhedralBoundedSolidOperator.java` |
| 13.6 (wide-sector test) | Implemented (`checkWideness`) with orientation-consistent signed-angle predicate using face normal (`atan2`) | Implemented | Add targeted regression tests for mirrored sectors, near-colinear limits, and tolerance-boundary cases | `PolyhedralBoundedSolidOperator.java`, `PolyhedralBoundedSolidSetOperator.java`, operator tests |
| 14.1 (`bisector`) | Implemented | Implemented | Add robustness handling for near-colinear edge vectors | `PolyhedralBoundedSolidOperator.java` |
| 14.2 (`cleanup`) | Implemented | Implemented | Separate "rebuild indices" from "silent repair", and document mutation contract | `PolyhedralBoundedSolidOperator.java` |
| 14.3 (slicing from splitting) | Splitter exists; dedicated slicing algorithm absent | Missing | Implement slicing output mode (section polygons/wires) as first-class operator | `PolyhedralBoundedSolidSplitter.java`, new output types |
| 15.1 (containment no-intersection case) | Implemented in `setOpNoIntersectionCase` and tested | Implemented | Expand algebraic conformance tests for all operation pairs and operand orders | `PolyhedralBoundedSolidSetOperator.java`, set-op tests |
| 15.2 (`maximize_faces`) | Implemented (`maximizeFaces`) | Implemented (with known gaps) | Resolve open cases (`Case 2`, nested coplanar-face scenarios), formal convergence criteria | `PolyhedralBoundedSolid.java` |
| 15.3 (eliminate recursion in edge-face comparison) | Migrated to iterative worklist flow | Implemented | Add complexity/performance regression tests under high intersection density | `PolyhedralBoundedSolidSetOperator.java` |
| 15.4 (vertex-face classifier for set-op) | Implemented | Implemented | Reduce heuristic branches for in-plane/coplanar tie-breaking | `PolyhedralBoundedSolidSetOperator.java` |
| 15.5 (`sectoroverlap`) | Implemented | Implemented | Replace tolerance-driven overlap heuristic with exact interval/orientation formalism | `PolyhedralBoundedSolidSetOperator.java` |
| 15.6 (`revert`) | Implemented in solid core | Implemented (partial semantics) | Ensure normal/plane consistency contract post-revert (today explicitly warned as not fully corrected) | `PolyhedralBoundedSolid.java`, validator hooks |
| 15.7 (compute dual results simultaneously) | Not implemented | Missing | Add dual-result mode (`UNION+INTERSECTION`, `A\\B+B\\A`) sharing classification/connect phases | `PolyhedralBoundedSolidSetOperator.java` |
| 15.8 (2D set-op from 3D algorithm) | Not implemented | Missing | Add planar 2D B-Rep profile boolean module reusing classifier core | new package under processing |
| 15.9 (understanding check about `A\\B = A ∩ reverse(B)`) | No explicit executable check | Missing (as assertion suite) | Add theorem/regression test module to codify expected/non-expected equivalences | `base/src/test/...setOperator...` |
| 16.1 (`printproperties`) | No production utility in kernel core | Missing | Add component/genus inspector for diagnostics and QA gates | new analysis utility under geometry/processing |
| 16.2 (`scopy`) | No explicit deep-copy contract for solids | Missing | Add deterministic deep-copy API preserving topology ids optionally | `PolyhedralBoundedSolid.java` |
| 16.4 (undo for transforms) | No transaction-grade undo subsystem | Missing | Introduce operation log / reversible command layer for kernel operations | new transaction module around operators |

Execution guidance:
- Prioritize Chapter 15 and 14 items first (`15.2`, `15.5`, `15.7`, `14.3`) because they directly affect industrial Boolean/split reliability.
- Treat Chapter 16 items as enablers for production operation safety and recoverability.

## 9) Industrial Robustness Strategy (Evidence-Driven)
The table below turns the observed weaknesses into an explicit hardening plan, with concrete steps to replace heuristics by formal operations and tighten contracts.

| Workstream | Current heuristic / weak contract | Formal replacement target | Explicit implementation steps | Contract hardening outcome |
|---|---|---|---|---|
| Sector wideness and orientation | Formal signed-angle predicate already implemented in `checkWideness` and reused by `sectorwide` | Keep signed-angle classification as canonical rule and harden its numeric regression envelope | 1. Preserve face-normal-oriented signed turn (`atan2`) as single source of truth. 2. Add mirror-symmetry and orientation invariance tests. 3. Add stress corpus for near-colinear sectors around tolerance boundaries. 4. Track behavioral deltas before/after numeric-policy tuning. | Deterministic sector classification independent of incidental vector magnitudes, with explicit regression guardrails |
| Coplanar overlap/touching decisions | Probe-point stepping and branchy tolerance comparisons | Interval- and orientation-based coplanar intersection algebra | 1. Project candidate sectors to dominant 2D plane. 2. Compute exact topological relation (disjoint/touching/overlap) via segment interval predicates. 3. Replace probe-step branches with relation table lookup only. 4. Add exhaustive coplanar micro-cases in CI. | Reduced false positives/negatives on coplanar neighborhoods |
| Point-in-solid fallback logic | Multi-ray parity with hardcoded directions and ambiguity fallback | Certified inside/outside with deterministic degeneracy policy | 1. Introduce robust ray selection seeded by model hash + retry budget. 2. Add explicit degeneracy resolver for `LIMIT` cases (symbolic perturbation policy). 3. Record classification confidence/ambiguity in result metadata. 4. Require deterministic replay under fixed seed in tests. | Reproducible containment classification, auditable ambiguity handling |
| Topology normalization (`maximizeFaces`) | Mutation-restart loop with open cases and heuristic loop-size decisions | Rule-complete normalization graph with progress measure and bounded fixpoint | 1. Encode each normalization case as named rule with precondition predicate. 2. Compute rule candidates first, then apply one deterministic priority order. 3. Track monotonic progress metrics (`|edges|`, `|inessential edges|`, etc.). 4. Stop on no-progress and emit structured diagnostic instead of silent drift. 5. Implement missing nested-coplanar-face rule set. | Convergent, explainable normalization with clear non-convergence failure mode |
| Split/set-op static mutable state | Global static arrays/context (`son*`, debug flags) | Per-operation context object with immutable config and local mutable state | 1. Create `SetOpContext` and `SplitContext` classes. 2. Move all static operation-state fields into context instances. 3. Pass context through all phase methods. 4. Keep only read-only lookup tables as static constants. | Thread-safe and reentrant execution model |
| Input mutation semantics | `setOp` mutates operands in-place (ids, face maximization, orientation) | Non-mutating public API with explicit destructive internal variant | 1. Introduce `setOpImmutable(a,b,op)` that deep-copies operands. 2. Rename current behavior to `setOpInPlace` (internal/advanced). 3. Document complexity and memory tradeoffs. 4. Add tests proving input solids are unchanged for immutable API. | Safer production contracts for pipelines and concurrent services |
| Validation behavior | Mixed validate/repair semantics, warnings over typed failures | Strict contract states: `validate`, `repair`, `assertValid` separated | 1. Split validators from mutating repair utilities. 2. Introduce typed exceptions (`TopologyViolation`, `GeometricViolation`, `NonConvergentNormalization`). 3. Add mandatory strict-validation checkpoints on production path. 4. Emit structured failure reports (offending face/edge ids). | Fail-fast, diagnosable correctness guarantees |
| Algebraic consistency | Known-drift tests intentionally assert inequality | Property-based conformance suite with bounded accepted exceptions | 1. Reframe drift tests into target conformance tests by corpus tier. 2. Classify cases into `must-pass`, `known-exception`, `unsupported`. 3. Shrink known-exception set over milestones. 4. Gate releases on must-pass algebraic invariants. | Quantified progress toward CAD-grade Boolean algebra reliability |
| Numeric policy calibration | Fixed global epsilons scaled by model extent only | Context-aware tolerance model + calibration benchmark suite | 1. Add calibration fixtures across scales and aspect ratios. 2. Measure predicate stability under perturbation sweeps. 3. Tune per-predicate tolerances (`angle`, `coplanar`, `interval`). 4. Version tolerance profiles and lock them in CI. | Controlled numeric behavior across industrial scale ranges |
| Observability and forensics | Ad-hoc textual debug outputs | Structured phase telemetry and replayable traces | 1. Emit per-phase counters/timings and branch decisions. 2. Store deterministic replay seed and tolerance context in logs. 3. Add compact JSON trace export for failing cases. 4. Integrate trace diffing in regression triage. | Fast root-cause analysis and reproducible bug diagnosis |

Recommended milestone order:
1. Contracts + context isolation (`setOp` immutability, static-state removal, validation separation).
2. Formal predicate replacement (coplanar relation algebra, containment determinism). (`checkWideness` completed)
3. Normalization completion (`maximizeFaces` missing cases + convergence proofs-in-practice).
4. Algebraic conformance hard gates and telemetry-backed continuous hardening.
