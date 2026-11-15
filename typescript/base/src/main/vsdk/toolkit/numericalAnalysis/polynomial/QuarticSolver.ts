import { Math as JavaMath } from "../../../../java/lang/Math.js";
import { CubicSolver } from "./CubicSolver.js";
import { PolynomialSolver } from "./PolynomialSolver.js";
import { QuadraticSolver } from "./QuadraticSolver.js";

/**
This file was written by Alexander Enzmann.  He wrote the code for
4th-6th order shapes and generously provided us these enhancements.
(Port of cpp `QuarticSolver`.)
*/
export class QuarticSolver {
    private static readonly COEFFICIENT_LIMIT = 1.0e-20;
    private static readonly FUDGE_FACTOR1 = 1.0e11;
    private static readonly FUDGE_FACTOR2 = -1.0e-5;
    private static readonly FUDGE_FACTOR3 = 1.0e-7;

    private static hasDifficultCoefficients(n: number, coefficients: ArrayLike<number>): number {
        let largestCoefficient = 0.0;
        for (let coefficientIndex = 0; coefficientIndex <= n; coefficientIndex++) {
            if (JavaMath.abs(coefficients[coefficientIndex]!) > largestCoefficient) {
                largestCoefficient = coefficients[coefficientIndex]!;
            }
        }

        if (largestCoefficient === 0.0) {
            return 0;
        }

        for (let coefficientIndex = 0; coefficientIndex <= n; coefficientIndex++) {
            if (coefficients[coefficientIndex] !== 0.0) {
                if (JavaMath.abs(largestCoefficient / coefficients[coefficientIndex]!) > QuarticSolver.FUDGE_FACTOR1) {
                    return 1;
                }
            }
        }

        return 0;
    }

    public static solve(coefficients: ArrayLike<number>, roots: number[], minValue: number, epsilon: number): number {
        const cubic: number[] = [0, 0, 0, 0];
        const cubicRoots: number[] = [0, 0, 0];
        let normalizedY: number;
        let linearAdjustment: number;
        let auxiliaryRoot: number;
        let firstThreshold: number;
        let secondThreshold: number;
        let normalizedC1: number;
        let normalizedC2: number;
        let normalizedC3: number;
        let normalizedC4: number;
        let discriminant: number;
        let quadraticCoefficient: number;
        let constantTerm: number;

        if (QuarticSolver.hasDifficultCoefficients(4, coefficients) !== 0) {
            if (JavaMath.abs(coefficients[0]!) < QuarticSolver.COEFFICIENT_LIMIT) {
                if (JavaMath.abs(coefficients[1]!) < QuarticSolver.COEFFICIENT_LIMIT) {
                    return QuadraticSolver.solve(coefficients, 2, roots);
                }
                return CubicSolver.solve(coefficients, 1, roots);
            }
            const polynomialSolver = new PolynomialSolver(4, coefficients);
            return polynomialSolver.solve(roots, minValue, epsilon);
        }

        const normalizedC0 = coefficients[0]!;
        if (JavaMath.abs(normalizedC0) < QuarticSolver.COEFFICIENT_LIMIT) {
            return CubicSolver.solve(coefficients, 1, roots);
        }
        if (JavaMath.abs(coefficients[4]!) < QuarticSolver.COEFFICIENT_LIMIT) {
            return CubicSolver.solve(coefficients, roots);
        }
        if (normalizedC0 !== 1.0) {
            normalizedC1 = coefficients[1]! / normalizedC0;
            normalizedC2 = coefficients[2]! / normalizedC0;
            normalizedC3 = coefficients[3]! / normalizedC0;
            normalizedC4 = coefficients[4]! / normalizedC0;
        } else {
            normalizedC1 = coefficients[1]!;
            normalizedC2 = coefficients[2]!;
            normalizedC3 = coefficients[3]!;
            normalizedC4 = coefficients[4]!;
        }

        let leadingCoefficient = 4.0 * normalizedC4;
        cubic[0] = 1.0;
        cubic[1] = -1.0 * normalizedC2;
        cubic[2] = normalizedC1 * normalizedC3 - leadingCoefficient;
        cubic[3] =
            leadingCoefficient * normalizedC2 -
            normalizedC1 * normalizedC1 * normalizedC4 -
            normalizedC3 * normalizedC3;
        let rootCount = CubicSolver.solve(cubic, cubicRoots);
        if (rootCount > 0) {
            normalizedY = cubicRoots[0]!;
        } else {
            return 0;
        }

        rootCount = 0;
        leadingCoefficient = normalizedC1 / 2.0;
        normalizedY = normalizedY / 2.0;

        firstThreshold = leadingCoefficient * leadingCoefficient - normalizedC2 + normalizedY * 2.0;
        if (firstThreshold < 0.0) {
            if (firstThreshold > QuarticSolver.FUDGE_FACTOR2) {
                firstThreshold = 0.0;
            } else {
                return 0;
            }
        }
        if (firstThreshold < QuarticSolver.FUDGE_FACTOR3) {
            secondThreshold = normalizedY * normalizedY - normalizedC4;
            if (secondThreshold < 0.0) {
                return 0;
            }
            auxiliaryRoot = 0.0;
            linearAdjustment = JavaMath.sqrt(secondThreshold);
        } else {
            auxiliaryRoot = JavaMath.sqrt(firstThreshold);
            linearAdjustment = (0.5 * (leadingCoefficient * (normalizedY * 2.0) - normalizedC3)) / auxiliaryRoot;
        }

        quadraticCoefficient = -leadingCoefficient - auxiliaryRoot;
        constantTerm = normalizedY + linearAdjustment;
        discriminant = quadraticCoefficient * quadraticCoefficient - 4.0 * constantTerm;
        if (discriminant >= 0.0) {
            discriminant = JavaMath.sqrt(discriminant);
            roots[0] = 0.5 * (quadraticCoefficient + discriminant);
            roots[1] = 0.5 * (quadraticCoefficient - discriminant);
            rootCount = 2;
        }

        quadraticCoefficient = quadraticCoefficient + auxiliaryRoot + auxiliaryRoot;
        constantTerm = normalizedY - linearAdjustment;
        discriminant = quadraticCoefficient * quadraticCoefficient - 4.0 * constantTerm;
        if (discriminant >= 0.0) {
            discriminant = JavaMath.sqrt(discriminant);
            roots[rootCount++] = 0.5 * (quadraticCoefficient + discriminant);
            roots[rootCount++] = 0.5 * (quadraticCoefficient - discriminant);
        }
        return rootCount;
    }
}
