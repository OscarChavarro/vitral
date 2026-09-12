import { describe, expect, it } from "vitest";

import { ArrayList } from "java/util/ArrayList.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { PolyhedralBoundedSolidNumericPolicy } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.js";
import { PolyhedralBoundedSolidTestFixtures } from "./PolyhedralBoundedSolidTestFixtures.js";

/**
Validates the scale-aware tolerance policy used by B-Rep predicates.

<p>Traceability: [MANT1988] Ch. 13.1-13.2, where face equations,
containment, and intersection predicates depend on robust numerical
comparisons even though the book presents them in exact arithmetic.</p>
 */
describe("PolyhedralBoundedSolidNumericPolicyTest", () => {
    it("given_notFiniteScale_when_fromScale_then_usesMinimumScaleContext", () => {
        // Arrange
        const scale = Number.NaN;

        // Action
        const context = PolyhedralBoundedSolidNumericPolicy.fromScale(scale);

        // Assert
        expect(context.modelScale()).toBe(1.0);
        expect(context.epsilon()).toBeCloseTo(PolyhedralBoundedSolidNumericPolicy.BREP_EPSILON, 18);
        expect(context.bigEpsilon()).toBeCloseTo(PolyhedralBoundedSolidNumericPolicy.BREP_BIG_EPSILON, 18);
    });

    it("given_largeScale_when_fromScale_then_scalesEpsilonsWithModelSize", () => {
        // Arrange
        const scale = 10.0;

        // Action
        const context = PolyhedralBoundedSolidNumericPolicy.fromScale(scale);

        // Assert
        expect(context.modelScale()).toBe(scale);
        expect(context.epsilon()).toBeCloseTo(PolyhedralBoundedSolidNumericPolicy.BREP_EPSILON * scale, 18);
        expect(context.bigEpsilon()).toBeCloseTo(PolyhedralBoundedSolidNumericPolicy.BREP_BIG_EPSILON * scale, 18);
    });

    it("given_twoPoints_when_forPoints_then_usesBoundingDiagonalAsScale", () => {
        // Arrange
        const points = new ArrayList<Vector3Dd>();
        points.add(new Vector3Dd(0.0, 0.0, 0.0));
        points.add(new Vector3Dd(3.0, 4.0, 0.0));

        // Action
        const context = PolyhedralBoundedSolidNumericPolicy.forPoints(points.toArray());

        // Assert
        expect(context.modelScale()).toBeCloseTo(5.0, 12);
    });

    it("given_boxSolid_when_forSolid_then_usesSolidScaleAboveMinimum", () => {
        // Arrange
        const solid = PolyhedralBoundedSolidTestFixtures.createBoxSolid(2.0, 4.0, 4.0, 0.0, 0.0, 0.0);

        // Action
        const context = PolyhedralBoundedSolidNumericPolicy.forSolid(solid);

        // Assert
        expect(context.modelScale()).toBeCloseTo(6.0, 12);
        expect(context.unitIntervalTolerance()).toBeGreaterThan(0.0);
    });
});
