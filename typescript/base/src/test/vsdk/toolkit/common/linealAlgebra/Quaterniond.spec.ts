// Translation of java/base/src/test/vsdk/toolkit/common/linealAlgebra/QuaternionTest.java
import { describe, expect, it } from "vitest";
import { IllegalArgumentException } from "java/lang/IllegalArgumentException.js";
import { Quaterniond } from "vsdk/toolkit/common/linealAlgebra/Quaterniond.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";

const EPS = 1.0e-9;

describe("QuaternionTest", () => {
    it("given_copyFactory_when_copyingQuaternion_then_itKeepsValues", () => {
        const source = new Quaterniond(new Vector3Dd(1.0, 2.0, 3.0), 4.0);

        const copy = Quaterniond.copyOf(source);

        expect(copy).not.toBe(source);
        expect(copy.direction().epsilonEquals(source.direction(), EPS)).toBe(true);
        expect(copy.magnitude()).toBe(source.magnitude());
    });

    const rotationCases = (): [Quaterniond, Vector3Dd, Vector3Dd][] => {
        const half90 = Math.PI / 4.0;
        const rotZ90 = new Quaterniond(new Vector3Dd(0.0, 0.0, Math.sin(half90)), Math.cos(half90));
        const identity = new Quaterniond(new Vector3Dd(0.0, 0.0, 0.0), 1.0);

        return [
            [rotZ90, new Vector3Dd(1.0, 0.0, 0.0), new Vector3Dd(0.0, 1.0, 0.0)],
            [identity, new Vector3Dd(2.0, -1.0, 0.5), new Vector3Dd(2.0, -1.0, 0.5)],
        ];
    };

    it.each(rotationCases())(
        "given_unitQuaternions_when_rotatingVector_then_matchesExpected %#",
        (q, input, expected) => {
            const rotated = q.rotate(input);
            expect(rotated.epsilonEquals(expected, 1.0e-8)).toBe(true);
        },
    );

    it("given_nonZeroQuaternion_when_normalizingAndConjugating_then_normAndSignAreConsistent", () => {
        const q = new Quaterniond(new Vector3Dd(0.0, 3.0, 4.0), 12.0);

        const normalized = q.normalized();
        const conjugated = q.conjugated();

        expect(Math.abs(normalized.length() - 1.0)).toBeLessThanOrEqual(EPS);
        expect(conjugated.direction().epsilonEquals(q.direction().multiply(-1.0), EPS)).toBe(true);
        expect(conjugated.magnitude()).toBe(q.magnitude());
    });

    it("given_zeroQuaternion_when_normalized_then_itReturnsSameInstanceState", () => {
        const q = new Quaterniond(new Vector3Dd(0.0, 0.0, 0.0), 0.0);
        const normalized = q.normalized();
        expect(normalized.epsilonEquals(q, 0.0)).toBe(true);
    });

    it("given_closeQuaternions_when_epsilonEquals_then_itDependsOnTolerance", () => {
        const a = new Quaterniond(new Vector3Dd(0.0, 0.5, 0.0), 0.86602540378);
        const b = new Quaterniond(new Vector3Dd(0.0, 0.5000001, 0.0), 0.86602540378);

        expect(a.epsilonEquals(b, 1.0e-6)).toBe(true);
        expect(a.epsilonEquals(b, 1.0e-9)).toBe(false);
        expect(() => a.epsilonEquals(b, -1.0)).toThrow(IllegalArgumentException);
    });
});
