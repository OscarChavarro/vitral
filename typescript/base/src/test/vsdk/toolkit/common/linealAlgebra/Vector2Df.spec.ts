// Translation of java/base/src/test/vsdk/toolkit/common/linealAlgebra/Vector2DfTest.java
import { describe, expect, it } from "vitest";
import { IllegalArgumentException } from "java/lang/IllegalArgumentException.js";
import { Vector2Df } from "vsdk/toolkit/common/linealAlgebra/Vector2Df.js";

const f = Math.fround;
const EPS = f(1.0e-6);

describe("Vector2DfTest", () => {
    const addCases = (): [Vector2Df, Vector2Df, Vector2Df][] => [
        [new Vector2Df(1.5, -2.0), new Vector2Df(-0.5, 3.0), new Vector2Df(1.0, 1.0)],
        [new Vector2Df(0.0, 0.0), new Vector2Df(0.0, 0.0), new Vector2Df(0.0, 0.0)],
        [new Vector2Df(f(1.0e-6), f(-1.0e-6)), new Vector2Df(f(-1.0e-6), f(1.0e-6)), new Vector2Df(0.0, 0.0)],
    ];

    it.each(addCases())("given_twoVectors_when_adding_then_componentsMatch %#", (a, b, expected) => {
        const c = a.add(b);
        expect(c.epsilonEquals(expected, EPS)).toBe(true);
    });

    const scaleCases = (): [Vector2Df, number, Vector2Df][] => [
        [new Vector2Df(2.0, -3.0), 2.5, new Vector2Df(5.0, -7.5)],
        [new Vector2Df(0.0, 0.0), 9.0, new Vector2Df(0.0, 0.0)],
        [new Vector2Df(4.0, -8.0), 0.0, new Vector2Df(0.0, 0.0)],
    ];

    it.each(scaleCases())("given_vector_when_scaling_then_componentsAndLengthScale %#", (v, scalar, expected) => {
        const scaled = v.multiply(scalar);

        expect(scaled.epsilonEquals(expected, f(1.0e-5))).toBe(true);
        expect(Math.abs(scaled.length() - f(Math.abs(scalar) * v.length()))).toBeLessThanOrEqual(f(1.0e-5));
    });

    it("given_vector_when_usingWithMethods_then_originalRemainsImmutable", () => {
        const v = new Vector2Df(3.0, 4.0);

        const withX = v.withX(10.0);
        const withY = v.withY(-1.0);

        expect(withX.epsilonEquals(new Vector2Df(10.0, 4.0), EPS)).toBe(true);
        expect(withY.epsilonEquals(new Vector2Df(3.0, -1.0), EPS)).toBe(true);
        expect(v.epsilonEquals(new Vector2Df(3.0, 4.0), EPS)).toBe(true);
    });

    it("given_closeVectors_when_epsilonEquals_then_itDependsOnTolerance", () => {
        const a = new Vector2Df(1.0, 2.0);
        const b = new Vector2Df(f(1.0 + f(1.0e-4)), f(2.0 - f(1.0e-4)));

        expect(a.epsilonEquals(b, f(1.0e-3))).toBe(true);
        expect(a.epsilonEquals(b, f(1.0e-6))).toBe(false);
        expect(() => a.epsilonEquals(b, -1.0)).toThrow(IllegalArgumentException);
    });

    // Port of the Vector2DfTest case of `cpp/base/src/test/vector_tests.cpp`;
    // the test method is named `<cpp suite>_<cpp test>`.

    it("Vector2DfTest_SubtractAndLengthSquared", () => {
        const d = new Vector2Df(4.0, -1.0).subtract(new Vector2Df(1.0, 3.0));
        expect(d.x()).toBe(3.0);
        expect(d.y()).toBe(-4.0);
        expect(d.lengthSquared()).toBe(25.0);
    });
});
