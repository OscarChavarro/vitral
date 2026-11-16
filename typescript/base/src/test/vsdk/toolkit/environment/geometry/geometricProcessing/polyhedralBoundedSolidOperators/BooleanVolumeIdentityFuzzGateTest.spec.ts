import { describe, expect, it } from "vitest";

import { javaMapToString } from "./BooleanCompositeOracleFuzz.js";
import { BooleanVolumeIdentityFuzz, Family } from "./BooleanVolumeIdentityFuzz.js";

/**
Gate over `BooleanVolumeIdentityFuzz` (plan-stage11 Phase B): prisms
of N sides and star prisms in general position, checked with the volume
identities of the four operations.

<p>Two conditions per family:</p>
<ul>
<li>No silent error, ever: a returned set of results that breaks an
    identity, or an intersection outside [0, min(|A|, |B|)], fails the
    test whatever the ratchet says.</li>
<li>Ratchet on the count of correct cases: raise it when a fix lands,
    never lower it. The cases below the total are typed rejections of the
    pipeline on non-convex faces (plan-stage11 Phase F).</li>
</ul>

<p>Traceability: [MANT1988] Sec. 13.3 (volume) and Ch. 15.</p>
 */
describe("BooleanVolumeIdentityFuzzGateTest", () => {
    /** First measurement (2026-11-16): convex 16/16, star and convex 13/16, stars 11/16. After plan-stage11 Phase F: 16, 15, 16. */
    const MINIMUM_OK = new Map<Family, number>([
        [Family.CONVEX_PRISMS, 16],
        [Family.STAR_AND_CONVEX, 16],
        [Family.STAR_PRISMS, 16],
    ]);

    it.each(Family.values())(
        "given_seededPrisms_when_fourOperations_then_volumeIdentitiesHold: %s",
        (family: Family) => {
            const summary = BooleanVolumeIdentityFuzz.runFamily(family);
            console.log(
                family + ": " + summary.correct + " / " + summary.total + " " + javaMapToString(summary.byOutcome),
            );
            summary.failures.forEach((f) => console.log("  " + f));
            const keys = Array.from(summary.byOutcome.keys());
            expect(
                keys.filter((k) => k === "IDENTITY" || k === "RANGE" || k === "OPERAND"),
                "results that break a volume identity are silent errors",
            ).toEqual([]);
            expect(summary.correct, "correct cases in family " + family).toBeGreaterThanOrEqual(
                MINIMUM_OK.get(family)!,
            );
        },
    );
});
