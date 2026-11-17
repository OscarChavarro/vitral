#include "java/lang/Double.h"
#include "java/lang/IllegalStateException.h"
#include "java/lang/Math.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/element/GeometryBounds.h"
#include "vsdk/toolkit/environment/geometry/element/Ray.h"
#include "vsdk/toolkit/environment/geometry/element/RayHit.h"
#include "vsdk/toolkit/environment/geometry/element/RaySolidClassification.h"
#include "vsdk/toolkit/environment/geometry/volume/PlanarHalfSpace.h"
#include "vsdk/toolkit/common/numerics/FloatingInterval.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/environment/geometry/volume/Box.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"

PlanarHalfSpace::PlanarHalfSpace(double a, double b, double c, double d) : plane(a,b,c,d) {
    addControlSpecifications();
}
PlanarHalfSpace::PlanarHalfSpace(const InfinitePlane& boundary) : plane(boundary) {
    addControlSpecifications();
}
void PlanarHalfSpace::addControlSpecifications() {
    addControlSpecification("double;a;(-INFINITE, INFINITE)",
        &PlanarHalfSpace::getA, &PlanarHalfSpace::setA);
    addControlSpecification("double;b;(-INFINITE, INFINITE)",
        &PlanarHalfSpace::getB, &PlanarHalfSpace::setB);
    addControlSpecification("double;c;(-INFINITE, INFINITE)",
        &PlanarHalfSpace::getC, &PlanarHalfSpace::setC);
    addControlSpecification("double;d;(-INFINITE, INFINITE)",
        &PlanarHalfSpace::getD, &PlanarHalfSpace::setD);
}
Geometry* PlanarHalfSpace::copyGeometry() const { return new PlanarHalfSpace(plane); }
void PlanarHalfSpace::getBounds(GeometryBounds& result) const { plane.getBounds(result); }
double* PlanarHalfSpace::getMinMax() { return plane.getMinMax(); }

GeometryContainment PlanarHalfSpace::classifyBoxCertified(const Vector3Dd& center,
    const Vector3Dd& halfWidths) const
{
    const double a = plane.getA(), b = plane.getB(), c = plane.getC(), d = plane.getD();
    if (!java::Double::isFinite(a) || !java::Double::isFinite(b) || !java::Double::isFinite(c) ||
        !java::Double::isFinite(d) || (a == 0 && b == 0 && c == 0))
        return GeometryContainment::InvalidInput;
    if (!FloatingInterval::isSupportedEnvironment()) return GeometryContainment::Unsupported;
    const FloatingInterval x = FloatingInterval::around(center.x(), halfWidths.x());
    const FloatingInterval y = FloatingInterval::around(center.y(), halfWidths.y());
    const FloatingInterval z = FloatingInterval::around(center.z(), halfWidths.z());
    if (!x.isValid() || !y.isValid() || !z.isValid()) return GeometryContainment::InvalidInput;
    const FloatingInterval value = FloatingInterval::exact(a).multiply(x)
        .add(FloatingInterval::exact(b).multiply(y))
        .add(FloatingInterval::exact(c).multiply(z)).add(FloatingInterval::exact(d));
    if (!value.isValid()) return GeometryContainment::Indeterminate;
    if (value.getUpper() < 0) return GeometryContainment::Inside;
    if (value.getLower() > 0) return GeometryContainment::Outside;
    return GeometryContainment::Indeterminate;
}

GeometryContainment PlanarHalfSpace::classifyPointCertified(const Vector3Dd& point, double tolerance) const
{
    const double a = plane.getA(), b = plane.getB(), c = plane.getC(), d = plane.getD();
    if (!java::Double::isFinite(a) || !java::Double::isFinite(b) || !java::Double::isFinite(c) ||
        !java::Double::isFinite(d) || (a == 0 && b == 0 && c == 0) ||
        !java::Double::isFinite(point.x()) || !java::Double::isFinite(point.y()) ||
        !java::Double::isFinite(point.z()) || !java::Double::isFinite(tolerance) || tolerance < 0)
        return GeometryContainment::InvalidInput;
    if (!FloatingInterval::isSupportedEnvironment()) return GeometryContainment::Unsupported;
    const FloatingInterval A = FloatingInterval::exact(a), B = FloatingInterval::exact(b),
        C = FloatingInterval::exact(c);
    const FloatingInterval value = A.multiply(FloatingInterval::exact(point.x()))
        .add(B.multiply(FloatingInterval::exact(point.y())))
        .add(C.multiply(FloatingInterval::exact(point.z()))).add(FloatingInterval::exact(d));
    const FloatingInterval norm = A.square().add(B.square()).add(C.square()).squareRoot();
    if (!value.isValid() || !norm.isValid() || !(norm.getLower() > 0)) return GeometryContainment::Indeterminate;
    const FloatingInterval signedDistance = value.divide(norm);
    if (!signedDistance.isValid()) return GeometryContainment::Indeterminate;
    if (signedDistance.getUpper() < -tolerance) return GeometryContainment::Inside;
    if (signedDistance.getLower() > tolerance) return GeometryContainment::Outside;
    if (signedDistance.getLower() >= -tolerance && signedDistance.getUpper() <= tolerance)
        return GeometryContainment::Boundary;
    return GeometryContainment::Indeterminate;
}

GeometryContainment PlanarHalfSpace::classifyPoint(const Vector3Dd& point, double tolerance) const
{
    const double a = plane.getA(), b = plane.getB(), c = plane.getC(), d = plane.getD();
    if (!java::Double::isFinite(a) || !java::Double::isFinite(b) || !java::Double::isFinite(c) ||
        !java::Double::isFinite(d) || (a == 0 && b == 0 && c == 0) ||
        !java::Double::isFinite(point.x()) || !java::Double::isFinite(point.y()) ||
        !java::Double::isFinite(point.z()) || !java::Double::isFinite(tolerance) || tolerance < 0)
        return GeometryContainment::InvalidInput;
    if (tolerance > 0) return classifyPointCertified(point, tolerance);
    const double squared = a*a + b*b + c*c;
    const double value = plane.pointDistance(point);
    if (!java::Double::isFinite(squared) || squared == 0 || !java::Double::isFinite(value))
        return GeometryContainment::Indeterminate;
    if (value == 0 &&
        ((a != 0 && point.x() != 0 && a * point.x() == 0) ||
         (b != 0 && point.y() != 0 && b * point.y() == 0) ||
         (c != 0 && point.z() != 0 && c * point.z() == 0)))
        return GeometryContainment::Indeterminate;
    const double signedDistance = value / java::Math::sqrt(squared);
    if (!java::Double::isFinite(signedDistance) || (value != 0 && signedDistance == 0))
        return GeometryContainment::Indeterminate;
    if (signedDistance < -tolerance) return GeometryContainment::Inside;
    if (signedDistance > tolerance) return GeometryContainment::Outside;
    return GeometryContainment::Boundary;
}

int PlanarHalfSpace::doContainmentTest(const Vector3Dd& point, double tolerance)
{
    const GeometryContainment result = classifyPoint(point, tolerance);
    if (result == GeometryContainment::Inside) return INSIDE;
    if (result == GeometryContainment::Outside) return OUTSIDE;
    if (result == GeometryContainment::Boundary) return LIMIT;
    throw java::IllegalStateException("Planar half-space containment is indeterminate or invalid");
}

RayFirstHitStatus PlanarHalfSpace::queryFirstHit(const Ray& ray, const RayCrossingQuery& query,
    RayHit* hit, RayBoundary* boundary) const
{
    if (boundary) *boundary = RayBoundary();
    RayBoundary planeBoundary;
    const RayFirstHitStatus status = plane.queryFirstHit(ray, query, hit,
        boundary ? &planeBoundary : nullptr);
    if (status == RayFirstHitStatus::Hit && boundary) *boundary = RayBoundary(planeBoundary.t, this);
    return status;
}

RayFirstHitStatus PlanarHalfSpace::querySurfaceDetail(const Ray& ray,
    const RayBoundary& boundary, RayHit* hit) const
{
    if (hit) hit->clear();
    if (!hit || boundary.geometry != this || !java::Double::isFinite(boundary.t))
        return RayFirstHitStatus::InvalidInput;
    if (boundary.getInstanceCount() != 0) return RayFirstHitStatus::Unsupported;
    return plane.querySurfaceDetail(ray, RayBoundary(boundary.t, &plane, boundary.normalReversed), hit);
}

RayCrossingSupport PlanarHalfSpace::emitRayCrossings(const Ray& ray,
    const RayCrossingQuery& query, RayCrossingSink& sink) const
{
    RayHit hit(RayHit::DETAIL_NONE, false);
    const RayFirstHitStatus status = plane.queryFirstHit(ray, query, &hit);
    if (status == RayFirstHitStatus::Unsupported) return RayCrossingSupport::Unsupported;
    if (status == RayFirstHitStatus::InvalidInput) return RayCrossingSupport::InvalidInput;
    if (status == RayFirstHitStatus::Indeterminate) return RayCrossingSupport::Indeterminate;
    if (status == RayFirstHitStatus::Hit) {
        const Vector3Dd normal(plane.getA(), plane.getB(), plane.getC());
        const double slope = normal.dotProduct(ray.getDirection());
        if (!java::Double::isFinite(slope) || slope == 0) return RayCrossingSupport::Indeterminate;
        sink.crossing(RayCrossing(hit.getHitDistance(), this,
            slope < 0 ? RayCrossing::Kind::Entering : RayCrossing::Kind::Leaving));
    }
    return RayCrossingSupport::Supported;
}

RaySolidQueryStatus PlanarHalfSpace::classifyRay(const Ray& ray,
    const RayCrossingQuery& query, RaySolidClassification& result) const
{
    result.clear();
    if (java::Double::isNaN(query.minT) || java::Double::isNaN(query.maxT) || query.minT >= query.maxT)
        return RaySolidQueryStatus::InvalidInput;
    const RayCrossingQuery full(java::Double::NEGATIVE_INFINITY, java::Double::POSITIVE_INFINITY,
        query.minimumChordSquared, query.minimumExteriorApproach);
    RayHit hit(RayHit::DETAIL_NONE, false);
    const RayFirstHitStatus status = plane.queryFirstHit(ray, full, &hit);
    if (status == RayFirstHitStatus::Unsupported) return RaySolidQueryStatus::Unsupported;
    if (status == RayFirstHitStatus::InvalidInput) return RaySolidQueryStatus::InvalidInput;
    if (status == RayFirstHitStatus::Indeterminate) return RaySolidQueryStatus::Indeterminate;
    if (status == RayFirstHitStatus::Miss) {
        const double value = plane.pointDistance(ray.getOrigin());
        if (!java::Double::isFinite(value) || value == 0) return RaySolidQueryStatus::Indeterminate;
        return result.insideSegments.add(value < 0) ? RaySolidQueryStatus::Supported :
            RaySolidQueryStatus::Indeterminate;
    }
    const Vector3Dd normal(plane.getA(), plane.getB(), plane.getC());
    const double slope = normal.dotProduct(ray.getDirection());
    const double t = hit.getHitDistance();
    if (!java::Double::isFinite(slope) || slope == 0 || !java::Double::isFinite(t))
        return RaySolidQueryStatus::Indeterminate;
    const bool before = slope > 0;
    const bool initial = query.minT <= t ? before : !before;
    if (!result.insideSegments.add(initial)) return RaySolidQueryStatus::Indeterminate;
    if (t >= query.minT && t <= query.maxT) {
        if (!result.boundaries.add(RaySolidBoundary(t, this)) || !result.insideSegments.add(!before)) {
            result.clear(); return RaySolidQueryStatus::Indeterminate;
        }
    }
    return RaySolidQueryStatus::Supported;
}

bool PlanarHalfSpace::doIntersectionFirstHit(const Ray& ray, RayHit* hit)
{
    return queryFirstHit(ray, RayCrossingQuery(0, java::Double::POSITIVE_INFINITY), hit) == RayFirstHitStatus::Hit;
}

void PlanarHalfSpace::doExtraInformation(const Ray& ray, double t, RayHit* hit)
{
    plane.doExtraInformation(ray, t, hit);
}

//= Boundary representation =============================================

PolyhedralBoundedSolid* PlanarHalfSpace::exportToPolyhedralBoundedSolid()
{
    return exportToPolyhedralBoundedSolid(DEFAULT_EXPORT_HALF_SIZE,
        DEFAULT_EXPORT_DEPTH);
}

PolyhedralBoundedSolid* PlanarHalfSpace::exportToPolyhedralBoundedSolid(
    double halfSize, double depth)
{
    Vector3Dd n(plane.getA(), plane.getB(), plane.getC());
    double length = n.length();
    if ( !(length > 0) || !java::Double::isFinite(length) ||
         !java::Double::isFinite(plane.getD()) || !(halfSize > 0) ||
         !(depth > 0) ) {
        return nullptr;
    }
    Vector3Dd u = n.multiply(1.0 / length);
    Vector3Dd center = u.multiply(-plane.getD() / length);
    // Tangents of the plane: (t1, t2, u) is a right handed frame
    Vector3Dd axis = java::Math::abs(u.x()) < 0.9 ? Vector3Dd(1, 0, 0) :
        Vector3Dd(0, 1, 0);
    Vector3Dd t1 = axis.subtract(u.multiply(axis.dotProduct(u))).normalized();
    Vector3Dd t2 = u.crossProduct(t1);
    const double frameValues[4][4] = {
        {t1.x(), t2.x(), u.x(), center.x()},
        {t1.y(), t2.y(), u.y(), center.y()},
        {t1.z(), t2.z(), u.z(), center.z()},
        {0, 0, 0, 1}
    };
    Matrix4x4d frame(frameValues);

    PolyhedralBoundedSolid* solid = Box(
        Vector3Dd(-halfSize, -halfSize, -depth),
        Vector3Dd(halfSize, halfSize, 0)).exportToPolyhedralBoundedSolid();
    int i;
    for ( i = 0; i < solid->getVerticesList().size(); i++ ) {
        solid->getVerticesList().get(i)->position =
            frame.multiply(solid->getVerticesList().get(i)->position);
    }
    return solid;
}
