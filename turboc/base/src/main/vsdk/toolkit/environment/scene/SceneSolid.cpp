#include "vsdk/toolkit/common/VSDK.h"
#include <float.h>
#include <math.h>

#include "java/lang/String.h"
#include "java/lang/Double.h"
#include "java/lang/Math.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/numerics/FloatingInterval.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/geometry/element/GeometryBounds.h"
#include "vsdk/toolkit/environment/geometry/element/Ray.h"
#include "vsdk/toolkit/environment/geometry/element/RaySolidClassification.h"
#include "vsdk/toolkit/environment/geometry/volume/AffineEllipsoidDistance.h"
#include "vsdk/toolkit/environment/geometry/volume/AffineEllipsoidDistanceBounds.h"
#include "vsdk/toolkit/environment/geometry/volume/AffineEllipsoidMinimumDistance.h"
#include "vsdk/toolkit/environment/geometry/volume/Solid.h"
#include "vsdk/toolkit/environment/geometry/volume/Sphere.h"
#include "vsdk/toolkit/environment/scene/AffinePlacement.h"
#include "vsdk/toolkit/environment/scene/AffineToleranceMap.h"
#include "vsdk/toolkit/environment/scene/InstanceRayCrossingBuffer.h"
#include "vsdk/toolkit/environment/scene/SceneSolid.h"

SceneSolid::SceneSolid()
    : geometry(0), ownsGeometry(TRUE), geometryIsSphere(FALSE), name(""), modificationVersion(0)
{
}

SceneSolid::~SceneSolid()
{
    if ( geometry != 0 && ownsGeometry ) {
        delete geometry;
    }
    geometry = 0;
}

BOOLEAN SceneSolid::denotesPlacedGeometry() const { return FALSE; }

BOOLEAN SceneSolid::isStructuralTraversalSafe() const { return denotesPlacedGeometry(); }

BOOLEAN SceneSolid::queryDenotedSolid(const Geometry*& intrinsic, BOOLEAN& inverted) const
{
    intrinsic = NULL; inverted = FALSE;
    if (!denotesPlacedGeometry() || geometry == NULL) return FALSE;
    intrinsic = geometry;
    return TRUE;
}

GeometryContainment::Value SceneSolid::orientSide(GeometryContainment::Value side, BOOLEAN inverted)
{
    if (inverted && side==GeometryContainment::Inside) return GeometryContainment::Outside;
    if (inverted && side==GeometryContainment::Outside) return GeometryContainment::Inside;
    return side;
}

GeometryDistanceStatus::Value SceneSolid::queryDenotedSphere(const Sphere*& sphere, BOOLEAN& inverted,
    Matrix4x4d& forward, Matrix4x4d& inverse) const
{
    const Geometry* intrinsic=NULL;
    if (!queryDenotedSolid(intrinsic,inverted)) return GeometryDistanceStatus::Unsupported;
    sphere=(intrinsic!=NULL && intrinsic->instanceOf(Sphere::CLASS)) ? (const Sphere*)intrinsic : (const Sphere*)NULL;
    if (sphere==NULL) return GeometryDistanceStatus::Unsupported;
    const AffinePlacementStatus::Value status=queryAffinePlacement(forward,inverse);
    if (status==AffinePlacementStatus::Invalid) return GeometryDistanceStatus::InvalidInput;
    if (status==AffinePlacementStatus::Unsupported) return GeometryDistanceStatus::Unsupported;
    if (status==AffinePlacementStatus::Indeterminate) return GeometryDistanceStatus::Indeterminate;
    return GeometryDistanceStatus::Valid;
}
BOOLEAN SceneSolid::hasSphereGeometry() const { return geometryIsSphere; }
Material* SceneSolid::getSurfaceMaterial() const { return NULL; }

void SceneSolid::getWorldBounds(GeometryBounds& result) const
{
    Geometry* shape = getGeometry();
    if (shape == NULL) { result.setEmpty(); return; }
    GeometryBounds local;
    shape->getBounds(local);
    result = local.transformed(getAffinePlacement());
}

void SceneSolid::getSolidWorldBounds(GeometryBounds& result) const
{
    if (!denotesPlacedGeometry() || getGeometry()==NULL || !getGeometry()->instanceOf(Solid::CLASS)) {
        result.setUnsupported();
        return;
    }
    getWorldBounds(result);
}

GeometryContainment::Value SceneSolid::classifyWorldBoxCertified(const GeometryBounds& box) const
{
    const Geometry* intrinsic=NULL; BOOLEAN inverted=FALSE;
    if (!queryDenotedSolid(intrinsic,inverted)) return GeometryContainment::Unsupported;
    Matrix4x4d forward,inverse;
    const AffinePlacementStatus::Value placement=queryAffinePlacement(forward,inverse);
    if (placement==AffinePlacementStatus::Invalid) return GeometryContainment::InvalidInput;
    if (placement==AffinePlacementStatus::Unsupported) return GeometryContainment::Unsupported;
    if (placement==AffinePlacementStatus::Indeterminate) return GeometryContainment::Indeterminate;
    return classifyPlacedBoxCertified(*intrinsic,inverted,inverse,box);
}

GeometryContainment::Value SceneSolid::classifyPlacedBoxCertified(const Geometry& geometry, BOOLEAN inverted,
    const Matrix4x4d& inverse, const GeometryBounds& box)
{
    if (box.getKind()==GeometryBounds::Kind::Unsupported) return GeometryContainment::Unsupported;
    if (box.getKind()!=GeometryBounds::Kind::Finite) return GeometryContainment::Indeterminate;
    // The conservative local bounds of the box's preimage, as centre and
    // outward-rounded half widths.
    const GeometryBounds local=box.transformed(inverse);
    if (local.getKind()!=GeometryBounds::Kind::Finite) return GeometryContainment::Indeterminate;
    const double lo[3]={local.getMinimum().x(),local.getMinimum().y(),local.getMinimum().z()};
    const double hi[3]={local.getMaximum().x(),local.getMaximum().y(),local.getMaximum().z()};
    double centre[3], half[3];
    for (int i=0;i<3;++i) {
        centre[i]=lo[i]*0.5+hi[i]*0.5;
        const FloatingInterval below=FloatingInterval::exact(centre[i]).subtract(FloatingInterval::exact(lo[i]));
        const FloatingInterval above=FloatingInterval::exact(hi[i]).subtract(FloatingInterval::exact(centre[i]));
        if (!Double::isFinite(centre[i]) || !below.isValid() || !above.isValid())
            return GeometryContainment::Indeterminate;
        half[i]=Math::max(Math::max(below.getUpper(),above.getUpper()),0.0);
    }
    const GeometryContainment::Value result=geometry.classifyBoxCertified(Vector3Dd(centre[0],centre[1],centre[2]),
        Vector3Dd(half[0],half[1],half[2]));
    if (inverted && result==GeometryContainment::Inside) return GeometryContainment::Outside;
    if (inverted && result==GeometryContainment::Outside) return GeometryContainment::Inside;
    return result;
}

RayCrossingSupport::Value SceneSolid::emitRayCrossings(const Ray& ray,
    const RayCrossingQuery& query, RayCrossingSink& sink) const
{
    if (geometry == NULL) return RayCrossingSupport::Unsupported;
    Matrix4x4d forward, inverse;
    const AffinePlacementStatus::Value status = queryAffinePlacement(forward, inverse);
    if (status == AffinePlacementStatus::Unsupported) return RayCrossingSupport::Unsupported;
    if (status == AffinePlacementStatus::Invalid) return RayCrossingSupport::InvalidInput;
    if (status == AffinePlacementStatus::Indeterminate) return RayCrossingSupport::Indeterminate;
    const Vector3Dd origin = inverse.multiply(ray.getOrigin());
    const Vector3Dd direction = inverse.withoutTranslation().multiply(ray.getDirection());
    if (!Double::isFinite(origin.x()) || !Double::isFinite(origin.y()) ||
        !Double::isFinite(origin.z()) || !Double::isFinite(direction.x()) ||
        !Double::isFinite(direction.y()) || !Double::isFinite(direction.z()))
        return RayCrossingSupport::Indeterminate;
    const Ray local = Ray::fromAffine(origin, direction, ray.getT());
    InstanceRayCrossingBuffer buffer(RayInstanceStep(this,forward,inverse), this, getSurfaceMaterial());
    const RayCrossingSupport::Value result = geometry->emitRayCrossings(local,query,buffer);
    if (result != RayCrossingSupport::Supported) return result;
    if (!buffer.isValid()) return RayCrossingSupport::Indeterminate;
    buffer.deliver(sink);
    return RayCrossingSupport::Supported;
}

RaySolidQueryStatus::Value SceneSolid::classifyRay(const Ray& ray, const RayCrossingQuery& query,
    RaySolidClassification& result) const
{
    result.clear();
    if (geometry == NULL) return RaySolidQueryStatus::Unsupported;
    Matrix4x4d childToParent, parentToChild;
    const AffinePlacementStatus::Value placement = queryAffinePlacement(childToParent, parentToChild);
    if (placement == AffinePlacementStatus::Unsupported) return RaySolidQueryStatus::Unsupported;
    if (placement == AffinePlacementStatus::Invalid) return RaySolidQueryStatus::InvalidInput;
    if (placement == AffinePlacementStatus::Indeterminate) return RaySolidQueryStatus::Indeterminate;
    const Vector3Dd& origin = ray.getOrigin();
    const Vector3Dd& direction = ray.getDirection();
    if (!Double::isFinite(origin.x()) || !Double::isFinite(origin.y()) ||
        !Double::isFinite(origin.z()) || !Double::isFinite(direction.x()) ||
        !Double::isFinite(direction.y()) || !Double::isFinite(direction.z()))
        return RaySolidQueryStatus::InvalidInput;
    for (int row = 0; row < 4; row++) {
        for (int column = 0; column < 4; column++) {
            if (!Double::isFinite(childToParent.get(row,column)) ||
                !Double::isFinite(parentToChild.get(row,column))) return RaySolidQueryStatus::Indeterminate;
        }
    }
    const Vector3Dd localOrigin = parentToChild.multiply(origin);
    const Vector3Dd localDirection = parentToChild.withoutTranslation().multiply(direction);
    if (!Double::isFinite(localOrigin.x()) || !Double::isFinite(localOrigin.y()) ||
        !Double::isFinite(localOrigin.z()) || !Double::isFinite(localDirection.x()) ||
        !Double::isFinite(localDirection.y()) || !Double::isFinite(localDirection.z()))
        return RaySolidQueryStatus::Indeterminate;
    const Ray localRay = Ray::fromAffine(localOrigin, localDirection, ray.getT());
    const RaySolidQueryStatus::Value status = geometry->classifyRay(localRay, query, result);
    if (status != RaySolidQueryStatus::Supported) { result.clear(); return status; }
    const RayInstanceStep step(this, childToParent, parentToChild);
    for (long i = 0; i < result.boundaries.size(); i++) {
        if (!result.boundaries[i].appendInstance(step)) {
            result.clear();
            return RaySolidQueryStatus::Indeterminate;
        }
    }
    return status;
}

GeometryDistanceStatus::Value SceneSolid::encloseMinimumBoundaryDistance(const Vector3Dd& point,
    double maxWidth, GeometryContainment::Value& side, double& lower, double& upper) const
{
    const Sphere* sphere=NULL; BOOLEAN inverted=FALSE;
    Matrix4x4d forward,inverse;
    const GeometryDistanceStatus::Value availability=queryDenotedSphere(sphere,inverted,forward,inverse);
    if (availability!=GeometryDistanceStatus::Valid) return availability;
    GeometryContainment::Value candidateSide=GeometryContainment::Indeterminate;
    double candidateLower=0,candidateUpper=0;
    const GeometryDistanceStatus::Value status=AffineEllipsoidMinimumDistance::query(point,sphere->getRadius(),
        forward,inverse,maxWidth,candidateSide,candidateLower,candidateUpper);
    if (status!=GeometryDistanceStatus::Valid) return status;
    side=orientSide(candidateSide,inverted); lower=candidateLower; upper=candidateUpper;
    return GeometryDistanceStatus::Valid;
}

GeometryDistanceStatus::Value SceneSolid::encloseBoundaryDistance(const Vector3Dd& point,
    const Vector3Dd& localDirection, GeometryContainment::Value& side, double& lower, double& upper) const
{
    const Sphere* sphere=NULL; BOOLEAN inverted=FALSE;
    Matrix4x4d forward,inverse;
    const GeometryDistanceStatus::Value availability=queryDenotedSphere(sphere,inverted,forward,inverse);
    if (availability!=GeometryDistanceStatus::Valid) return availability;
    GeometryContainment::Value candidateSide=GeometryContainment::Indeterminate;
    double candidateLower=0,candidateUpper=0;
    const GeometryDistanceStatus::Value status=AffineEllipsoidDistanceBounds::query(point,sphere->getRadius(),
        forward,localDirection,candidateSide,candidateLower,candidateUpper);
    if (status!=GeometryDistanceStatus::Valid) return status;
    side=orientSide(candidateSide,inverted); lower=candidateLower; upper=candidateUpper;
    return GeometryDistanceStatus::Valid;
}

const Geometry* SceneSolid::getSolidEquivalenceGeometry() const
{
    const Geometry* intrinsic=NULL; BOOLEAN inverted=FALSE;
    return queryDenotedSolid(intrinsic,inverted) && !inverted ? intrinsic : (const Geometry*)NULL;
}

GeometryContainment::Value SceneSolid::classifyPointCertified(const Vector3Dd& point, double tolerance) const
{
    if (!Double::isFinite(point.x()) || !Double::isFinite(point.y()) ||
        !Double::isFinite(point.z()) || !Double::isFinite(tolerance) || tolerance<0)
        return GeometryContainment::InvalidInput;
    if (geometry==NULL) return GeometryContainment::Unsupported;
    Matrix4x4d forward,inverse;
    const AffinePlacementStatus::Value placement=queryAffinePlacement(forward,inverse);
    if (placement==AffinePlacementStatus::Invalid) return GeometryContainment::InvalidInput;
    if (placement==AffinePlacementStatus::Unsupported) return GeometryContainment::Unsupported;
    if (placement==AffinePlacementStatus::Indeterminate) return GeometryContainment::Indeterminate;
    return classifyPlacedPointCertified(*geometry,FALSE,forward,inverse,point,tolerance);
}

GeometryContainment::Value SceneSolid::classifyPlacedPointCertified(const Geometry& geometry, BOOLEAN inverted,
    const Matrix4x4d& forward, const Matrix4x4d& inverse, const Vector3Dd& point, double tolerance)
{
    if (!Double::isFinite(point.x()) || !Double::isFinite(point.y()) ||
        !Double::isFinite(point.z()) || !Double::isFinite(tolerance) || tolerance<0)
        return GeometryContainment::InvalidInput;
    const Sphere* sphere=geometry.instanceOf(Sphere::CLASS) ? (const Sphere*)&geometry : (const Sphere*)NULL;
    const GeometryContainment::Value result=sphere ?
        classifyPlacedSphereCertified(sphere->getRadius(),forward,inverse,point,tolerance,128) :
        classifyPlacedSolidCertified(geometry,forward,point,tolerance);
    if (inverted && result==GeometryContainment::Inside) return GeometryContainment::Outside;
    if (inverted && result==GeometryContainment::Outside) return GeometryContainment::Inside;
    return result;
}

GeometryContainment::Value SceneSolid::classifyPlacedSolidCertified(const Geometry& geometry,
    const Matrix4x4d& forward, const Vector3Dd& point, double tolerance)
{
    BOOLEAN identity=TRUE;
    for (int row=0;row<4;++row) for (int column=0;column<4;++column)
        if (forward.get(row,column)!=(row==column ? 1.0 : 0.0)) identity=FALSE;
    if (identity) return geometry.classifyPointCertified(point,tolerance);
    Vector3Dd local, halfWidths;
    double outer=0,inner=-1;
    const AffinePlacementStatus::Value mapped=AffineToleranceMap::map(forward,point,tolerance,local,outer,&inner,&halfWidths);
    if (mapped==AffinePlacementStatus::Invalid) return GeometryContainment::InvalidInput;
    if (mapped==AffinePlacementStatus::Unsupported) return GeometryContainment::Unsupported;
    if (mapped!=AffinePlacementStatus::Valid) return GeometryContainment::Indeterminate;
    // The axis-aligned local box also contains the preimage and is tighter
    // under anisotropic placement; it can only prove Inside or Outside.
    const GeometryContainment::Value region=geometry.classifyBoxCertified(local,halfWidths);
    if (region==GeometryContainment::Inside || region==GeometryContainment::Outside ||
        region==GeometryContainment::InvalidInput) return region;
    // The outer local ball contains the preimage of the world ball, so a
    // definite side for it is a definite world side. A zero outer radius means
    // the preimage is exactly the local point.
    const GeometryContainment::Value result=geometry.classifyPointCertified(local,outer);
    if (result==GeometryContainment::Inside || result==GeometryContainment::Outside ||
        result==GeometryContainment::InvalidInput || result==GeometryContainment::Unsupported) return result;
    if (result==GeometryContainment::Boundary && outer==0) return result;
    // A boundary inside the inner local ball maps inside the world ball.
    if (inner>0) {
        const GeometryContainment::Value near=geometry.classifyPointCertified(local,inner);
        if (near==GeometryContainment::Boundary || near==GeometryContainment::InvalidInput ||
            near==GeometryContainment::Unsupported) return near;
    }
    if (!(tolerance>0)) return GeometryContainment::Indeterminate;
    // Under anisotropic placement the inner ball is small. Two world points of
    // the (convex) tolerance ball with certified opposite membership prove a
    // boundary point on the segment joining them.
    const GeometryContainment::Value centre=classifyPlacedSolidCertified(geometry,forward,point,0);
    int centreSign=centre==GeometryContainment::Inside ? -1 : centre==GeometryContainment::Outside ? 1 : 0;
    BOOLEAN foundInside=centreSign<0, foundOutside=centreSign>0;
    const double step=tolerance*(1.0-1.0/1048576.0);
    const double diagonal=step/Math::sqrt(3.0)*(1.0-1.0/1048576.0);
    for (int direction=0;direction<14;++direction) {
        Vector3Dd offset;
        if (direction<6) {
            const double sign=direction%2==0 ? 1.0 : -1.0;
            offset=Vector3Dd(direction/2==0 ? sign*step : 0, direction/2==1 ? sign*step : 0,
                direction/2==2 ? sign*step : 0);
        } else {
            const int corner=direction-6;
            offset=Vector3Dd(corner&1 ? -diagonal : diagonal, corner&2 ? -diagonal : diagonal,
                corner&4 ? -diagonal : diagonal);
        }
        const Vector3Dd sample=point.add(offset);
        FloatingInterval squared=FloatingInterval::exact(0);
        squared=squared.add(FloatingInterval::exact(sample.x()).subtract(FloatingInterval::exact(point.x())).square());
        squared=squared.add(FloatingInterval::exact(sample.y()).subtract(FloatingInterval::exact(point.y())).square());
        squared=squared.add(FloatingInterval::exact(sample.z()).subtract(FloatingInterval::exact(point.z())).square());
        const FloatingInterval distance=squared.squareRoot();
        if (!distance.isValid() || distance.getUpper()>tolerance) continue;
        const GeometryContainment::Value side=classifyPlacedSolidCertified(geometry,forward,sample,0);
        if (side==GeometryContainment::Inside) foundInside=TRUE;
        else if (side==GeometryContainment::Outside) foundOutside=TRUE;
        else if (side==GeometryContainment::Boundary) return GeometryContainment::Boundary;
        if (foundInside && foundOutside) return GeometryContainment::Boundary;
    }
    return GeometryContainment::Indeterminate;
}

GeometryContainment::Value SceneSolid::classifyPlacedSphereCertified(double radius,
    const Matrix4x4d& forward, const Matrix4x4d& inverse, const Vector3Dd& point,
    double tolerance, int iterationLimit)
{
    Vector3Dd seed=inverse.multiply(point);
    if (!Double::isFinite(seed.x()) || !Double::isFinite(seed.y()) ||
        !Double::isFinite(seed.z())) return GeometryContainment::Indeterminate;
    const BOOLEAN center=(point.x()==forward.get(0,3) && point.y()==forward.get(1,3) && point.z()==forward.get(2,3)) ||
        (seed.x()==0 && seed.y()==0 && seed.z()==0);
    const Vector3Dd axes[]={Vector3Dd(1,0,0),Vector3Dd(0,1,0),Vector3Dd(0,0,1)};
    double forwardNormSquared=0;
    for (int i=0;i<3;++i) for (int j=0;j<3;++j)
        forwardNormSquared+=forward.get(i,j)*forward.get(i,j);
    const double lipschitz=radius*radius*forwardNormSquared;
    const int seedCount=center ? 3 : 1;
    for (int candidate=0;candidate<seedCount;++candidate) {
      if (center) seed=axes[candidate];
      for (int iteration=0;iteration<iterationLimit;++iteration) {
        if (iteration==0 || iteration%4==0 || iteration+1==iterationLimit) {
            GeometryContainment::Value side=GeometryContainment::Indeterminate;
            double lower=0,upper=0;
            const GeometryDistanceStatus::Value status=AffineEllipsoidDistanceBounds::query(point,
                radius,forward,seed,side,lower,upper,iteration==0 && candidate==0);
            if (status==GeometryDistanceStatus::InvalidInput) return GeometryContainment::InvalidInput;
            if (status==GeometryDistanceStatus::Unsupported) return GeometryContainment::Unsupported;
            if (status!=GeometryDistanceStatus::Valid) return GeometryContainment::Indeterminate;
            if (upper<=tolerance) return GeometryContainment::Boundary;
            if (lower>tolerance) return side;
        }
        if (!(lipschitz>0) || !Double::isFinite(lipschitz)) break;
        const double seedLength=seed.length();
        if (!(seedLength>0) || !Double::isFinite(seedLength)) break;
        const Vector3Dd unit=seed.multiply(1.0/seedLength);
        const Vector3Dd delta=forward.multiply(unit.multiply(radius)).subtract(point);
        const double components[3]={delta.x(),delta.y(),delta.z()};
        double gradient[3]={0,0,0};
        for (int i=0;i<3;++i) for (int j=0;j<3;++j)
            gradient[i]+=radius*forward.get(j,i)*components[j];
        const Vector3Dd g(gradient[0],gradient[1],gradient[2]);
        const Vector3Dd tangent=g.subtract(unit.multiply(g.dotProduct(unit)));
        if (tangent.dotProduct(tangent)==0) break;
        seed=unit.subtract(tangent.multiply(1.0/lipschitz));
        if (!Double::isFinite(seed.x()) || !Double::isFinite(seed.y()) ||
            !Double::isFinite(seed.z())) break;
      }
    }
    // If surface-directed search cannot decide, use a separately certified
    // minimum-distance enclosure. The numerical estimate only proposes its
    // witness; interval bounds, never the estimate, determine this result.
    const double requestedWidth=tolerance*1e-3;
    if (requestedWidth>0 && Double::isFinite(requestedWidth)) {
        GeometryContainment::Value side=GeometryContainment::Indeterminate;
        double lower=0,upper=0;
        const GeometryDistanceStatus::Value status=AffineEllipsoidMinimumDistance::query(
            point,radius,forward,inverse,requestedWidth,side,lower,upper);
        if (status==GeometryDistanceStatus::InvalidInput) return GeometryContainment::InvalidInput;
        if (status==GeometryDistanceStatus::Unsupported) return GeometryContainment::Unsupported;
        if (status==GeometryDistanceStatus::Valid) {
            if (upper<=tolerance) return GeometryContainment::Boundary;
            if (lower>tolerance) return side;
        }
    }
    return GeometryContainment::Indeterminate;
}

GeometryDistanceStatus::Value SceneSolid::estimateBoundaryDistance(const Vector3Dd& point,
    double& distance, double& estimatedError) const
{
    if (!Double::isFinite(point.x()) || !Double::isFinite(point.y()) ||
        !Double::isFinite(point.z())) return GeometryDistanceStatus::InvalidInput;
    const Sphere* sphere=NULL; BOOLEAN inverted=FALSE;
    Matrix4x4d forward,inverse;
    const GeometryDistanceStatus::Value availability=queryDenotedSphere(sphere,inverted,forward,inverse);
    if (availability!=GeometryDistanceStatus::Valid) return availability;
    return AffineEllipsoidDistance::query(point,sphere->getRadius(),forward,distance,estimatedError);
}

GeometryContainment::Value SceneSolid::classifyPoint(const Vector3Dd& point, double tolerance) const
{
    if (geometry == NULL) return GeometryContainment::Unsupported;
    if (!Double::isFinite(tolerance) || tolerance < 0 ||
        !Double::isFinite(point.x()) || !Double::isFinite(point.y()) ||
        !Double::isFinite(point.z())) return GeometryContainment::InvalidInput;
    if (tolerance == 0) {
        Matrix4x4d forward, inverse;
        const AffinePlacementStatus::Value status = queryAffinePlacement(forward, inverse);
        if (status == AffinePlacementStatus::Unsupported) return GeometryContainment::Unsupported;
        if (status == AffinePlacementStatus::Invalid) return GeometryContainment::InvalidInput;
        if (status == AffinePlacementStatus::Indeterminate) return GeometryContainment::Indeterminate;
        const Vector3Dd local = inverse.multiply(point);
        if (!Double::isFinite(local.x()) || !Double::isFinite(local.y()) ||
            !Double::isFinite(local.z())) return GeometryContainment::Indeterminate;
        return geometry->classifyPoint(local, 0);
    }
    return classifyPointCertified(point, tolerance);
}

void SceneSolid::markModified() { modificationVersion++; }

const String& SceneSolid::getName() const { return name; }

long long SceneSolid::getModificationVersion() const { return modificationVersion; }

void SceneSolid::setName(const String& n) { name = n; markModified(); }

Geometry* SceneSolid::getGeometry() const { return geometry; }

void SceneSolid::setGeometry(Geometry* g)
{
    if ( geometry != g && geometry != 0 && ownsGeometry ) {
        delete geometry;
    }
    geometry = g;
    ownsGeometry = TRUE;
    geometryIsSphere = geometry != 0 && geometry->instanceOf(Sphere::CLASS);
    markModified();
}

void SceneSolid::setGeometryReference(Geometry* g)
{
    if ( geometry != g && geometry != 0 && ownsGeometry ) {
        delete geometry;
    }
    geometry = g;
    ownsGeometry = FALSE;
    geometryIsSphere = geometry != 0 && geometry->instanceOf(Sphere::CLASS);
    markModified();
}

const Class SceneSolid::CLASS = { "SceneSolid", &Entity::CLASS };

const Class *
SceneSolid::getClass() const
{
    return &CLASS;
}
