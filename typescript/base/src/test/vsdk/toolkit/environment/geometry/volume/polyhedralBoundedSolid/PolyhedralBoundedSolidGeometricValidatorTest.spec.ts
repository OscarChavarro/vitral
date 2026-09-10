import { describe, expect, it } from "vitest";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { Box } from "vsdk/toolkit/environment/geometry/volume/Box.js";
import { PolyhedralBoundedSolidGeometricValidator } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidGeometricValidator.js";
import { PolyhedralBoundedSolidNumericPolicy } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.js";

describe("PolyhedralBoundedSolidGeometricValidatorTest", () => {
    it("classifies coplanar, non-coplanar, and tolerance-close point sets", () => {
        const coplanar = [
                new Vector3Dd(0, 0, 0),
                new Vector3Dd(1, 0, 0),
                new Vector3Dd(1, 1, 0),
                new Vector3Dd(0, 1, 0),
            ],
            nonCoplanar = [new Vector3Dd(), new Vector3Dd(1, 0, 0), new Vector3Dd(0, 1, 0), new Vector3Dd(0, 0, 1)],
            almostCoplanar = [
                new Vector3Dd(),
                new Vector3Dd(2, 0, 0),
                new Vector3Dd(2, 2, 1e-12),
                new Vector3Dd(0, 2, 0),
            ];
        expect(PolyhedralBoundedSolidGeometricValidator.validateFacePointsAreCoplanar(coplanar)).toBe(true);
        expect(PolyhedralBoundedSolidGeometricValidator.validateFacePointsAreCoplanar(nonCoplanar)).toBe(false);
        expect(PolyhedralBoundedSolidGeometricValidator.validateFacePointsAreCoplanar(almostCoplanar)).toBe(true);
    });

    it("validates each box face through the public strict contracts", () => {
        const solid = new Box(2, 2, 2).exportToPolyhedralBoundedSolid(),
            context = PolyhedralBoundedSolidNumericPolicy.forSolid(solid);
        expect(
            solid
                .getPolygonsList()
                .every((face) => PolyhedralBoundedSolidGeometricValidator.validateFaceIsPlanar(face, context)),
        ).toBe(true);
        expect(PolyhedralBoundedSolidGeometricValidator.validateLoopsStrict(solid, context)).toBe(true);
        expect(PolyhedralBoundedSolidGeometricValidator.validateFaceIntersectionsStrict(solid, context)).toBe(true);
    });
});
