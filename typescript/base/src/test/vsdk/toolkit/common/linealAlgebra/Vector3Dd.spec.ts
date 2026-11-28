// Translation of java/base/src/test/vsdk/toolkit/common/linealAlgebra/Vector3DTest.java
import { describe, expect, it } from "vitest";
import { IllegalArgumentException } from "java/lang/IllegalArgumentException.js";
import { VSDK } from "vsdk/toolkit/common/VSDK.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";

describe("Vector3DTest", () => {
    const crossCases = (): [Vector3Dd, Vector3Dd, Vector3Dd][] => [
        [new Vector3Dd(1.0, 0.0, 0.0), new Vector3Dd(0.0, 1.0, 0.0), new Vector3Dd(0.0, 0.0, 1.0)],
        [new Vector3Dd(0.0, 0.0, 0.0), new Vector3Dd(1.0, 2.0, 3.0), new Vector3Dd(0.0, 0.0, 0.0)],
        [new Vector3Dd(1.0e-12, 0.0, 0.0), new Vector3Dd(0.0, 1.0e-12, 0.0), new Vector3Dd(0.0, 0.0, 1.0e-24)],
    ];

    it.each(crossCases())("given_vectors_when_crossProduct_then_resultIsExpected %#", (a, b, expected) => {
        const cross = a.crossProduct(b);

        expect(cross.epsilonEquals(expected, 1.0e-9)).toBe(true);
        expect(Math.abs(cross.dotProduct(a) - 0.0)).toBeLessThanOrEqual(1.0e-9);
        expect(Math.abs(cross.dotProduct(b) - 0.0)).toBeLessThanOrEqual(1.0e-9);
    });

    const normalizeCases = (): [Vector3Dd, Vector3Dd, number][] => [
        [new Vector3Dd(3.0, 4.0, 12.0), new Vector3Dd(3.0, 4.0, 12.0).multiply(1.0 / 13.0), 1.0e-12],
        [new Vector3Dd(0.0, 0.0, 0.0), new Vector3Dd(0.0, 0.0, 0.0), 0.0],
        [new Vector3Dd(VSDK.EPSILON / 2.0, 0.0, 0.0), new Vector3Dd(VSDK.EPSILON / 2.0, 0.0, 0.0), 0.0],
    ];

    it.each(normalizeCases())(
        "given_vector_when_normalized_then_handlesZerosAndSmallValues %#",
        (input, expected, tolerance) => {
            const normalized = input.normalized();
            expect(normalized.epsilonEquals(expected, tolerance)).toBe(true);
        },
    );

    it("given_sphericalAngles_when_roundTripping_then_coordinatesMatch", () => {
        const original = new Vector3Dd(2.0, 2.0, 1.0);

        const r = original.length();
        const theta = original.obtainSphericalThetaAngle();
        const phi = original.obtainSphericalPhiAngle();
        const rebuilt = Vector3Dd.fromSpherical(r, theta, phi);

        expect(rebuilt.epsilonEquals(original, 1.0e-6)).toBe(true);
    });

    it("given_closeVectors_when_epsilonEquals_then_itDependsOnTolerance", () => {
        const a = new Vector3Dd(1.0, 2.0, 3.0);
        const b = new Vector3Dd(1.0 + 1.0e-7, 2.0, 3.0 - 1.0e-7);

        expect(a.epsilonEquals(b, 1.0e-6)).toBe(true);
        expect(a.epsilonEquals(b, 1.0e-9)).toBe(false);
        expect(() => a.epsilonEquals(b, -1.0)).toThrow(IllegalArgumentException);
    });

    it("given_vector_when_lengthsAndDistances_then_resultsAreExact", () => {
        const a = new Vector3Dd(1.0, -2.0, 2.0);
        expect(a.lengthSquared()).toBe(9.0);
        expect(a.manhattanLength()).toBe(5.0);
        const b = new Vector3Dd(4.0, 2.0, 2.0);
        expect(a.distance(b)).toBe(5.0);
        expect(a.distanceSquared(b)).toBe(25.0);
        expect(a.distance(b)).toBe(b.subtract(a).length());
    });
    it("given_smallDivisor_when_multiplyByInverse_then_vectorIsKept", () => {
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
    it("given_epsilon_when_normalized_then_tinyVectorIsKept", () => {
        const n = new Vector3Dd(3.0, 4.0, 12.0).normalized(1e-6);
        const factor = 1.0 / 13.0;
        expect(n.x()).toBe(factor * 3.0);
        expect(n.y()).toBe(factor * 4.0);
        expect(n.z()).toBe(factor * 12.0);
        const tiny = new Vector3Dd(1e-8, 0.0, 0.0);
        expect(tiny.normalized(1e-6).equals(tiny)).toBe(true);
    });
    it("given_vectors_when_dominantAxis_then_tiesGoToFirstAxis", () => {
        expect(new Vector3Dd(-5.0, 1.0, 2.0).dominantAxis()).toBe(0);
        expect(new Vector3Dd(1.0, -5.0, 2.0).dominantAxis()).toBe(1);
        expect(new Vector3Dd(1.0, 2.0, -5.0).dominantAxis()).toBe(2);
        expect(new Vector3Dd(3.0, -3.0, 3.0).dominantAxis()).toBe(0);
        expect(new Vector3Dd(1.0, 3.0, -3.0).dominantAxis()).toBe(1);
    });
    it("given_vector_when_component_then_matchesNamedAccessors", () => {
        const value = new Vector3Dd(1.5, -2.0, 3.25);
        expect(value.component(0)).toBe(value.x());
        expect(value.component(1)).toBe(value.y());
        expect(value.component(2)).toBe(value.z());
    });
});
