#ifndef __CSG_EXACT_QUADRIC__
#define __CSG_EXACT_QUADRIC__

#include "vsdk/toolkit/common/VSDK.h"
class Matrix4x4d;
// Coefficient order: xx, yy, zz, xy, xz, yz, x, y, z, constant.
class CsgExactQuadric {
public:
    static BOOLEAN world(const double* local, const Matrix4x4d& forward, double* result);
};
#endif
