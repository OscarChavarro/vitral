#ifndef __CUBIC_SOLVER__
#define __CUBIC_SOLVER__

class CubicSolver {
  private:
    static const double TWO_PI_3;
    static const double TWO_PI_43;
    static double absoluteValue(double x);

  public:
    static int solve(const double *coefficients, double *roots);
};

#endif
