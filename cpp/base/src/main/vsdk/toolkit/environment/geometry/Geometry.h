#ifndef __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_GEOMETRY_H__
#define __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_GEOMETRY_H__

#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/processing/Containment.h"

class Ray;
class RayHit;

class Geometry {
public:
    static constexpr int INSIDE = static_cast<int>(Containment::INSIDE);
    static constexpr int LIMIT = static_cast<int>(Containment::LIMIT);
    static constexpr int OUTSIDE = static_cast<int>(Containment::OUTSIDE);

    virtual ~Geometry() {}

    virtual bool doIntersection(const Ray& inRay, RayHit* outHit) = 0;
    virtual void doExtraInformation(const Ray& inRay, double inT, RayHit* outHit);
    virtual int computeQuantitativeInvisibility(const Vector3Dd& origin, const Vector3Dd& p);
    virtual double* getMinMax() = 0;
    virtual int doContainmentTest(const Vector3Dd& p, double distanceTolerance);
};

#endif
