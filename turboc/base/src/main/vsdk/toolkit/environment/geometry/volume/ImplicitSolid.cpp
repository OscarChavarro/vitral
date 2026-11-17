#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/Double.h"
#include "java/lang/Math.h"
#include "java/lang/IllegalStateException.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/element/GeometryBounds.h"
#include "vsdk/toolkit/environment/geometry/element/Ray.h"
#include "vsdk/toolkit/environment/geometry/element/RayBoundary.h"
#include "vsdk/toolkit/environment/geometry/element/RayCrossing.h"
#include "vsdk/toolkit/environment/geometry/element/RayHit.h"
#include "vsdk/toolkit/environment/geometry/element/RaySolidClassification.h"
#include "vsdk/toolkit/environment/geometry/volume/ImplicitSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidFieldConstructions.h"

// C++ port note: `new double[6]{...}` of the cpp version
static double*
newMinMax6(double x0, double y0, double z0, double x1, double y1, double z1)
{
    double* minMax = new double[6];
    minMax[0] = x0; minMax[1] = y0; minMax[2] = z0;
    minMax[3] = x1; minMax[4] = y1; minMax[5] = z1;
    return minMax;
}

RayFirstHitStatus::Value ImplicitSolid::collectRoots(const Ray& ray, const RayCrossingQuery& query,
    ArrayList<double>& distances, Vector3Dd& start, Vector3Dd& unit,
    double& length, double& maximumDistance) const
{ long int i;
    distances.clear();
    const Vector3Dd& o = ray.getOrigin();
    const Vector3Dd& d = ray.getDirection();
    if (!Double::isFinite(o.x()) || !Double::isFinite(o.y()) || !Double::isFinite(o.z()) ||
        !Double::isFinite(d.x()) || !Double::isFinite(d.y()) || !Double::isFinite(d.z()) ||
        Double::isNaN(query.minT) || Double::isNaN(query.maxT) || query.minT > query.maxT)
        return RayFirstHitStatus::InvalidInput;
    length = d.length();
    if (!(length > 0) || !Double::isFinite(length)) return RayFirstHitStatus::InvalidInput;
    if (query.minimumChordSquared != 0 || query.minimumExteriorApproach != Double::NEGATIVE_INFINITY ||
        !Double::isFinite(query.minT)) return RayFirstHitStatus::Unsupported;
    unit = d.multiply(1.0/length);
    start = o.add(d.multiply(query.minT));
    maximumDistance = query.maxT == Double::POSITIVE_INFINITY ?
        Double::POSITIVE_INFINITY : (query.maxT - query.minT)*length;
    if (!Double::isFinite(start.x()) || !Double::isFinite(start.y()) ||
        !Double::isFinite(start.z()) || Double::isNaN(maximumDistance))
        return RayFirstHitStatus::Indeterminate;
    ArrayList<double> found;
    if (!findRayRoots(start, unit, maximumDistance, found)) return RayFirstHitStatus::Indeterminate;
    for (i = 0; i < found.size(); ++i) {
        const double value = polishRoot(start, unit, found[i]);
        if (!Double::isFinite(value)) return RayFirstHitStatus::Indeterminate;
        if (value >= 0 && value <= maximumDistance) distances.add(value);
    }
    distances.sort(NaturalOrderComparator<double>());
    // The same root reported by two solver intervals collapses to one.
    long int kept = 0;
    for (i = 0; i < distances.size(); ++i)
        if (kept == 0 || distances[i] - distances[kept-1] > ROOT_MERGE_TOLERANCE*(1.0 + distances[i]))
            distances[kept++] = distances[i];
    while (distances.size() > kept) distances.remove(distances.size()-1);
    return distances.isEmpty() ? RayFirstHitStatus::Miss : RayFirstHitStatus::Hit;
}

double ImplicitSolid::polishRoot(const Vector3Dd& start, const Vector3Dd& unit, double distance) const
{
    // Newton steps on F along the ray, kept only while they shrink |F| and stay
    // close, so solver roots reach the accuracy of the field evaluation.
    if (!Double::isFinite(distance)) return distance;
    double value = evaluateField(start.add(unit.multiply(distance)));
    for (int iteration = 0; iteration < 8 && Double::isFinite(value) && value != 0; ++iteration) {
        const double slope = evaluateFieldGradient(start.add(unit.multiply(distance))).dotProduct(unit);
        if (!Double::isFinite(slope) || slope == 0) break;
        const double next = distance - value/slope;
        if (!Double::isFinite(next) ||
            Math::abs(next - distance) > ROOT_POLISH_REACH*(1.0 + Math::abs(distance))) break;
        const double nextValue = evaluateField(start.add(unit.multiply(next)));
        if (!(Math::abs(nextValue) < Math::abs(value))) break;
        distance = next;
        value = nextValue;
    }
    return distance;
}

int ImplicitSolid::sampleSign(const Vector3Dd& start, const Vector3Dd& unit, double distance) const
{
    const double value = evaluateField(start.add(unit.multiply(distance)));
    if (!Double::isFinite(value)) return 0;
    return value < 0 ? -1 : value > 0 ? 1 : 0;
}

RaySolidQueryStatus::Value ImplicitSolid::classifySegments(const ArrayList<double>& distances,
    const Vector3Dd& start, const Vector3Dd& unit, double maximumDistance,
    ArrayList<BOOLEAN>& inside) const
{
    inside.clear();
    const long int n = distances.size();
    for (long int i = 0; i <= n; ++i) {
        const double a = i == 0 ? 0.0 : distances[i-1];
        const double b = i == n ? maximumDistance : distances[i];
        if (!(b > a)) return RaySolidQueryStatus::Indeterminate;
        // Any point of a root-free segment decides it; long segments are
        // sampled near their start to keep F finite.
        const double reach = Math::max(1.0, a);
        const double sample = b - a > 2.0*reach ? a + reach : 0.5*(a + b);
        const int sign = sampleSign(start, unit, sample);
        if (sign == 0) return RaySolidQueryStatus::Indeterminate;
        inside.add(sign < 0);
    }
    return RaySolidQueryStatus::Supported;
}

RaySolidQueryStatus::Value ImplicitSolid::classifyRay(const Ray& ray, const RayCrossingQuery& query,
    RaySolidClassification& result) const
{
    result.clear();
    if (Double::isNaN(query.minT) || Double::isNaN(query.maxT) ||
        query.minT >= query.maxT) return RaySolidQueryStatus::InvalidInput;
    ArrayList<double> distances;
    Vector3Dd start, unit;
    double length = 0, maximumDistance = 0;
    const RayFirstHitStatus::Value status = collectRoots(ray, query, distances, start, unit,
        length, maximumDistance);
    if (status == RayFirstHitStatus::InvalidInput) return RaySolidQueryStatus::InvalidInput;
    if (status == RayFirstHitStatus::Unsupported) return RaySolidQueryStatus::Unsupported;
    if (status == RayFirstHitStatus::Indeterminate) return RaySolidQueryStatus::Indeterminate;
    ArrayList<BOOLEAN> inside;
    if (classifySegments(distances, start, unit, maximumDistance, inside) !=
        RaySolidQueryStatus::Supported) return RaySolidQueryStatus::Indeterminate;
    BOOLEAN state = inside[0];
    result.insideSegments.add(state);
    for (long int i = 0; i < distances.size(); ++i) {
        if (inside[i+1] == state) continue;
        state = inside[i+1];
        if (!result.boundaries.add(RaySolidBoundary(query.minT + distances[i]/length, this)) ||
            !result.insideSegments.add(state)) {
            result.clear(); return RaySolidQueryStatus::Indeterminate;
        }
    }
    return RaySolidQueryStatus::Supported;
}

RayCrossingSupport::Value ImplicitSolid::emitRayCrossings(const Ray& ray, const RayCrossingQuery& query,
    RayCrossingSink& sink) const
{
    ArrayList<double> distances;
    Vector3Dd start, unit;
    double length = 0, maximumDistance = 0;
    const RayFirstHitStatus::Value status = collectRoots(ray, query, distances, start, unit,
        length, maximumDistance);
    if (status == RayFirstHitStatus::Miss) return RayCrossingSupport::Supported;
    if (status == RayFirstHitStatus::InvalidInput) return RayCrossingSupport::InvalidInput;
    if (status == RayFirstHitStatus::Unsupported) return RayCrossingSupport::Unsupported;
    if (status != RayFirstHitStatus::Hit) return RayCrossingSupport::Indeterminate;
    ArrayList<BOOLEAN> inside;
    if (classifySegments(distances, start, unit, maximumDistance, inside) !=
        RaySolidQueryStatus::Supported) return RayCrossingSupport::Indeterminate;
    for (long int i = 0; i < distances.size(); ++i) {
        const RayCrossing::Kind kind = inside[i] == inside[i+1] ? RayCrossingKind::Tangent :
            inside[i+1] ? RayCrossingKind::Entering : RayCrossingKind::Leaving;
        sink.crossing(RayCrossing(query.minT + distances[i]/length, this, kind));
    }
    return RayCrossingSupport::Supported;
}

RayFirstHitStatus::Value ImplicitSolid::queryFirstHit(const Ray& ray, const RayCrossingQuery& query,
    RayHit* hit, RayBoundary* boundary) const
{
    if (hit) hit->clear();
    if (boundary) *boundary = RayBoundary();
    ArrayList<double> distances;
    Vector3Dd start, unit;
    double length = 0, maximumDistance = 0;
    const RayFirstHitStatus::Value status = collectRoots(ray, query, distances, start, unit,
        length, maximumDistance);
    if (status != RayFirstHitStatus::Hit) return status;
    const double t = query.minT + distances[0]/length;
    if (t > query.maxT) return RayFirstHitStatus::Miss;
    if (boundary) *boundary = RayBoundary(t, this);
    if (hit) {
        hit->setHitDistance(t);
        if (hit->shouldStoreRay()) hit->setRay(ray.withT(t));
        if (hit->needsAnySurfaceData()) ((ImplicitSolid*)(this))->doExtraInformation(ray, t, hit);
    }
    return RayFirstHitStatus::Hit;
}

RayFirstHitStatus::Value ImplicitSolid::querySurfaceDetail(const Ray& ray,
    const RayBoundary& boundary, RayHit* hit) const
{
    if (hit) hit->clear();
    if (!hit || boundary.geometry != this || !Double::isFinite(boundary.t))
        return RayFirstHitStatus::InvalidInput;
    if (boundary.getInstanceCount() != 0) return RayFirstHitStatus::Unsupported;
    hit->setHitDistance(boundary.t);
    if (hit->shouldStoreRay()) hit->setRay(ray.withT(boundary.t));
    ((ImplicitSolid*)(this))->doExtraInformation(ray, boundary.t, hit);
    if (boundary.normalReversed && hit->needsNormal() &&
        (hit->getAvailableDetailMask() & RayHit::DETAIL_NORMAL))
        hit->normal = hit->normal.multiply(-1);
    return RayFirstHitStatus::Hit;
}

int ImplicitSolid::enclosureSign(const Vector3Dd& point) const
{
    const FloatingInterval value = encloseField(FloatingInterval::exact(point.x()),
        FloatingInterval::exact(point.y()), FloatingInterval::exact(point.z()));
    if (!value.isValid()) return 0;
    return value.getUpper() < 0 ? -1 : value.getLower() > 0 ? 1 : 0;
}

BOOLEAN ImplicitSolid::encloseFieldGradient(const FloatingInterval&, const FloatingInterval&,
    const FloatingInterval&, FloatingInterval&, FloatingInterval&, FloatingInterval&) const
{
    return FALSE;
}

// -1/1 when the natural or the mean-value enclosure proves F < 0 / F > 0.
int ImplicitSolid::boxSign(const FloatingInterval& x, const FloatingInterval& y,
    const FloatingInterval& z, const Vector3Dd& center) const
{
    const FloatingInterval range = encloseField(x, y, z);
    if (range.isValid() && range.getUpper() < 0) return -1;
    if (range.isValid() && range.getLower() > 0) return 1;
    FloatingInterval gx, gy, gz;
    if (!encloseFieldGradient(x, y, z, gx, gy, gz) || !gx.isValid() || !gy.isValid() || !gz.isValid())
        return 0;
    const FloatingInterval cx = FloatingInterval::exact(center.x());
    const FloatingInterval cy = FloatingInterval::exact(center.y());
    const FloatingInterval cz = FloatingInterval::exact(center.z());
    FloatingInterval value = encloseField(cx, cy, cz);
    value = value.add(gx.multiply(x.subtract(cx)));
    value = value.add(gy.multiply(y.subtract(cy)));
    value = value.add(gz.multiply(z.subtract(cz)));
    if (!value.isValid()) return 0;
    return value.getUpper() < 0 ? -1 : value.getLower() > 0 ? 1 : 0;
}

GeometryContainment::Value ImplicitSolid::classifyBoxCertified(const Vector3Dd& center,
    const Vector3Dd& halfWidths) const
{
    if (!FloatingInterval::isSupportedEnvironment()) return GeometryContainment::Unsupported;
    const FloatingInterval x = FloatingInterval::around(center.x(), halfWidths.x());
    const FloatingInterval y = FloatingInterval::around(center.y(), halfWidths.y());
    const FloatingInterval z = FloatingInterval::around(center.z(), halfWidths.z());
    if (!x.isValid() || !y.isValid() || !z.isValid()) return GeometryContainment::InvalidInput;
    const int sign = boxSign(x, y, z, center);
    return sign < 0 ? GeometryContainment::Inside : sign > 0 ? GeometryContainment::Outside :
        GeometryContainment::Indeterminate;
}

GeometryContainment::Value ImplicitSolid::classifyPointCertified(const Vector3Dd& point, double tolerance) const
{
    if (!Double::isFinite(tolerance) || tolerance < 0 || !Double::isFinite(point.x()) ||
        !Double::isFinite(point.y()) || !Double::isFinite(point.z()))
        return GeometryContainment::InvalidInput;
    if (!FloatingInterval::isSupportedEnvironment()) return GeometryContainment::Unsupported;
    const FloatingInterval x = FloatingInterval::around(point.x(), tolerance);
    const FloatingInterval y = FloatingInterval::around(point.y(), tolerance);
    const FloatingInterval z = FloatingInterval::around(point.z(), tolerance);
    if (!x.isValid() || !y.isValid() || !z.isValid()) return GeometryContainment::Indeterminate;
    const int sign = boxSign(x, y, z, point);
    if (sign < 0) return GeometryContainment::Inside;
    if (sign > 0) return GeometryContainment::Outside;
    if (tolerance == 0) return GeometryContainment::Indeterminate;
    // F is continuous and the ball is convex: certified opposite signs at two
    // points of the ball prove a boundary point on the segment joining them.
    const Vector3Dd g = evaluateFieldGradient(point);
    const double length = g.length();
    if (!(length > 0) || !Double::isFinite(length)) return GeometryContainment::Indeterminate;
    const Vector3Dd unit = g.multiply(1.0/length);
    const int centre = enclosureSign(point);
    const double steps[2] = {tolerance*(1.0 - 1.0/1048576.0), tolerance*0.5};
    for (int s = 0; s < 2; ++s) {
        int signs[2] = {0, 0};
        for (int side = 0; side < 2; ++side) {
            const Vector3Dd q = point.add(unit.multiply(side == 0 ? -steps[s] : steps[s]));
            FloatingInterval offset = FloatingInterval::exact(0);
            offset = offset.add(FloatingInterval::exact(q.x()).subtract(FloatingInterval::exact(point.x())).square());
            offset = offset.add(FloatingInterval::exact(q.y()).subtract(FloatingInterval::exact(point.y())).square());
            offset = offset.add(FloatingInterval::exact(q.z()).subtract(FloatingInterval::exact(point.z())).square());
            const FloatingInterval distance = offset.squareRoot();
            if (!distance.isValid() || distance.getUpper() > tolerance) continue;
            signs[side] = enclosureSign(q);
        }
        if (signs[0]*signs[1] < 0) return GeometryContainment::Boundary;
        if (centre != 0 && (signs[0] == -centre || signs[1] == -centre)) return GeometryContainment::Boundary;
    }
    return GeometryContainment::Indeterminate;
}

GeometryContainment::Value ImplicitSolid::classifyPoint(const Vector3Dd& point, double tolerance) const
{
    if (!Double::isFinite(tolerance) || tolerance < 0) return GeometryContainment::InvalidInput;
    if (tolerance != 0) return classifyPointCertified(point, tolerance);
    const double value = evaluateField(point);
    if (!Double::isFinite(value)) return GeometryContainment::Indeterminate;
    return value < 0 ? GeometryContainment::Inside : value > 0 ?
        GeometryContainment::Outside : GeometryContainment::Boundary;
}

BOOLEAN ImplicitSolid::doIntersectionFirstHit(const Ray& ray, RayHit* hit)
{
    return queryFirstHit(ray, RayCrossingQuery(1e-9, Double::POSITIVE_INFINITY), hit) ==
        RayFirstHitStatus::Hit;
}

void ImplicitSolid::doExtraInformation(const Ray& ray, double t, RayHit* hit)
{
    if (!hit) return;
    hit->beginDetailEvaluation();
    const Vector3Dd point = ray.getOrigin().add(ray.getDirection().multiply(t));
    if (hit->needsPoint()) { hit->point = point; hit->markDetailAvailable(RayHit::DETAIL_POINT); }
    if (hit->needsNormal()) {
        const Vector3Dd normal = evaluateFieldGradient(point);
        const double length = normal.length();
        if (Double::isFinite(length) && length > 0) {
            hit->normal = normal.multiply(1.0/length);
            hit->markDetailAvailable(RayHit::DETAIL_NORMAL);
        }
    }
}

int ImplicitSolid::doContainmentTest(const Vector3Dd& point, double tolerance)
{
    // Legacy integer adapter over the typed contract.
    const GeometryContainment::Value result = classifyPoint(point, tolerance);
    if (result == GeometryContainment::Inside) return INSIDE;
    if (result == GeometryContainment::Boundary) return LIMIT;
    if (result == GeometryContainment::Outside) return OUTSIDE;
    throw IllegalStateException("Implicit solid containment cannot be represented by the legacy integer result");
}

double* ImplicitSolid::getMinMax()
{
    GeometryBounds bounds;
    getBounds(bounds);
    if (bounds.getKind() == GeometryBounds::Kind::Finite)
        return newMinMax6(bounds.getMinimum().x(), bounds.getMinimum().y(), bounds.getMinimum().z(),
            bounds.getMaximum().x(), bounds.getMaximum().y(), bounds.getMaximum().z());
    return newMinMax6(-1e308,-1e308,-1e308,1e308,1e308,1e308);
}

//= Boundary representation =============================================

const double ImplicitSolid::DEFAULT_CLIPPING_HALF_SIZE = 3.0;

/**
C++ port note: the field given to `PolyhedralBoundedSolidFieldConstructions`
(`this::evaluateField` in Java).
*/
class _ImplicitSolidField : public ToDoubleFunction<Vector3Dd> {
private:
    const ImplicitSolid* solid;
public:
    _ImplicitSolidField(const ImplicitSolid* solid) : solid(solid) {}
    virtual double applyAsDouble(Vector3Dd point)
    {
        return solid->evaluateField(point);
    }
};

PolyhedralBoundedSolid* ImplicitSolid::exportToPolyhedralBoundedSolid()
{
    return exportToPolyhedralBoundedSolid(DEFAULT_POLYGONIZATION_DIVISIONS);
}

PolyhedralBoundedSolid* ImplicitSolid::exportToPolyhedralBoundedSolid(
    int divisions)
{
    GeometryBounds bounds;
    getBounds(bounds);
    if ( bounds.getKind() == GeometryBounds::Kind::Finite ) {
        _ImplicitSolidField field(this);
        return PolyhedralBoundedSolidFieldConstructions::createFromBoundedScalarField(
            field, bounds.getMinimum(), bounds.getMaximum(),
            Math::max(1, divisions));
    }
    if ( bounds.getKind() == GeometryBounds::Kind::Unbounded ) {
        double c = DEFAULT_CLIPPING_HALF_SIZE;
        return exportToPolyhedralBoundedSolid(divisions,
            Vector3Dd(-c, -c, -c), Vector3Dd(c, c, c));
    }
    return NULL;
}

PolyhedralBoundedSolid* ImplicitSolid::exportToPolyhedralBoundedSolid(
    int divisions, const Vector3Dd& minimum, const Vector3Dd& maximum)
{
    _ImplicitSolidField field(this);
    return PolyhedralBoundedSolidFieldConstructions::createFromScalarField(
        field, minimum, maximum, Math::max(1, divisions));
}

const double ImplicitSolid::ROOT_POLISH_REACH = 1.0e-3;
const double ImplicitSolid::ROOT_MERGE_TOLERANCE = 1.0e-12;

const Class ImplicitSolid::CLASS = { "ImplicitSolid", &Solid::CLASS };

const Class *
ImplicitSolid::getClass() const
{
    return &CLASS;
}
