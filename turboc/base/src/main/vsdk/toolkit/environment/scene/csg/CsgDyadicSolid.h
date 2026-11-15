#ifndef __CSG_DYADIC_SOLID__
#define __CSG_DYADIC_SOLID__

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/environment/scene/csg/CsgDyadic.h"
class Matrix4x4d;
// Exact sufficient certificates; failure never establishes non-equivalence.
class CsgDyadicSolid {
private:
    static BOOLEAN proportional(const CsgDyadic* a, const CsgDyadic* b, int count);
    static BOOLEAN planeWorld(const double* local, const Matrix4x4d& forward, CsgDyadic* result);
    static BOOLEAN quadricWorld(const CsgDyadic* local, const Matrix4x4d& forward, CsgDyadic* result);
    static BOOLEAN quadricLocal(const double* local, CsgDyadic* result);
public:
    static BOOLEAN samePlanes(const double* a, const Matrix4x4d& pa, const double* b, const Matrix4x4d& pb);
    static BOOLEAN sameQuadrics(const double* a, const Matrix4x4d& pa, const double* b, const Matrix4x4d& pb);
    static BOOLEAN sphereQuadric(double radius, const Matrix4x4d& ps, const double* q, const Matrix4x4d& pq);
    static BOOLEAN sameEllipsoids(double ra, const Matrix4x4d& pa, double rb, const Matrix4x4d& pb);
};
#endif
