// Translation of java/base/src/test/vsdk/toolkit/common/linealAlgebra/Vector2DTest.java
import { describe, expect, it } from "vitest";
import { IllegalArgumentException } from "java/lang/IllegalArgumentException.js";
import { Vector2Dd } from "vsdk/toolkit/common/linealAlgebra/Vector2Dd.js";

const EPS = 1.0e-9;

describe("Vector2DTest", () => {
    const addCases = (): [Vector2Dd, Vector2Dd, Vector2Dd][] => [
        [new Vector2Dd(1.5, -2.0), new Vector2Dd(-0.5, 3.0), new Vector2Dd(1.0, 1.0)],
        [new Vector2Dd(0.0, 0.0), new Vector2Dd(0.0, 0.0), new Vector2Dd(0.0, 0.0)],
        [new Vector2Dd(1.0e-12, -1.0e-12), new Vector2Dd(-1.0e-12, 1.0e-12), new Vector2Dd(0.0, 0.0)],
    ];

    it.each(addCases())("given_twoVectors_when_adding_then_componentsMatch %#", (a, b, expected) => {
        const c = a.add(b);
        expect(c.epsilonEquals(expected, EPS)).toBe(true);
    });

    const scaleCases = (): [Vector2Dd, number, Vector2Dd][] => [
        [new Vector2Dd(2.0, -3.0), 2.5, new Vector2Dd(5.0, -7.5)],
        [new Vector2Dd(0.0, 0.0), 9.0, new Vector2Dd(0.0, 0.0)],
        [new Vector2Dd(4.0, -8.0), 0.0, new Vector2Dd(0.0, 0.0)],
    ];

    it.each(scaleCases())("given_vector_when_scaling_then_componentsAndLengthScale %#", (v, scalar, expected) => {
        const scaled = v.multiply(scalar);

        expect(scaled.epsilonEquals(expected, 1.0e-12)).toBe(true);
        expect(Math.abs(scaled.length() - Math.abs(scalar) * v.length())).toBeLessThanOrEqual(1.0e-12);
    });

    it("given_vector_when_usingWithMethods_then_originalRemainsImmutable", () => {
        const v = new Vector2Dd(3.0, 4.0);

        const withX = v.withX(10.0);
        const withY = v.withY(-1.0);

        expect(withX.epsilonEquals(new Vector2Dd(10.0, 4.0), EPS)).toBe(true);
        expect(withY.epsilonEquals(new Vector2Dd(3.0, -1.0), EPS)).toBe(true);
        expect(v.epsilonEquals(new Vector2Dd(3.0, 4.0), EPS)).toBe(true);
    });

    it("given_closeVectors_when_epsilonEquals_then_itDependsOnTolerance", () => {
        const a = new Vector2Dd(1.0, 2.0);
        const b = new Vector2Dd(1.0 + 1.0e-7, 2.0 - 1.0e-7);

        expect(a.epsilonEquals(b, 1.0e-6)).toBe(true);
        expect(a.epsilonEquals(b, 1.0e-9)).toBe(false);
        expect(() => a.epsilonEquals(b, -1.0)).toThrow(IllegalArgumentException);
    });

    it("given_vectors_when_subtract_then_lengthSquaredIsExact", () => {
        const d = new Vector2Dd(4.0, -1.0).subtract(new Vector2Dd(1.0, 3.0));
        expect(d.x).toBe(3.0);
        expect(d.y).toBe(-4.0);
        expect(d.lengthSquared()).toBe(25.0);
    });
});
