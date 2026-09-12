import { describe, expect, it } from "vitest";

import { ArrayList } from "java/util/ArrayList.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { PolyhedralBoundedSolidGeometricValidator } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidGeometricValidator.js";
import { PolyhedralBoundedSolidNumericPolicy } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.js";
import type { _PolyhedralBoundedSolidFace } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.js";
import { PolyhedralBoundedSolidTestFixtures } from "./PolyhedralBoundedSolidTestFixtures.js";

/**
Exercises face-planarity predicates used by the B-Rep validation layer.

<p>Traceability: [MANT1988] Ch. 13.1, face equations and plane
consistency for polyhedral faces.</p>
 */
describe("PolyhedralBoundedSolidGeometricValidatorTest", () => {
    function coplanaritySamples(): [ArrayList<Vector3Dd>, boolean][] {
        const coplanarSquare = new ArrayList<Vector3Dd>();
        coplanarSquare.add(new Vector3Dd(0.0, 0.0, 0.0));
        coplanarSquare.add(new Vector3Dd(1.0, 0.0, 0.0));
        coplanarSquare.add(new Vector3Dd(1.0, 1.0, 0.0));
        coplanarSquare.add(new Vector3Dd(0.0, 1.0, 0.0));

        const nonCoplanar = new ArrayList<Vector3Dd>();
        nonCoplanar.add(new Vector3Dd(0.0, 0.0, 0.0));
        nonCoplanar.add(new Vector3Dd(1.0, 0.0, 0.0));
        nonCoplanar.add(new Vector3Dd(0.0, 1.0, 0.0));
        nonCoplanar.add(new Vector3Dd(0.0, 0.0, 1.0));

        const almostCoplanar = new ArrayList<Vector3Dd>();
        almostCoplanar.add(new Vector3Dd(0.0, 0.0, 0.0));
        almostCoplanar.add(new Vector3Dd(2.0, 0.0, 0.0));
        almostCoplanar.add(new Vector3Dd(2.0, 2.0, 1.0e-12));
        almostCoplanar.add(new Vector3Dd(0.0, 2.0, 0.0));

        return [
            [coplanarSquare, true],
            [nonCoplanar, false],
            [almostCoplanar, true],
        ];
    }

    function fixtureFaces(): _PolyhedralBoundedSolidFace[] {
        const solid = PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0, 0.0, 0.0, 0.0);

        const faces: _PolyhedralBoundedSolidFace[] = [];
        let i: number;
        for (i = 0; i < solid.getPolygonsList().size(); i++) {
            faces.push(solid.getPolygonsList().get(i)!);
        }
        return faces;
    }

    it.each(coplanaritySamples())(
        "given_pointsSet_when_validateFacePointsAreCoplanar_then_matchesExpected",
        (points: ArrayList<Vector3Dd>, expected: boolean) => {
            // Arrange
            const numericContext = PolyhedralBoundedSolidNumericPolicy.forPoints(points.toArray());

            // Action
            const result = PolyhedralBoundedSolidGeometricValidator.validateFacePointsAreCoplanar(
                points.toArray(),
                numericContext,
            );

            // Assert
            expect(result).toBe(expected);
        },
    );

    it.each(fixtureFaces())(
        "given_fixtureFace_when_validateFaceIsPlanar_then_returnsTrue",
        (face: _PolyhedralBoundedSolidFace) => {
            // Arrange
            const numericContext = PolyhedralBoundedSolidNumericPolicy.forFace(face);

            // Action
            const result = PolyhedralBoundedSolidGeometricValidator.validateFaceIsPlanar(face, numericContext);

            // Assert
            expect(result).toBe(true);
        },
    );
});
