#ifndef __CURVE__
#define __CURVE__

#include "vsdk/toolkit/environment/geometry/Geometry.h"
class Ray;
class RayHit;

class Curve : public Geometry {
public:
    static const Class CLASS;
    virtual const Class *getClass() const;

    virtual ~Curve() {}

    virtual Ray* doIntersectionFirstHit(const Ray& r);
    virtual bool doIntersectionFirstHit(const Ray& inRay, RayHit* outHit);
    virtual void doExtraInformation(const Ray& inRay, double inT, RayHit* outData);
};

#endif
