import { describe, expect, it } from "vitest";

import { BooleanOracleFuzz, Family } from "./BooleanOracleFuzz.js";

/**
Ratchet gate over the seeded box-pair oracle fuzz (Fable5.2Analysis.md
Section 1 and Stage A step 2). Every family must keep at least its recorded
number of correct, strict-valid results. Raise a minimum when a fix lands;
never lower one.

<p>Run with `gradle :base:slowTest` in Java. The per-family failures are printed
so a regression names the cases that broke.</p>

<p>Traceability: [MANT1988] Sec. 13.3 (volume as integral property) and
Ch. 15 (set operations).</p>
 */
describe("BooleanOracleFuzzGateTest", () => {
    /**
    Ratchet: raise only. Stage A (2026-11-16) measured 48/41/38/38/304/16;
    the B-01 ring fix (Stage C) raised the three rotated families to 48 and
    the coplanar family to 18. Stage D (vertex/face neighbourhood, witness
    containment, face-contact union) raised coplanar to 48 and the grid to
    306. The 14 grid unions touching only along an edge (B-04) are now
    rejected as non-manifold, which counts as correct: grid 320.
    */
    const MINIMUM_OK = new Map<Family, number>([
        [Family.AXIS_ALIGNED_GENERAL_POSITION, 48],
        [Family.B_ROTATED_ABOUT_Z, 48],
        [Family.B_ROTATED_ABOUT_RANDOM_AXIS, 48],
        [Family.RANDOM_AXIS_TRANSLATED_TO_1000, 48],
        [Family.INTEGER_GRID, 320],
        [Family.COPLANAR_TOP_BOTTOM_ROTATED_45, 48],
    ]);

    // Java: @Tag("slow") — excluded from the regular `gradle test` run and
    // run by `gradle :base:slowTest`; skipped here for the same reason.
    it.skip.each(Family.values())(
        "given_seededBoxPairs_when_setOp_then_correctCountDoesNotRegress: %s",
        (family: Family) => {
            const summary = BooleanOracleFuzz.runFamily(family);
            console.log(
                family +
                    ": " +
                    summary.correct +
                    " / " +
                    summary.total +
                    " correct {" +
                    Array.from(summary.byOutcome.entries())
                        .map(([k, v]) => k + "=" + v)
                        .join(", ") +
                    "}",
            );
            summary.failures.forEach((f) => console.log("  " + f));
            expect(
                summary.correct,
                "correct results in family " + family + " must not regress below the ratchet",
            ).toBeGreaterThanOrEqual(MINIMUM_OK.get(family)!);
        },
    );
});
