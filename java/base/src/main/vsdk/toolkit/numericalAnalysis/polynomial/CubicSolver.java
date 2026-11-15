package vsdk.toolkit.numericalAnalysis.polynomial;

/**
Java port of cpp `CubicSolver`. cpp passes `&coefficients[k]`; Java passes
the array and the offset of its first coefficient.
*/
public class CubicSolver {
    private static final double TWO_PI_3 = 2.0943951023931954923084;
    private static final double TWO_PI_43 = 4.1887902047863909846168;

    private static double absoluteValue(double x)
    {
        return (x < 0.0) ? (0.0 - x) : x;
    }

    public static int solve(double[] coefficients, double[] roots)
    {
        return solve(coefficients, 0, roots);
    }

    public static int solve(double[] coefficients, int offset, double[] roots)
    {
        double scaleFactor;
        double angle;
        double firstNormalizedCoefficient;
        double secondNormalizedCoefficient;
        double thirdNormalizedCoefficient;
        double leadingCoefficient = coefficients[offset];
        if (leadingCoefficient == 0.0) {
            return QuadraticSolver.solve(coefficients, offset + 1, roots);
        }
        if (leadingCoefficient != 1.0) {
            firstNormalizedCoefficient = coefficients[offset + 1] / leadingCoefficient;
            secondNormalizedCoefficient = coefficients[offset + 2] / leadingCoefficient;
            thirdNormalizedCoefficient = coefficients[offset + 3] / leadingCoefficient;
        } else {
            firstNormalizedCoefficient = coefficients[offset + 1];
            secondNormalizedCoefficient = coefficients[offset + 2];
            thirdNormalizedCoefficient = coefficients[offset + 3];
        }
        double firstCoefficientSquared = firstNormalizedCoefficient * firstNormalizedCoefficient;
        double quadraticTerm = (firstCoefficientSquared - 3.0 * secondNormalizedCoefficient) / 9.0;
        double cubicTerm = (2.0 * firstCoefficientSquared * firstNormalizedCoefficient -
            9.0 * firstNormalizedCoefficient * secondNormalizedCoefficient +
            27.0 * thirdNormalizedCoefficient) /
            54.0;
        double quadraticTermCubed = quadraticTerm * quadraticTerm * quadraticTerm;
        double cubicTermSquared = cubicTerm * cubicTerm;
        double discriminant = quadraticTermCubed - cubicTermSquared;
        double offsetValue = firstNormalizedCoefficient / 3.0;
        if (discriminant >= 0.0) {
            discriminant = cubicTerm / Math.sqrt(quadraticTermCubed);
            angle = Math.acos(discriminant) / 3.0;
            scaleFactor = -2.0 * Math.sqrt(quadraticTerm);
            roots[0] = scaleFactor * Math.cos(angle) - offsetValue;
            roots[1] = scaleFactor * Math.cos(angle + TWO_PI_3) - offsetValue;
            roots[2] = scaleFactor * Math.cos(angle + TWO_PI_43) - offsetValue;
            return 3;
        }
        scaleFactor = Math.pow(
            Math.sqrt(cubicTermSquared - quadraticTermCubed) +
                CubicSolver.absoluteValue(cubicTerm),
            1.0 / 3.0);
        if (cubicTerm < 0) {
            roots[0] = (scaleFactor + quadraticTerm / scaleFactor) - offsetValue;
        } else {
            roots[0] = -(scaleFactor + quadraticTerm / scaleFactor) - offsetValue;
        }
        return 1;
    }
}
