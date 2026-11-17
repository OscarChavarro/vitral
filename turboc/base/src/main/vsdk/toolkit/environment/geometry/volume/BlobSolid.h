#ifndef __BLOB_SOLID__
#define __BLOB_SOLID__

#include "vsdk/toolkit/common/VSDK.h"

#include "vsdk/toolkit/environment/geometry/volume/BlobComponent.h"
#include "vsdk/toolkit/environment/geometry/volume/BlobInfluenceInterval.h"
#include "vsdk/toolkit/environment/geometry/volume/ImplicitSolid.h"

// Blob of spherical field components (PovRay 1.0 blob): the solid where the
// summed component density exceeds the threshold, F = threshold - density.
// Along a ray each set of overlapping influence spheres gives a quartic in the
// ray distance, solved by the closed-form quartic solver or, when requested, by
// Sturm sequences. Certified containment encloses the exact field
// strength * (1 - d^2/radius^2)^2; bounds are the boxes of positive components
// for a nonnegative threshold, and unbounded for a negative one.
class BlobSolid : public ImplicitSolid {
public:
    static const Class CLASS;
    virtual const Class *getClass() const;
    static const double COEFFICIENT_LIMIT;
    static const double SOLVER_EPSILON;
    // Rays grazing an influence sphere closer than this are ignored, and
    // roots with a squared gradient below it are rejected as spurious.
    static const double GRAZING_LIMIT;
private:
    ArrayList<BlobComponent> components;
    double threshold;
    BOOLEAN sturm;
    long int determineInfluences(const Vector3Dd& origin, const Vector3Dd& direction,
        double influenceStart, ArrayList<BlobInfluenceInterval>& intervals) const;
    FloatingInterval clampedFalloff(long int index, const FloatingInterval& x,
        const FloatingInterval& y, const FloatingInterval& z) const;
    /**
    The state edited here is the threshold. The components are a list of
    their own, and using Sturm sequences is a choice of the intersection
    algorithm, not part of the shape. A density above a nonpositive
    threshold would fill the whole space, so it must be positive.
    */
    void addControlSpecifications();
public:
    BlobSolid(double threshold, BOOLEAN useSturmSequences);
    void addComponent(const BlobComponent& component);
    long int getComponentCount() const { return components.size(); }
    const BlobComponent& getComponent(long int index) const { return components[index]; }
    double getThreshold() const { return threshold; }
    void setThreshold(double value) { threshold = value; }
    BOOLEAN usesSturmSequences() const { return sturm; }
    // Summed component density at the point.
    double evaluateDensity(const Vector3Dd& point) const;
    // Gradient of threshold - density, pointing out of the solid.
    Vector3Dd gradient(const Vector3Dd& point) const;
    // Appends the PovRay ray roots: distances d >= minimumDistance along the
    // unit direction, inside the influence intervals that start no earlier than
    // influenceStart (earlier starts are moved to 0), whose gradient is usable.
    void intersect(const Vector3Dd& origin, const Vector3Dd& unitDirection,
        double minimumDistance, double influenceStart, ArrayList<double>& distances) const;

    virtual Geometry* copyGeometry() const;
    virtual double evaluateField(const Vector3Dd& point) const { return threshold - evaluateDensity(point); }
    virtual Vector3Dd evaluateFieldGradient(const Vector3Dd& point) const { return gradient(point); }
    virtual FloatingInterval encloseField(const FloatingInterval& x, const FloatingInterval& y,
        const FloatingInterval& z) const;
    virtual BOOLEAN encloseFieldGradient(const FloatingInterval& x, const FloatingInterval& y,
        const FloatingInterval& z, FloatingInterval& gx, FloatingInterval& gy,
        FloatingInterval& gz) const;
    virtual BOOLEAN findRayRoots(const Vector3Dd& origin, const Vector3Dd& unitDirection,
        double maximumDistance, ArrayList<double>& distances) const;
    virtual void getBounds(GeometryBounds& result) const;
};
#endif
