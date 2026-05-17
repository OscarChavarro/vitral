#ifndef __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_CURVE_CURVE_H__
#define __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_CURVE_CURVE_H__

#include "vsdk/toolkit/environment/geometry/Geometry.h"

class Ray;
class RayHit;

class Curve : public Geometry {
public:
    virtual ~Curve() override {}

    virtual Ray* doIntersection(const Ray& r);
    virtual bool doIntersection(const Ray& inRay, RayHit* outHit) override;
    virtual void doExtraInformation(const Ray& inRay, double inT, RayHit* outData) override;
};

#endif
