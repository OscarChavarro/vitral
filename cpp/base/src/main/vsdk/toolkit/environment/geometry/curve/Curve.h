#ifndef __CURVE__
#define __CURVE__

#include "vsdk/toolkit/environment/geometry/Geometry.h"
class Ray;
class RayHit;
class Calligraphic3DBuffer;

class Curve : public Geometry {
public:
    virtual ~Curve() override {}

    virtual Ray* doIntersectionFirstHit(const Ray& r);
    virtual bool doIntersectionFirstHit(const Ray& inRay, RayHit* outHit) override;
    virtual void doExtraInformation(const Ray& inRay, double inT, RayHit* outData) override;

    /**
    Exports a line approximation of current curve, as the 3D segments of a
    polyline per curve segment, in the coordinates of the curve. It is the
    generic representation used to draw curves without a 3D graphics API,
    and to remove their hidden parts (see `HiddenLineRenderer`).
    @return a new buffer (owned by the caller) with the segments
    approximating the curve
    */
    virtual Calligraphic3DBuffer* exportToCalligraphic3DBuffer() = 0;
};

#endif
