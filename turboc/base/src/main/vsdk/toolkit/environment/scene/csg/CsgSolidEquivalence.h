#ifndef __CSG_SOLID_EQUIVALENCE__
#define __CSG_SOLID_EQUIVALENCE__

#include "vsdk/toolkit/common/VSDK.h"
class SceneSolid;
class Matrix4x4d;
class Vector3Dd;
class Box;
class Sphere;
class PlanarHalfSpace;
class QuadricSurface;
// Conservative, exact sufficient conditions for equality of toolkit solids.
class CsgSolidEquivalence {
private:
    static BOOLEAN sameEllipsoidLinear(const Matrix4x4d& a, double radiusA,
        const Matrix4x4d& b, double radiusB);
    static BOOLEAN sameTranslation(const Matrix4x4d& a, const Matrix4x4d& b);
    static BOOLEAN signedPermutation(const Matrix4x4d& a, const Matrix4x4d& b,
        int* axes, int* signs);
    static BOOLEAN proportional(const double* a, const double* b, int count);
    static BOOLEAN sameBox(const Box& a, const Matrix4x4d& pa, const Box& b, const Matrix4x4d& pb);
    static BOOLEAN axisPlane(const double* coefficients, const Matrix4x4d& placement,
        int& axis, double& scale, double& offset, double& root, BOOLEAN& positive);
    static BOOLEAN sameOrientedPlane(const double* a, const Matrix4x4d& pa,
        const double* b, const Matrix4x4d& pb);
    static BOOLEAN sameHalfSpace(const PlanarHalfSpace& a, const Matrix4x4d& pa,
        const PlanarHalfSpace& b, const Matrix4x4d& pb);
    static BOOLEAN sphereQuadric(const Sphere& sphere, const Matrix4x4d& ps,
        const QuadricSurface& quadric, const Matrix4x4d& pq);
    static BOOLEAN halfSpaceQuadric(const PlanarHalfSpace& plane, const Matrix4x4d& pp,
        const QuadricSurface& quadric, const Matrix4x4d& pq);
    static BOOLEAN worldQuadric(const QuadricSurface& quadric, const Matrix4x4d& placement, double* result);
    static BOOLEAN sameQuadric(const QuadricSurface& a, const Matrix4x4d& pa,
        const QuadricSurface& b, const Matrix4x4d& pb);
public:
    static BOOLEAN same(const SceneSolid& a, const SceneSolid& b);
};
#endif
