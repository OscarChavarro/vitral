import { Math as JavaMath } from "../../../../java/lang/Math.js";

/**
Port of cpp `PolynomialSolver` (Sturm sequences). cpp pointer arithmetic
over the Sturm sequence array and the roots array becomes array indices and
offsets.
*/
export class PolynomialSolver {
    public static readonly MAX_ORDER = 7;

    private order: number;
    private readonly coefficients: number[] = new Array<number>(PolynomialSolver.MAX_ORDER + 1).fill(0.0);

    private static readonly COEFFICIENT_LIMIT = 1.0e-20;
    private static readonly MAX_ITERATIONS = 50;
    private static readonly POLYNOMIAL_MAX_DISTANCE = 1.0e7;

    public constructor();
    public constructor(order: number, coefficients: ArrayLike<number>);
    /** cpp `PolynomialSolver(order, &coefficients[offset])`. */
    public constructor(order: number, coefficients: ArrayLike<number>, offset: number);
    public constructor(order?: number, coefficients?: ArrayLike<number>, offset: number = 0) {
        if (order === undefined || coefficients === undefined) {
            this.order = 0;
            return;
        }
        this.order = order;
        for (let coefficientIndex = 0; coefficientIndex <= PolynomialSolver.MAX_ORDER; coefficientIndex++) {
            this.coefficients[coefficientIndex] = 0.0;
        }
        for (let coefficientIndex = 0; coefficientIndex <= order; coefficientIndex++) {
            this.coefficients[order - coefficientIndex] = coefficients[offset + coefficientIndex]!;
        }
    }

    /** cpp copy assignment. */
    private assign(other: PolynomialSolver): void {
        this.order = other.order;
        for (let i = 0; i <= PolynomialSolver.MAX_ORDER; i++) {
            this.coefficients[i] = other.coefficients[i]!;
        }
    }

    /**
    Calculates the modulus of u(x) / v(x) leaving it in r, it
    returns 0 if r(x) is a constant.
    note: this function assumes the leading coefficient of v
    is 1 or -1
    */
    private polynomialRemainder(divisor: PolynomialSolver, remainder: PolynomialSolver): number {
        let quotientIndex: number;
        let coefficientIndex: number;
        for (let dividendIndex = 0; dividendIndex <= this.order; dividendIndex++) {
            remainder.coefficients[dividendIndex] = this.coefficients[dividendIndex]!;
        }
        for (let dividendIndex = this.order + 1; dividendIndex <= PolynomialSolver.MAX_ORDER; dividendIndex++) {
            remainder.coefficients[dividendIndex] = 0.0;
        }

        if (divisor.coefficients[divisor.order]! < 0.0) {
            for (quotientIndex = this.order - divisor.order - 1; quotientIndex >= 0; quotientIndex -= 2) {
                remainder.coefficients[quotientIndex] = -remainder.coefficients[quotientIndex]!;
            }
            for (quotientIndex = this.order - divisor.order; quotientIndex >= 0; quotientIndex--) {
                for (
                    coefficientIndex = divisor.order + quotientIndex - 1;
                    coefficientIndex >= quotientIndex;
                    coefficientIndex--
                ) {
                    remainder.coefficients[coefficientIndex] =
                        -remainder.coefficients[coefficientIndex]! -
                        remainder.coefficients[divisor.order + quotientIndex]! *
                            divisor.coefficients[coefficientIndex - quotientIndex]!;
                }
            }
        } else {
            for (quotientIndex = this.order - divisor.order; quotientIndex >= 0; quotientIndex--) {
                for (
                    coefficientIndex = divisor.order + quotientIndex - 1;
                    coefficientIndex >= quotientIndex;
                    coefficientIndex--
                ) {
                    remainder.coefficients[coefficientIndex]! -=
                        remainder.coefficients[divisor.order + quotientIndex]! *
                        divisor.coefficients[coefficientIndex - quotientIndex]!;
                }
            }
        }

        quotientIndex = divisor.order - 1;
        while (
            quotientIndex >= 0 &&
            JavaMath.abs(remainder.coefficients[quotientIndex]!) < PolynomialSolver.COEFFICIENT_LIMIT
        ) {
            remainder.coefficients[quotientIndex] = 0.0;
            quotientIndex--;
        }
        remainder.order = quotientIndex < 0 ? 0 : quotientIndex;
        return remainder.order;
    }

    /**
    Build the Sturmian sequence for a Polynomial
    */
    private buildSturmSequence(sturmSequence: PolynomialSolver[]): number {
        let sequenceTerm: number;

        sturmSequence[0]!.assign(this);
        sturmSequence[1]!.order = this.order - 1;

        // Calculate the derivative and normalize the leading coefficient
        let normalizationFactor = JavaMath.abs(sturmSequence[0]!.coefficients[this.order]! * this.order);
        let normalizedCoefficient = 0;
        let sourceCoefficient = 1;
        for (let coefficientIndex = 1; coefficientIndex <= this.order; coefficientIndex++) {
            sturmSequence[1]!.coefficients[normalizedCoefficient++] =
                (sturmSequence[0]!.coefficients[sourceCoefficient++]! * coefficientIndex) / normalizationFactor;
        }

        // Construct the rest of the Sturm sequence
        for (
            sequenceTerm = 2;
            sturmSequence[sequenceTerm - 2]!.polynomialRemainder(
                sturmSequence[sequenceTerm - 1]!,
                sturmSequence[sequenceTerm]!,
            ) !== 0;
            sequenceTerm++
        ) {
            // Reverse the sign and normalize
            const term = sturmSequence[sequenceTerm]!;
            normalizationFactor = -JavaMath.abs(term.coefficients[term.order]!);
            for (normalizedCoefficient = term.order; normalizedCoefficient >= 0; normalizedCoefficient--) {
                term.coefficients[normalizedCoefficient]! /= normalizationFactor;
            }
        }
        sturmSequence[sequenceTerm]!.coefficients[0] = -sturmSequence[sequenceTerm]!.coefficients[0]!;
        return sequenceTerm;
    }

    /**
    Find out how many visible intersections there are
    @param atZero one-element output holder
    @param atPositive one-element output holder
    */
    private countVisibleRoots(
        sequenceLength: number,
        sturmSequence: PolynomialSolver[],
        atZero: number[],
        atPositive: number[],
    ): number {
        let currentValue: number;
        let lastValue: number;
        let changesAtPositiveInfinity = 0;
        let changesAtZero = 0;

        lastValue = sturmSequence[0]!.coefficients[sturmSequence[0]!.order]!;
        for (let sequenceTerm = 1; sequenceTerm <= sequenceLength; sequenceTerm++) {
            currentValue = sturmSequence[sequenceTerm]!.coefficients[sturmSequence[sequenceTerm]!.order]!;
            if (lastValue === 0.0 || lastValue * currentValue < 0) {
                changesAtPositiveInfinity++;
            }
            lastValue = currentValue;
        }

        lastValue = sturmSequence[0]!.coefficients[0]!;
        for (let sequenceTerm = 1; sequenceTerm <= sequenceLength; sequenceTerm++) {
            currentValue = sturmSequence[sequenceTerm]!.coefficients[0]!;
            if (lastValue === 0.0 || lastValue * currentValue < 0) {
                changesAtZero++;
            }
            lastValue = currentValue;
        }

        atZero[0] = changesAtZero;
        atPositive[0] = changesAtPositiveInfinity;
        return changesAtZero - changesAtPositiveInfinity;
    }

    /**
    Return the number of sign changes in the Sturm sequence in
    sseq at the value a.
    */
    private countSignChanges(sequenceLength: number, sturmSequence: PolynomialSolver[], value: number): number {
        let currentValue: number;
        let signChanges = 0;
        let lastValue = sturmSequence[0]!.evaluatePolynomial(value);
        for (let sequenceTerm = 1; sequenceTerm <= sequenceLength; sequenceTerm++) {
            currentValue = sturmSequence[sequenceTerm]!.evaluatePolynomial(value);
            if (lastValue === 0.0 || lastValue * currentValue < 0) {
                signChanges++;
            }
            lastValue = currentValue;
        }
        return signChanges;
    }

    /**
    Uses a bisection based on the sturm sequence for the Polynomial
    described in sseq to isolate intervals in which roots occur,
    the roots are returned in the roots array in order of magnitude.

    Note: This routine has one severe bug: When the interval containing the
    root [min, max] has a root at one of its endpoints, as well as one
    within the interval, the root at the endpoint will be returned rather
    than the one inside.
    */
    private bisectRoots(
        sequenceLength: number,
        sturmSequence: PolynomialSolver[],
        minimumValue: number,
        maximumValue: number,
        changesAtMinimum: number,
        changesAtMaximum: number,
        epsilon: number,
        roots: number[],
        rootOffset: number,
    ): void {
        // cpp leaves midpoint uninitialized; every path assigns it first.
        let midpoint = 0;
        let rootsInLeftHalf: number;
        let rootsInRightHalf: number;
        let iteration: number;
        let changesAtMidpoint: number;
        const rootCountInInterval = changesAtMinimum - changesAtMaximum;

        if (rootCountInInterval === 1) {
            const root: number[] = [0];
            if (sturmSequence[0]!.solveByRegulaFalsi(minimumValue, maximumValue, epsilon, root) !== 0) {
                roots[rootOffset] = root[0]!;
                return;
            }

            for (iteration = 0; iteration < PolynomialSolver.MAX_ITERATIONS; iteration++) {
                midpoint = (minimumValue + maximumValue) / 2;
                changesAtMidpoint = this.countSignChanges(sequenceLength, sturmSequence, midpoint);
                if (JavaMath.abs(midpoint) > epsilon) {
                    if (JavaMath.abs((maximumValue - minimumValue) / midpoint) < epsilon) {
                        roots[rootOffset] = midpoint;
                        return;
                    }
                } else if (JavaMath.abs(maximumValue - minimumValue) < epsilon) {
                    roots[rootOffset] = midpoint;
                    return;
                } else if (changesAtMinimum - changesAtMidpoint === 0) {
                    minimumValue = midpoint;
                } else {
                    maximumValue = midpoint;
                }
            }

            roots[rootOffset] = midpoint;
            return;
        }

        for (iteration = 0; iteration < PolynomialSolver.MAX_ITERATIONS; iteration++) {
            midpoint = (minimumValue + maximumValue) / 2;
            changesAtMidpoint = this.countSignChanges(sequenceLength, sturmSequence, midpoint);
            rootsInLeftHalf = changesAtMinimum - changesAtMidpoint;
            rootsInRightHalf = changesAtMidpoint - changesAtMaximum;
            if (rootsInLeftHalf !== 0 && rootsInRightHalf !== 0) {
                this.bisectRoots(
                    sequenceLength,
                    sturmSequence,
                    minimumValue,
                    midpoint,
                    changesAtMinimum,
                    changesAtMidpoint,
                    epsilon,
                    roots,
                    rootOffset,
                );
                this.bisectRoots(
                    sequenceLength,
                    sturmSequence,
                    midpoint,
                    maximumValue,
                    changesAtMidpoint,
                    changesAtMaximum,
                    epsilon,
                    roots,
                    rootOffset + rootsInLeftHalf,
                );
                return;
            }
            if (rootsInLeftHalf === 0) {
                minimumValue = midpoint;
            } else {
                maximumValue = midpoint;
            }
        }

        for (rootsInLeftHalf = changesAtMaximum; rootsInLeftHalf < changesAtMinimum; rootsInLeftHalf++) {
            roots[rootOffset + rootsInLeftHalf - changesAtMaximum] = midpoint;
        }
    }

    private evaluatePolynomial(x: number): number {
        let value = this.coefficients[this.order]!;
        for (let coefficientIndex = this.order - 1; coefficientIndex >= 0; coefficientIndex--) {
            value = value * x + this.coefficients[coefficientIndex]!;
        }
        return value;
    }

    /**
    Close in on a root by using regula-falsa
    @param root one-element output holder
    */
    private solveByRegulaFalsi(a: number, b: number, epsilon: number, root: number[]): number {
        let estimatedRoot: number;
        let valueAtRoot: number;
        let valueAtA = this.evaluatePolynomial(a);
        let valueAtB = this.evaluatePolynomial(b);

        if (valueAtA * valueAtB > 0.0) {
            return 0;
        }

        if (JavaMath.abs(valueAtA) < PolynomialSolver.COEFFICIENT_LIMIT) {
            root[0] = a;
            return 1;
        }

        if (JavaMath.abs(valueAtB) < PolynomialSolver.COEFFICIENT_LIMIT) {
            root[0] = b;
            return 1;
        }

        let lastValueAtRoot = valueAtA;
        for (let iteration = 0; iteration < PolynomialSolver.MAX_ITERATIONS; iteration++) {
            estimatedRoot = (valueAtB * a - valueAtA * b) / (valueAtB - valueAtA);
            valueAtRoot = this.evaluatePolynomial(estimatedRoot);

            if (JavaMath.abs(estimatedRoot) > epsilon) {
                if (JavaMath.abs(valueAtRoot / estimatedRoot) < epsilon) {
                    root[0] = estimatedRoot;
                    return 1;
                }
            } else if (JavaMath.abs(valueAtRoot) < epsilon) {
                root[0] = estimatedRoot;
                return 1;
            }

            if (valueAtA < 0) {
                if (valueAtRoot < 0) {
                    a = estimatedRoot;
                    valueAtA = valueAtRoot;
                    if (lastValueAtRoot * valueAtRoot > 0) {
                        valueAtB /= 2;
                    }
                } else {
                    b = estimatedRoot;
                    valueAtB = valueAtRoot;
                    if (lastValueAtRoot * valueAtRoot > 0) {
                        valueAtA /= 2;
                    }
                }
            } else if (valueAtRoot < 0) {
                b = estimatedRoot;
                valueAtB = valueAtRoot;
                if (lastValueAtRoot * valueAtRoot > 0) {
                    valueAtA /= 2;
                }
            } else {
                a = estimatedRoot;
                valueAtA = valueAtRoot;
                if (lastValueAtRoot * valueAtRoot > 0) {
                    valueAtB /= 2;
                }
            }
            if (JavaMath.abs(b - a) < epsilon) {
                root[0] = estimatedRoot;
                return 1;
            }
            lastValueAtRoot = valueAtRoot;
        }
        return 0;
    }

    /**
    Root solver based on the Sturm sequences for a Polynomial
    */
    public solve(roots: number[], minValue: number, epsilon: number): number {
        const sturmSequence: PolynomialSolver[] = [];
        for (let i = 0; i < PolynomialSolver.MAX_ORDER + 1; i++) {
            sturmSequence.push(new PolynomialSolver());
        }
        let rootCount: number;
        const changesAtMinimum: number[] = [0];
        const changesAtMaximum: number[] = [0];

        const sequenceLength = this.buildSturmSequence(sturmSequence);

        rootCount = this.countVisibleRoots(sequenceLength, sturmSequence, changesAtMinimum, changesAtMaximum);
        if (rootCount === 0) {
            return 0;
        }

        const maximumValue = PolynomialSolver.POLYNOMIAL_MAX_DISTANCE;

        changesAtMinimum[0] = this.countSignChanges(sequenceLength, sturmSequence, minValue);
        changesAtMaximum[0] = this.countSignChanges(sequenceLength, sturmSequence, maximumValue);
        rootCount = changesAtMinimum[0]! - changesAtMaximum[0]!;
        if (rootCount === 0) {
            return 0;
        }

        this.bisectRoots(
            sequenceLength,
            sturmSequence,
            minValue,
            maximumValue,
            changesAtMinimum[0]!,
            changesAtMaximum[0]!,
            epsilon,
            roots,
            0,
        );

        return rootCount;
    }
}
