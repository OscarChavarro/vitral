import { afterEach, beforeEach, describe, expect, it } from "vitest";

import { VSDK } from "vsdk/toolkit/common/VSDK.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { InfinitePlane } from "vsdk/toolkit/environment/geometry/surface/InfinitePlane.js";
import { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidTopologySummary } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologySummary.js";
import { PolyhedralBoundedSolidValidationEngine } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
import { PolyhedralBoundedSolidModeler } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";
import { SimpleTestGeometryLibrary } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fixtures/SimpleTestGeometryLibrary.js";

/**
Regression coverage for the solid/plane split used by
PolyhedralBoundedSolidExample SPLIT_TEST_PART_2 and SPLIT_TEST_PART_3.
 */
describe("PolyhedralBoundedSolidSplitterMantylaRegressionTest", () => {
    beforeEach(() => {
        VSDK.setWithSystemExit(false);
        VSDK.setWithFatalExceptions(true);
    });

    afterEach(() => {
        VSDK.setWithSystemExit(true);
        VSDK.setWithFatalExceptions(true);
    });

    function assertValidSolid(solid: PolyhedralBoundedSolid, side: string): void {
        const topology = PolyhedralBoundedSolidTopologySummary.from(solid);

        expect(
            PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid),
            `${side} split result must pass intermediate validation`,
        ).toBe(true);
        expect(topology.getFaceCount(), `${side} faces`).toBeGreaterThan(0);
        expect(topology.getShellCount(), `${side} shells`).toBeGreaterThan(0);
        expect(topology.hasUniversalContradiction(), `${side} topology: ${topology}`).toBe(false);
    }

    it("given_mantylaFixture_when_splitAtZPointThree_then_bothResultsAreValidSolids", () => {
        const input = SimpleTestGeometryLibrary.createTestObjectMANT1986_1();
        const splittingPlane = new InfinitePlane(new Vector3Dd(0, 0, 1), new Vector3Dd(0, 0, 0.3));
        const above: PolyhedralBoundedSolid[] = [];
        const below: PolyhedralBoundedSolid[] = [];

        expect(() => PolyhedralBoundedSolidModeler.split(input, splittingPlane, above, below)).not.toThrow();

        expect(above).toHaveLength(1);
        expect(below).toHaveLength(1);
        assertValidSolid(above[0]!, "above");
        assertValidSolid(below[0]!, "below");
    });
});
