#ifndef __QUADRATIC_SOLVER__
#define __QUADRATIC_SOLVER__

class QuadraticSolver {
  private:
    static const double COEFFICIENT_LIMIT;

  public:
    static int solve(const double *coefficients, double *roots);
};

#endif
