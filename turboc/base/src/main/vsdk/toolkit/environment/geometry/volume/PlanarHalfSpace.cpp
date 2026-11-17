#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/Double.h"
#include "java/lang/IllegalStateException.h"
#include "java/lang/Math.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/element/GeometryBounds.h"
#include "vsdk/toolkit/environment/geometry/element/Ray.h"
#include "vsdk/toolkit/environment/geometry/element/RayHit.h"
#include "vsdk/toolkit/environment/geometry/element/RaySolidClassification.h"
#include "vsdk/toolkit/environment/geometry/volume/PlanarHalfSpace.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/environment/geometry/volume/Box.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"
#include "vsdk/toolkit/common/numerics/FloatingInterval.h"

/**
C++ port note: true when the binary64 product of two nonzero factors is zero.
The product is stored (volatile) first: Turbo C++ 3.0 and gcc 2.7.2 compare
it in the extended exponent range of the x87, where it does not underflow.
*/
static BOOLEAN
productUnderflows(double a, double b)
{
    volatile double product = a * b;
    return a != 0 && b != 0 && product == 0;
}

const double PlanarHalfSpace::DEFAULT_EXPORT_HALF_SIZE = 2.0;
const double PlanarHalfSpace::DEFAULT_EXPORT_DEPTH = 0.5;

PlanarHalfSpace::PlanarHalfSpace(double a, double b, double c, double d) : plane(a,b,c,d)
{
    addControlSpecifications();
}

PlanarHalfSpace::PlanarHalfSpace(const InfinitePlane& boundary) : plane(boundary)
{
    addControlSpecifications();
}

void PlanarHalfSpace::addControlSpecifications()
{
    // Turbo C++ 3.0 crashes with the new of a template class as an argument
    MemberEntityControlAccessor<PlanarHalfSpace, double> *accessor;
    accessor = new MemberEntityControlAccessor<PlanarHalfSpace, double>(
        &PlanarHalfSpace::getA, &PlanarHalfSpace::setA);
    addControlSpecification("double;a;(-INFINITE, INFINITE)", accessor);
    accessor = new MemberEntityControlAccessor<PlanarHalfSpace, double>(
        &PlanarHalfSpace::getB, &PlanarHalfSpace::setB);
    addControlSpecification("double;b;(-INFINITE, INFINITE)", accessor);
    accessor = new MemberEntityControlAccessor<PlanarHalfSpace, double>(
        &PlanarHalfSpace::getC, &PlanarHalfSpace::setC);
    addControlSpecification("double;c;(-INFINITE, INFINITE)", accessor);
    accessor = new MemberEntityControlAccessor<PlanarHalfSpace, double>(
        &PlanarHalfSpace::getD, &PlanarHalfSpace::setD);
    addControlSpecification("double;d;(-INFINITE, INFINITE)", accessor);
}

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
    if ( !(length > 0) || !Double::isFinite(length) ||
         !Double::isFinite(plane.getD()) || !(halfSize > 0) || !(depth > 0) ) {
        return NULL;
    }
    Vector3Dd u = n.multiply(1.0 / length);
    Vector3Dd center = u.multiply(-plane.getD() / length);
    // Tangents of the plane: (t1, t2, u) is a right handed frame
    Vector3Dd axis = Math::abs(u.x()) < 0.9 ? Vector3Dd(1, 0, 0) :
        Vector3Dd(0, 1, 0);
    Vector3Dd t1 = axis.subtract(u.multiply(axis.dotProduct(u))).normalized();
    Vector3Dd t2 = u.crossProduct(t1);
    const double values[4][4] = {
        {t1.x(), t2.x(), u.x(), center.x()},
        {t1.y(), t2.y(), u.y(), center.y()},
        {t1.z(), t2.z(), u.z(), center.z()},
        {0, 0, 0, 1}
    };
    Matrix4x4d frame(values);

    Vector3Dd minimum(-halfSize, -halfSize, -depth);
    Vector3Dd maximum(halfSize, halfSize, 0);
    Box box(minimum, maximum);
    PolyhedralBoundedSolid* solid = box.exportToPolyhedralBoundedSolid();
    long int i;
    for ( i = 0; i < solid->getVerticesList().size(); i++ ) {
        solid->getVerticesList().get(i)->position =
            frame.multiply(solid->getVerticesList().get(i)->position);
    }
    return solid;
}
Geometry* PlanarHalfSpace::copyGeometry() const { return new PlanarHalfSpace(plane); }
void PlanarHalfSpace::getBounds(GeometryBounds& result) const { plane.getBounds(result); }
double* PlanarHalfSpace::getMinMax() { return plane.getMinMax(); }

GeometryContainment::Value PlanarHalfSpace::classifyBoxCertified(const Vector3Dd& center,
    const Vector3Dd& halfWidths) const
{
    const double a = plane.getA(), b = plane.getB(), c = plane.getC(), d = plane.getD();
    if (!Double::isFinite(a) || !Double::isFinite(b) || !Double::isFinite(c) ||
        !Double::isFinite(d) || (a == 0 && b == 0 && c == 0))
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

GeometryContainment::Value PlanarHalfSpace::classifyPointCertified(const Vector3Dd& point, double tolerance) const
{
    const double a = plane.getA(), b = plane.getB(), c = plane.getC(), d = plane.getD();
    if (!Double::isFinite(a) || !Double::isFinite(b) || !Double::isFinite(c) ||
        !Double::isFinite(d) || (a == 0 && b == 0 && c == 0) ||
        !Double::isFinite(point.x()) || !Double::isFinite(point.y()) ||
        !Double::isFinite(point.z()) || !Double::isFinite(tolerance) || tolerance < 0)
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

GeometryContainment::Value PlanarHalfSpace::classifyPoint(const Vector3Dd& point, double tolerance) const
{
    const double a = plane.getA(), b = plane.getB(), c = plane.getC(), d = plane.getD();
    if (!Double::isFinite(a) || !Double::isFinite(b) || !Double::isFinite(c) ||
        !Double::isFinite(d) || (a == 0 && b == 0 && c == 0) ||
        !Double::isFinite(point.x()) || !Double::isFinite(point.y()) ||
        !Double::isFinite(point.z()) || !Double::isFinite(tolerance) || tolerance < 0)
        return GeometryContainment::InvalidInput;
    if (tolerance > 0) return classifyPointCertified(point, tolerance);
    const double squared = a*a + b*b + c*c;
    const double value = plane.pointDistance(point);
    if (!Double::isFinite(squared) || squared == 0 || !Double::isFinite(value))
        return GeometryContainment::Indeterminate;
    if (value == 0 &&
        ((productUnderflows(a, point.x())) ||
         (productUnderflows(b, point.y())) ||
         (productUnderflows(c, point.z()))))
        return GeometryContainment::Indeterminate;
    const double signedDistance = value / Math::sqrt(squared);
    if (!Double::isFinite(signedDistance) || (value != 0 && signedDistance == 0))
        return GeometryContainment::Indeterminate;
    if (signedDistance < -tolerance) return GeometryContainment::Inside;
    if (signedDistance > tolerance) return GeometryContainment::Outside;
    return GeometryContainment::Boundary;
}

int PlanarHalfSpace::doContainmentTest(const Vector3Dd& point, double tolerance)
{
    const GeometryContainment::Value result = classifyPoint(point, tolerance);
    if (result == GeometryContainment::Inside) return INSIDE;
    if (result == GeometryContainment::Outside) return OUTSIDE;
    if (result == GeometryContainment::Boundary) return LIMIT;
    throw IllegalStateException("Planar half-space containment is indeterminate or invalid");
}

RayFirstHitStatus::Value PlanarHalfSpace::queryFirstHit(const Ray& ray, const RayCrossingQuery& query,
    RayHit* hit, RayBoundary* boundary) const
{
    if (boundary) *boundary = RayBoundary();
    RayBoundary planeBoundary;
    const RayFirstHitStatus::Value status = plane.queryFirstHit(ray, query, hit,
        boundary ? &planeBoundary : (RayBoundary*)NULL);
    if (status == RayFirstHitStatus::Hit && boundary) *boundary = RayBoundary(planeBoundary.t, this);
    return status;
}

RayFirstHitStatus::Value PlanarHalfSpace::querySurfaceDetail(const Ray& ray,
    const RayBoundary& boundary, RayHit* hit) const
{
    if (hit) hit->clear();
    if (!hit || boundary.geometry != this || !Double::isFinite(boundary.t))
        return RayFirstHitStatus::InvalidInput;
    if (boundary.getInstanceCount() != 0) return RayFirstHitStatus::Unsupported;
    return plane.querySurfaceDetail(ray, RayBoundary(boundary.t, &plane, boundary.normalReversed), hit);
}

RayCrossingSupport::Value PlanarHalfSpace::emitRayCrossings(const Ray& ray,
    const RayCrossingQuery& query, RayCrossingSink& sink) const
{
    RayHit hit(RayHit::DETAIL_NONE, FALSE);
    const RayFirstHitStatus::Value status = plane.queryFirstHit(ray, query, &hit);
    if (status == RayFirstHitStatus::Unsupported) return RayCrossingSupport::Unsupported;
    if (status == RayFirstHitStatus::InvalidInput) return RayCrossingSupport::InvalidInput;
    if (status == RayFirstHitStatus::Indeterminate) return RayCrossingSupport::Indeterminate;
    if (status == RayFirstHitStatus::Hit) {
        const Vector3Dd normal(plane.getA(), plane.getB(), plane.getC());
        const double slope = normal.dotProduct(ray.getDirection());
        if (!Double::isFinite(slope) || slope == 0) return RayCrossingSupport::Indeterminate;
        sink.crossing(RayCrossing(hit.getHitDistance(), this,
            slope < 0 ? RayCrossingKind::Entering : RayCrossingKind::Leaving));
    }
    return RayCrossingSupport::Supported;
}

RaySolidQueryStatus::Value PlanarHalfSpace::classifyRay(const Ray& ray,
    const RayCrossingQuery& query, RaySolidClassification& result) const
{
    result.clear();
    if (Double::isNaN(query.minT) || Double::isNaN(query.maxT) || query.minT >= query.maxT)
        return RaySolidQueryStatus::InvalidInput;
    const RayCrossingQuery full(Double::NEGATIVE_INFINITY, Double::POSITIVE_INFINITY,
        query.minimumChordSquared, query.minimumExteriorApproach);
    RayHit hit(RayHit::DETAIL_NONE, FALSE);
    const RayFirstHitStatus::Value status = plane.queryFirstHit(ray, full, &hit);
    if (status == RayFirstHitStatus::Unsupported) return RaySolidQueryStatus::Unsupported;
    if (status == RayFirstHitStatus::InvalidInput) return RaySolidQueryStatus::InvalidInput;
    if (status == RayFirstHitStatus::Indeterminate) return RaySolidQueryStatus::Indeterminate;
    if (status == RayFirstHitStatus::Miss) {
        const double value = plane.pointDistance(ray.getOrigin());
        if (!Double::isFinite(value) || value == 0) return RaySolidQueryStatus::Indeterminate;
        return result.insideSegments.add(value < 0) ? RaySolidQueryStatus::Supported :
            RaySolidQueryStatus::Indeterminate;
    }
    const Vector3Dd normal(plane.getA(), plane.getB(), plane.getC());
    const double slope = normal.dotProduct(ray.getDirection());
    const double t = hit.getHitDistance();
    if (!Double::isFinite(slope) || slope == 0 || !Double::isFinite(t))
        return RaySolidQueryStatus::Indeterminate;
    const BOOLEAN before = slope > 0;
    const BOOLEAN initial = query.minT <= t ? before : (BOOLEAN)!before;
    if (!result.insideSegments.add(initial)) return RaySolidQueryStatus::Indeterminate;
    if (t >= query.minT && t <= query.maxT) {
        if (!result.boundaries.add(RaySolidBoundary(t, this)) || !result.insideSegments.add(!before)) {
            result.clear(); return RaySolidQueryStatus::Indeterminate;
        }
    }
    return RaySolidQueryStatus::Supported;
}

BOOLEAN PlanarHalfSpace::doIntersectionFirstHit(const Ray& ray, RayHit* hit)
{
    return queryFirstHit(ray, RayCrossingQuery(0, Double::POSITIVE_INFINITY), hit) == RayFirstHitStatus::Hit;
}

void PlanarHalfSpace::doExtraInformation(const Ray& ray, double t, RayHit* hit)
{
    plane.doExtraInformation(ray, t, hit);
}

const Class PlanarHalfSpace::CLASS = { "PlanarHalfSpace", &Solid::CLASS };

const Class *
PlanarHalfSpace::getClass() const
{
    return &CLASS;
}
