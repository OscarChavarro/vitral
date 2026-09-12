import { describe, expect, it } from "vitest";

import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { PolyhedralBoundedSolidTestFixtures } from "./PolyhedralBoundedSolidTestFixtures.js";

/**
Exercises boundary-contact handling for quantitative invisibility queries.

<p>Traceability: APPEL hidden-line quantitative invisibility, used here
against [MANT1988] Ch. 6/10 polyhedral B-Rep topology so edge and vertex
contacts are not double-counted as volume piercings.</p>
 */
describe("PolyhedralBoundedSolidQuantitativeInvisibilityTest", () => {
    it("given_rayEnteringBoxThroughEdge_when_measuringQuantitativeInvisibility_then_countsSinglePiercing", () => {
        // Arrange
        const solid = PolyhedralBoundedSolidTestFixtures.createBoxSolid(2.0, 2.0, 2.0, 0.0, 0.0, 0.0);

        // Action
        const qi = solid.computeQuantitativeInvisibility(
            new Vector3Dd(-3.0, -2.0, 0.0),
            new Vector3Dd(-0.5, -0.75, 0.0),
        );

        // Assert
        expect(qi).toBe(1);
    });

    it("given_rayEnteringBoxThroughVertex_when_measuringQuantitativeInvisibility_then_countsSinglePiercing", () => {
        // Arrange
        const solid = PolyhedralBoundedSolidTestFixtures.createBoxSolid(2.0, 2.0, 2.0, 0.0, 0.0, 0.0);

        // Action
        const qi = solid.computeQuantitativeInvisibility(
            new Vector3Dd(-3.0, -2.0, -2.0),
            new Vector3Dd(-0.5, -0.75, -0.75),
        );

        // Assert
        expect(qi).toBe(1);
    });

    it("given_raySlidingAlongBoundaryWithoutEnteringVolume_when_measuringQuantitativeInvisibility_then_itDoesNotCountTangentialContact", () => {
        // Arrange
        const solid = PolyhedralBoundedSolidTestFixtures.createBoxSolid(2.0, 2.0, 2.0, 0.0, 0.0, 0.0);

        // Action
        const qi = solid.computeQuantitativeInvisibility(
            new Vector3Dd(-3.0, -2.0, -1.0),
            new Vector3Dd(3.0, 1.0, -1.0),
        );

        // Assert
        expect(qi).toBe(0);
    });
});
