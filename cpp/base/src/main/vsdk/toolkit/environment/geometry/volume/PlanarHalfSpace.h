#ifndef __PLANAR_HALF_SPACE__
#define __PLANAR_HALF_SPACE__

#include "vsdk/toolkit/environment/geometry/volume/Solid.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
class PolyhedralBoundedSolid;

//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

// The closed solid n dot p + d <= 0. The plane owns its mathematical
// coefficients; placement and complement are independent scene operations.
class PlanarHalfSpace : public Solid {
private:
    InfinitePlane plane;

    /**
    The state is the equation ax + by + cz + d = 0 of the boundary plane, with
    its raw (not normalized) coefficients. Any real value is valid for each
    one, but (a, b, c) must not be all zero.
    */
    void addControlSpecifications();
public:
    /// Default half side of the square of the plane kept by the export
    static constexpr double DEFAULT_EXPORT_HALF_SIZE = 2.0;
    /// Default thickness of the slab kept by the export
    static constexpr double DEFAULT_EXPORT_DEPTH = 0.5;

    PlanarHalfSpace(double a, double b, double c, double d);
    explicit PlanarHalfSpace(const InfinitePlane& boundary);
    double getA() const { return plane.getA(); }
    double getB() const { return plane.getB(); }
    double getC() const { return plane.getC(); }
    double getD() const { return plane.getD(); }
    void setA(double a) { plane.setA(a); }
    void setB(double b) { plane.setB(b); }
    void setC(double c) { plane.setC(c); }
    void setD(double d) { plane.setD(d); }
    Geometry* copyGeometry() const override;
    const InfinitePlane& getBoundaryPlane() const { return plane; }
    InfinitePlane& getBoundaryPlane() { return plane; }
    void getBounds(GeometryBounds& result) const override;
    GeometryContainment classifyPoint(const Vector3Dd& point, double tolerance) const override;
    GeometryContainment classifyPointCertified(const Vector3Dd& point, double tolerance) const override;
    GeometryContainment classifyBoxCertified(const Vector3Dd& center, const Vector3Dd& halfWidths) const override;
    int doContainmentTest(const Vector3Dd& point, double tolerance) override;
    RayCrossingSupport emitRayCrossings(const Ray& ray, const RayCrossingQuery& query,
        RayCrossingSink& sink) const override;
    RayFirstHitStatus querySurfaceDetail(const Ray& ray, const RayBoundary& boundary, RayHit* hit) const override;
    RayFirstHitStatus queryFirstHit(const Ray& ray, const RayCrossingQuery& query,
        RayHit* hit, RayBoundary* boundary = nullptr) const override;
    RaySolidQueryStatus classifyRay(const Ray& ray, const RayCrossingQuery& query,
        RaySolidClassification& result) const override;
    bool doIntersectionFirstHit(const Ray& ray, RayHit* hit) override;
    void doExtraInformation(const Ray& ray, double t, RayHit* hit) override;
    double* getMinMax() override;

    //= Boundary representation =============================================

    /**
    Bounded part of the half space with the default size (see
    `exportToPolyhedralBoundedSolid(double, double)`).
    */
    PolyhedralBoundedSolid* exportToPolyhedralBoundedSolid() override;

    /**
    The half space is unbounded, so its boundary representation is a bounded
    part of it: the slab under a square of its plane, centered at the point
    of the plane nearest to the origin. It is the boundary representation of
    a `Box` (built with Euler operators, [MANT1988].12.4) placed in the frame
    of the plane: its top face on the plane, its outward normal along the
    normal of the plane.
    @param halfSize half the side of the square of the plane
    @param depth thickness of the slab, below the plane
    @return a new polyhedral solid, or null if the plane is not valid
    */
    PolyhedralBoundedSolid* exportToPolyhedralBoundedSolid(double halfSize,
        double depth);
};
#endif
