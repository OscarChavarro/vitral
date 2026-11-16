import { describe, expect, it } from "vitest";

import type { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidTopologySummary } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologySummary.js";
import { NonManifoldBooleanResultException } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/NonManifoldBooleanResultException.js";
import { PolyhedralBoundedSolidModeler } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";
import { CsgSampleCorpus } from "./CsgSampleCorpus.js";
import { CsgSampleCorpusFixtures } from "./CsgSampleCorpusFixtures.js";
import { OrientedBox, PolyhedralBoundedSolidVolumeOracle } from "./PolyhedralBoundedSolidVolumeOracle.js";

/**
Reentrancy (Fable5.2Analysis.md Section 8 and pending item 9): a boolean
that fails in the middle of the pipeline must leave nothing behind that
changes the next one. The connect stage keeps its working state in the
connector instance, so an unrelated boolean after a failure gives the same
answer as on a fresh JVM.
 */
describe("SetOpReentrancyTest", () => {
    /**
    CROSS_PAIR A - B: the two pieces left of A touch along an edge, so the
    result is rejected as non-manifold, by decision, after generate,
    classify, connect and finish have all run. (This test used three
    defects in turn as its failing boolean, a rotated non-convex pair, one
    with coplanar bottoms and one inside the tolerance band; plan-stage11
    fixed each of them. A rejection by decision does not go away.)
    */
    function failingBoolean(): void {
        const pair = CsgSampleCorpusFixtures.createPair(CsgSampleCorpus.CROSS_PAIR);
        expect(() =>
            PolyhedralBoundedSolidModeler.setOp(pair[0]!, pair[1]!, PolyhedralBoundedSolidModeler.SUBTRACT),
        ).toThrow(NonManifoldBooleanResultException);
    }

    /** A rotated box pair, answered by the normal pipeline. */
    function unrelatedUnion(): PolyhedralBoundedSolid {
        const a = new OrientedBox(2, 2, 1, PolyhedralBoundedSolidVolumeOracle.translation(0, 0, 0)).toSolid();
        const b = new OrientedBox(
            1,
            1,
            1,
            PolyhedralBoundedSolidVolumeOracle.translation(1.0, 0.1, 0.2).multiply(
                PolyhedralBoundedSolidVolumeOracle.rotation(0.5, 0, 0, 1),
            ),
        ).toSolid();
        return PolyhedralBoundedSolidModeler.setOp(a, b, PolyhedralBoundedSolidModeler.UNION);
    }

    it("given_failedBoolean_when_unrelatedBooleanFollows_then_sameResult", () => {
        const reference = unrelatedUnion();
        const expected = PolyhedralBoundedSolidTopologySummary.from(reference);
        const expectedVolume = PolyhedralBoundedSolidVolumeOracle.solidVolume(reference);

        failingBoolean();
        const after = unrelatedUnion();

        expect(PolyhedralBoundedSolidTopologySummary.from(after).toString()).toBe(expected.toString());
        expect(Math.abs(PolyhedralBoundedSolidVolumeOracle.solidVolume(after) - expectedVolume)).toBeLessThanOrEqual(
            1.0e-9,
        );
    });
});
