// Translation of java/base/src/test/vsdk/toolkit/common/linealAlgebra/MatrixNxMTest.java
import { describe, expect, it } from "vitest";
import { IllegalArgumentException } from "java/lang/IllegalArgumentException.js";
import { MatrixNxM } from "vsdk/toolkit/common/linealAlgebra/MatrixNxM.js";
import { MatrixDimensionMismatchException } from "vsdk/toolkit/common/linealAlgebra/exceptions/MatrixDimensionMismatchException.js";
import { MatrixIndexOutOfBoundsException } from "vsdk/toolkit/common/linealAlgebra/exceptions/MatrixIndexOutOfBoundsException.js";
import { MatrixNotSquareException } from "vsdk/toolkit/common/linealAlgebra/exceptions/MatrixNotSquareException.js";
import { MatrixSingularException } from "vsdk/toolkit/common/linealAlgebra/exceptions/MatrixSingularException.js";

describe("MatrixNxMTest", () => {
    it("given_squareMatrix_when_computingDeterminantAndInverse_then_resultsAreConsistent", () => {
        const matrix = new MatrixNxM(2, 2).withVal(0, 0, 4.0).withVal(0, 1, 7.0).withVal(1, 0, 2.0).withVal(1, 1, 6.0);

        const determinant = matrix.determinant();
        const inverse = matrix.inverse();
        const identity = matrix.multiply(inverse);

        expect(Math.abs(determinant - 10.0)).toBeLessThanOrEqual(1.0e-9);
        expect(identity.epsilonEquals(new MatrixNxM(2, 2), 1.0e-8)).toBe(true);
    });

    const determinantCases = (): [MatrixNxM, number][] => {
        const m1 = new MatrixNxM(1, 1).withVal(0, 0, 3.5);
        const m2 = new MatrixNxM(2, 2).withVal(0, 0, 1.0).withVal(0, 1, 2.0).withVal(1, 0, 3.0).withVal(1, 1, 4.0);
        const m3 = new MatrixNxM(3, 3)
            .withVal(0, 0, 6.0)
            .withVal(0, 1, 1.0)
            .withVal(0, 2, 1.0)
            .withVal(1, 0, 4.0)
            .withVal(1, 1, -2.0)
            .withVal(1, 2, 5.0)
            .withVal(2, 0, 2.0)
            .withVal(2, 1, 8.0)
            .withVal(2, 2, 7.0);

        return [
            [m1, 3.5],
            [m2, -2.0],
            [m3, -306.0],
        ];
    };

    it.each(determinantCases())("given_knownMatrices_when_determinant_then_matchesExpected %#", (matrix, expected) => {
        expect(Math.abs(matrix.determinant() - expected)).toBeLessThanOrEqual(1.0e-9);
    });

    const exceptionCases = (): [() => void, abstract new (...args: never[]) => unknown][] => {
        const twoByThree = new MatrixNxM(2, 3);
        const twoByTwo = new MatrixNxM(2, 2);
        const oneByOne = new MatrixNxM(1, 1);
        const nonSquare = new MatrixNxM(2, 3);
        const singular = new MatrixNxM(2, 2)
            .withVal(0, 0, 1.0)
            .withVal(0, 1, 2.0)
            .withVal(1, 0, 2.0)
            .withVal(1, 1, 4.0);

        return [
            [() => twoByThree.multiply(twoByTwo), MatrixDimensionMismatchException],
            [() => oneByOne.buildMinor(0, 0), MatrixDimensionMismatchException],
            [() => nonSquare.determinant(), MatrixNotSquareException],
            [() => singular.inverse(), MatrixSingularException],
            [() => twoByTwo.getVal(5, 0), MatrixIndexOutOfBoundsException],
            [() => new MatrixNxM(0, 2), MatrixDimensionMismatchException],
        ];
    };

    it.each(exceptionCases())(
        "given_invalidOperations_when_executing_then_throwsTypedExceptions %#",
        (operation, expected) => {
            expect(operation).toThrow(expected);
        },
    );

    it("given_closeMatrices_when_epsilonEquals_then_itDependsOnTolerance", () => {
        const a = new MatrixNxM(2, 2);
        const b = a.withVal(1, 0, 1.0e-7);
        const c = new MatrixNxM(3, 3);

        expect(a.epsilonEquals(b, 1.0e-6)).toBe(true);
        expect(a.epsilonEquals(b, 1.0e-9)).toBe(false);
        expect(a.epsilonEquals(c, 1.0e-6)).toBe(false);
        expect(() => a.epsilonEquals(b, -1.0)).toThrow(IllegalArgumentException);
    });
});
