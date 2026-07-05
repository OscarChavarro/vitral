#ifndef __SURFACERAYINTERSECTION__
#define __SURFACERAYINTERSECTION__

class Geometry;
class Ray;
class RayHit;

class SurfaceRayIntersection {
public:
    static bool doIntersectionFirstHit(Geometry* geometry, const Ray& inRay, RayHit* outHit);
};

#endif
