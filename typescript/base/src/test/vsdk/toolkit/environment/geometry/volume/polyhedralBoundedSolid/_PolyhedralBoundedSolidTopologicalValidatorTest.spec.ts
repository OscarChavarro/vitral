import { describe, expect, it } from "vitest";
import { Box } from "vsdk/toolkit/environment/geometry/volume/Box.js";
import { _PolyhedralBoundedSolidTopologicalValidator } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/_PolyhedralBoundedSolidTopologicalValidator.js";

describe("_PolyhedralBoundedSolidTopologicalValidatorTest", () => {
    it("accepts a valid closed box", () => {
        expect(
            _PolyhedralBoundedSolidTopologicalValidator.validateTopologicalIntegrity(
                new Box(1, 1, 1).exportToPolyhedralBoundedSolid(),
            ),
        ).toBe(true);
    });

    it("rejects an edge with a missing mate", () => {
        const solid = new Box(1, 1, 1).exportToPolyhedralBoundedSolid();
        solid.getEdgesList()[0]!.leftHalf = null;
        expect(_PolyhedralBoundedSolidTopologicalValidator.validateTopologicalIntegrity(solid)).toBe(false);
    });
});
