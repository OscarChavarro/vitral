#ifndef __IMPLICIT_SOLID__
#define __IMPLICIT_SOLID__

#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/numerics/FloatingInterval.h"
#include "vsdk/toolkit/environment/geometry/volume/Solid.h"
class PolyhedralBoundedSolid;

// Solid F(p) < 0 of a continuous scalar field F whose ray restriction a
// subclass can solve. This class turns the roots of F along a ray into the
// typed queries: first hit, crossings, ray classification, certified point and
// box containment, and surface detail with the outward normal grad F / |grad F|.
// Solver roots are refined by Newton steps on F before use.
// Ray membership between consecutive roots is decided by the sign of F at a
// point inside the segment, so tangent double roots do not split segments and a zero or
// non-finite sample is reported as Indeterminate. Queries need a finite minT.
class ImplicitSolid : public Solid {
public:
    // Largest relative Newton correction applied to a solver root.
    static constexpr double ROOT_POLISH_REACH = 1.0e-3;
    // Roots closer than this relative distance are one root.
    static constexpr double ROOT_MERGE_TOLERANCE = 1.0e-12;
    /// Default number of grid cells along the largest side of the box
    /// polygonized by `exportToPolyhedralBoundedSolid`
    static const int DEFAULT_POLYGONIZATION_DIVISIONS = 16;
    /// Half the side of the cube, centered at the origin, that clips an
    /// unbounded solid when exported to a boundary representation
    static constexpr double DEFAULT_CLIPPING_HALF_SIZE = 3.0;
private:
    double polishRoot(const Vector3Dd& start, const Vector3Dd& unit, double distance) const;
    RayFirstHitStatus collectRoots(const Ray& ray, const RayCrossingQuery& query,
        java::ArrayList<double>& distances, Vector3Dd& start, Vector3Dd& unit,
        double& length, double& maximumDistance) const;
    int sampleSign(const Vector3Dd& start, const Vector3Dd& unit, double distance) const;
    int enclosureSign(const Vector3Dd& point) const;
    int boxSign(const FloatingInterval& x, const FloatingInterval& y, const FloatingInterval& z,
        const Vector3Dd& center) const;
    RaySolidQueryStatus classifySegments(const java::ArrayList<double>& distances,
        const Vector3Dd& start, const Vector3Dd& unit, double maximumDistance,
        java::ArrayList<bool>& inside) const;
public:
    ~ImplicitSolid() override {}
    // Field value; the solid is F < 0.
    virtual double evaluateField(const Vector3Dd& point) const = 0;
    // Gradient of the field, pointing out of the solid.
    virtual Vector3Dd evaluateFieldGradient(const Vector3Dd& point) const = 0;
    // Certified enclosure of F over the box x by y by z; invalid when unknown.
    virtual FloatingInterval encloseField(const FloatingInterval& x,
        const FloatingInterval& y, const FloatingInterval& z) const = 0;
    // Certified enclosures of the gradient over the box, used for the
    // mean-value form F(c) + grad F(box) . (box - c); false when unavailable.
    virtual bool encloseFieldGradient(const FloatingInterval& x, const FloatingInterval& y,
        const FloatingInterval& z, FloatingInterval& gx, FloatingInterval& gy,
        FloatingInterval& gz) const;
    // Appends the distances d >= 0 at which F(origin + d*unitDirection) = 0,
    // in any order, including at least every root up to maximumDistance.
    // Returns false when the roots cannot be found.
    virtual bool findRayRoots(const Vector3Dd& origin, const Vector3Dd& unitDirection,
        double maximumDistance, java::ArrayList<double>& distances) const = 0;

    GeometryContainment classifyPoint(const Vector3Dd& point, double tolerance) const override;
    // Sign enclosures over the ball's bounding box prove Inside/Outside;
    // Boundary requires two certified points of opposite sign within the ball.
    GeometryContainment classifyPointCertified(const Vector3Dd& point, double tolerance) const override;
    GeometryContainment classifyBoxCertified(const Vector3Dd& center, const Vector3Dd& halfWidths) const override;
    RayFirstHitStatus queryFirstHit(const Ray& ray, const RayCrossingQuery& query,
        RayHit* hit, RayBoundary* boundary = nullptr) const override;
    RayFirstHitStatus querySurfaceDetail(const Ray& ray, const RayBoundary& boundary,
        RayHit* hit) const override;
    RayCrossingSupport emitRayCrossings(const Ray& ray, const RayCrossingQuery& query,
        RayCrossingSink& sink) const override;
    RaySolidQueryStatus classifyRay(const Ray& ray, const RayCrossingQuery& query,
        RaySolidClassification& result) const override;
    bool doIntersectionFirstHit(const Ray& ray, RayHit* hit) override;
    void doExtraInformation(const Ray& ray, double t, RayHit* hit) override;
    int doContainmentTest(const Vector3Dd& point, double tolerance) override;
    double* getMinMax() override;

    //= Boundary representation =============================================

    /**
    Polyhedral approximation with the default number of divisions (see
    `exportToPolyhedralBoundedSolid(int)`).
    */
    PolyhedralBoundedSolid* exportToPolyhedralBoundedSolid() override;

    /**
    Polyhedral approximation of the solid F < 0, built with Euler
    operators by `PolyhedralBoundedSolidFieldConstructions`. A bounded solid
    is polygonized inside its bounds; an unbounded one is clipped by the
    cube of half side `DEFAULT_CLIPPING_HALF_SIZE` centered at the origin.
    @param divisions number of grid cells along the largest side of the
    polygonized box (at least 1)
    @return a new polyhedral approximation, or null if the solid is empty or
    its bounds can not be determined
    */
    PolyhedralBoundedSolid* exportToPolyhedralBoundedSolid(int divisions);

    /**
    Polyhedral approximation of the part of the solid F < 0 inside a box.
    @param divisions number of grid cells along the largest side of the box
    @param minimum minimum corner of the box
    @param maximum maximum corner of the box
    @return a new polyhedral approximation, or null if it is empty
    */
    PolyhedralBoundedSolid* exportToPolyhedralBoundedSolid(int divisions,
        const Vector3Dd& minimum, const Vector3Dd& maximum);
};
#endif
