#ifndef __PLANAR_HALF_SPACE__
#define __PLANAR_HALF_SPACE__

#include "vsdk/toolkit/common/VSDK.h"

#include "vsdk/toolkit/environment/geometry/volume/Solid.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"

// The closed solid n dot p + d <= 0. The plane owns its mathematical
// coefficients; placement and complement are independent scene operations.
class PlanarHalfSpace : public Solid {
private:
    InfinitePlane plane;
public:
    static const Class CLASS;
    virtual const Class *getClass() const;
    PlanarHalfSpace(double a, double b, double c, double d);
    explicit PlanarHalfSpace(const InfinitePlane& boundary);
    virtual Geometry* copyGeometry() const;
    const InfinitePlane& getBoundaryPlane() const { return plane; }
    InfinitePlane& getBoundaryPlane() { return plane; }
    virtual void getBounds(GeometryBounds& result) const;
    virtual GeometryContainment::Value classifyPoint(const Vector3Dd& point, double tolerance) const;
    virtual GeometryContainment::Value classifyPointCertified(const Vector3Dd& point, double tolerance) const;
    virtual GeometryContainment::Value classifyBoxCertified(const Vector3Dd& center, const Vector3Dd& halfWidths) const;
    virtual int doContainmentTest(const Vector3Dd& point, double tolerance);
    virtual RayCrossingSupport::Value emitRayCrossings(const Ray& ray, const RayCrossingQuery& query,
        RayCrossingSink& sink) const;
    virtual RayFirstHitStatus::Value querySurfaceDetail(const Ray& ray, const RayBoundary& boundary, RayHit* hit) const;
    virtual RayFirstHitStatus::Value queryFirstHit(const Ray& ray, const RayCrossingQuery& query,
        RayHit* hit, RayBoundary* boundary = NULL) const;
    virtual RaySolidQueryStatus::Value classifyRay(const Ray& ray, const RayCrossingQuery& query,
        RaySolidClassification& result) const;
    virtual BOOLEAN doIntersectionFirstHit(const Ray& ray, RayHit* hit);
    virtual void doExtraInformation(const Ray& ray, double t, RayHit* hit);
    virtual double* getMinMax();
};
#endif
