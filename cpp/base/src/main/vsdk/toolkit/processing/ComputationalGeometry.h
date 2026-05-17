#ifndef __VSDK_TOOLKIT_PROCESSING_COMPUTATIONALGEOMETRY_H__
#define __VSDK_TOOLKIT_PROCESSING_COMPUTATIONALGEOMETRY_H__

#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"

class Ray;

class ComputationalGeometry {
public:
    struct TriangleIntersection {
        double t;
        Vector3Dd point;
        Vector3Dd normal;
    };

    static TriangleIntersection* doIntersectionWithTriangle(const Ray& ray, const Vector3Dd& v0, const Vector3Dd& v1, const Vector3Dd& v2);
    static int triangleContainmentTest(const Vector3Dd& p0, const Vector3Dd& p1, const Vector3Dd& p2, const Vector3Dd& p, double distanceTolerance);
    static void triangleMinMax(const Vector3Dd& p0, const Vector3Dd& p1, const Vector3Dd& p2, double mm[6]);
};

#endif
