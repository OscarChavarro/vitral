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

<p>Every case of the matrix must close: the cases that used to leave
loose endpoints (MANT1988_15_1 intersection and difference) close since
connect drops isolated struts.</p>
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
            // These two left four loose ends (an isolated strut on each
            // solid at a coplanar boundary-ray contact) until connect
            // learned to drop isolated struts (2026-11-16).
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
});
