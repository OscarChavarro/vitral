import { describe, expect, it } from "vitest";

import type { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidModeler } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";
import { _PolyhedralBoundedSolidSetFinisher } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/topology/_PolyhedralBoundedSolidSetFinisher.js";
import { PolyhedralBoundedSolidTestFixtures } from "../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTestFixtures.js";

/**
§9 regression guard for the Finisher invariants:
<ul>
<li>§9.1 — `sanitizePairedFaces` must never fall back to legacy
    index ordering. If Connect tags faces correctly, pairIndex matching
    always succeeds and the fallback counter stays at 0.</li>
<li>§9.2 — `triangulateNonPlanarFaces` must not triangulate any
    face in the baseline fixtures that already pass validation. A non-zero
    count indicates the loopGlue produced a non-planar face, which is a
    signal for §9 follow-up work.</li>
</ul>
Traceability: plan-csg-boolean-fix-stage2.md §9.1 and §9.2.
 */
describe("SetOpFinishInvariantsTest", () => {
    type FixtureCase = [string, PolyhedralBoundedSolid, PolyhedralBoundedSolid, number];

    function baselineFixtures(): FixtureCase[] {
        const mant1986 = PolyhedralBoundedSolidTestFixtures.createMant1986_2Pair();
        const limit = PolyhedralBoundedSolidTestFixtures.createMant1988_15_2Pair(0);
        const fig6 = PolyhedralBoundedSolidTestFixtures.createMant1988_6_13Pair();

        return [
            [
                "MANT1986_2 UNION",
                PolyhedralBoundedSolidTestFixtures.createMant1986_2Pair()[0]!,
                PolyhedralBoundedSolidTestFixtures.createMant1986_2Pair()[1]!,
                PolyhedralBoundedSolidModeler.UNION,
            ],
            ["MANT1986_2 INTERSECTION", mant1986[0]!, mant1986[1]!, PolyhedralBoundedSolidModeler.INTERSECTION],
            [
                "MANT1986_2 SUBTRACT",
                PolyhedralBoundedSolidTestFixtures.createMant1986_2Pair()[0]!,
                PolyhedralBoundedSolidTestFixtures.createMant1986_2Pair()[1]!,
                PolyhedralBoundedSolidModeler.SUBTRACT,
            ],
            ["MANT1988_15_2 UNION", limit[0]!, limit[1]!, PolyhedralBoundedSolidModeler.UNION],
            ["MANT1988_6_13 SUBTRACT", fig6[0]!, fig6[1]!, PolyhedralBoundedSolidModeler.SUBTRACT],
        ];
    }

    it.each(baselineFixtures())(
        "§9.1 no legacy fallback: %s",
        (label: string, solidA: PolyhedralBoundedSolid, solidB: PolyhedralBoundedSolid, op: number) => {
            PolyhedralBoundedSolidModeler.setOp(solidA, solidB, op, false, true, false);

            expect(
                _PolyhedralBoundedSolidSetFinisher.getLastLegacyFallbackCount(),
                `[${label}] sanitizePairedFaces must not use legacy ordering fallback`,
            ).toBe(0);
        },
    );

    it.each(baselineFixtures())(
        "§9.2 no triangulation: %s",
        (label: string, solidA: PolyhedralBoundedSolid, solidB: PolyhedralBoundedSolid, op: number) => {
            PolyhedralBoundedSolidModeler.setOp(solidA, solidB, op, false, true, false);

            expect(
                _PolyhedralBoundedSolidSetFinisher.getLastTriangulatedFaceCount(),
                `[${label}] triangulateNonPlanarFaces must split 0 faces in clean baseline`,
            ).toBe(0);
        },
    );
});
