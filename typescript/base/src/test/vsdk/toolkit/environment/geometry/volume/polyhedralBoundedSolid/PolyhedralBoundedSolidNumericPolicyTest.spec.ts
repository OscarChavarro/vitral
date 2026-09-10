import { describe, expect, it } from "vitest";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { PolyhedralBoundedSolidNumericPolicy as Policy } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.js";
describe("PolyhedralBoundedSolidNumericPolicyTest", () => {
    it("scales and clamps tolerances", () => {
        const c = Policy.fromScale(100);
        expect(c.modelScale()).toBe(100);
        expect(c.epsilon()).toBeCloseTo(Policy.BREP_EPSILON * 100);
        expect(Policy.unitIntervalContainsStrictly(0.5, c)).toBe(true);
        expect(Policy.unitIntervalContainsStrictly(0, c)).toBe(false);
    });
    it("classifies points and vectors", () => {
        const c = Policy.defaultContext();
        expect(Policy.pointsCoincident(new Vector3Dd(), new Vector3Dd(c.bigEpsilon() / 2, 0, 0), c)).toBe(true);
        expect(Policy.vectorsColinear(new Vector3Dd(1, 0, 0), new Vector3Dd(4, 0, 0), c)).toBe(true);
    });
});
