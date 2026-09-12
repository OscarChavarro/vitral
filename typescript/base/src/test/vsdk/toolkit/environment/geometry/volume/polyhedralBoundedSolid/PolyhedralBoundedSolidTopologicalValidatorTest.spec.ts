import { describe, expect, it } from "vitest";

import { _PolyhedralBoundedSolidTopologicalValidator } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/_PolyhedralBoundedSolidTopologicalValidator.js";
import { PolyhedralBoundedSolidTestFixtures } from "./PolyhedralBoundedSolidTestFixtures.js";

/**
Checks half-edge topology invariants for valid and intentionally damaged
bounded solids.

<p>Traceability: [MANT1988] Ch. 10.2-10.4, especially the solid, face,
loop, edge, half-edge, and vertex incidence structure.</p>
 */
describe("PolyhedralBoundedSolidTopologicalValidatorTest", () => {
    it("given_validBoxSolid_when_validateTopologicalIntegrity_then_returnsTrue", () => {
        // Arrange
        const solid = PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0, 0.0, 0.0, 0.0);

        // Action
        const result = _PolyhedralBoundedSolidTopologicalValidator.validateTopologicalIntegrity(solid);

        // Assert
        expect(result).toBe(true);
    });

    it("given_edgeWithMissingHalfEdge_when_validateTopologicalIntegrity_then_returnsFalse", () => {
        // Arrange
        const solid = PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0, 0.0, 0.0, 0.0);
        const edge = solid.getEdgesList().get(0)!;
        edge.leftHalf = null;

        // Action
        const result = _PolyhedralBoundedSolidTopologicalValidator.validateTopologicalIntegrity(solid);

        // Assert
        expect(result).toBe(false);
    });
});
