import { describe, expect, it } from "vitest";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
describe("Vector3Dd", () =>
    it("computes cross product and spherical reconstruction", () => {
        const a = new Vector3Dd(1, 0, 0),
            b = new Vector3Dd(0, 1, 0);
        expect(a.crossProduct(b).epsilonEquals(new Vector3Dd(0, 0, 1), 1e-12)).toBe(true);
        expect(
            Vector3Dd.fromSpherical(3, a.obtainSphericalThetaAngle(), a.obtainSphericalPhiAngle()).length(),
        ).toBeCloseTo(3);
    }));
