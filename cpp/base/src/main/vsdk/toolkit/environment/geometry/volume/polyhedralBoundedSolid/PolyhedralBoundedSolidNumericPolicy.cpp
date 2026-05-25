#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.h"

#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"

#include <cmath>

PolyhedralBoundedSolidNumericPolicy::ToleranceContext::ToleranceContext()
    : modelScale_(1.0), epsilon_(1e-9), bigEpsilon_(1e-6), unitVectorTolerance_(1e-8),
      angleTolerance_(1e-8), coplanarDotTolerance_(1e-8), unitIntervalTolerance_(1e-8) {}

PolyhedralBoundedSolidNumericPolicy::ToleranceContext::ToleranceContext(double modelScale, double epsilon, double bigEpsilon,
    double unitVectorTolerance, double angleTolerance, double coplanarDotTolerance, double unitIntervalTolerance)
    : modelScale_(modelScale), epsilon_(epsilon), bigEpsilon_(bigEpsilon), unitVectorTolerance_(unitVectorTolerance),
      angleTolerance_(angleTolerance), coplanarDotTolerance_(coplanarDotTolerance), unitIntervalTolerance_(unitIntervalTolerance) {}

double PolyhedralBoundedSolidNumericPolicy::ToleranceContext::modelScale() const { return modelScale_; }
double PolyhedralBoundedSolidNumericPolicy::ToleranceContext::epsilon() const { return epsilon_; }
double PolyhedralBoundedSolidNumericPolicy::ToleranceContext::bigEpsilon() const { return bigEpsilon_; }
double PolyhedralBoundedSolidNumericPolicy::ToleranceContext::unitVectorTolerance() const { return unitVectorTolerance_; }
double PolyhedralBoundedSolidNumericPolicy::ToleranceContext::angleTolerance() const { return angleTolerance_; }
double PolyhedralBoundedSolidNumericPolicy::ToleranceContext::coplanarDotTolerance() const { return coplanarDotTolerance_; }
double PolyhedralBoundedSolidNumericPolicy::ToleranceContext::unitIntervalTolerance() const { return unitIntervalTolerance_; }

PolyhedralBoundedSolidNumericPolicy::ToleranceContext PolyhedralBoundedSolidNumericPolicy::defaultContext() { return ToleranceContext(); }
PolyhedralBoundedSolidNumericPolicy::ToleranceContext PolyhedralBoundedSolidNumericPolicy::fromScale(double modelScale)
{
    double s = std::abs(modelScale);
    if ( s < 1e-9 ) s = 1.0;
    return ToleranceContext(s, 1e-9*s, 1e-6*s, 1e-8, 1e-8, 1e-8, 1e-8);
}
PolyhedralBoundedSolidNumericPolicy::ToleranceContext PolyhedralBoundedSolidNumericPolicy::forSolid(PolyhedralBoundedSolid* solid)
{
    if ( solid == 0 ) return defaultContext();
    double* mm = solid->getMinMax();
    double dx = mm[3]-mm[0], dy = mm[4]-mm[1], dz = mm[5]-mm[2];
    delete[] mm;
    return fromScale(std::sqrt(dx*dx + dy*dy + dz*dz));
}
PolyhedralBoundedSolidNumericPolicy::ToleranceContext PolyhedralBoundedSolidNumericPolicy::forFace(_PolyhedralBoundedSolidFace*) { return defaultContext(); }

int PolyhedralBoundedSolidNumericPolicy::compare(double a, double b, double tolerance)
{
    if ( std::abs(a - b) <= tolerance ) return 0;
    return (a > b) ? 1 : -1;
}
int PolyhedralBoundedSolidNumericPolicy::compare(double a, double b, const ToleranceContext& context) { return compare(a, b, context.epsilon()); }
bool PolyhedralBoundedSolidNumericPolicy::pointsCoincident(const Vector3Dd& a, const Vector3Dd& b, const ToleranceContext& context) { return a.subtract(b).length() <= context.bigEpsilon(); }
bool PolyhedralBoundedSolidNumericPolicy::pointsSeparated(const Vector3Dd& a, const Vector3Dd& b, const ToleranceContext& context) { return !pointsCoincident(a,b,context); }
bool PolyhedralBoundedSolidNumericPolicy::isZero(double value, const ToleranceContext& context) { return std::abs(value) <= context.epsilon(); }
double PolyhedralBoundedSolidNumericPolicy::linearTolerance2D(const ToleranceContext& context) { return context.bigEpsilon(); }
