#ifndef __QUARTIC_SOLVER__
#define __QUARTIC_SOLVER__

class QuarticSolver {
  private:
    static const double COEFFICIENT_LIMIT;
    static const double FUDGE_FACTOR1;
    static const double FUDGE_FACTOR2;
    static const double FUDGE_FACTOR3;
    static int hasDifficultCoefficients(int n, const double *coefficients);

  public:
    static int solve(
        const double *coefficients, double *roots, double minValue, double epsilon);
};

#endif
