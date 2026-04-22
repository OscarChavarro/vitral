# Optimizations Over MANT1988

## Applied Legacy Strategy Changes

| Level | Applied Change | Intent | Implication / Risk |
|---|---|---|---|
| (1) Base Euler operators | `lmev`, `lmef`, `lkemr`, `lkef`, and related low-level Euler operators were not changed. | Avoid changing the fundamental structural semantics of the B-Rep. | Current failures are not caused by direct edits to base Euler operators, but the restored Boolean strategy exercises them through older usage patterns. |
| (2) Complex Euler operators | `checkWideness` was restored to the legacy predicate based on the cross product against the parent face normal. | Recover the legacy interpretation of concave/wide sectors. | This changes many split/classify paths. It is simpler and older, but less robust than the newer angular computation. |
| (3) Splice | No direct changes were made to splice or half-edge list manipulation routines. | Keep local topology manipulation intact. | There is indirect risk: legacy null-edge ordering can feed different pairs into `join`, `cut`, `lkemr`, and `lkef`. |
| (4) Booleans | `processEdge` was restored from the iterative queue version to the legacy recursive version. | Recover the old edge-subdivision order during `setOpGenerate`. | The operation is again more order-dependent. This may recover old visual cases, but increases risk of repeated traversal or recursion depth issues. |
| (5) Booleans | `sonea` and `soneb` are sorted again before `setOpConnect`. | Recover legacy null-edge pairing. | This helps old cases, but can break correspondences that the newer classifier intentionally emitted in insertion order. |
| (6) Classification | Vertex/face coplanar handling was restored to the old IN/OUT rules based on orientation and operation. | Remove the newer precise coplanar-relation logic. | This is more permissive and closer to the old code; tangential cases may be classified as intersections. |
| (7) Classification | Vertex/vertex coplanar and on-edge handling was restored to legacy rules. | Recover the implicit old sector table used for coincident sectors. | The newer side/orientation-specific correction is lost; intersection and difference are especially affected. |
| (8) Predicates | `sectoroverlap` was restored to the old permissive angular-interval predicate. | Recover broad coplanar-overlap detection. | This can produce false positives: ray-only contact or separated sectors may count as overlap. |
| (9) Tests | Newer conflictive or diagnostic tests were disabled. | Allow the legacy kernel strategy to be validated without experimental specs blocking the build. | `gradle :base:test` passes, but part of the recent coverage is explicitly pending revalidation. |

## Recovery Guidance For Agentic Programming Work

### (1) Base Euler Operators

Scope work around `PolyhedralBoundedSolid` low-level Euler operators first. Treat `lmev`, `lmef`, `lkemr`, `lkef`, `lmfkrh`, `lkfmrh`, and their direct invariants as foundational contracts. Do not change them while debugging Boolean behavior unless a failing case is reduced to a minimal Euler-only repro.

Before editing, add or run focused tests that validate topology after each individual Euler operation. Preserve half-edge mirror links, loop ownership, face ownership, vertex `emanatingHalfEdge`, and global lists. If a Boolean fix appears to need an Euler change, isolate that change behind a very small failing Euler test.

### (2) Complex Euler Operators

Treat wide-sector detection as a shared primitive, not a Boolean detail. Compare the legacy `checkWideness` predicate against any newer angular predicate on the same fixtures before changing it. Record which fixtures depend on degenerate, colinear, concave, and reversed-orientation behavior.

When recovering this level, prefer a compatibility wrapper that can run both legacy and new wideness predicates under trace flags. Make the final choice with fixture evidence, not by elegance. If the angular version is reintroduced, keep the legacy result visible in diagnostics until all dependent Boolean cases are rebaselined.

### (3) Splice

Do not refactor list-splice or half-edge insertion/removal routines during Boolean debugging. Splice defects are expensive to distinguish from classifier defects, so only touch this level after a reduced topology-only reproduction shows incorrect list order, lost half-edges, or invalid loop cycles.

When work is necessary, instrument before changing behavior. Capture loop sequences before and after `join`, `cut`, `lkemr`, `lkef`, and related operations. Any splice recovery must preserve cyclic order, parent loop reassignment, boundary starts, edge ownership, and mirror consistency.

### (4) Booleans: Generation Order

Treat `setOpGenerate` and `processEdge` as order-sensitive. The legacy recursive strategy can produce different subdivision order than an iterative queue, and later phases depend on that order through vertex ids, null-edge insertion, and pairing.

To recover this level, build a small fixture matrix and compare recursive versus iterative traces: generated vertices, `sonva`, `sonvb`, `sonvv`, and resulting mutated operands. Only optimize traversal after the old cases and the selected new cases produce equivalent generation traces.

### (5) Booleans: Null-Edge Connect

Treat `sonea` / `soneb` pairing as a Boolean phase contract. Sorting before connect is legacy behavior; insertion-order pairing is a newer strategy. Do not switch between them casually, because `setOpConnect` assumes corresponding null edges can be joined in paired order.

Recovery work should log the full null-edge list before sorting, after sorting, and before each `canJoin`. For each target fixture, identify whether the correct result depends on sorted pairing or emission-order pairing. If both are needed, introduce an explicit pairing policy selected by local evidence rather than a global guess.

### (6) Classification: Vertex/Face

Recover vertex/face classification by separating geometric relation detection from operation-specific IN/OUT assignment. The legacy code assigns classes from orientation and operation; the newer code attempts precise coplanar relation classification. Both must be compared on the same neighborhood traces.

When editing this level, print or assert the neighborhood sequence before reclassification, after coplanar rules, after `updateLabel`, and after edge reclassification. The goal is not only a valid result, but the expected null-edge insertion count and orientation for both operands.

### (7) Classification: Vertex/Vertex

Treat vertex/vertex classification as the highest-risk Boolean phase. Coincident vertices, coplanar sectors, on-edge sectors, struts, and repeated endpoint recovery interact strongly with connect. Avoid broad rewrites; work from one fixture and one vertex pair at a time.

Recovery should preserve traceability of `nba`, `nbb`, sector pairs, `intersect` flags, and chosen `ha1`, `ha2`, `hb1`, `hb2`. If endpoint recovery is used, prove it does not change legacy-good cases. If legacy rules are used, identify which intersection/difference cases lose necessary side/orientation information.

### (8) Predicates

Predicates must be classified as permissive legacy predicates or precise geometric predicates. Do not silently replace one with the other. A permissive predicate such as legacy `sectoroverlap` can recover old Boolean behavior while also creating false positives in tangential or separated coplanar cases.

When recovering this level, keep predicate tests grouped by intent: overlap, boundary-only contact, disjoint coplanar intervals, reversed orientation, and degenerate sectors. If a Boolean phase requires permissive behavior, name that behavior explicitly and avoid presenting it as mathematically exact.

### (9) Tests

Treat disabled tests as a work queue, not as discarded coverage. Each disabled test should state whether it conflicts with legacy behavior, hits a known fatal path, or encodes a newer experimental strategy. Do not re-enable a broad corpus all at once.

Recovery should proceed by re-enabling one test or one fixture at a time after the relevant level is fixed. Prefer adding narrower tests that capture the recovered behavior before restoring large algebraic or diagnostic suites. Keep `gradle :base:test` green, but do not let green status hide unresolved Boolean correctness questions.

# Code improvement impact matrix

Following table covers two commited versions of PolyhedralBoundedSolid suport:
- Legacy logic: the one in commit 7762ecfb947e8fcb49652bb9ec43b649f8f59a89
- New logic: the one in commit d7c1bb2c9aed0a177d99f522e4e284174ecb4539

Having a green check means test worked, red cross means test not working and empty means not covered - not checked.

| model name / operation | legacy logic | new logic | change (1) | change (2) | change (3) | change (4) | change (5) | change (6) | change (7) | change (8) | change (9) |
|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| MANT1986_2 + UNION | ✅ | ✅ | - | - | - | - | - | - | - | - | - |
| MANT1986_2 + INTERSECTION | ✅ | ✅ | - | - | - | - | - | - | - | - | - |
| MANT1986_2 + A-B | ✅ | ✅ | - | - | - | - | - | - | - | - | - |
| MANT1986_2 + B-A | ✅ | ✅ | - | - | - | - | - | - | - | - | - |
| STACKED_BLOCKS + UNION | ✅ | ✅ | - | - | - | - | - | - | - | - | - |
| STACKED_BLOCKS + INTERSECTION | ❌ | ✅ | - | - | - | - | - | - | - | ⬆️ | - |
| STACKED_BLOCKS + A-B | ❌ | ✅ | - | - | - | - | - | - | - | ⬆️ | - |
| STACKED_BLOCKS + B-A | ❌ | ✅ | - | - | - | - | - | - | - | ⬆️ | - |
| MOON_BLOCK + UNION | ✅ | ✅ | - | - | - | - | - | - | - | - | - |
| MOON_BLOCK + INTERSECTION | ✅ | ✅ | - | - | - | - | - | - | - | - | - |
| MOON_BLOCK + A-B | ✅ | ✅ | - | - | - | - | - | - | - | - | - |
| MOON_BLOCK + B-A | ✅ | ✅ | - | - | - | - | - | - | - | - | - |
| CROSS_PAIR + UNION | ✅ | ✅ | - | - | - | - | - | - | - | ⬇️ | - |
| CROSS_PAIR + INTERSECTION | ❌ | ❌ | - | - | - | - | - | - | - | - |   |
| CROSS_PAIR + A-B | ❌ | ❌ | - | - | - | - | - | - | - | - |   |
| CROSS_PAIR + B-A | ❌ | ❌ | - | - | - | - | - | - | - | - |   |
| HOLLOW_BRICK + UNION | ✅ | ✅ | - | - | - | - | - | - | - | ⬇️ | - |
| HOLLOW_BRICK + INTERSECTION | ✅ | ❌ | - | ⬇️ | - | - | - | - | - | ⬇️ | - |
| HOLLOW_BRICK + A-B | ✅ | ❌ | - | ⬇️ | - | - | - | - | - | ⬇️ | - |
| HOLLOW_BRICK + B-A | ✅ | ❌ | - | ⬇️ | - | - | - | - | - | ⬇️ | - |
| MANT1988_6_13 + UNION | ❌ | ✅ |   |   |   |   |   |   |   | - |   |
| MANT1988_6_13 + INTERSECTION | ❌ | ❌ | - | - | - | - | - | - | - | - |   |
| MANT1988_6_13 + A-B | ❌ | ❌ | - | - | - | - | - | - | - | - |   |
| MANT1988_6_13 + B-A | ❌ | ❌ | - | - | - | - | - | - | - | - |   |
| MANT1988_15_1 + UNION | ✅ | ✅ | - | - | - | - | - | - | - | - | - |
| MANT1988_15_1 + INTERSECTION | ✅ | ❌ | - | ⬇️ | - | - | - | - | - | ⬇️ | - |
| MANT1988_15_1 + A-B | ✅ | ❌ | - | ⬇️ | - | - | - | - | - | ⬇️ | - |
| MANT1988_15_1 + B-A | ❌ | ❌ | - | - | - | - | - | - | - | - |   |
| MANT1986_3 + UNION | ❌ | ❌ | - | - | - | - | - | - | - | - |   |
| MANT1986_3 + INTERSECTION | ❌ | ❌ | - | - | - | - | - | - | - | - |   |
| MANT1986_3 + A-B | ❌ | ❌ | - | - | - | - | - | - | - | - |   |
| MANT1986_3 + B-A | ❌ | ❌ | - | - | - | - | - | - | - | - |   |
| MANT1988_15_2_HOLED + UNION | ❌ | ❌ | - | - | - | - | - | - | - | - |   |
| MANT1988_15_2_HOLED + INTERSECTION | ✅ | ✅ | - | - | - | - | - | - | - | - |   |
| MANT1988_15_2_HOLED + A-B | ❌ | ❌ | - | - | - | - | - | - | - | - |   |
| MANT1988_15_2_HOLED + B-A | ❌ | ❌ | - | - | - | - | - | - | - | - |   |
| CSG_LAMP_SHELL | ✅ | ✅ | - | - | - | - | - | - | - | - | - |
| FEATURED_OBJECT | ✅ | ❌ | - | - | - | - | ⬇️ | - | - | ⬇️ | - |
| CSG_KURLANDER_BOWL | ❌ | ❌ | - | - | - | - | - | - | - | - |   |
