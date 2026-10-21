#ifndef __POLYNOMIAL_SOLVER__
#define __POLYNOMIAL_SOLVER__

class PolynomialSolver {
  public:
    static const int MAX_ORDER = 7;

    PolynomialSolver();
    PolynomialSolver(int order, const double *coefficients);

    int solve(double *roots, double minValue, double epsilon) const;

  private:
    int order;
    double coefficients[MAX_ORDER + 1];

    static const double COEFFICIENT_LIMIT;
    static const int MAX_ITERATIONS = 50;
    static const double POLYNOMIAL_MAX_DISTANCE;

    int polynomialRemainder(
        const PolynomialSolver &divisor, PolynomialSolver *remainder) const;
    int solveByRegulaFalsi(
        double a, double b, double epsilon, double *root) const;
    void bisectRoots(int sequenceLength, const PolynomialSolver *sturmSequence,
        double minimumValue, double maximumValue, int changesAtMinimum,
        int changesAtMaximum, double epsilon, double *roots) const;
    int countSignChanges(
        int sequenceLength, const PolynomialSolver *sturmSequence, double value) const;
    double evaluatePolynomial(double x) const;
    int buildSturmSequence(PolynomialSolver *sturmSequence) const;
    int countVisibleRoots(int sequenceLength, const PolynomialSolver *sturmSequence,
        int *atNegative, int *atPositive) const;
};

#endif
