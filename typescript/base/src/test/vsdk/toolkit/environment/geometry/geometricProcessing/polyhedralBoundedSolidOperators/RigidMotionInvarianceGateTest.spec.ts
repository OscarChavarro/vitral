import { describe, expect, it } from "vitest";

import { BooleanOracleFuzz } from "./BooleanOracleFuzz.js";
import { CsgSampleCorpus } from "./CsgSampleCorpus.js";
import { RigidMotionInvarianceGateTestSupport } from "./RigidMotionInvarianceGateTestSupport.js";

/**
A boolean must commute with a rigid motion applied to both operands. This
gate runs every reference pair of `CsgSampleCorpus` under the four
operations twice, as built and after one fixed generic rotation, and
compares face/edge/vertex/shell counts and the divergence-theorem volume.
It measures how much of the green reference matrix depends on axis
alignment (Fable5.2Analysis.md Stage A step 3, defect B-05).

<p>Ratchet: the number of invariant cases must not drop below
`MINIMUM_INVARIANT`. Raise it when a fix lands.</p>

<p>Traceability: [MANT1988] Ch. 15; Appendix B (regularised operations are
defined on point sets, hence invariant under isometries).</p>
 */
describe("RigidMotionInvarianceGateTest", () => {
    /**
    Stage A measured 24; Stage D raised it to 31. With the fallbacks retired
    (every answer from the Ch. 15 pipeline) all 36 cases are invariant; the
    same exception on both sides (CROSS_PAIR A-B, rejected as non-manifold)
    counts as invariant.
    */
    const MINIMUM_INVARIANT = 36;
    const OPERATIONS = BooleanOracleFuzz.OPERATIONS;

    // Java: @Tag("slow") — excluded from the regular `gradle test` run and
    // run by `gradle :base:slowTest`; skipped here for the same reason.
    it.skip("given_referencePairs_when_rotatedRigidly_then_resultsAreInvariant", () => {
        const report: string[] = [];
        let invariant = 0;
        let total = 0;

        for (const sample of CsgSampleCorpus.values()) {
            for (const op of OPERATIONS) {
                const plain = RigidMotionInvarianceGateTestSupport.run(sample, op, null);
                const rotated = RigidMotionInvarianceGateTestSupport.run(
                    sample,
                    op,
                    RigidMotionInvarianceGateTestSupport.GENERIC_ROTATION,
                );
                const bothRejected = Number.isNaN(plain.volume) && Number.isNaN(rotated.volume);
                const same =
                    plain.summary === rotated.summary &&
                    plain.strictValid === rotated.strictValid &&
                    (bothRejected ||
                        Math.abs(plain.volume - rotated.volume) <= 1.0e-6 * Math.max(1.0, Math.abs(plain.volume)));
                total++;
                if (same) {
                    invariant++;
                }
                report.push(
                    (same ? "  same " : "  DIFF ") +
                        sample +
                        " " +
                        BooleanOracleFuzz.opName(op) +
                        ": plain[" +
                        plain.summary +
                        " vol=" +
                        plain.volume +
                        " strict=" +
                        plain.strictValid +
                        " path=" +
                        plain.path +
                        "] rotated[" +
                        rotated.summary +
                        " vol=" +
                        rotated.volume +
                        " strict=" +
                        rotated.strictValid +
                        " path=" +
                        rotated.path +
                        "]",
                );
            }
        }
        console.log("Rigid-motion invariance: " + invariant + " / " + total);
        report.forEach((line) => console.log(line));
        expect(invariant, "rigid-motion invariant reference cases (ratchet)").toBeGreaterThanOrEqual(MINIMUM_INVARIANT);
    });
});
