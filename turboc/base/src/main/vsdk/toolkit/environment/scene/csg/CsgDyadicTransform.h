#ifndef __CSG_DYADIC_TRANSFORM__
#define __CSG_DYADIC_TRANSFORM__

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/environment/scene/csg/CsgDyadic.h"
class Matrix4x4d;
// Exact homogeneous inverse numerator; no intermediate binary64 rounding.
class CsgDyadicTransform {
public:
    static BOOLEAN dot(const CsgDyadic* a, const CsgDyadic* b, int count, CsgDyadic& result);
    static BOOLEAN inverseNumerator(const Matrix4x4d& forward, CsgDyadic result[4][4]);
};
#endif
