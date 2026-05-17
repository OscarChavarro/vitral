#ifndef __VSDK_PBS_NUMERIC_POLICY_H__
#define __VSDK_PBS_NUMERIC_POLICY_H__

#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"

class PolyhedralBoundedSolid;
class _PolyhedralBoundedSolidFace;

class PolyhedralBoundedSolidNumericPolicy {
public:
    class ToleranceContext {
    private:
        double modelScale_;
        double epsilon_;
        double bigEpsilon_;
        double unitVectorTolerance_;
        double angleTolerance_;
        double coplanarDotTolerance_;
        double unitIntervalTolerance_;
    public:
        ToleranceContext();
        ToleranceContext(double modelScale, double epsilon, double bigEpsilon,
            double unitVectorTolerance, double angleTolerance,
            double coplanarDotTolerance, double unitIntervalTolerance);

        double modelScale() const;
        double epsilon() const;
        double bigEpsilon() const;
        double unitVectorTolerance() const;
        double angleTolerance() const;
        double coplanarDotTolerance() const;
        double unitIntervalTolerance() const;
    };

    static ToleranceContext defaultContext();
    static ToleranceContext fromScale(double modelScale);
    static ToleranceContext forSolid(PolyhedralBoundedSolid* solid);
    static ToleranceContext forFace(_PolyhedralBoundedSolidFace* face);

    static int compare(double a, double b, double tolerance);
    static int compare(double a, double b, const ToleranceContext& context);
    static bool pointsCoincident(const Vector3Dd& a, const Vector3Dd& b, const ToleranceContext& context);
    static bool pointsSeparated(const Vector3Dd& a, const Vector3Dd& b, const ToleranceContext& context);
    static bool isZero(double value, const ToleranceContext& context);
    static double linearTolerance2D(const ToleranceContext& context);
};

#endif
