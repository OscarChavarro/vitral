#ifndef __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_GEOMETRICPROCESSING_SURFACERAYINTERSECTION_H__
#define __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_GEOMETRICPROCESSING_SURFACERAYINTERSECTION_H__

class Geometry;
class Ray;
class RayHit;

class SurfaceRayIntersection {
public:
    static bool doIntersectionFirstHit(Geometry* geometry, const Ray& inRay, RayHit* outHit);
};

#endif
