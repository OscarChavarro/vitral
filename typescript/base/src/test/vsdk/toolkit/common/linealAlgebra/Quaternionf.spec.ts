// Translation of java/base/src/test/vsdk/toolkit/common/linealAlgebra/QuaternionfTest.java
import { describe, expect, it } from "vitest";
import { IllegalArgumentException } from "java/lang/IllegalArgumentException.js";
import { Quaternionf } from "vsdk/toolkit/common/linealAlgebra/Quaternionf.js";
import { Vector3Df } from "vsdk/toolkit/common/linealAlgebra/Vector3Df.js";

const f = Math.fround;
const EPS = f(1.0e-5);

describe("QuaternionfTest", () => {
    it("given_copyFactory_when_copyingQuaternion_then_itKeepsValues", () => {
        const source = new Quaternionf(new Vector3Df(1.0, 2.0, 3.0), 4.0);

        const copy = Quaternionf.copyOf(source);

        expect(copy).not.toBe(source);
        expect(copy.direction().epsilonEquals(source.direction(), EPS)).toBe(true);
        expect(copy.magnitude()).toBe(source.magnitude());
    });

    const rotationCases = (): [Quaternionf, Vector3Df, Vector3Df][] => {
        const half90 = f(Math.PI / 4.0);
        const rotZ90 = new Quaternionf(new Vector3Df(0.0, 0.0, f(Math.sin(half90))), f(Math.cos(half90)));
        const identity = new Quaternionf(new Vector3Df(0.0, 0.0, 0.0), 1.0);

        return [
            [rotZ90, new Vector3Df(1.0, 0.0, 0.0), new Vector3Df(0.0, 1.0, 0.0)],
            [identity, new Vector3Df(2.0, -1.0, 0.5), new Vector3Df(2.0, -1.0, 0.5)],
        ];
    };

    it.each(rotationCases())(
        "given_unitQuaternions_when_rotatingVector_then_matchesExpected %#",
        (q, input, expected) => {
            const rotated = q.rotate(input);
            expect(rotated.epsilonEquals(expected, f(1.0e-4))).toBe(true);
        },
    );

    it("given_nonZeroQuaternion_when_normalizingAndConjugating_then_normAndSignAreConsistent", () => {
        const q = new Quaternionf(new Vector3Df(0.0, 3.0, 4.0), 12.0);

        const normalized = q.normalized();
        const conjugated = q.conjugated();

        expect(Math.abs(normalized.length() - 1.0)).toBeLessThanOrEqual(EPS);
        expect(conjugated.direction().epsilonEquals(q.direction().multiply(-1.0), EPS)).toBe(true);
        expect(conjugated.magnitude()).toBe(q.magnitude());
    });

    it("given_zeroQuaternion_when_normalized_then_itReturnsSameInstanceState", () => {
        const q = new Quaternionf(new Vector3Df(0.0, 0.0, 0.0), 0.0);
        const normalized = q.normalized();
        expect(normalized.epsilonEquals(q, 0.0)).toBe(true);
    });

    it("given_closeQuaternions_when_epsilonEquals_then_itDependsOnTolerance", () => {
        const a = new Quaternionf(new Vector3Df(0.0, 0.5, 0.0), f(0.8660254));
        const b = new Quaternionf(new Vector3Df(0.0, f(0.5001), 0.0), f(0.8660254));

        expect(a.epsilonEquals(b, f(1.0e-3))).toBe(true);
        expect(a.epsilonEquals(b, f(1.0e-6))).toBe(false);
        expect(() => a.epsilonEquals(b, -1.0)).toThrow(IllegalArgumentException);
    });
});
