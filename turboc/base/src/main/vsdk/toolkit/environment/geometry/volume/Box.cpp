#include <stddef.h>
#include <math.h>

#include "java/lang/Double.h"
#include "java/lang/Math.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/common/Cpp98Support.h"
#include "vsdk/toolkit/environment/geometry/element/GeometryBounds.h"
#include "vsdk/toolkit/environment/geometry/element/Ray.h"
#include "vsdk/toolkit/environment/geometry/element/RayHit.h"
#include "vsdk/toolkit/environment/geometry/element/RaySolidClassification.h"
#include "vsdk/toolkit/environment/geometry/volume/Box.h"
#include "vsdk/toolkit/common/numerics/FloatingInterval.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.h"
#include "vsdk/toolkit/common/Cpp98Support.h"
static const Vector3Dd NORMAL_POS_Z(0, 0, 1);
static const Vector3Dd NORMAL_NEG_Z(0, 0, -1);
static const Vector3Dd NORMAL_POS_Y(0, 1, 0);
static const Vector3Dd NORMAL_NEG_Y(0, -1, 0);
static const Vector3Dd NORMAL_POS_X(1, 0, 0);
static const Vector3Dd NORMAL_NEG_X(-1, 0, 0);
static const Vector3Dd TANGENT_POS_Y(0, 1, 0);
static const Vector3Dd TANGENT_NEG_Y(0, -1, 0);
static const Vector3Dd TANGENT_NEG_X(-1, 0, 0);
static const Vector3Dd TANGENT_POS_X(1, 0, 0);
static const Vector3Dd ZERO_VECTOR(0, 0, 0);

Box::Box(double dx, double dy, double dz)
{
    bounds[0]=Vector3Dd(-dx/2,-dy/2,-dz/2);
    bounds[1]=Vector3Dd(dx/2,dy/2,dz/2);
}
Box::Box(const Vector3Dd& s)
{
    bounds[0]=Vector3Dd(-s.x()/2,-s.y()/2,-s.z()/2);
    bounds[1]=Vector3Dd(s.x()/2,s.y()/2,s.z()/2);
}
Box::Box(const Vector3Dd& minimum, const Vector3Dd& maximum)
{
    bounds[0]=minimum;
    bounds[1]=maximum;
}

void Box::getBounds(GeometryBounds& result) const {
    const Vector3Dd size = getSize();
    if (!Double::isFinite(size.x()) || !Double::isFinite(size.y()) ||
        !Double::isFinite(size.z()) || size.x() <= 0 || size.y() <= 0 || size.z() <= 0) {
        result.setIndeterminate(); return;
    }
    result.setFinite(bounds[0], bounds[1]);
}

GeometryContainment::Value Box::classifyPoint(const Vector3Dd& point, double tolerance) const {
    if (Double::isFinite(tolerance) && tolerance > 0) return classifyPointCertified(point, tolerance);
    const double minimum[3] = {bounds[0].x(), bounds[0].y(), bounds[0].z()};
    const double maximum[3] = {bounds[1].x(), bounds[1].y(), bounds[1].z()};
    const double coordinates[3] = {point.x(), point.y(), point.z()};
    if (!Double::isFinite(tolerance) || tolerance < 0) return GeometryContainment::InvalidInput;
    BOOLEAN outside = FALSE;
    BOOLEAN onFace = FALSE;
    for (int i = 0; i < 3; ++i) {
        const double dimension = maximum[i] - minimum[i];
        if (!Double::isFinite(dimension) || dimension <= 0 ||
            !Double::isFinite(coordinates[i])) return GeometryContainment::InvalidInput;
        if (!Double::isFinite(minimum[i]) || !Double::isFinite(maximum[i]) ||
            dimension / 2 == 0) return GeometryContainment::Indeterminate;
        if (coordinates[i] < minimum[i] || coordinates[i] > maximum[i]) outside = TRUE;
        else if (coordinates[i] == minimum[i] || coordinates[i] == maximum[i]) onFace = TRUE;
    }
    if (outside) return GeometryContainment::Outside;
    return onFace ? GeometryContainment::Boundary : GeometryContainment::Inside;
}

GeometryContainment::Value Box::classifyBoxCertified(const Vector3Dd& center, const Vector3Dd& halfWidths) const {
    const double minimum[3] = {bounds[0].x(), bounds[0].y(), bounds[0].z()};
    const double maximum[3] = {bounds[1].x(), bounds[1].y(), bounds[1].z()};
    const double c[3] = {center.x(), center.y(), center.z()};
    const double h[3] = {halfWidths.x(), halfWidths.y(), halfWidths.z()};
    for (int i = 0; i < 3; ++i) {
        if (!Double::isFinite(c[i]) || !Double::isFinite(h[i]) || h[i] < 0)
            return GeometryContainment::InvalidInput;
        if (!Double::isFinite(minimum[i]) || !Double::isFinite(maximum[i]))
            return GeometryContainment::Indeterminate;
        if (!(minimum[i] < maximum[i])) return GeometryContainment::InvalidInput;
    }
    if (!FloatingInterval::isSupportedEnvironment()) return GeometryContainment::Unsupported;
    BOOLEAN inside = TRUE;
    for (int i = 0; i < 3; ++i) {
        const FloatingInterval range = FloatingInterval::around(c[i], h[i]);
        if (!range.isValid()) return GeometryContainment::Indeterminate;
        if (range.getUpper() < minimum[i] || range.getLower() > maximum[i]) return GeometryContainment::Outside;
        if (!(range.getLower() > minimum[i] && range.getUpper() < maximum[i])) inside = FALSE;
    }
    return inside ? GeometryContainment::Inside : GeometryContainment::Indeterminate;
}

GeometryContainment::Value Box::classifyPointCertified(const Vector3Dd& point, double tolerance) const {
    const double minimum[3] = {bounds[0].x(), bounds[0].y(), bounds[0].z()};
    const double maximum[3] = {bounds[1].x(), bounds[1].y(), bounds[1].z()};
    const double coordinates[3] = {point.x(), point.y(), point.z()};
    if (!Double::isFinite(tolerance) || tolerance < 0) return GeometryContainment::InvalidInput;
    for (int i = 0; i < 3; ++i) {
        if (!Double::isFinite(coordinates[i])) return GeometryContainment::InvalidInput;
        if (!Double::isFinite(minimum[i]) || !Double::isFinite(maximum[i]))
            return GeometryContainment::Indeterminate;
        if (!(minimum[i] < maximum[i])) return GeometryContainment::InvalidInput;
    }
    if (!FloatingInterval::isSupportedEnvironment()) return GeometryContainment::Unsupported;
    BOOLEAN inside = TRUE;
    for (int i = 0; i < 3; ++i)
        if (coordinates[i] < minimum[i] || coordinates[i] > maximum[i]) inside = FALSE;
    if (inside) {
        // The distance to the boundary is the smallest face distance.
        BOOLEAN deep = TRUE, near = FALSE;
        for (int i = 0; i < 3; ++i) {
            if (coordinates[i] == minimum[i] || coordinates[i] == maximum[i]) { near = TRUE; deep = FALSE; continue; }
            const FloatingInterval low = FloatingInterval::exact(coordinates[i]).subtract(FloatingInterval::exact(minimum[i]));
            const FloatingInterval high = FloatingInterval::exact(maximum[i]).subtract(FloatingInterval::exact(coordinates[i]));
            if (!low.isValid() || !high.isValid()) return GeometryContainment::Indeterminate;
            if (!(low.getLower() > tolerance) || !(high.getLower() > tolerance)) deep = FALSE;
            if (low.getUpper() <= tolerance || high.getUpper() <= tolerance) near = TRUE;
        }
        if (deep) return GeometryContainment::Inside;
        return near ? GeometryContainment::Boundary : GeometryContainment::Indeterminate;
    }
    FloatingInterval squared = FloatingInterval::exact(0);
    for (int i = 0; i < 3; ++i) {
        if (coordinates[i] < minimum[i])
            squared = squared.add(FloatingInterval::exact(minimum[i]).subtract(FloatingInterval::exact(coordinates[i])).square());
        else if (coordinates[i] > maximum[i])
            squared = squared.add(FloatingInterval::exact(coordinates[i]).subtract(FloatingInterval::exact(maximum[i])).square());
    }
    const FloatingInterval distance = squared.squareRoot();
    if (!distance.isValid()) return GeometryContainment::Indeterminate;
    if (distance.getLower() > tolerance) return GeometryContainment::Outside;
    return distance.getUpper() <= tolerance ? GeometryContainment::Boundary : GeometryContainment::Indeterminate;
}

// Slab intersection in the original affine ray parameterization. A ray that
// overlaps a face for an interval has no finite sequence of surface events.
RayFirstHitStatus::Value Box::clipRay(const Ray& ray, const RayCrossingQuery& query,
    double& entry, double& exit) const {
    const Vector3Dd size = getSize();
    const double dimensions[3] = {size.x(), size.y(), size.z()};
    const double origins[3] = {ray.getOrigin().x(), ray.getOrigin().y(), ray.getOrigin().z()};
    const double directions[3] = {ray.getDirection().x(), ray.getDirection().y(), ray.getDirection().z()};
    if (Double::isNaN(query.minT) || Double::isNaN(query.maxT) || query.minT > query.maxT ||
        !Double::isFinite(query.minimumChordSquared) || query.minimumChordSquared < 0 ||
        Double::isNaN(query.minimumExteriorApproach)) return RayFirstHitStatus::InvalidInput;
    if (query.minimumChordSquared != 0 ||
        query.minimumExteriorApproach != Double::NEGATIVE_INFINITY) return RayFirstHitStatus::Unsupported;
    if (directions[0] == 0 && directions[1] == 0 && directions[2] == 0) return RayFirstHitStatus::InvalidInput;
    for (int i = 0; i < 3; ++i) {
        if (!Double::isFinite(dimensions[i]) || dimensions[i] <= 0 ||
            !Double::isFinite(origins[i]) || !Double::isFinite(directions[i]))
            return RayFirstHitStatus::InvalidInput;
    }
    entry = Double::NEGATIVE_INFINITY;
    exit = Double::POSITIVE_INFINITY;
    const Vector3Dd half(size.x()/2, size.y()/2, size.z()/2);
    if (half.x() == 0 || half.y() == 0 || half.z() == 0)
        return RayFirstHitStatus::Indeterminate;
    BOOLEAN faceOverlap = FALSE;
    const RayFirstHitStatus::Value clipped = intersectBounds(ray.getOrigin(), ray.getDirection(),
        bounds[0], bounds[1], 0, TRUE, entry, exit, faceOverlap);
    if (clipped != RayFirstHitStatus::Hit) return clipped;
    if (exit < query.minT || entry > query.maxT) return RayFirstHitStatus::Miss;
    if (faceOverlap && entry < exit) return RayFirstHitStatus::Indeterminate;
    return RayFirstHitStatus::Hit;
}

RayCrossingSupport::Value Box::emitRayCrossings(const Ray& ray, const RayCrossingQuery& query,
    RayCrossingSink& sink) const {
    double entry, exit;
    const RayFirstHitStatus::Value status = clipRay(ray, query, entry, exit);
    if (status == RayFirstHitStatus::Miss) return RayCrossingSupport::Supported;
    if (status == RayFirstHitStatus::Unsupported) return RayCrossingSupport::Unsupported;
    if (status == RayFirstHitStatus::InvalidInput) return RayCrossingSupport::InvalidInput;
    if (status != RayFirstHitStatus::Hit) return RayCrossingSupport::Indeterminate;
    const Vector3Dd first = ray.getOrigin().add(ray.getDirection().multiply(entry));
    const Vector3Dd last = ray.getOrigin().add(ray.getDirection().multiply(exit));
    if (!Double::isFinite(first.x()) || !Double::isFinite(first.y()) ||
        !Double::isFinite(first.z()) || !Double::isFinite(last.x()) ||
        !Double::isFinite(last.y()) || !Double::isFinite(last.z()))
        return RayCrossingSupport::Indeterminate;
    if (entry >= query.minT && entry <= query.maxT)
        sink.crossing(RayCrossing(entry, this, entry == exit ? RayCrossingKind::Tangent : RayCrossingKind::Entering));
    if (exit != entry && exit >= query.minT && exit <= query.maxT)
        sink.crossing(RayCrossing(exit, this, RayCrossingKind::Leaving));
    return RayCrossingSupport::Supported;
}

RayFirstHitStatus::Value Box::queryFirstHit(const Ray& ray, const RayCrossingQuery& query,
    RayHit* result, RayBoundary* boundary) const {
    if (result) result->clear();
    if (boundary) *boundary = RayBoundary();
    double entry, exit;
    const RayFirstHitStatus::Value status = clipRay(ray, query, entry, exit);
    if (status != RayFirstHitStatus::Hit) return status;
    const double t = entry >= query.minT ? entry : exit;
    if (t > query.maxT) return RayFirstHitStatus::Miss;
    const Vector3Dd point = ray.getOrigin().add(ray.getDirection().multiply(t));
    if (!Double::isFinite(point.x()) || !Double::isFinite(point.y()) ||
        !Double::isFinite(point.z())) return RayFirstHitStatus::Indeterminate;
    if (boundary) *boundary = RayBoundary(t, this);
    if (result) {
        result->setHitDistance(t);
        if (result->shouldStoreRay()) result->setRay(ray.withT(t));
        if (result->needsAnySurfaceData()) ((Box*)(this))->doExtraInformation(ray, t, result);
    }
    return RayFirstHitStatus::Hit;
}

RayFirstHitStatus::Value Box::querySurfaceDetail(const Ray& ray, const RayBoundary& boundary,
    RayHit* hit) const {
    if (hit) hit->clear();
    if (!hit || boundary.geometry != this || !Double::isFinite(boundary.t))
        return RayFirstHitStatus::InvalidInput;
    if (boundary.getInstanceCount() != 0) return RayFirstHitStatus::Unsupported;
    const RayFirstHitStatus::Value status = queryFirstHit(ray, RayCrossingQuery(boundary.t, boundary.t), NULL);
    if (status != RayFirstHitStatus::Hit) return status;
    hit->setHitDistance(boundary.t);
    if (hit->shouldStoreRay()) hit->setRay(ray.withT(boundary.t));
    ((Box*)(this))->doExtraInformation(ray, boundary.t, hit);
    if (boundary.normalReversed && hit->needsNormal() &&
        (hit->getAvailableDetailMask() & RayHit::DETAIL_NORMAL))
        hit->normal = hit->normal.multiply(-1);
    return RayFirstHitStatus::Hit;
}

RaySolidQueryStatus::Value Box::classifyRay(const Ray& ray, const RayCrossingQuery& query,
    RaySolidClassification& result) const {
    result.clear();
    if (query.minT >= query.maxT) return RaySolidQueryStatus::InvalidInput;
    double entry, exit;
    const RayFirstHitStatus::Value status = clipRay(ray, query, entry, exit);
    if (status == RayFirstHitStatus::Unsupported) return RaySolidQueryStatus::Unsupported;
    if (status == RayFirstHitStatus::InvalidInput) return RaySolidQueryStatus::InvalidInput;
    if (status == RayFirstHitStatus::Indeterminate) return RaySolidQueryStatus::Indeterminate;
    const BOOLEAN intersectsInterior = status == RayFirstHitStatus::Hit && entry < exit;
    if (!result.insideSegments.add(intersectsInterior && query.minT > entry && query.minT <= exit))
        return RaySolidQueryStatus::Indeterminate;
    if (intersectsInterior && entry >= query.minT && entry <= query.maxT) {
        if (!result.boundaries.add(RaySolidBoundary(entry, this)) || !result.insideSegments.add(TRUE)) {
            result.clear(); return RaySolidQueryStatus::Indeterminate;
        }
    }
    if (intersectsInterior && exit >= query.minT && exit <= query.maxT) {
        if (!result.boundaries.add(RaySolidBoundary(exit, this)) || !result.insideSegments.add(FALSE)) {
            result.clear(); return RaySolidQueryStatus::Indeterminate;
        }
    }
    return RaySolidQueryStatus::Supported;
}

Ray* Box::doIntersectionFirstHit(const Ray& inOutRay) {
    RayHit hit;
    if (doIntersectionFirstHit(inOutRay, &hit) && hit.getRay() != NULL) {
        return new Ray(*hit.getRay());
    }
    return NULL;
}

BOOLEAN Box::doIntersectionFirstHit(const Ray& inRay, RayHit* outHit) {
    double minT = 1e308;
    int hitPlane = 0;
    const Vector3Dd size = getSize();
    const Vector3Dd center = getCenter();
    double x2 = size.x()/2, y2 = size.y()/2, z2 = size.z()/2;
    double ox = inRay.getOrigin().x()-center.x(), oy = inRay.getOrigin().y()-center.y(), oz = inRay.getOrigin().z()-center.z();
    double dx = inRay.getDirection().x(), dy = inRay.getDirection().y(), dz = inRay.getDirection().z();

    if (Cpp98Support::abs(dz) > VSDK::EPSILON) {
        double t = (z2 - oz) / dz;
        if (t > -VSDK::EPSILON) {
            double cx = ox + dx*t, cy = oy + dy*t;
            if (cx >= -x2 && cx <= x2 && cy >= -y2 && cy <= y2) { minT = t; hitPlane = 1; }
        }
    }
    if (Cpp98Support::abs(dz) > VSDK::EPSILON) {
        double t = (-z2 - oz) / dz;
        if (t > -VSDK::EPSILON && t < minT) {
            double cx = ox + dx*t, cy = oy + dy*t;
            if (cx >= -x2 && cx <= x2 && cy >= -y2 && cy <= y2) { minT = t; hitPlane = 2; }
        }
    }
    if (Cpp98Support::abs(dy) > VSDK::EPSILON) {
        double t = (y2 - oy) / dy;
        if (t > -VSDK::EPSILON && t < minT) {
            double cx = ox + dx*t, cz = oz + dz*t;
            if (cx >= -x2 && cx <= x2 && cz >= -z2 && cz <= z2) { minT = t; hitPlane = 3; }
        }
    }
    if (Cpp98Support::abs(dy) > VSDK::EPSILON) {
        double t = (-y2 - oy) / dy;
        if (t > -VSDK::EPSILON && t < minT) {
            double cx = ox + dx*t, cz = oz + dz*t;
            if (cx >= -x2 && cx <= x2 && cz >= -z2 && cz <= z2) { minT = t; hitPlane = 4; }
        }
    }
    if (Cpp98Support::abs(dx) > VSDK::EPSILON) {
        double t = (x2 - ox) / dx;
        if (t > -VSDK::EPSILON && t < minT) {
            double cy = oy + dy*t, cz = oz + dz*t;
            if (cy >= -y2 && cy <= y2 && cz >= -z2 && cz <= z2) { minT = t; hitPlane = 5; }
        }
    }
    if (Cpp98Support::abs(dx) > VSDK::EPSILON) {
        double t = (-x2 - ox) / dx;
        if (t > -VSDK::EPSILON && t < minT) {
            double cy = oy + dy*t, cz = oz + dz*t;
            if (cy >= -y2 && cy <= y2 && cz >= -z2 && cz <= z2) { minT = t; hitPlane = 6; }
        }
    }

    if (minT == 1e308) return FALSE;
    if (outHit == NULL) return TRUE;

    if (outHit->shouldStoreRay() || outHit->needsAnySurfaceData()) outHit->setRay(inRay.withT(minT));
    else outHit->setHitDistance(minT);

    if (outHit->needsAnySurfaceData()) {
        double hitX = ox + dx*minT, hitY = oy + dy*minT, hitZ = oz + dz*minT;
        if (outHit->needsPoint()) outHit->point = Vector3Dd(hitX, hitY, hitZ).add(center);
        if (outHit->needsTextureCoordinates()) {
            outHit->u = 0; outHit->v = 0;
            switch (hitPlane) {
                case 1: outHit->u = hitY / size.y() - 0.5; outHit->v = 1-(hitX / size.x() - 0.5); break;
                case 2: outHit->u = hitY / size.y() - 0.5; outHit->v = hitX / size.x() - 0.5; break;
                case 3: outHit->u = 1-(hitX / size.x() - 0.5); outHit->v = hitZ / size.z() - 0.5; break;
                case 4: outHit->u = hitX / size.x() - 0.5; outHit->v = hitZ / size.z() - 0.5; break;
                case 5: outHit->u = hitY / size.y() - 0.5; outHit->v = hitZ / size.z() - 0.5; break;
                case 6: outHit->u = 1-(hitY / size.y() - 0.5); outHit->v = hitZ / size.z() - 0.5; break;
            }
        }
        if (outHit->needsNormal()) outHit->normal = planeNormal(hitPlane);
        if (outHit->needsTangent()) outHit->tangent = planeTangent(hitPlane);
    }
    return TRUE;
}

void Box::doExtraInformation(const Ray& inRay, double inT, RayHit* outData) {
    if (outData == NULL) return;
    outData->beginDetailEvaluation();
    double hitX = inRay.getOrigin().x() + inRay.getDirection().x()*inT;
    double hitY = inRay.getOrigin().y() + inRay.getDirection().y()*inT;
    double hitZ = inRay.getOrigin().z() + inRay.getDirection().z()*inT;
    const Vector3Dd center = getCenter();
    int hitPlane = classifyHitPlane(hitX-center.x(), hitY-center.y(), hitZ-center.z());
    if (outData->needsPoint()) {
        outData->point = Vector3Dd(hitX, hitY, hitZ);
        outData->markDetailAvailable(RayHit::DETAIL_POINT);
    }
    if (outData->needsNormal()) {
        outData->normal = planeNormal(hitPlane);
        outData->markDetailAvailable(RayHit::DETAIL_NORMAL);
    }
    if (outData->needsTangent()) {
        outData->tangent = planeTangent(hitPlane);
        outData->markDetailAvailable(RayHit::DETAIL_TANGENT);
    }
}

Vector3Dd Box::planeNormal(int h) { switch (h){case 1:return NORMAL_POS_Z;case 2:return NORMAL_NEG_Z;case 3:return NORMAL_POS_Y;case 4:return NORMAL_NEG_Y;case 5:return NORMAL_POS_X;case 6:return NORMAL_NEG_X;default:return ZERO_VECTOR;}}
Vector3Dd Box::planeTangent(int h) { switch (h){case 1:case 2:case 5:return TANGENT_POS_Y;case 3:return TANGENT_NEG_X;case 4:return TANGENT_POS_X;case 6:return TANGENT_NEG_Y;default:return ZERO_VECTOR;}}

int Box::classifyHitPlane(double x, double y, double z) const {
    const Vector3Dd size = getSize();
    double x2=size.x()/2,y2=size.y()/2,z2=size.z()/2;
    double dxPlus=Cpp98Support::abs(x-x2),dxMinus=Cpp98Support::abs(x+x2),dyPlus=Cpp98Support::abs(y-y2),dyMinus=Cpp98Support::abs(y+y2),dzPlus=Cpp98Support::abs(z-z2),dzMinus=Cpp98Support::abs(z+z2);
    double min=dzPlus; int plane=1;
    if(dzMinus<min){min=dzMinus;plane=2;} if(dyPlus<min){min=dyPlus;plane=3;} if(dyMinus<min){min=dyMinus;plane=4;} if(dxPlus<min){min=dxPlus;plane=5;} if(dxMinus<min){plane=6;} return plane;
}

/**
Check the general interface contract in superclass method
Geometry.doContainmentTest. The box is centered at the origin, with its
faces perpendicular to the axes.
@param p point to classify, in the space of the box
@param distanceTolerance distance to the faces of the box under which
the point is classified as `LIMIT`
@return INSIDE, OUTSIDE or LIMIT constant value
*/
int Box::doContainmentTest(const Vector3Dd& p, double distanceTolerance)
{
    const Vector3Dd size = getSize();
    const Vector3Dd local = p.subtract(getCenter());
    double half[3] = { size.x() / 2, size.y() / 2, size.z() / 2 };
    double coordinates[3] = { local.x(), local.y(), local.z() };
    BOOLEAN inside = TRUE;

    for ( int i = 0; i < 3; i++ ) {
        double distance = Cpp98Support::abs(coordinates[i]);
        if ( distance > half[i] + distanceTolerance ) {
            return OUTSIDE;
        }
        if ( distance >= half[i] - distanceTolerance ) {
            inside = FALSE;
        }
    }
    return inside ? INSIDE : LIMIT;
}

double* Box::getMinMax() { double* m=new double[6]; m[0]=bounds[0].x(); m[1]=bounds[0].y(); m[2]=bounds[0].z(); m[3]=bounds[1].x(); m[4]=bounds[1].y(); m[5]=bounds[1].z(); return m; }
void Box::setSize(double dx,double dy,double dz){ setSize(Vector3Dd(dx,dy,dz)); }
void Box::setSize(const Vector3Dd& s){
    const Vector3Dd center = getCenter();
    const Vector3Dd half = s.multiply(0.5);
    bounds[0] = center.subtract(half); bounds[1] = center.add(half);
}

PolyhedralBoundedSolid* Box::exportToPolyhedralBoundedSolid()
{
    return buildPolyhedralBoundedSolid();
}

PolyhedralBoundedSolid* Box::buildPolyhedralBoundedSolid()
{
    PolyhedralBoundedSolid* solid = new PolyhedralBoundedSolid();
    PolyhedralBoundedSolidEulerOperators::mvfs(
        solid, Vector3Dd(bounds[0].x(), bounds[0].y(), bounds[0].z()), 1, 1);
    PolyhedralBoundedSolidEulerOperators::smev(
        solid, 1, 1, 4, Vector3Dd(bounds[0].x(), bounds[1].y(), bounds[0].z()));
    PolyhedralBoundedSolidEulerOperators::smev(
        solid, 1, 4, 3, Vector3Dd(bounds[1].x(), bounds[1].y(), bounds[0].z()));
    PolyhedralBoundedSolidEulerOperators::smev(
        solid, 1, 3, 2, Vector3Dd(bounds[1].x(), bounds[0].y(), bounds[0].z()));
    PolyhedralBoundedSolidEulerOperators::mef(
        solid, 1, 1, 1, 4, 2, 3, 2);

    PolyhedralBoundedSolidEulerOperators::smev(
        solid, 1, 1, 5, Vector3Dd(bounds[0].x(), bounds[0].y(), bounds[1].z()));
    PolyhedralBoundedSolidEulerOperators::smev(
        solid, 1, 2, 6, Vector3Dd(bounds[1].x(), bounds[0].y(), bounds[1].z()));
    PolyhedralBoundedSolidEulerOperators::mef(
        solid, 1, 1, 5, 1, 6, 2, 3);
    PolyhedralBoundedSolidEulerOperators::smev(
        solid, 1, 3, 7, Vector3Dd(bounds[1].x(), bounds[1].y(), bounds[1].z()));
    PolyhedralBoundedSolidEulerOperators::mef(
        solid, 1, 1, 6, 2, 7, 3, 4);
    PolyhedralBoundedSolidEulerOperators::smev(
        solid, 1, 4, 8, Vector3Dd(bounds[0].x(), bounds[1].y(), bounds[1].z()));
    PolyhedralBoundedSolidEulerOperators::mef(
        solid, 1, 1, 7, 3, 8, 4, 5);
    PolyhedralBoundedSolidEulerOperators::mef(
        solid, 1, 1, 5, 6, 8, 4, 6);
    return solid;
}

const Class Box::CLASS = { "Box", &Solid::CLASS };

const Class *
Box::getClass() const
{
    return &CLASS;
}
