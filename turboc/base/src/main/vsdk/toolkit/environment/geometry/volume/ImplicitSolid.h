#ifndef __IMPLICIT_SOLID__
#define __IMPLICIT_SOLID__

#include "vsdk/toolkit/common/VSDK.h"

#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/numerics/FloatingInterval.h"
#include "vsdk/toolkit/environment/geometry/volume/Solid.h"

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
    static const Class CLASS;
    virtual const Class *getClass() const;
    // Largest relative Newton correction applied to a solver root.
    static const double ROOT_POLISH_REACH;
    // Roots closer than this relative distance are one root.
    static const double ROOT_MERGE_TOLERANCE;
private:
    double polishRoot(const Vector3Dd& start, const Vector3Dd& unit, double distance) const;
    RayFirstHitStatus::Value collectRoots(const Ray& ray, const RayCrossingQuery& query,
        ArrayList<double>& distances, Vector3Dd& start, Vector3Dd& unit,
        double& length, double& maximumDistance) const;
    int sampleSign(const Vector3Dd& start, const Vector3Dd& unit, double distance) const;
    int enclosureSign(const Vector3Dd& point) const;
    int boxSign(const FloatingInterval& x, const FloatingInterval& y, const FloatingInterval& z,
        const Vector3Dd& center) const;
    RaySolidQueryStatus::Value classifySegments(const ArrayList<double>& distances,
        const Vector3Dd& start, const Vector3Dd& unit, double maximumDistance,
        ArrayList<BOOLEAN>& inside) const;
public:
    virtual ~ImplicitSolid() {}
    // Field value; the solid is F < 0.
    virtual double evaluateField(const Vector3Dd& point) const = 0;
    // Gradient of the field, pointing out of the solid.
    virtual Vector3Dd evaluateFieldGradient(const Vector3Dd& point) const = 0;
    // Certified enclosure of F over the box x by y by z; invalid when unknown.
    virtual FloatingInterval encloseField(const FloatingInterval& x,
        const FloatingInterval& y, const FloatingInterval& z) const = 0;
    // Certified enclosures of the gradient over the box, used for the
    // mean-value form F(c) + grad F(box) . (box - c); false when unavailable.
    virtual BOOLEAN encloseFieldGradient(const FloatingInterval& x, const FloatingInterval& y,
        const FloatingInterval& z, FloatingInterval& gx, FloatingInterval& gy,
        FloatingInterval& gz) const;
    // Appends the distances d >= 0 at which F(origin + d*unitDirection) = 0,
    // in any order, including at least every root up to maximumDistance.
    // Returns false when the roots cannot be found.
    virtual BOOLEAN findRayRoots(const Vector3Dd& origin, const Vector3Dd& unitDirection,
        double maximumDistance, ArrayList<double>& distances) const = 0;

    virtual GeometryContainment::Value classifyPoint(const Vector3Dd& point, double tolerance) const;
    // Sign enclosures over the ball's bounding box prove Inside/Outside;
    // Boundary requires two certified points of opposite sign within the ball.
    virtual GeometryContainment::Value classifyPointCertified(const Vector3Dd& point, double tolerance) const;
    virtual GeometryContainment::Value classifyBoxCertified(const Vector3Dd& center, const Vector3Dd& halfWidths) const;
    virtual RayFirstHitStatus::Value queryFirstHit(const Ray& ray, const RayCrossingQuery& query,
        RayHit* hit, RayBoundary* boundary = NULL) const;
    virtual RayFirstHitStatus::Value querySurfaceDetail(const Ray& ray, const RayBoundary& boundary,
        RayHit* hit) const;
    virtual RayCrossingSupport::Value emitRayCrossings(const Ray& ray, const RayCrossingQuery& query,
        RayCrossingSink& sink) const;
    virtual RaySolidQueryStatus::Value classifyRay(const Ray& ray, const RayCrossingQuery& query,
        RaySolidClassification& result) const;
    virtual BOOLEAN doIntersectionFirstHit(const Ray& ray, RayHit* hit);
    virtual void doExtraInformation(const Ray& ray, double t, RayHit* hit);
    virtual int doContainmentTest(const Vector3Dd& point, double tolerance);
    virtual double* getMinMax();

    //= Boundary representation =========================================

    /// Default number of grid cells along the largest side of the box
    /// polygonized by `exportToPolyhedralBoundedSolid`
    enum { DEFAULT_POLYGONIZATION_DIVISIONS = 16 };
    /// Half the side of the cube, centered at the origin, that clips an
    /// unbounded solid when exported to a boundary representation
    static const double DEFAULT_CLIPPING_HALF_SIZE;

    /**
    Polyhedral approximation with the default number of divisions (see
    `exportToPolyhedralBoundedSolid(int)`).
    */
    virtual PolyhedralBoundedSolid* exportToPolyhedralBoundedSolid();

    /**
    Polyhedral approximation of the solid F &lt; 0, built with Euler
    operators by `PolyhedralBoundedSolidFieldConstructions`. A bounded solid
    is polygonized inside its bounds; an unbounded one is clipped by the
    cube of half side `DEFAULT_CLIPPING_HALF_SIZE` centered at the origin.
    @param divisions number of grid cells along the largest side of the
    polygonized box (at least 1)
    @return a new polyhedral approximation, owned by the caller, or null if
    the solid is empty or its bounds can not be determined
    */
    PolyhedralBoundedSolid* exportToPolyhedralBoundedSolid(int divisions);

    /**
    Polyhedral approximation of the part of the solid F &lt; 0 inside a box.
    @param divisions number of grid cells along the largest side of the box
    @param minimum minimum corner of the box
    @param maximum maximum corner of the box
    @return a new polyhedral approximation, owned by the caller, or null if
    it is empty
    */
    PolyhedralBoundedSolid* exportToPolyhedralBoundedSolid(int divisions,
        const Vector3Dd& minimum, const Vector3Dd& maximum);
};
#endif
