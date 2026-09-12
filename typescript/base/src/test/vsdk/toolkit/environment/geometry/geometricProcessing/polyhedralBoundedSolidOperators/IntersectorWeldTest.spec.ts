import { describe, expect, it } from "vitest";

import { StringBuilder } from "java/lang/StringBuilder.js";
import { PolyhedralBoundedSolidGeometricValidator } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidGeometricValidator.js";
import { PolyhedralBoundedSolidNumericPolicy } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.js";
import { PolyhedralBoundedSolidModeler } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";
import { PolyhedralBoundedSolidTestFixtures } from "../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTestFixtures.js";

/**
Acceptance tests for §4.2 of the stage-2 hardening plan: post-Generate weld
of coincident intersection vertices.

<p>These tests verify that after setOpGenerate runs and the weld pass fires,
the result solid contains no spatially coincident vertices.  The weld pass
is critical for preventing the Classify and Connect phases from seeing
duplicate nodes that share the same position.</p>

<p>Traceability: §4.2 (weldIntersectionVertices + pruneStaleVertexFaceEntries),
plan-csg-boolean-fix-stage2.md, 2026-05-14.</p>
*/
describe("IntersectorWeldTest", () => {
    /**
    Two overlapping boxes produce an intersection ring.  After the boolean
    operation completes, the result must have no two vertices at the same
    position (within bigEpsilon).
    */
    it("given_overlappingBoxes_when_union_then_resultHasNoCoincidentVertices", () => {
        const solidA = PolyhedralBoundedSolidTestFixtures.createBoxSolid(2.0, 2.0, 2.0, 0.0, 0.0, 0.0);
        const solidB = PolyhedralBoundedSolidTestFixtures.createBoxSolid(2.0, 2.0, 2.0, 1.0, 0.0, 0.0);

        const result = PolyhedralBoundedSolidModeler.setOp(solidA, solidB, PolyhedralBoundedSolidModeler.UNION, false);

        expect(result).not.toBeNull();

        const msg = new StringBuilder();
        const noCoincident = PolyhedralBoundedSolidGeometricValidator.validateNoCoincidentVertices(
            result,
            PolyhedralBoundedSolidNumericPolicy.forSolid(result),
            msg,
        );
        expect(noCoincident, "result must have no coincident vertices after weld: " + msg).toBe(true);
    });

    /**
    Subtraction of an overlapping box should also leave no coincident vertices.
    */
    it("given_overlappingBoxes_when_subtract_then_resultHasNoCoincidentVertices", () => {
        const solidA = PolyhedralBoundedSolidTestFixtures.createBoxSolid(2.0, 2.0, 2.0, 0.0, 0.0, 0.0);
        const solidB = PolyhedralBoundedSolidTestFixtures.createBoxSolid(2.0, 2.0, 2.0, 1.0, 0.0, 0.0);

        const result = PolyhedralBoundedSolidModeler.setOp(
            solidA,
            solidB,
            PolyhedralBoundedSolidModeler.SUBTRACT,
            false,
        );

        expect(result).not.toBeNull();

        const msg = new StringBuilder();
        const noCoincident = PolyhedralBoundedSolidGeometricValidator.validateNoCoincidentVertices(
            result,
            PolyhedralBoundedSolidNumericPolicy.forSolid(result),
            msg,
        );
        expect(noCoincident, "result must have no coincident vertices after weld: " + msg).toBe(true);
    });
});
