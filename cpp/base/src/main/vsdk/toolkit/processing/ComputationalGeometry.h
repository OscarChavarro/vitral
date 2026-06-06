#ifndef __VSDK_TOOLKIT_PROCESSING_COMPUTATIONALGEOMETRY_H__
#define __VSDK_TOOLKIT_PROCESSING_COMPUTATIONALGEOMETRY_H__

#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"

class ComputationalGeometry {
public:
    static double lineToPointDistance(
        const Vector3Dd& p0,
        const Vector3Dd& p1,
        const Vector3Dd& p);
    static int lineContainmentTest(
        const Vector3Dd& p0,
        const Vector3Dd& p1,
        const Vector3Dd& p,
        double distanceTolerance);
    static int lineSegmentContainmentTest(
        const Vector3Dd& p0,
        const Vector3Dd& p1,
        const Vector3Dd& p,
        double distanceTolerance);
};

#endif
