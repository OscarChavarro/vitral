import { describe, expect, it } from "vitest";

import type { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidModeler } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";
import { SimpleTestGeometryLibrary } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fixtures/SimpleTestGeometryLibrary.js";
import { _PolyhedralBoundedSolidSetNullEdgesConnector } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/topology/_PolyhedralBoundedSolidSetNullEdgesConnector.js";
import { CsgSampleCorpus } from "./CsgSampleCorpus.js";
import { CsgSampleCorpusFixtures } from "./CsgSampleCorpusFixtures.js";

/**
Acceptance test for the `setopconnect` contract from
[MANT1988] §15.7, Program 15.14.

<p>Program 15.14 (sgetnextnulledge / scanjoin / join / cuta / cutb)
guarantees by construction that, when the loop terminates, every
null-edge endpoint has been joined into the active topology and no
"loose" half-edges remain pending: <em>looseA = looseB = 0</em>. This
invariant is the hard contract that any compliant implementation of
the Connect phase must satisfy — it is what allows `setopfinish`
to consume `sonfa`/`sonfb` without ad-hoc recoveries.</p>

<p>This test does not rewrite the connector — it audits the existing
`_PolyhedralBoundedSolidSetNullEdgesConnector` against the book's
invariant on a curated set of canonical operand pairs and reads the
loose counters via the `getLastLoose*Count` accessors.</p>

<p>The matrix is split into two groups:
<ul>
<li><b>baseline</b>: cases the current connector already closes
correctly. They lock in the parts of Program 15.14 that are conformant
today — any future regression is caught by these tests.</li>
<li><b>pending §6.1</b>: cases that still leave loose endpoints; they
are skipped (Java `@Disabled`) with a precise count so progress on §6.1 of
plan-csg-boolean-fix-stage2 (and the §5.2 deferred sectoroverlap
fix) can be measured incrementally — when the connector becomes
compliant for one of these, un-skipping should make the test green
without further changes.</li>
</ul>
</p>
 */
describe("SetOpConnectNoLooseInvariantTest", () => {
    type PairCase = [string, string, PolyhedralBoundedSolid, PolyhedralBoundedSolid, number];

    function createPair(name: string): PolyhedralBoundedSolid[] {
        if (name === "STACKED_BLOCKS") {
            return CsgSampleCorpusFixtures.createPair(CsgSampleCorpus.STACKED_BLOCKS);
        }
        return SimpleTestGeometryLibrary.createTestObjectPairMANT1988_15_1();
    }

    function pairCase(pairName: string, opName: string, op: number): PairCase {
        const pair = createPair(pairName);
        return [pairName, opName, pair[0]!, pair[1]!, op];
    }

    function baselineCases(): PairCase[] {
        // Cases where the current connector already satisfies Program 15.14:
        //   looseA == looseB == 0 after Connect. Locked in as regression
        //   guard; do not move to "pending" without an investigation.
        return [
            pairCase("MANT1988_15_1", "UNION", PolyhedralBoundedSolidModeler.UNION),
            pairCase("STACKED_BLOCKS", "UNION", PolyhedralBoundedSolidModeler.UNION),
            pairCase("STACKED_BLOCKS", "INTERSECTION", PolyhedralBoundedSolidModeler.INTERSECTION),
            pairCase("STACKED_BLOCKS", "SUBTRACT", PolyhedralBoundedSolidModeler.SUBTRACT),
        ];
    }

    function pendingCases(): PairCase[] {
        // Cases where the current connector leaves loose endpoints
        // (looseA > 0). Documented baseline at the time of writing:
        //   - MANT1988_15_1 + INTERSECTION → looseA = 4
        //   - MANT1988_15_1 + SUBTRACT     → looseA = 4
        // Both share the §5.2 sectoroverlap deferral as suspected root cause.
        return [
            pairCase("MANT1988_15_1", "INTERSECTION", PolyhedralBoundedSolidModeler.INTERSECTION),
            pairCase("MANT1988_15_1", "SUBTRACT", PolyhedralBoundedSolidModeler.SUBTRACT),
        ];
    }

    it.each(baselineCases())(
        "given_baselinePair_when_setopRuns_then_connectLeavesNoLooseEndpoints: %s + %s",
        (
            pairName: string,
            opName: string,
            solidA: PolyhedralBoundedSolid,
            solidB: PolyhedralBoundedSolid,
            op: number,
        ) => {
            PolyhedralBoundedSolidModeler.setOp(solidA, solidB, op, false);

            const looseA = _PolyhedralBoundedSolidSetNullEdgesConnector.getLastLooseACount();
            const looseB = _PolyhedralBoundedSolidSetNullEdgesConnector.getLastLooseBCount();

            expect(
                looseA,
                `[${pairName} + ${opName}] looseA after Connect must be 0 per [MANT1988] Program 15.14`,
            ).toBe(0);
            expect(
                looseB,
                `[${pairName} + ${opName}] looseB after Connect must be 0 per [MANT1988] Program 15.14`,
            ).toBe(0);
        },
    );

    // Java: @Disabled("Pending §6.1-C/§5.2 unified: scanjoin's main loop misses
    // latent loose-pair closures for MANT1988_15_1 INTERSECTION/SUBTRACT
    // (looseA=4). Diagnosis: the loose halves satisfy neighbor() between
    // themselves but scanjoin only compares new-vs-loose. A post-pass closure
    // was attempted in §6.1-C-attempt-1 but fused legitimately-separate shells
    // (HOLLOW_BRICK case) — needs upstream §5.2 fix.")
    it.skip.each(pendingCases())(
        "given_pendingPair_when_setopRuns_then_connectShouldLeaveNoLooseEndpoints: %s + %s",
        (
            pairName: string,
            opName: string,
            solidA: PolyhedralBoundedSolid,
            solidB: PolyhedralBoundedSolid,
            op: number,
        ) => {
            PolyhedralBoundedSolidModeler.setOp(solidA, solidB, op, false);

            const looseA = _PolyhedralBoundedSolidSetNullEdgesConnector.getLastLooseACount();
            const looseB = _PolyhedralBoundedSolidSetNullEdgesConnector.getLastLooseBCount();

            expect(
                looseA,
                `[${pairName} + ${opName}] looseA after Connect must be 0 per [MANT1988] Program 15.14`,
            ).toBe(0);
            expect(
                looseB,
                `[${pairName} + ${opName}] looseB after Connect must be 0 per [MANT1988] Program 15.14`,
            ).toBe(0);
        },
    );
});
