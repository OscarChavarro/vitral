import { describe, expect, it } from "vitest";

import { BooleanCompositeOracleFuzz, Family, Outcome, javaMapToString } from "./BooleanCompositeOracleFuzz.js";

/**
Ratchet gate over the seeded non-convex oracle fuzz (Fable5.2Analysis.md
Section 8, pending item 2): L, U, stepped and genus-1 operands, rotated,
scaled to 10^-3 and 10^3, and chains of three operations, checked against
the exact volume of `CompositeShapeOracle`. Every family must keep at
least its recorded number of correct, strict-valid results. Raise a minimum
when a fix lands; never lower one.

<p>The three rotated families replay the same configurations at three
sizes, so their counts must stay equal: a difference means a tolerance that
does not follow the model scale.</p>

<p>Traceability: [MANT1988] Sec. 13.3 (volume as integral property) and
Ch. 15 (set operations).</p>
 */
describe("BooleanCompositeOracleFuzzGateTest", () => {
    /**
    Ratchet: raise only. First measurement (2026-11-16), after the
    scale-aware tolerances and the ring placement of join: the rotated
    families 47 of 72, the chains 23 of 36 and the coplanar bottoms 46 of
    72. In general position a non-manifold rejection counts as a failure.
    After plan-stage11 Phase F (connect joins curve neighbours only; lmekr
    keeps the outer loop first; finish keeps whole shells): rotated 65,
    chains 31, coplanar bottoms 50, and the 72 axis-aligned ones are
    answered, none rejected. No family returns a wrong volume any more,
    and that is now a condition of its own: see the test. With rings
    redistributed on both solids for every operation: rotated 72 of 72 at
    the three scales, chains 36 of 36, coplanar bottoms 54; 72 once
    contact nodes are removed before connect. Every family is complete.
    */
    const MINIMUM_OK = new Map<Family, number>([
        [Family.NON_CONVEX_AXIS_ALIGNED, 72],
        [Family.NON_CONVEX_ROTATED, 72],
        [Family.NON_CONVEX_ROTATED_SCALE_1E_MINUS_3, 72],
        [Family.NON_CONVEX_ROTATED_SCALE_1E_PLUS_3, 72],
        [Family.CHAINS, 36],
        [Family.NON_CONVEX_COPLANAR_BOTTOMS, 72],
    ]);

    it.each(Family.values())(
        "given_seededNonConvexOperands_when_setOp_then_correctCountDoesNotRegress: %s",
        (family: Family) => {
            const summary = BooleanCompositeOracleFuzz.runFamily(family);
            console.log(
                family +
                    ": " +
                    summary.correct +
                    " / " +
                    summary.total +
                    " correct " +
                    javaMapToString(summary.byOutcome),
            );
            summary.failures.forEach((f) => console.log("  " + f));
            const wrong = summary.byOutcome.get(Outcome.WRONG_VOLUME);
            expect(
                wrong === undefined || wrong === 0,
                "solids returned with a wrong volume in family " +
                    family +
                    " (a silent error, whatever the ratchet says)",
            ).toBe(true);
            expect(
                summary.correct,
                "correct results in family " + family + " must not regress below the ratchet",
            ).toBeGreaterThanOrEqual(MINIMUM_OK.get(family)!);
        },
        20_000,
    );
});
