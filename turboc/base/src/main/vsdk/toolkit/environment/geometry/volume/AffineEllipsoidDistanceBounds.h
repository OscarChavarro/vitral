#ifndef __AFFN_ELLPSD_DSTNC_BNDS__
#define __AFFN_ELLPSD_DSTNC_BNDS__

#include "vsdk/toolkit/common/VSDK.h"

#include "vsdk/toolkit/environment/geometry/element/GeometryDistanceStatus.h"
#include "vsdk/toolkit/environment/geometry/element/GeometryContainment.h"
class Matrix4x4d;
class Vector3Dd;

// A certified enclosure for distance to the surface of an affine sphere.
// The lower bound uses a supporting plane or an interior inverse-norm bound;
// the upper bound uses a surface point in a nonzero local direction. Side may
// be Indeterminate. Outputs change only on Valid.
class AffineEllipsoidDistanceBounds {
public:
    static GeometryDistanceStatus::Value query(const Vector3Dd& point, double radius,
        const Matrix4x4d& forward, const Vector3Dd& localDirection,
        GeometryContainment::Value& side, double& lower, double& upper, BOOLEAN refineInterior=TRUE);
};

#endif
