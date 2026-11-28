import { describe, expect, it } from "vitest";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { ParametricCurve } from "vsdk/toolkit/environment/geometry/curve/ParametricCurve.js";
import { PolyhedralBoundedSolidModeler } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";
import type { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import type { _PolyhedralBoundedSolidVertex } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.js";

/** Translation of `java/base/src/test/vsdk/toolkit/processing/CurveModelerGlyphWeldTest.java`. */
describe("CurveModelerGlyphWeldTest", () => {
    it("given_contourWithNearDuplicateClosingVertex_when_buildBrep_then_noCoincidentVertexPair", () => {
        const curve = new ParametricCurve();
        curve.setApproximationSteps(8);
        addCorner(curve, 0.326563, 0.640625);
        addCorner(curve, 0.214063, 0.301563);
        addCorner(curve, 0.44375, 0.301563);
        addCorner(curve, 0.329688, 0.640625);
        addCorner(curve, 0.326563, 0.640625);

        const solid: PolyhedralBoundedSolid = PolyhedralBoundedSolidModeler.createBrepFromParametricCurve(curve);

        expect(hasCoincidentVertexPair(solid, 1.0e-3)).toBe(false);
        expect(solid.getVerticesList().size()).toBe(3);
    });

    it("given_squareContourWithoutNearDuplicates_when_buildBrep_then_preservesAllVertices", () => {
        const curve = new ParametricCurve();
        curve.setApproximationSteps(8);
        addCorner(curve, 0.0, 0.0);
        addCorner(curve, 1.0, 0.0);
        addCorner(curve, 1.0, 1.0);
        addCorner(curve, 0.0, 1.0);
        addCorner(curve, 0.0, 0.0);

        const solid: PolyhedralBoundedSolid = PolyhedralBoundedSolidModeler.createBrepFromParametricCurve(curve);

        expect(solid.getVerticesList().size()).toBe(4);
    });

    function addCorner(curve: ParametricCurve, x: number, y: number): void {
        const point: Vector3Dd[] = new Array<Vector3Dd>(1);
        point[0] = new Vector3Dd(x, y, 0.0);
        curve.addPoint(point, ParametricCurve.CORNER);
    }

    function hasCoincidentVertexPair(solid: PolyhedralBoundedSolid, tolerance: number): boolean {
        let i: number;
        let j: number;

        for (i = 0; i < solid.getVerticesList().size(); i++) {
            const firstVertex: _PolyhedralBoundedSolidVertex = solid.getVerticesList().get(i);
            for (j = i + 1; j < solid.getVerticesList().size(); j++) {
                const secondVertex: _PolyhedralBoundedSolidVertex = solid.getVerticesList().get(j);
                if (Vector3Dd.distance(firstVertex.position, secondVertex.position) < tolerance) {
                    return true;
                }
            }
        }
        return false;
    }
});
