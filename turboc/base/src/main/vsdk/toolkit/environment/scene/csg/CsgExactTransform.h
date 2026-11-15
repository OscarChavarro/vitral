#ifndef __CSG_EXACT_TRANSFORM__
#define __CSG_EXACT_TRANSFORM__

#include "vsdk/toolkit/common/VSDK.h"
class Matrix4x4d;
// Homogeneous inverse numerator det(A) * [A^-1, -A^-1 t; 0, 1].
class CsgExactTransform {
public:
    static BOOLEAN exactSum(const double* a, const double* b, int count, double& result);
    static BOOLEAN inverseNumerator(const Matrix4x4d& forward, double result[4][4]);
};
#endif
