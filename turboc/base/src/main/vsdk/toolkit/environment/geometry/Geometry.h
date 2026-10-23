#ifndef __GEOMETRY__
#define __GEOMETRY__

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/common/Entity.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/processing/Containment.h"
class Ray;
class RayHit;

class Geometry : public Entity {
public:
    static const Class CLASS;
    virtual const Class *getClass() const;

    enum {
        INSIDE = ((int)(Containment::INSIDE)),
        LIMIT = ((int)(Containment::LIMIT)),
        OUTSIDE = ((int)(Containment::OUTSIDE))
    };

    virtual ~Geometry() {}

    virtual BOOLEAN doIntersectionFirstHit(const Ray& inRay, RayHit* outHit) = 0;
    virtual void doExtraInformation(const Ray& inRay, double inT, RayHit* outHit);
    virtual int computeQuantitativeInvisibility(const Vector3Dd& origin, const Vector3Dd& p);
    virtual double* getMinMax() = 0;
    virtual int doContainmentTest(const Vector3Dd& p, double distanceTolerance);
};

#endif
