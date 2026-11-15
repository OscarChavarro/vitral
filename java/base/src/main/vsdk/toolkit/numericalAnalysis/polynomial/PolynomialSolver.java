package vsdk.toolkit.numericalAnalysis.polynomial;

/**
Java port of cpp `PolynomialSolver` (Sturm sequences). cpp pointer
arithmetic over the Sturm sequence array and the roots array becomes array
indices and offsets.
*/
public class PolynomialSolver {
    public static final int MAX_ORDER = 7;

    private int order;
    private final double[] coefficients = new double[MAX_ORDER + 1];

    private static final double COEFFICIENT_LIMIT = 1.0e-20;
    private static final int MAX_ITERATIONS = 50;
    private static final double POLYNOMIAL_MAX_DISTANCE = 1.0e7;

    public PolynomialSolver()
    {
        order = 0;
    }

    public PolynomialSolver(int order, double[] coefficients)
    {
        this(order, coefficients, 0);
    }

    /** cpp `PolynomialSolver(order, &coefficients[offset])`. */
    public PolynomialSolver(int order, double[] coefficients, int offset)
    {
        this.order = order;
        for (int coefficientIndex = 0; coefficientIndex <= MAX_ORDER; coefficientIndex++) {
            this.coefficients[coefficientIndex] = 0.0;
        }
        for (int coefficientIndex = 0; coefficientIndex <= order; coefficientIndex++) {
            this.coefficients[order - coefficientIndex] = coefficients[offset + coefficientIndex];
        }
    }

    /** cpp copy assignment. */
    private void assign(PolynomialSolver other)
    {
        order = other.order;
        System.arraycopy(other.coefficients, 0, coefficients, 0, MAX_ORDER + 1);
    }

    /**
    Calculates the modulus of u(x) / v(x) leaving it in r, it
    returns 0 if r(x) is a constant.
    note: this function assumes the leading coefficient of v
    is 1 or -1
    */
    private int polynomialRemainder(PolynomialSolver divisor, PolynomialSolver remainder)
    {
        int quotientIndex;
        int coefficientIndex;
        for (int dividendIndex = 0; dividendIndex <= order; dividendIndex++) {
            remainder.coefficients[dividendIndex] = coefficients[dividendIndex];
        }
        for (int dividendIndex = order + 1; dividendIndex <= MAX_ORDER; dividendIndex++) {
            remainder.coefficients[dividendIndex] = 0.0;
        }

        if (divisor.coefficients[divisor.order] < 0.0) {
            for (quotientIndex = order - divisor.order - 1; quotientIndex >= 0;
                quotientIndex -= 2) {
                remainder.coefficients[quotientIndex] = -remainder.coefficients[quotientIndex];
            }
            for (quotientIndex = order - divisor.order; quotientIndex >= 0;
                quotientIndex--) {
                for (coefficientIndex = divisor.order + quotientIndex - 1;
                    coefficientIndex >= quotientIndex;
                    coefficientIndex--) {
                    remainder.coefficients[coefficientIndex] =
                        -remainder.coefficients[coefficientIndex] -
                        remainder.coefficients[divisor.order + quotientIndex] *
                            divisor.coefficients[coefficientIndex - quotientIndex];
                }
            }
        } else {
            for (quotientIndex = order - divisor.order; quotientIndex >= 0;
                quotientIndex--) {
                for (coefficientIndex = divisor.order + quotientIndex - 1;
                    coefficientIndex >= quotientIndex;
                    coefficientIndex--) {
                    remainder.coefficients[coefficientIndex] -=
                        remainder.coefficients[divisor.order + quotientIndex] *
                        divisor.coefficients[coefficientIndex - quotientIndex];
                }
            }
        }

        quotientIndex = divisor.order - 1;
        while (quotientIndex >= 0 &&
            Math.abs(remainder.coefficients[quotientIndex]) <
                   COEFFICIENT_LIMIT) {
            remainder.coefficients[quotientIndex] = 0.0;
            quotientIndex--;
        }
        remainder.order = (quotientIndex < 0) ? 0 : quotientIndex;
        return remainder.order;
    }

    /**
    Build the Sturmian sequence for a Polynomial
    */
    private int buildSturmSequence(PolynomialSolver[] sturmSequence)
    {
        int sequenceTerm;

        sturmSequence[0].assign(this);
        sturmSequence[1].order = order - 1;

        // Calculate the derivative and normalize the leading coefficient
        double normalizationFactor =
            Math.abs(sturmSequence[0].coefficients[order] * order);
        int normalizedCoefficient = 0;
        int sourceCoefficient = 1;
        for (int coefficientIndex = 1; coefficientIndex <= order; coefficientIndex++) {
            sturmSequence[1].coefficients[normalizedCoefficient++] =
                sturmSequence[0].coefficients[sourceCoefficient++] * coefficientIndex /
                normalizationFactor;
        }

        // Construct the rest of the Sturm sequence
        for (sequenceTerm = 2;
            sturmSequence[sequenceTerm - 2].polynomialRemainder(sturmSequence[sequenceTerm - 1], sturmSequence[sequenceTerm]) != 0;
            sequenceTerm++) {
            // Reverse the sign and normalize
            PolynomialSolver term = sturmSequence[sequenceTerm];
            normalizationFactor = -Math.abs(term.coefficients[term.order]);
            for (normalizedCoefficient = term.order;
                normalizedCoefficient >= 0;
                normalizedCoefficient--) {
                term.coefficients[normalizedCoefficient] /= normalizationFactor;
            }
        }
        sturmSequence[sequenceTerm].coefficients[0] = -sturmSequence[sequenceTerm].coefficients[0];
        return sequenceTerm;
    }

    /**
    Find out how many visible intersections there are
    @param atZero one-element output holder
    @param atPositive one-element output holder
    */
    private int countVisibleRoots(
        int sequenceLength, PolynomialSolver[] sturmSequence, int[] atZero,
        int[] atPositive)
    {
        double currentValue;
        double lastValue;
        int changesAtPositiveInfinity = 0;
        int changesAtZero = 0;

        lastValue = sturmSequence[0].coefficients[sturmSequence[0].order];
        for (int sequenceTerm = 1; sequenceTerm <= sequenceLength; sequenceTerm++) {
            currentValue = sturmSequence[sequenceTerm].coefficients[sturmSequence[sequenceTerm].order];
            if (lastValue == 0.0 || lastValue * currentValue < 0) {
                changesAtPositiveInfinity++;
            }
            lastValue = currentValue;
        }

        lastValue = sturmSequence[0].coefficients[0];
        for (int sequenceTerm = 1; sequenceTerm <= sequenceLength; sequenceTerm++) {
            currentValue = sturmSequence[sequenceTerm].coefficients[0];
            if (lastValue == 0.0 || lastValue * currentValue < 0) {
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
    private int countSignChanges(
        int sequenceLength, PolynomialSolver[] sturmSequence, double value)
    {
        double currentValue;
        int signChanges = 0;
        double lastValue = sturmSequence[0].evaluatePolynomial(value);
        for (int sequenceTerm = 1;
            sequenceTerm <= sequenceLength;
            sequenceTerm++) {
            currentValue = sturmSequence[sequenceTerm].evaluatePolynomial(value);
            if (lastValue == 0.0 || lastValue * currentValue < 0) {
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
    private void bisectRoots(
        int sequenceLength, PolynomialSolver[] sturmSequence,
        double minimumValue, double maximumValue, int changesAtMinimum,
        int changesAtMaximum, double epsilon, double[] roots, int rootOffset)
    {
        // cpp leaves midpoint uninitialized; every path assigns it first.
        double midpoint = 0;
        int rootsInLeftHalf;
        int rootsInRightHalf;
        int iteration;
        int changesAtMidpoint;
        int rootCountInInterval = changesAtMinimum - changesAtMaximum;

        if (rootCountInInterval == 1) {
            double[] root = new double[1];
            if (sturmSequence[0].solveByRegulaFalsi(minimumValue, maximumValue, epsilon, root) != 0) {
                roots[rootOffset] = root[0];
                return;
            }

            for (iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
                midpoint = (minimumValue + maximumValue) / 2;
                changesAtMidpoint =
                    countSignChanges(sequenceLength, sturmSequence, midpoint);
                if (Math.abs(midpoint) > epsilon) {
                    if (Math.abs((maximumValue - minimumValue) / midpoint) < epsilon) {
                        roots[rootOffset] = midpoint;
                        return;
                    }
                } else if (Math.abs(maximumValue - minimumValue) < epsilon) {
                    roots[rootOffset] = midpoint;
                    return;
                } else if ((changesAtMinimum - changesAtMidpoint) == 0) {
                    minimumValue = midpoint;
                } else {
                    maximumValue = midpoint;
                }
            }

            roots[rootOffset] = midpoint;
            return;
        }

        for (iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            midpoint = (minimumValue + maximumValue) / 2;
            changesAtMidpoint =
                countSignChanges(sequenceLength, sturmSequence, midpoint);
            rootsInLeftHalf = changesAtMinimum - changesAtMidpoint;
            rootsInRightHalf = changesAtMidpoint - changesAtMaximum;
            if (rootsInLeftHalf != 0 && rootsInRightHalf != 0) {
                bisectRoots(sequenceLength, sturmSequence, minimumValue, midpoint,
                    changesAtMinimum, changesAtMidpoint, epsilon, roots, rootOffset);
                bisectRoots(sequenceLength, sturmSequence, midpoint, maximumValue,
                    changesAtMidpoint, changesAtMaximum, epsilon,
                    roots, rootOffset + rootsInLeftHalf);
                return;
            }
            if (rootsInLeftHalf == 0) {
                minimumValue = midpoint;
            } else {
                maximumValue = midpoint;
            }
        }

        for (rootsInLeftHalf = changesAtMaximum; rootsInLeftHalf < changesAtMinimum;
            rootsInLeftHalf++) {
            roots[rootOffset + rootsInLeftHalf - changesAtMaximum] = midpoint;
        }
    }

    private double evaluatePolynomial(double x)
    {
        double value = coefficients[order];
        for (int coefficientIndex = order - 1; coefficientIndex >= 0; coefficientIndex--) {
            value = value * x + coefficients[coefficientIndex];
        }
        return value;
    }

    /**
    Close in on a root by using regula-falsa
    @param root one-element output holder
    */
    private int solveByRegulaFalsi(double a, double b, double epsilon, double[] root)
    {
        double estimatedRoot;
        double valueAtRoot;
        double valueAtA = evaluatePolynomial(a);
        double valueAtB = evaluatePolynomial(b);

        if (valueAtA * valueAtB > 0.0) {
            return 0;
        }

        if (Math.abs(valueAtA) < COEFFICIENT_LIMIT) {
            root[0] = a;
            return 1;
        }

        if (Math.abs(valueAtB) < COEFFICIENT_LIMIT) {
            root[0] = b;
            return 1;
        }

        double lastValueAtRoot = valueAtA;
        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            estimatedRoot = (valueAtB * a - valueAtA * b) / (valueAtB - valueAtA);
            valueAtRoot = evaluatePolynomial(estimatedRoot);

            if (Math.abs(estimatedRoot) > epsilon) {
                if (Math.abs(valueAtRoot / estimatedRoot) < epsilon) {
                    root[0] = estimatedRoot;
                    return 1;
                }
            } else if (Math.abs(valueAtRoot) < epsilon) {
                root[0] = estimatedRoot;
                return 1;
            }

            if (valueAtA < 0) {
                if (valueAtRoot < 0) {
                    a = estimatedRoot;
                    valueAtA = valueAtRoot;
                    if ((lastValueAtRoot * valueAtRoot) > 0) {
                        valueAtB /= 2;
                    }
                } else {
                    b = estimatedRoot;
                    valueAtB = valueAtRoot;
                    if ((lastValueAtRoot * valueAtRoot) > 0) {
                        valueAtA /= 2;
                    }
                }
            } else if (valueAtRoot < 0) {
                b = estimatedRoot;
                valueAtB = valueAtRoot;
                if ((lastValueAtRoot * valueAtRoot) > 0) {
                    valueAtA /= 2;
                }
            } else {
                a = estimatedRoot;
                valueAtA = valueAtRoot;
                if ((lastValueAtRoot * valueAtRoot) > 0) {
                    valueAtB /= 2;
                }
            }
            if (Math.abs(b - a) < epsilon) {
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
    public int solve(double[] roots, double minValue, double epsilon)
    {
        PolynomialSolver[] sturmSequence = new PolynomialSolver[MAX_ORDER + 1];
        for (int i = 0; i < sturmSequence.length; i++) sturmSequence[i] = new PolynomialSolver();
        int rootCount;
        int[] changesAtMinimum = {0};
        int[] changesAtMaximum = {0};

        int sequenceLength = buildSturmSequence(sturmSequence);

        rootCount = countVisibleRoots(
            sequenceLength, sturmSequence, changesAtMinimum, changesAtMaximum);
        if (rootCount == 0) {
            return 0;
        }

        double maximumValue = POLYNOMIAL_MAX_DISTANCE;

        changesAtMinimum[0] = countSignChanges(sequenceLength, sturmSequence, minValue);
        changesAtMaximum[0] = countSignChanges(sequenceLength, sturmSequence, maximumValue);
        rootCount = changesAtMinimum[0] - changesAtMaximum[0];
        if (rootCount == 0) {
            return 0;
        }

        bisectRoots(sequenceLength, sturmSequence, minValue, maximumValue, changesAtMinimum[0],
            changesAtMaximum[0], epsilon, roots, 0);

        return rootCount;
    }
}
