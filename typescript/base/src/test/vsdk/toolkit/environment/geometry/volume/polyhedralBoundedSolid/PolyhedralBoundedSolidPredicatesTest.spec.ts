import { describe, expect, it } from "vitest";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { Box } from "vsdk/toolkit/environment/geometry/volume/Box.js";
import { PolyhedralBoundedSolidPredicates } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidPredicates.js";

describe("PolyhedralBoundedSolidPredicatesTest", () => {
    it("classifies interior and exterior box points", () => {
        const solid = new Box(2, 2, 2).exportToPolyhedralBoundedSolid();
        expect(PolyhedralBoundedSolidPredicates.isPointInside(solid, new Vector3Dd())).toBe(true);
        expect(PolyhedralBoundedSolidPredicates.isPointInside(solid, new Vector3Dd(2, 0, 0))).toBe(false);
    });

    it("counts edge and vertex entries once while ignoring tangential contacts", () => {
        const solid = new Box(2, 2, 2).exportToPolyhedralBoundedSolid();
        expect(solid.computeQuantitativeInvisibility(new Vector3Dd(-3, -2, 0), new Vector3Dd(-0.5, -0.75, 0))).toBe(1);
        expect(
            solid.computeQuantitativeInvisibility(new Vector3Dd(-3, -2, -2), new Vector3Dd(-0.5, -0.75, -0.75)),
        ).toBe(1);
        expect(solid.computeQuantitativeInvisibility(new Vector3Dd(-3, -2, -1), new Vector3Dd(3, 1, -1))).toBe(0);
    });
});
