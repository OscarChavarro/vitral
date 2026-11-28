// Translation of java/base/src/test/vsdk/toolkit/common/linealAlgebra/Vector4DfTest.java
import { describe, expect, it } from "vitest";
import { IllegalArgumentException } from "java/lang/IllegalArgumentException.js";
import { Vector4Df } from "vsdk/toolkit/common/linealAlgebra/Vector4Df.js";

const f = Math.fround;
const EPS = f(1.0e-5);

describe("Vector4DfTest", () => {
    const divideByWCases = (): [Vector4Df, Vector4Df][] => [
        [new Vector4Df(4.0, 6.0, 8.0, 2.0), new Vector4Df(2.0, 3.0, 4.0, 1.0)],
        [new Vector4Df(1.0, 2.0, 3.0, 0.0), new Vector4Df(1.0, 2.0, 3.0, 0.0)],
        [new Vector4Df(1.0, 2.0, 3.0, f(1.0e-8)), new Vector4Df(1.0, 2.0, 3.0, f(1.0e-8))],
    ];

    it.each(divideByWCases())(
        "given_homogeneousVector_when_dividingByW_then_handlesEdgeCases %#",
        (input, expected) => {
            const result = input.dividedByW();
            expect(result.epsilonEquals(expected, f(1.0e-5))).toBe(true);
        },
    );

    it("given_vectors_when_addAndScale_then_componentsMatch", () => {
        const a = new Vector4Df(1.0, 2.0, 3.0, 1.0);
        const b = new Vector4Df(-1.0, 4.0, 0.5, 2.0);

        const sum = a.add(b);
        const scaled = sum.multiply(2.0);

        expect(scaled.epsilonEquals(new Vector4Df(0.0, 12.0, 7.0, 6.0), EPS)).toBe(true);
    });

    it("given_closeVectors_when_epsilonEquals_then_itDependsOnTolerance", () => {
        const a = new Vector4Df(1.0, 2.0, 3.0, 1.0);
        const b = new Vector4Df(1.0, f(2.0 + f(1.0e-4)), 3.0, 1.0);

        expect(a.epsilonEquals(b, f(1.0e-3))).toBe(true);
        expect(a.epsilonEquals(b, f(1.0e-6))).toBe(false);
        expect(() => a.epsilonEquals(b, -1.0)).toThrow(IllegalArgumentException);
    });
});
