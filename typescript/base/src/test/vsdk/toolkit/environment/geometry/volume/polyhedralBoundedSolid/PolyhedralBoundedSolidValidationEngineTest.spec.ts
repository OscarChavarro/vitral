import { describe, expect, it } from "vitest";

import { StringBuilder } from "java/lang/StringBuilder.js";
import { PolyhedralBoundedSolidNumericPolicy } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.js";
import { PolyhedralBoundedSolidValidationEngine } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
import { _GeometricStrictLoopsStrategy } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/_GeometricStrictLoopsStrategy.js";
import { _PolyhedralBoundedSolidHalfEdge } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.js";
import { _PolyhedralBoundedSolidLoop } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.js";
import { PolyhedralBoundedSolidTestFixtures } from "./PolyhedralBoundedSolidTestFixtures.js";

/**
Covers intermediate and strict validation policies for B-Rep solids.

<p>Traceability: [MANT1988] Ch. 6 boundary models, Ch. 10 half-edge
storage invariants, and Ch. 13.1 face-equation geometric validity.</p>
 */
describe("PolyhedralBoundedSolidValidationEngineTest", () => {
    it("given_mantFixture_when_validateIntermediate_then_returnsTrueAndMarksSolidValid", () => {
        // Arrange
        const solid = PolyhedralBoundedSolidTestFixtures.createMant1986_1Solid();

        // Action
        const result = PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);

        // Assert
        expect(result).toBe(true);
        expect(solid.isValid()).toBe(true);
    });

    it("given_validBoxSolid_when_validateStrict_then_returnsTrueAndMarksSolidValid", () => {
        // Arrange
        const solid = PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0, 0.0, 0.0, 0.0);

        // Action
        const result = PolyhedralBoundedSolidValidationEngine.validateStrict(solid);

        // Assert
        expect(result).toBe(true);
        expect(solid.isValid()).toBe(true);
    });

    it("given_loopWithoutStartHalfEdge_when_validateStrict_then_returnsFalseAndMarksSolidInvalid", () => {
        // Arrange
        const solid = PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0, 0.0, 0.0, 0.0);
        const face = solid.getPolygonsList().get(0)!;
        const loop = face.boundariesList.get(0)!;
        loop.boundaryStartHalfEdge = null;

        // Action
        const result = PolyhedralBoundedSolidValidationEngine.validateStrict(solid);

        // Assert
        expect(result).toBe(false);
        expect(solid.isValid()).toBe(false);
    });

    it("given_twoEdgeWireLoop_when_validateStrict_then_rejectsMalformedLoop", () => {
        const solid = PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0, 0.0, 0.0, 0.0);
        const face = solid.getPolygonsList().get(0)!;
        const malformed = new _PolyhedralBoundedSolidLoop(face);
        const first = new _PolyhedralBoundedSolidHalfEdge(solid.getVerticesList().get(0)!, malformed, solid);
        const second = new _PolyhedralBoundedSolidHalfEdge(solid.getVerticesList().get(1)!, malformed, solid);
        malformed.halfEdgesList.add(first);
        malformed.halfEdgesList.add(second);
        malformed.boundaryStartHalfEdge = first;
        const message = new StringBuilder();

        const result = new _GeometricStrictLoopsStrategy().validate(
            solid,
            PolyhedralBoundedSolidNumericPolicy.forSolid(solid),
            message,
        );

        expect(result).toBe(false);
        expect(message.toString()).toContain("loop with fewer than 3 edges");
    });
});
