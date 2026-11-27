import { describe, expect, it } from "vitest";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
describe("Vector3Dd", () => {
    it("computes cross product and spherical reconstruction", () => {
        const a = new Vector3Dd(1, 0, 0),
            b = new Vector3Dd(0, 1, 0);
        expect(a.crossProduct(b).epsilonEquals(new Vector3Dd(0, 0, 1), 1e-12)).toBe(true);
        expect(
            Vector3Dd.fromSpherical(3, a.obtainSphericalThetaAngle(), a.obtainSphericalPhiAngle()).length(),
        ).toBeCloseTo(3);
    });
    it("computes lengths and distances", () => {
        const a = new Vector3Dd(1.0, -2.0, 2.0);
        expect(a.lengthSquared()).toBe(9.0);
        expect(a.manhattanLength()).toBe(5.0);
        const b = new Vector3Dd(4.0, 2.0, 2.0);
        expect(a.distance(b)).toBe(5.0);
        expect(a.distanceSquared(b)).toBe(25.0);
        expect(a.distance(b)).toBe(b.subtract(a).length());
    });
    it("multiplies by the inverse and keeps small divisors", () => {
        const v = new Vector3Dd(2.0, 4.0, -6.0);
        const scaled = v.multiplyByInverse(2.0, 1e-6);
        expect(scaled.x()).toBe(1.0);
        expect(scaled.y()).toBe(2.0);
        expect(scaled.z()).toBe(-3.0);
        expect(v.multiplyByInverse(1e-7, 1e-6).equals(v)).toBe(true);
        expect(v.multiplyByInverse(-1e-7, 1e-6).equals(v)).toBe(true);
        // The factor is computed once: x * (1 / s), not x / s
        const s = 3.0;
        expect(new Vector3Dd(1.0, 0.0, 0.0).multiplyByInverse(s, 1e-6).x()).toBe((1.0 / s) * 1.0);
    });
    it("normalizes with an epsilon", () => {
        const n = new Vector3Dd(3.0, 4.0, 12.0).normalized(1e-6);
        const factor = 1.0 / 13.0;
        expect(n.x()).toBe(factor * 3.0);
        expect(n.y()).toBe(factor * 4.0);
        expect(n.z()).toBe(factor * 12.0);
        const tiny = new Vector3Dd(1e-8, 0.0, 0.0);
        expect(tiny.normalized(1e-6).equals(tiny)).toBe(true);
    });
    it("finds the dominant axis", () => {
        expect(new Vector3Dd(-5.0, 1.0, 2.0).dominantAxis()).toBe(0);
        expect(new Vector3Dd(1.0, -5.0, 2.0).dominantAxis()).toBe(1);
        expect(new Vector3Dd(1.0, 2.0, -5.0).dominantAxis()).toBe(2);
        expect(new Vector3Dd(3.0, -3.0, 3.0).dominantAxis()).toBe(0);
        expect(new Vector3Dd(1.0, 3.0, -3.0).dominantAxis()).toBe(1);
    });
    it("gives indexed components matching the named accessors", () => {
        const value = new Vector3Dd(1.5, -2.0, 3.25);
        expect(value.component(0)).toBe(value.x());
        expect(value.component(1)).toBe(value.y());
        expect(value.component(2)).toBe(value.z());
    });
});
