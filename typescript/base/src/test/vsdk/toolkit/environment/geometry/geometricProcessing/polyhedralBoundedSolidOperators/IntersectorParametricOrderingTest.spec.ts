import { describe, expect, it } from "vitest";

import { PolyhedralBoundedSolidModeler } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";
import { PolyhedralBoundedSolidTestFixtures } from "../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTestFixtures.js";

/**
Acceptance tests for §4.3 of the stage-2 hardening plan: deterministic
parametric ordering of sonea/soneb null-edge sets.

<p>The comparator for `_PolyhedralBoundedSolidSetOperatorNullEdge` uses
exact double comparison (no epsilon band) so that `Collections.sort`
produces a consistent total order.  These tests verify that running the same
boolean operation on identical solid pairs returns structurally identical
results, regardless of any traversal-order variation at the JVM level.</p>

<p>Traceability: §4.3 (exact compareTo in NullEdge), plan stage-2 2026-05-14.</p>
*/
describe("IntersectorParametricOrderingTest", () => {
    /**
    Two independent union operations on the same geometry must produce results
    with identical face, edge and vertex counts.  A non-deterministic sort
    would sometimes pair null-edges from different intersection chains,
    producing different face counts across runs.
    */
    it("given_overlappingBoxes_when_unionTwice_then_sameStructure", () => {
        const a1 = PolyhedralBoundedSolidTestFixtures.createBoxSolid(2.0, 2.0, 2.0, 0.0, 0.0, 0.0);
        const b1 = PolyhedralBoundedSolidTestFixtures.createBoxSolid(2.0, 2.0, 2.0, 1.0, 0.0, 0.0);

        const a2 = PolyhedralBoundedSolidTestFixtures.createBoxSolid(2.0, 2.0, 2.0, 0.0, 0.0, 0.0);
        const b2 = PolyhedralBoundedSolidTestFixtures.createBoxSolid(2.0, 2.0, 2.0, 1.0, 0.0, 0.0);

        const result1 = PolyhedralBoundedSolidModeler.setOp(a1, b1, PolyhedralBoundedSolidModeler.UNION, false);
        const result2 = PolyhedralBoundedSolidModeler.setOp(a2, b2, PolyhedralBoundedSolidModeler.UNION, false);

        expect(result1).not.toBeNull();
        expect(result2).not.toBeNull();

        expect(result1.getPolygonsList().size(), "face count must be deterministic across runs").toBe(
            result2.getPolygonsList().size(),
        );
        expect(result1.getEdgesList().size(), "edge count must be deterministic across runs").toBe(
            result2.getEdgesList().size(),
        );
        expect(result1.getVerticesList().size(), "vertex count must be deterministic across runs").toBe(
            result2.getVerticesList().size(),
        );
    });

    /**
    Same determinism check for subtraction, which has a different connect path
    and is more sensitive to null-edge ordering in the chain traversal.
    */
    it("given_overlappingBoxes_when_subtractTwice_then_sameStructure", () => {
        const a1 = PolyhedralBoundedSolidTestFixtures.createBoxSolid(2.0, 2.0, 2.0, 0.0, 0.0, 0.0);
        const b1 = PolyhedralBoundedSolidTestFixtures.createBoxSolid(2.0, 2.0, 2.0, 1.0, 0.0, 0.0);

        const a2 = PolyhedralBoundedSolidTestFixtures.createBoxSolid(2.0, 2.0, 2.0, 0.0, 0.0, 0.0);
        const b2 = PolyhedralBoundedSolidTestFixtures.createBoxSolid(2.0, 2.0, 2.0, 1.0, 0.0, 0.0);

        const result1 = PolyhedralBoundedSolidModeler.setOp(a1, b1, PolyhedralBoundedSolidModeler.SUBTRACT, false);
        const result2 = PolyhedralBoundedSolidModeler.setOp(a2, b2, PolyhedralBoundedSolidModeler.SUBTRACT, false);

        expect(result1).not.toBeNull();
        expect(result2).not.toBeNull();

        expect(result1.getPolygonsList().size(), "face count must be deterministic across runs").toBe(
            result2.getPolygonsList().size(),
        );
        expect(result1.getEdgesList().size(), "edge count must be deterministic across runs").toBe(
            result2.getEdgesList().size(),
        );
        expect(result1.getVerticesList().size(), "vertex count must be deterministic across runs").toBe(
            result2.getVerticesList().size(),
        );
    });
});
