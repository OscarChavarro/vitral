#ifndef __CURVE__
#define __CURVE__

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
class Calligraphic3DBuffer;
class Ray;
class RayHit;

class Curve : public Geometry {
public:
    static const Class CLASS;
    virtual const Class *getClass() const;

    virtual ~Curve() {}

    virtual Ray* doIntersectionFirstHit(const Ray& r);
    virtual BOOLEAN doIntersectionFirstHit(const Ray& inRay, RayHit* outHit);
    virtual void doExtraInformation(const Ray& inRay, double inT, RayHit* outData);

    /**
    Exports a line approximation of current curve, as the 3D segments of a
    polyline per curve segment, in the coordinates of the curve. It is the
    generic representation used to draw curves without a 3D graphics API,
    and to remove their hidden parts (see `HiddenLineRenderer`).
    @return a new buffer with the segments approximating the curve, owned by
    the caller
    */
    virtual Calligraphic3DBuffer* exportToCalligraphic3DBuffer() = 0;
};

#endif
