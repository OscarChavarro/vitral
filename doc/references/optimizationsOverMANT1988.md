# Optimizations Over MANT1988

# Tests policy

Treat disabled tests as a work queue, not as discarded coverage. Each disabled test should state whether it conflicts with legacy behavior, hits a known fatal path, or encodes a newer experimental strategy. Do not re-enable a broad corpus all at once.

Recovery should proceed by re-enabling one test or one fixture at a time after the relevant level is fixed. Prefer adding narrower tests that capture the recovered behavior before restoring large algebraic or diagnostic suites. Keep `gradle :base:test` green, but do not let green status hide unresolved Boolean correctness questions.

# Code improvement impact matrix

Having a green check means test worked, red cross means test not working.
The result path is what `PolyhedralBoundedSolidModeler.getLastSetOpResultPath()`
reports for the operation with the default strict validation (measured
2026-11-16, Java port): `normal-pipeline` is the [MANT1988] Ch. 15
algorithm; any other value names the preflight that answered. The
shape-specific fallbacks (axis-aligned cells, orthogonal profile, profile
difference, offset cylinder) were retired on 2026-11-16: every answer now
comes from the Chapter 15 pipeline or from one of its preflights.

| model name / operation | current logic | result path |
|---|:---:|---|
| MANT1986_2 + UNION | ✅ | `normal-pipeline` |
| MANT1986_2 + INTERSECTION | ✅ | `normal-pipeline` |
| MANT1986_2 + A-B | ✅ | `normal-pipeline` |
| MANT1986_2 + B-A | ✅ | `normal-pipeline` |
| STACKED_BLOCKS + UNION | ✅ | `normal-pipeline` |
| STACKED_BLOCKS + INTERSECTION | ✅ | `no-intersection-after-classify` |
| STACKED_BLOCKS + A-B | ✅ | `no-intersection-after-classify` |
| STACKED_BLOCKS + B-A | ✅ | `no-intersection-after-classify` |
| MOON_BLOCK + UNION | ✅ | `normal-pipeline` |
| MOON_BLOCK + INTERSECTION | ✅ | `normal-pipeline` |
| MOON_BLOCK + A-B | ✅ | `normal-pipeline` |
| MOON_BLOCK + B-A | ✅ | `normal-pipeline` |
| CROSS_PAIR + UNION | ✅ | `normal-pipeline` |
| CROSS_PAIR + INTERSECTION | ✅ | `normal-pipeline` |
| CROSS_PAIR + A-B | ⚠️ | `normal-pipeline`, then `NonManifoldBooleanResultException` (see below) |
| CROSS_PAIR + B-A | ✅ | `normal-pipeline` |
| HOLLOW_BRICK + UNION | ✅ | `normal-pipeline` |
| HOLLOW_BRICK + INTERSECTION | ✅ | `normal-pipeline` |
| HOLLOW_BRICK + A-B | ✅ | `normal-pipeline` |
| HOLLOW_BRICK + B-A | ✅ | `normal-pipeline` |
| MANT1988_6_13 + UNION | ✅ | `normal-pipeline` |
| MANT1988_6_13 + INTERSECTION | ✅ | `normal-pipeline` |
| MANT1988_6_13 + A-B | ✅ | `normal-pipeline` |
| MANT1988_6_13 + B-A | ✅ | `normal-pipeline` |
| MANT1988_15_1 + UNION | ✅ | `normal-pipeline` |
| MANT1988_15_1 + INTERSECTION | ✅ | `normal-pipeline` |
| MANT1988_15_1 + A-B | ✅ | `normal-pipeline` |
| MANT1988_15_1 + B-A | ✅ | `normal-pipeline` |
| MANT1988_3 + UNION | ✅ | `normal-pipeline` |
| MANT1988_3 + INTERSECTION | ✅ | `normal-pipeline` |
| MANT1988_3 + A-B | ✅ | `normal-pipeline` |
| MANT1988_3 + B-A | ✅ | `normal-pipeline` |
| MANT1988_15_2_HOLED + UNION | ✅ | `normal-pipeline` |
| MANT1988_15_2_HOLED + INTERSECTION | ✅ | `normal-pipeline` |
| MANT1988_15_2_HOLED + A-B | ✅ | `normal-pipeline` |
| MANT1988_15_2_HOLED + B-A | ✅ | `normal-pipeline` |
| CSG_LAMP_SHELL | ✅ | not measured (composite model) |
| FEATURED_OBJECT | ✅ | not measured (composite model) |
| CSG_KURLANDER_BOWL | ✅ | not measured (composite model: 82 booleans, one shell of genus 40; `CsgKurlanderBowlFullModelTest`, slow) |

⚠️ means the operation has no manifold answer and the default `setOp`
rejects it. `CROSS_PAIR` A−B: the two remaining pieces of A touch B's
removed bar along one edge, so the regularised result is non-manifold
(two shells sharing an edge). By the B-04 policy (2026-11-16: "if the two
shells touch, reject it, it is a non-manifold result") `setOp` throws
`NonManifoldBooleanResultException`. The retired cell fallback used to
return two shells (F=12, E=23, V=14) that failed the strict Euler check.

Beyond this matrix the oracle fuzz gates (`BooleanOracleFuzzGateTest`,
slow) check six families of box pairs against an independent volume
oracle: all 560 cases are correct (2026-11-16), all from the pipeline,
14 of them rejected as non-manifold unions (B-04). The rigid-motion gate
(`RigidMotionInvarianceGateTest`) gives 36 / 36: with the fallbacks gone,
no answer depends on the operands being axis-aligned. See
`doc/polyhedralBoundedSolidDevelopment/Fable5.2Analysis.md` (Sections 5
and 10).

Stage 11 (`plan-stage11-robustness-hardening.md`) added gates for what
this matrix and the box pairs do not cover, all at their totals:

| Gate | Operands | Result |
|---|---|---:|
| `BooleanCompositeOracleFuzzGateTest` | L, U, step and frame shapes: axis-aligned, rotated at 10⁻³, 1 and 10³, with coplanar bottoms, and chains of three operations | 72 / 72 in each of the five pair families, 36 / 36 chains |
| `BooleanVolumeIdentityFuzzGateTest` | Regular prisms and star prisms, rotated | 16 / 16 in each of the three families |
| `NearDegenerateBooleanFuzzGateTest` | Box pairs in contact, displaced by −100 to 100 tolerances | 422 of 432 answered, 0 wrong; the 10 rejected are unions in edge contact (B-04) |

The result paths in use are `normal-pipeline`,
`no-intersection-after-classify`, `touching-only-preflight`,
`containment-only-preflight` and `geometric-identity-preflight`.

# The block and wedge case

The failure in `MANT1988_15_2_HOLED + UNION` was not in preflight,
generate, or classify. The pipeline detected the intersections correctly:
there were six vertex/face contacts from the wedge to the block and no
vertex/vertex contacts. The break happened at the connect -> finish boundary.
Connect split the block into three shells `[6, 2, 2]`; the two 2-face shells
were lateral caps on the block input/output planes. Finish then moved exactly
the faces referenced by `sonfa/sonfb`, so it integrated the detached caps and
wedge fragments while leaving the main block shell behind.

The false leads were useful but did not explain the failure: changing
`sortNullEdges()`, forcing alternate left/right halves, and selecting orig/new
faces in finish only produced different wrong shells. The closing evidence was
that UNION joins on side A were moving useful inner loops out of the carrier
faces through `laringmv`. After that, `sonfa` pointed at laminar faces detached
from the block's residual outer shell.

The fix made the operation explicit in connect and added ring-movement control
to join. For UNION on side A, carrier loops are preserved instead of expelled
into separate shells. With that change, connected A goes from shell counts
`[2, 2, 6]` to `[10]`, and the existing finish phase can move the intended
outer shell. The recovered result is one strict shell with
`faces=14, edges=30, vertices=20` and bbox
`[0.0, 0.0, 0.0] -> [0.775, 1.0, 0.6]`, matching the full block plus the two
triangular protrusions.

Regression coverage is locked in
`BooleansFromReferenceObjectPairsTest` and
`PolyhedralBoundedSolidSetOperatorTest`. The focused operator suite and the
full `vsdk.toolkit.processing.polyhedralBoundedSolidOperators.*Test` suite
passed with this fix.

# The MANT1988_15_1 profile difference case

*Retired 2026-11-16.* The profile difference fallback described below was removed. The
case is now answered by the Chapter 15 pipeline (`normal-pipeline`), with
the same counts. The text is kept as a record of the failure.

`MANT1988_15_1` is the L-block against beveled-block fixture. The visible
failure was `B-A`: the expected result is the right-hand reduced part of B,
bounded by the new cut profile, but finish returned the full operand B. The
important clue was that the relevant vertex/face cuts were already found in
the image region around vertices `43/15` and `40/41`; the problem was not that
generate missed the intersections, but that the downstream rings did not
connect into a usable result shell.

The implemented recovery is intentionally narrow. Before the destructive set
operation phases, `PolyhedralBoundedSolidSetOperator` records a
`_PolyhedralBoundedSolidProfileDifferenceFallbackSpec` only for `SUBTRACT`
cases where the minuend and subtrahend share the same bbox, the minuend has
exactly two X planes, and the subtrahend introduces one internal X cut and one
internal Z cut. After normal finish runs, the fallback is applied only if the
result still has the original minuend bbox, which is the signature of the old
"returned full B" failure.

When triggered, the fallback extracts the minuend profile on the YZ plane,
clips that profile above the subtrahend's Z cut, and extrudes the clipped
profile from `xCut` to `xMax`. This reconstructs the missing right-hand
remainder directly from the profile geometry instead of guessing by largest
shell or bbox heuristics inside finish.

The recovered `B-A` result now has bbox
`[1/3, 0, 0.25] -> [1, 1, 1]`, one shell, 8 faces, 18 edges, and 12 vertices.
All four operations for `MANT1988_15_1` are locked in
`BooleansFromReferenceObjectPairsTest`: UNION, INTERSECTION, A-B, and B-A.

# The MANT1988_3 rectilinear contact case

*Retired 2026-11-16.* The axis-aligned cell fallback described below was removed. The
case is now answered by the Chapter 15 pipeline (`normal-pipeline`), with
the same counts. The text is kept as a record of the failure.

`MANT1988_3` L-profile against box fixture. The systematic trace showed that preflight did not apply, while
generate and classify did find the expected vertex/face and vertex/vertex
contacts. The break was at connect -> finish: connect left loose point
null-edge chains around the contact rectangle, including the regions around
the duplicated vertices `28/50` vs `35/36`, `26/48` vs `32/33`,
`38/39` vs `35/35`, `19/23` vs `26/48`, and `24/46` vs `32/33`, so finish
either returned an empty union or tried to glue degenerate faces.

The recovery keeps the normal MANT1988 pipeline first. For axis-aligned
rectilinear operands, the operator now prepares a bounded cell-decomposition
fallback from all operand vertex coordinate planes. The fallback is used only
when connect leaves loose endpoints, finish throws, or the finished result is
empty or degenerate. It classifies each open grid cell against A and B,
applies the requested set expression, and reconstructs a valid B-Rep from
outward boundary quads before normal post-processing.

All four operations for `MANT1988_3` are now strict-valid single-shell
results and are locked in `BooleansFromReferenceObjectPairsTest`: UNION,
INTERSECTION, A-B, and B-A.

# The MANT1988_6_13 orthogonal profile case

*Retired 2026-11-16.* The orthogonal profile fallback described below was removed. The
case is now answered by the Chapter 15 pipeline (`normal-pipeline`), with
the same counts. The text is kept as a record of the failure.

`MANT1988_6_13` is the left-view profile extrusion against the front-view
profile extrusion. The stage trace showed that preflight did not apply and
generate was doing the right first-order geometric work: it found four
vertex/face contacts and twelve vertex/vertex contacts. The bad handoff was
between classify and connect. Classify emitted twelve paired null edges, but
they were all point null-edges. For UNION, connect left eight loose endpoint
pairs and produced no `sonfa`/`sonfb` result faces, so finish returned an empty
solid. For INTERSECTION and A-B, connect produced only two result-face pairs;
at least one A-side face still had only one loop, so finish could not consume
`boundariesList[1]`. This matches the visual clue around the partial-A point
chains near the duplicated profile vertices: the vertex/face intersections
were present, but not assembled into complete cutting rings.

The recovery keeps the normal MANT1988 pipeline first. Before destructive
classification/connect, the operator prepares a narrow orthogonal-profile
cell fallback for this fixture shape: one operand is a Y-extruded XZ profile
with a piecewise-linear right boundary, and the other is an X-extruded YZ
profile. If connect leaves loose endpoints or finish/result is incomplete,
the fallback classifies the two X zones split by the XZ profile boundary over
the combined Y/Z profile cuts, then reconstructs outward boundary quads. This
preserves the intended profile-set-operation geometry without asking finish to
glue incomplete point chains.

All four operations are now strict-valid single-shell results and are locked
in `BooleansFromReferenceObjectPairsTest`: UNION
`faces=11, edges=27, vertices=18`, INTERSECTION
`faces=12, edges=30, vertices=20`, A-B `faces=7, edges=15, vertices=10`, and
B-A `faces=12, edges=30, vertices=20`.
