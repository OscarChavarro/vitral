# Stage 4 — CSG Boolean Fix: Moon Motifs Investigation

**Date:** 2026-05-31
**Authors:** AI coding agents (Claude Sonnet 4.6, Claude Opus 4.8)
**Status:** In progress — 20/20 star motifs working, 0/20 moon motifs working (with the full curve-components algorithm; ~8/20 with the committed singleton guard fallback)

---

## 1. Background and Prior Stages

The Kurlander Bowl is a CAD kernel stress test: a hemispherical shell from which 40 motif solids (20 "stars" = prisms, 20 "moons" = cylinder-minus-cylinder crescents) are subtracted one at a time. The test is structured as four boolean operations per motif (A−B, B−A, A∩B, A+B). The kernel is implemented in `base/src/main/vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/`.

Full history of prior stages is in:
- `doc/plan-csg-boolean-fix-stage1.md`
- `doc/plan-csg-boolean-fix-stage2.md`
- `doc/plan-csg-boolean-fix-stage3.md`
- `KurlanderBowl.md` — original root-cause analysis
- `KurlanderBowlFixPlan.md` — the active executable plan (read before acting)

---

## 2. Main Achievement: All 20 Star Motifs Now Work (commit `6222bae9`)

**Model:** Claude Opus 4.8

**Root cause found and fixed for stars:** The `groupNullEdgesByRing()` method in `_PolyhedralBoundedSolidSetNullEdgesConnector.java` was applying a spatial-signature sort to null-edge pairs that are zero-length struts (both endpoints at the same geometric point but with distinct vertex IDs). Since struts form size-1 rings under vertex-ID chaining, the sort had no curve information to work from and simply scrambled the classifier's already-valid emission order. For star motifs, the classifier emits null-edges in a valid `scanjoin`-compatible curve-traversal order; the sort was destroying it.

**The fix (working tree, building on the commit):** Added a vertex-ID singleton check before any reordering. If no two null-edges share a vertex ID (= all are isolated struts with no topological adjacency between them), the classifier's insertion order is preserved. This is a necessary condition because the two vertex IDs of a genuine curve-adjacent null-edge pair ARE shared with the next null-edge along the curve. If there is sharing, the code falls through to a connected-component reordering by face adjacency (`nullEdgesShareFace`). The face-adjacency algorithm handles multi-ring cases (e.g. the shell-cylinder construction) while the singleton guard handles the star and moon all-strut cases.

**What was ruled out during investigation:**
- Position-key chaining (plan's literal prescription) — null-edges are isolated struts that do NOT share positions; confirmed empirically by dumping `DBG-ne` trace.
- Modifying `scanjoin` or `neighbor` — those are correctly implemented per Mäntylä §15.7.
- The `partitionNullEdgesIntoRings` vertex-ID ring partition — it was already the right gate; the new code reproduces its check directly.

**Regression test:** `KurlanderBowlStarInvariantTest` (20 parameterized tests, one per star motif) ensures A−B is non-empty, passes `validateIntermediate`, and passes `validateConsistentFaceOrientations`. This test is in the test suite and must stay green.

---

## 3. Open Problem: Moon Motifs

**Model for this section:** Claude Sonnet 4.6 (investigation), Claude Opus 4.8 (earlier analysis)

### 3.1 Symptom

With the current code (committed `6222bae9` + working tree changes), all 20 moon motifs produce an **EMPTY result** (the bowl disappears entirely) when A−B is computed. The raw operands A (bowl) and B (moon crescent) are individually **clean and valid** for all 20 moons (45 faces, 86 vertices, `validateIntermediate=true`), confirmed by `KbMoonCap` and `KbCapShape` harnesses.

In the committed state (simple singleton guard only), 8 of 20 moons were OK. The promoted curve-components algorithm inadvertently broke these 8 because the moon's null-edges DO share face IDs (many null-edges land on the same large concave crescent cap face), triggering the face-adjacency reordering and scrambling the emission order.

### 3.2 Root Cause (confirmed by instrumentation)

**Instrumentation added** (working tree, gated by `vsdk.setop.tracePipelineSummary`):  
`traceSelfTouchingLoops(solid, label)` in `PolyhedralBoundedSolidSetOperator`, called after `setOpGenerate` for both operands. `splitSelfTouchingLoops(solid)` is also present (see §3.3).

**Confirmed finding for motif 24 (representative EMPTY moon):**

After `setOpGenerate`, before `setOpClassify`:
```
[SelfTouch] B-after-generate face=205 loop=0 size=33
    idx[3]v284==idx[10]v366  NON-ADJACENT(pinch)  at (-0.7128,-0.5556,0.5073)
    idx[5]v283==idx[28]v363  NON-ADJACENT(pinch)  at (-0.7660,-0.5172,0.5073)
```

- **Only operand B (the moon)** has self-touching loops; operand A (the bowl) has none.
- **Exactly one face is affected:** face 205, the **concave crescent cap** — a single 33-vertex boundary loop that is geometrically pinched at the two crescent cusps.
- **The pinch is non-adjacent** (idx 3 ≡ idx 10, idx 5 ≡ idx 28 in the loop) — a genuine figure-8 / self-touching boundary, not a zero-length strut.
- **Cause:** at each cusp, a new intersection vertex coincides with the existing cusp vertex but was not merged by the post-Generate weld (`weldCoincidentVerticesIntoExisting`). Two distinct vertex IDs end up at the same position within one boundary loop.
- **Effect on Connect:** a pinched (self-touching) loop is not a simple polygon. `setOpClassify` / `setOpConnect` cannot form a clean cut ring from it → `sonfa=0` → `setOpFinish` produces an empty solid. The bowl disappears.
- **Why stars are not affected:** star prism faces are convex with no cusps; the sphere intersection curve never produces a self-touching boundary on any star face.

Visual confirmation: render `CSG_OPERAND2_PARTIAL` for EMPTY moons (motif 23, 24) shows a tangled, multi-loop broken cut face. For OK moons (motif 20), the cut face is a clean elongated cylinder section. For operand A (bowl) with motifs 21 and 24 side-by-side, the bowl looks identical — the problem is entirely in operand B.

**Important diagnostic note:** size-2 loops that appear as "self-touching" AFTER Classify are NOT bugs — they are the null-edge struts the algorithm creates by design (one pair of coincident-position vertices per strut). Scanning for self-touch is only meaningful **after Generate, before Classify**, when size-2 struts do not yet exist. The diagnostic is gated to avoid confusion.

### 3.3 Fix Attempted: `splitSelfTouchingLoops`

**What was added** (working tree, `PolyhedralBoundedSolidSetOperator.java`):

`splitSelfTouchingLoops(solid)` — called on both operands after `setOpGenerate` and before `setOpClassify`. For each boundary loop of size > 2, it finds non-adjacent coincident-position vertex pairs (the pinch) and applies `lmef(solid, he_a, he_b, newId)` to split the pinched loop into two simple loops. Iterates until no pinch remains.

**What happened:**

The split fires correctly for motif 24 (confirmed via `DBG-split` trace). After splitting, the crescent cap's pinched 33-vertex loop is divided at the first pinch; the second pinch falls in opposite sides of the split and is thereby resolved. However, all 20 moon motifs still produce EMPTY results with the split active.

Investigation revealed that the cause is the **face-adjacency curve-components algorithm**, not the split itself:
- When the split fires, it changes the moon's topology. But the face-adjacency reordering (triggered because moon null-edges share face IDs on the crescent cap) still scrambles the Connect order → EMPTY.
- When the split is disabled and the curve-components are reverted to the vertex-ID singleton guard, the 8 previously-OK moons return. But the split then has no effect on the EMPTY moons because the Connect phase still fails.

**Current state of working tree:**
- `groupNullEdgesByRing` uses the vertex-ID singleton check (reverted from the all-curve-components promotion) + curve-components fallback for genuinely multi-ring cases.
- `splitSelfTouchingLoops` is present and called (enabled), but its benefit is masked by the vertex-ID singleton guard reverting the order to insertion order, which for EMPTY moons is already the wrong order regardless of the split.

### 3.4 The Circular Dependency

The moon problem has a circular dependency between two layers:

| Layer | Status |
|-------|--------|
| **Generate**: crescent cap gets pinched loop | `splitSelfTouchingLoops` attempts to fix this, but the resulting new face topology changes how null-edges are distributed → Connect sees different pairs |
| **Connect**: ordering of null-edges from the moon's crescent cap determines whether `scanjoin` closes the pairs | The face-adjacency sort breaks previously-OK moons; the vertex-ID singleton guard preserves 8 OK moons but can't help the other 12 because their insertion order from Classify is wrong |

The two fixes interfere. Fixing Generate (split) without also fixing Connect order produces EMPTY. Fixing Connect order without fixing Generate produces EMPTY or broken cut faces for many moons.

---

## 4. Key Diagnostic Strategy for Agents Continuing This Work

**⚠️ Critical advice (learned the hard way):**  
When investigating moon failures, **DO NOT look at the result of the boolean operation** — the result disappears (EMPTY, 0 faces) before you can extract any information from it. Instead:

1. **Inspect operand B (`CSG_OPERAND2_PARTIAL`)** — the moon after `setOpGenerate`. This shows the cut face / double boundary that the sphere intersection created on the moon's crescent cap. A broken (tangled/multi-loop) cut face here means the Generate phase produced a self-touching boundary.

2. **Inspect operand A (`CSG_OPERAND1_PARTIAL`)** — the bowl after `setOpGenerate`. Comparing A across different motifs (OK vs EMPTY) reveals whether the problem is in the bowl geometry or in the moon.

3. **Use `traceSelfTouchingLoops`** (gated by `-Dvsdk.setop.tracePipelineSummary=true`) to find which face and loop have the self-touch, and at which loop indices.

4. **Compare pipeline trace lines** `connect end sonfa=N looseA=M` — for a correct operation, `sonfa≥1` and `looseA=0`. EMPTY moons end with `sonfa=0 looseA>>0`.

The renderer for `CSG_OPERAND2_PARTIAL` is the **canonical diagnostic tool for moons**. A clean operand B (smooth cylinder section) is necessary but not sufficient for a correct result. A broken/tangled operand B guarantees failure.

---

## 5. Active Instrumentation Inventory

This section lists every piece of diagnostic/debug code added during stages 3–4 that is currently in the working tree. It must be kept up to date so future agents know what to clean up when the moon problem is solved.

### 5.1 Gated instrumentation — safe to keep long-term

These are controlled by a system property and produce **no output in normal runs**.  
Gate property: `-Dvsdk.setop.tracePipelineSummary=true`

| File | Method / Symbol | What it does | Remove when? |
|------|----------------|--------------|-------------|
| `PolyhedralBoundedSolidSetOperator.java` | `traceSelfTouchingLoops(solid, label)` (private static, ~60 lines) | Scans all boundary loops after Generate; prints `[SelfTouch]` lines for every non-adjacent coincident vertex pair. Gated via `Boolean.getBoolean("vsdk.setop.tracePipelineSummary")`. | When moon problem is solved and no more pinch investigation is needed |
| `PolyhedralBoundedSolidSetOperator.java` | Two calls to `traceSelfTouchingLoops(inSolidA/B, ...)` in the main `setOp` pipeline, between `setOpGenerate` and `setOpClassify` | Triggers the above scan | Same as above |
| `PolyhedralBoundedSolidSetOperator.java` | Comment block `// NOTE: after Classify ...` replacing the former `traceSelfTouchingLoops` call after Classify | Documents why post-Classify scanning is suppressed | Can stay as a comment explaining the strut/pinch distinction |
| `_PolyhedralBoundedSolidSetNullEdgesConnector.java` | `dbgDumpNullEdges(label, sone)` (private static, ~20 lines) | Prints `[DBG-ne]` for every null-edge (vertex IDs, face IDs, positions). Gated via `isPipelineSummaryTraceEnabled()`. | When Connect ordering is no longer under investigation |
| `_PolyhedralBoundedSolidSetNullEdgesConnector.java` | Two calls to `dbgDumpNullEdges` inside `groupNullEdgesByRing` | Triggers the above dump | Same as above |

### 5.2 Un-gated debug code — MUST REMOVE before committing

These print to stdout **unconditionally** on every `setOp` call that fires `splitSelfTouchingLoops`. They were left in during active investigation and must be cleaned up before any commit.

| File | Symbol / Line | Output it produces | What to do |
|------|---------------|-------------------|------------|
| `PolyhedralBoundedSolidSetOperator.java` | `private static int dbgSplitCount = 0` field | Accumulates across all setOp calls within a JVM run | **Remove entirely** |
| `PolyhedralBoundedSolidSetOperator.java` | `int beforeCount = dbgSplitCount;` local in `splitSelfTouchingLoops` | N/A (local) | **Remove** |
| `PolyhedralBoundedSolidSetOperator.java` | `int beforeFaces = solid.getPolygonsList().size();` local | N/A (local) | **Remove** |
| `PolyhedralBoundedSolidSetOperator.java` | `dbgSplitCount++;` and `System.out.println("[DBG-split] #" + ...)` inside `splitSelfTouchingLoops` | Prints one `[DBG-split]` line to stdout for every lmef split performed | **Remove the print and counter increment; keep the `lmef` call itself** |
| `PolyhedralBoundedSolidSetOperator.java` | `int fired = dbgSplitCount - beforeCount; if (fired > 0 ...) System.out.println("[DBG-split-summary] ...")` at end of `splitSelfTouchingLoops` | Prints summary per call to stdout | **Remove entirely** |

### 5.3 Functional additions — keep, but decide status

These are **real algorithmic changes** (not debug-only). Their fate depends on whether the moon problem gets solved with or without them.

| File | Symbol | Status | Decision needed |
|------|--------|--------|----------------|
| `PolyhedralBoundedSolidSetOperator.java` | `splitSelfTouchingLoops(solid)` method (~70 lines, minus the debug lines in §5.2) | Correct implementation; splits self-touching boundary loops before Classify | Keep if Connect ordering gets fixed and the split then produces correct results; remove if the problem is solved another way |
| `PolyhedralBoundedSolidSetOperator.java` | Two calls `splitSelfTouchingLoops(inSolidA/B)` between Generate and Classify | Active in every setOp call | Keep or remove with the method above |
| `_PolyhedralBoundedSolidSetNullEdgesConnector.java` | Vertex-ID singleton check in `groupNullEdgesByRing` (~40 lines): HashSet-based check for shared vertex IDs → if none, preserve insertion order | Active, fixes all 20 star motifs | **Keep** — this is the committed behavior for stars; functionally correct |
| `_PolyhedralBoundedSolidSetNullEdgesConnector.java` | Face-adjacency curve-components: `curveComponentFind`, `nullEdgeFaceIds`, `nullEdgesShareFace` helpers + the LinkedHashMap reordering block | Active when shared vertex IDs exist | Keep as the multi-ring case handler; verify it does not regress reference tests |

### 5.4 Cleanup script (for when the moon problem is resolved)

Before committing the final moon fix, run through this checklist:

```
[ ] Remove dbgSplitCount field (PolyhedralBoundedSolidSetOperator.java)
[ ] Remove beforeCount, beforeFaces locals and the [DBG-split] / [DBG-split-summary] println calls
[ ] Decide fate of splitSelfTouchingLoops method + its two call sites
[ ] Decide fate of traceSelfTouchingLoops method + its two call sites
[ ] Decide fate of dbgDumpNullEdges method + its two call sites
[ ] Confirm gradle :base:cleanTest :base:test → 0 failures
[ ] Confirm KurlanderBowlStarInvariantTest → 20/20 green
[ ] Confirm BooleansFromReferenceObjectPairsTest → all green
```

---

## 6. Files Changed in Working Tree (not yet committed)

| File | Change |
|------|--------|
| `...PolyhedralBoundedSolidSetOperator.java` | Added `splitSelfTouchingLoops(solid)`, `traceSelfTouchingLoops(solid, label)`, calls to both after `setOpGenerate`; added `dbgSplitCount` counter (needs cleanup per §5.2) |
| `..._PolyhedralBoundedSolidSetNullEdgesConnector.java` | `groupNullEdgesByRing`: hybrid vertex-ID singleton guard + face-adjacency curve-components. Added `curveComponentFind`, `nullEdgeFaceIds`, `nullEdgesShareFace` helpers. Dead code removed: `partitionNullEdgesIntoRings`, `ringCentroidXYZ`, `ringAverageRadius`, `sortRingsBySignature`. |

---

## 7. Recommended Next Steps for Agents

### Priority 1 — Understand the 8 OK vs 12 EMPTY split under insertion order

With the vertex-ID singleton guard (all moons use insertion order from Classify), 8 moons are OK and 12 are EMPTY. The 12 EMPTY ones have a wrong insertion order. **Why?**

Hypothesis: the 12 EMPTY moons have their crescent cap oriented so that the bowl sphere intersects the crescent cap boundary in a way that the V/F and V/V classifiers emit null-edge pairs in a non-curve-monotonic order. The 8 OK moons happen to have their crescent cap oriented so the emission order IS curve-monotonic.

To verify: run `dbgDumpNullEdges` trace for one OK moon (e.g. motif 21) and one EMPTY moon (e.g. motif 23) and compare the face-ID patterns of the B-null-edges. If the EMPTY moon's B-null-edges are all on face 34 (the crescent cap floor) while the OK moon's are spread across multiple faces, that confirms the order is determined by the cap boundary traversal order in the classifier.

### Priority 2 — Correct ordering for the moon's crescent cap null-edges

The crescent cap is a large concave face. Multiple null-edges from the sphere intersection land on it. The classifier processes them in boundary-edge order. The correct curve order for `scanjoin` is the order in which the sphere intersection curve traces around the crescent cap boundary.

If the null-edges' positions along the cap boundary can be sorted by their parametric position on the cap boundary edge they were created from (i.e., which edge of the 30-32-edge cap boundary loop the null-edge sits on), this gives the correct curve order — **without modifying any classifier code**.

This is the "true curve order" mentioned in `KurlanderBowlFixPlan.md` §4-bis. It requires sorting the all-singleton null-edge pairs for the moon by their position along the crescent cap's boundary — not by spatial position (which gives wrong results) but by **which loop edge of the cap the null-edge was inserted into during Generate**.

### Priority 3 — Resolve the interaction between `splitSelfTouchingLoops` and Connect

Once Connect ordering is correct for the moon's all-singleton null-edges, re-enable `splitSelfTouchingLoops` and verify it no longer causes regressions. The split should be a clean improvement at that point: it fixes the crescent cap's pinched loop before Classify sees it, preventing the figure-8 topology from reaching Connect.

### Priority 4 — Suite regression gate

After any fix, run:
```bash
gradle :base:cleanTest :base:test
gradle :base:test --tests "*KurlanderBowlStarInvariantTest*"
gradle :base:test --tests "*BooleansFromReferenceObjectPairsTest*"
gradle :base:test --tests "*SetOpConnect*"
```

All must be green. The star invariant test (20 parameterized tests) must never regress.

---

## 8. Key Constants and Diagnostics Quick Reference

```bash
# Render operand B (moon after Generate) for motif N:
gradle :testsuite:Jogl4Examples:PolyhedralBoundedSolidExample:runMain \
  -PrunMainClass=PolyhedralBoundedSolidExample \
  -PrunJvmArgs='--add-exports=java.desktop/sun.awt=ALL-UNNAMED|--add-opens=java.desktop/sun.awt=ALL-UNNAMED' \
  --args="--offline --screenshot /tmp/opB_mN.png \
          --solidModel CSG_OPERAND2_PARTIAL \
          --csgSample KURLANDER_BOWL_SINGLE_MOTIF --motifIndex N \
          --points true --wires true" \
  --no-configuration-cache

# Full pipeline trace (includes SelfTouch + split + curve-component traces):
# Add to JVM args: -Dvsdk.setop.tracePipelineSummary=true

# Star invariant test (must always pass):
gradle :base:test --tests "*KurlanderBowlStarInvariantTest*"

# Reference canaries (must always pass):
gradle :base:test --tests "*BooleansFromReferenceObjectPairsTest*"
gradle :base:test --tests "*SetOpConnect*"
gradle :base:test --tests "*SetOpFinishInvariants*"
```

---

## 9. Summary

| Metric | Status |
|--------|--------|
| Star motifs A−B (20/20) | ✅ All OK — `KurlanderBowlStarInvariantTest` green |
| Moon motifs A−B | ❌ 0/20 OK with curve-components; ~8/20 OK with singleton guard only |
| Suite `:base:test` | ✅ 0 failures on committed `6222bae9` |
| Root cause of moon failure | ✅ Confirmed: Generate creates self-touching (pinched) boundary on crescent cap |
| Fix for self-touching cap | ⚠️ `splitSelfTouchingLoops` implemented but masked by Connect ordering problem |
| Fix for Connect ordering | ❌ Pending: true curve order for all-singleton moon null-edges not yet found |
