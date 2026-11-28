// Translation of java/base/src/test/vsdk/toolkit/common/linealAlgebra/Vector3DfTest.java
import { describe, expect, it } from "vitest";
import { IllegalArgumentException } from "java/lang/IllegalArgumentException.js";
import { VSDK } from "vsdk/toolkit/common/VSDK.js";
import { Vector3Df } from "vsdk/toolkit/common/linealAlgebra/Vector3Df.js";

const f = Math.fround;

describe("Vector3DfTest", () => {
    const crossCases = (): [Vector3Df, Vector3Df, Vector3Df][] => [
        [new Vector3Df(1.0, 0.0, 0.0), new Vector3Df(0.0, 1.0, 0.0), new Vector3Df(0.0, 0.0, 1.0)],
        [new Vector3Df(0.0, 0.0, 0.0), new Vector3Df(1.0, 2.0, 3.0), new Vector3Df(0.0, 0.0, 0.0)],
        [new Vector3Df(f(1.0e-5), 0.0, 0.0), new Vector3Df(0.0, f(1.0e-5), 0.0), new Vector3Df(0.0, 0.0, f(1.0e-10))],
    ];

    it.each(crossCases())("given_vectors_when_crossProduct_then_resultIsExpected %#", (a, b, expected) => {
        const cross = a.crossProduct(b);

        expect(cross.epsilonEquals(expected, f(1.0e-5))).toBe(true);
        expect(Math.abs(cross.dotProduct(a) - 0.0)).toBeLessThanOrEqual(f(1.0e-5));
        expect(Math.abs(cross.dotProduct(b) - 0.0)).toBeLessThanOrEqual(f(1.0e-5));
    });

    const normalizeCases = (): [Vector3Df, Vector3Df, number][] => [
        [new Vector3Df(3.0, 4.0, 12.0), new Vector3Df(3.0, 4.0, 12.0).multiply(f(1.0 / 13.0)), f(1.0e-5)],
        [new Vector3Df(0.0, 0.0, 0.0), new Vector3Df(0.0, 0.0, 0.0), 0.0],
        [new Vector3Df(f(f(VSDK.EPSILON) / 2.0), 0.0, 0.0), new Vector3Df(f(f(VSDK.EPSILON) / 2.0), 0.0, 0.0), 0.0],
    ];

    it.each(normalizeCases())(
        "given_vector_when_normalized_then_handlesZerosAndSmallValues %#",
        (input, expected, tolerance) => {
            const normalized = input.normalized();
            expect(normalized.epsilonEquals(expected, tolerance)).toBe(true);
        },
    );

    it("given_closeVectors_when_epsilonEquals_then_itDependsOnTolerance", () => {
        const a = new Vector3Df(1.0, 2.0, 3.0);
        const b = new Vector3Df(f(1.0 + f(1.0e-4)), 2.0, f(3.0 - f(1.0e-4)));

        expect(a.epsilonEquals(b, f(1.0e-3))).toBe(true);
        expect(a.epsilonEquals(b, f(1.0e-6))).toBe(false);
        expect(() => a.epsilonEquals(b, -1.0)).toThrow(IllegalArgumentException);
    });

    // Ports of the Vector3DfTest cases of `cpp/base/src/test/vector_tests.cpp`;
    // each test method is named `<cpp suite>_<cpp test>`.

    it("Vector3DfTest_LengthsAndDistances", () => {
        const a = new Vector3Df(1.0, -2.0, 2.0);
        expect(a.lengthSquared()).toBe(9.0);
        expect(a.manhattanLength()).toBe(5.0);
        const b = new Vector3Df(4.0, 2.0, 2.0);
        expect(a.distance(b)).toBe(5.0);
        expect(a.distanceSquared(b)).toBe(25.0);
        expect(a.distance(b)).toBe(b.subtract(a).length());
    });
    it("Vector3DfTest_MultiplyByInverseKeepsSmallDivisors", () => {
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
    it("Vector3DfTest_NormalizedWithEpsilon", () => {
        const n = new Vector3Df(3.0, 4.0, 12.0).normalized(1e-6);
        const factor = f(1.0 / 13.0);
        expect(n.x()).toBe(f(factor * 3.0));
        expect(n.y()).toBe(f(factor * 4.0));
        expect(n.z()).toBe(f(factor * 12.0));
        const tiny = new Vector3Df(1e-8, 0.0, 0.0);
        expect(tiny.normalized(1e-6).equals(tiny)).toBe(true);
    });
    it("Vector3DfTest_DominantAxis", () => {
        expect(new Vector3Df(-5.0, 1.0, 2.0).dominantAxis()).toBe(0);
        expect(new Vector3Df(1.0, -5.0, 2.0).dominantAxis()).toBe(1);
        expect(new Vector3Df(1.0, 2.0, -5.0).dominantAxis()).toBe(2);
        expect(new Vector3Df(3.0, -3.0, 3.0).dominantAxis()).toBe(0);
        expect(new Vector3Df(1.0, 3.0, -3.0).dominantAxis()).toBe(1);
    });
    it("Vector3DfTest_AssignmentReplacesValue", () => {
        let v = new Vector3Df(1.0, 2.0, 3.0);
        v = v.withY(5.0);
        expect(v.equals(new Vector3Df(1.0, 5.0, 3.0))).toBe(true);
    });
    it("indexedComponentsMatchNamedAccessors", () => {
        const vector = new Vector3Df(1.5, -2, 3.25);
        expect(vector.component(0)).toBe(vector.x());
        expect(vector.component(1)).toBe(vector.y());
        expect(vector.component(2)).toBe(vector.z());
    });
});
