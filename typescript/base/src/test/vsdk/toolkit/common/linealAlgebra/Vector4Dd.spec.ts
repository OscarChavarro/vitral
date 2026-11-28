// Translation of java/base/src/test/vsdk/toolkit/common/linealAlgebra/Vector4DTest.java
import { describe, expect, it } from "vitest";
import { IllegalArgumentException } from "java/lang/IllegalArgumentException.js";
import { Vector4Dd } from "vsdk/toolkit/common/linealAlgebra/Vector4Dd.js";

const EPS = 1.0e-9;

describe("Vector4DTest", () => {
    const divideByWCases = (): [Vector4Dd, Vector4Dd][] => [
        [new Vector4Dd(4.0, 6.0, 8.0, 2.0), new Vector4Dd(2.0, 3.0, 4.0, 1.0)],
        [new Vector4Dd(1.0, 2.0, 3.0, 0.0), new Vector4Dd(1.0, 2.0, 3.0, 0.0)],
        [new Vector4Dd(1.0, 2.0, 3.0, 1.0e-12), new Vector4Dd(1.0, 2.0, 3.0, 1.0e-12)],
    ];

    it.each(divideByWCases())(
        "given_homogeneousVector_when_dividingByW_then_handlesEdgeCases %#",
        (input, expected) => {
            const result = input.dividedByW();
            expect(result.epsilonEquals(expected, 1.0e-12)).toBe(true);
        },
    );

    it("given_vectors_when_addAndScale_then_componentsMatch", () => {
        const a = new Vector4Dd(1.0, 2.0, 3.0, 1.0);
        const b = new Vector4Dd(-1.0, 4.0, 0.5, 2.0);

        const sum = a.add(b);
        const scaled = sum.multiply(2.0);

        expect(scaled.epsilonEquals(new Vector4Dd(0.0, 12.0, 7.0, 6.0), EPS)).toBe(true);
    });

    it("given_closeVectors_when_epsilonEquals_then_itDependsOnTolerance", () => {
        const a = new Vector4Dd(1.0, 2.0, 3.0, 1.0);
        const b = new Vector4Dd(1.0, 2.0 + 1.0e-7, 3.0, 1.0);

        expect(a.epsilonEquals(b, 1.0e-6)).toBe(true);
        expect(a.epsilonEquals(b, 1.0e-9)).toBe(false);
        expect(() => a.epsilonEquals(b, -1.0)).toThrow(IllegalArgumentException);
    });
});
