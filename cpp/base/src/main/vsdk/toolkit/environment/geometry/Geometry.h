#ifndef __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_GEOMETRY_H__
#define __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_GEOMETRY_H__

#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"

class Ray;
class RayHit;
class PolyhedralBoundedSolid;

class Geometry {
public:
    static const int INSIDE = 1;
    static const int LIMIT = 0;
    static const int OUTSIDE = -1;

    virtual ~Geometry() {}

    virtual bool doIntersection(const Ray& inRay, RayHit* outHit) = 0;
    virtual void doExtraInformation(const Ray& inRay, double inT, RayHit* outHit);
    virtual int computeQuantitativeInvisibility(const Vector3Dd& origin, const Vector3Dd& p);
    virtual double* getMinMax() = 0;
    virtual PolyhedralBoundedSolid* exportToPolyhedralBoundedSolid();
    virtual int doContainmentTest(const Vector3Dd& p, double distanceTolerance);
};

#endif
