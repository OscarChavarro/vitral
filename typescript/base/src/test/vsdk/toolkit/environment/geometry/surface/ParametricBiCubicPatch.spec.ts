import { describe, expect, it } from "vitest";
import { Vector3Dd } from "../../../../../../main/vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { ParametricBiCubicPatch } from "../../../../../../main/vsdk/toolkit/environment/geometry/surface/ParametricBiCubicPatch.js";

describe("ParametricBiCubicPatch", () => {
    it("matches Java Bezier blending for a planar bilinear control mesh", () => {
        const points = Array.from({ length: 4 }, (_, i) =>
            Array.from({ length: 4 }, (_, j) => new Vector3Dd(i / 3, j / 3, 0)),
        );
        const patch = new ParametricBiCubicPatch();
        patch.buildBezierPatch(points);
        const position = patch.evaluate(0.5, 0.5),
            tangent = patch.evaluateTangent(0.5, 0.5),
            binormal = patch.evaluateBinormal(0.5, 0.5),
            normal = patch.evaluateNormal(0.5, 0.5);
        expect(position.x()).toBeCloseTo(0.5);
        expect(position.y()).toBeCloseTo(0.5);
        expect(position.z()).toBeCloseTo(0);
        expect(tangent.x()).toBeCloseTo(1);
        expect(binormal.y()).toBeCloseTo(1);
        expect(normal.z()).toBeCloseTo(1);
    });
});
