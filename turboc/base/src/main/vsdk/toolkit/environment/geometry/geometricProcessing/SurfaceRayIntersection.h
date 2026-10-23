#ifndef __SURFACE_RAY_INTERSECTION__
#define __SURFACE_RAY_INTERSECTION__

#include "vsdk/toolkit/common/VSDK.h"

class Geometry;
class Ray;
class RayHit;

class SurfaceRayIntersection {
public:
    static BOOLEAN doIntersectionFirstHit(Geometry* geometry, const Ray& inRay, RayHit* outHit);
};

#endif
