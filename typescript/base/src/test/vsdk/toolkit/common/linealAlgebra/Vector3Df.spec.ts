import { describe, expect, it } from "vitest";
import { Vector3Df } from "vsdk/toolkit/common/linealAlgebra/Vector3Df.js";
const f = Math.fround;
describe("Vector3Df", () => {
    it("computes cross product", () => {
        expect(
            new Vector3Df(1, 0, 0).crossProduct(new Vector3Df(0, 1, 0)).epsilonEquals(new Vector3Df(0, 0, 1), 1e-6),
        ).toBe(true);
    });
    it("lengths and distances", () => {
        const a = new Vector3Df(1.0, -2.0, 2.0);
        expect(a.lengthSquared()).toBe(9.0);
        expect(a.manhattanLength()).toBe(5.0);
        const b = new Vector3Df(4.0, 2.0, 2.0);
        expect(a.distance(b)).toBe(5.0);
        expect(a.distanceSquared(b)).toBe(25.0);
        expect(a.distance(b)).toBe(b.subtract(a).length());
    });
    it("multiply by inverse keeps small divisors", () => {
        const v = new Vector3Df(2.0, 4.0, -6.0);
        const scaled = v.multiplyByInverse(2.0, 1e-6);
        expect(scaled.x()).toBe(1.0);
        expect(scaled.y()).toBe(2.0);
        expect(scaled.z()).toBe(-3.0);
        expect(v.multiplyByInverse(1e-7, 1e-6).equals(v)).toBe(true);
        expect(v.multiplyByInverse(-1e-7, 1e-6).equals(v)).toBe(true);
        // The factor is computed once: x * (1 / s), not x / s
        const s = 3.0;
        expect(new Vector3Df(1.0, 0.0, 0.0).multiplyByInverse(s, 1e-6).x()).toBe(f(f(1.0 / s) * 1.0));
    });
    it("normalized with epsilon", () => {
        const n = new Vector3Df(3.0, 4.0, 12.0).normalized(1e-6);
        const factor = f(1.0 / 13.0);
        expect(n.x()).toBe(f(factor * 3.0));
        expect(n.y()).toBe(f(factor * 4.0));
        expect(n.z()).toBe(f(factor * 12.0));
        const tiny = new Vector3Df(1e-8, 0.0, 0.0);
        expect(tiny.normalized(1e-6).equals(tiny)).toBe(true);
    });
    it("dominant axis", () => {
        expect(new Vector3Df(-5.0, 1.0, 2.0).dominantAxis()).toBe(0);
        expect(new Vector3Df(1.0, -5.0, 2.0).dominantAxis()).toBe(1);
        expect(new Vector3Df(1.0, 2.0, -5.0).dominantAxis()).toBe(2);
        expect(new Vector3Df(3.0, -3.0, 3.0).dominantAxis()).toBe(0);
        expect(new Vector3Df(1.0, 3.0, -3.0).dominantAxis()).toBe(1);
    });
    it("assignment replaces value", () => {
        let v = new Vector3Df(1.0, 2.0, 3.0);
        v = v.withY(5.0);
        expect(v.equals(new Vector3Df(1.0, 5.0, 3.0))).toBe(true);
    });
});
