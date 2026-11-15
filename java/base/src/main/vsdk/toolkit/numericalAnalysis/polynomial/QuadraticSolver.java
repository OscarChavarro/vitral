package vsdk.toolkit.numericalAnalysis.polynomial;

/**
Java port of cpp `QuadraticSolver`. cpp passes `&coefficients[k]`; Java
passes the array and the offset of its first coefficient.
*/
public class QuadraticSolver {
    private static final double COEFFICIENT_LIMIT = 1.0e-20;

    public static int solve(double[] coefficients, double[] roots)
    {
        return solve(coefficients, 0, roots);
    }

    public static int solve(double[] coefficients, int offset, double[] roots)
    {
        double quadraticCoefficient = coefficients[offset];
        double linearCoefficient = -coefficients[offset + 1];
        double constantCoefficient = coefficients[offset + 2];
        if (quadraticCoefficient == 0.0) {
            if (linearCoefficient == 0.0) {
                return 0;
            }
            roots[0] = constantCoefficient / linearCoefficient;
            return 1;
        }
        double discriminant = linearCoefficient * linearCoefficient -
            4.0 * quadraticCoefficient * constantCoefficient;
        if (discriminant < 0.0) {
            return 0;
        }
        if (Math.abs(discriminant) < COEFFICIENT_LIMIT) {
            roots[0] = 0.5 * linearCoefficient / quadraticCoefficient;
            return 1;
        }
        discriminant = Math.sqrt(discriminant);
        double denominator = 2.0 * quadraticCoefficient;
        roots[0] = (linearCoefficient + discriminant) / denominator;
        roots[1] = (linearCoefficient - discriminant) / denominator;
        return 2;
    }
}
