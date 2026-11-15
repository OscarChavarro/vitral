#ifndef __BOX__
#define __BOX__

#include "vsdk/toolkit/common/VSDK.h"

#include "java/lang/Double.h"
#include "java/lang/Math.h"
#include "vsdk/toolkit/environment/geometry/volume/Solid.h"
class Ray;
class RayHit;

class Box : public Solid {
private:
    Vector3Dd bounds[2];

    static Vector3Dd planeNormal(int hitPlane);
    static Vector3Dd planeTangent(int hitPlane);
    int classifyHitPlane(double x, double y, double z) const;
    RayFirstHitStatus::Value clipRay(const Ray& ray, const RayCrossingQuery& query,
        double& entry, double& exit) const;
    static RayFirstHitStatus::Value clipAxis(double origin, double direction, double minimum,
        double maximum, double parallelTolerance, BOOLEAN checkArithmetic,
        double& entry, double& exit, BOOLEAN& faceOverlap);

public:
    static const Class CLASS;
    virtual const Class *getClass() const;

    Box(double dx, double dy, double dz);
    explicit Box(const Vector3Dd& s);
    Box(const Vector3Dd& minimum, const Vector3Dd& maximum);
    Vector3Dd* getRawBounds() { return bounds; }
    const Vector3Dd* getRawBounds() const { return bounds; }
    Vector3Dd getCenter() const { return bounds[0].multiply(0.5).add(bounds[1].multiply(0.5)); }
    virtual ~Box() {}
    virtual Geometry* copyGeometry() const { return new Box(bounds[0], bounds[1]); }
    virtual void getBounds(GeometryBounds& result) const;
    virtual GeometryContainment::Value classifyPoint(const Vector3Dd& p, double tolerance) const;
    virtual GeometryContainment::Value classifyPointCertified(const Vector3Dd& p, double tolerance) const;
    virtual GeometryContainment::Value classifyBoxCertified(const Vector3Dd& center, const Vector3Dd& halfWidths) const;
    virtual RayCrossingSupport::Value emitRayCrossings(const Ray& ray, const RayCrossingQuery& query,
        RayCrossingSink& sink) const;
    virtual RayFirstHitStatus::Value queryFirstHit(const Ray& ray, const RayCrossingQuery& query,
        RayHit* result, RayBoundary* boundary = NULL) const;
    virtual RayFirstHitStatus::Value querySurfaceDetail(const Ray& ray, const RayBoundary& boundary, RayHit* hit) const;
    virtual RaySolidQueryStatus::Value classifyRay(const Ray& ray, const RayCrossingQuery& query,
        RaySolidClassification& result) const;

    // Mathematical operations on arbitrary axis-aligned bounds. Boundary
    // inclusion and the face selection tolerance are chosen by the caller.
    static BOOLEAN containsBounds(const Vector3Dd& point, const Vector3Dd& minimum,
        const Vector3Dd& maximum);
    static Vector3Dd normalForBounds(const Vector3Dd& point, const Vector3Dd& minimum,
        const Vector3Dd& maximum, double faceTolerance);

    // Intersect arbitrary axis-aligned bounds with an initial parameter interval.
    // The caller selects its parallel threshold and whether arithmetic failures
    // are reported; a parallel ray on a face is reported separately.
    static RayFirstHitStatus::Value intersectBounds(const Vector3Dd& origin,
        const Vector3Dd& direction, const Vector3Dd& minimum, const Vector3Dd& maximum,
        double parallelTolerance, BOOLEAN checkArithmetic, double& entry, double& exit,
        BOOLEAN& faceOverlap);

    Ray* doIntersectionFirstHit(const Ray& inOutRay);
    virtual BOOLEAN doIntersectionFirstHit(const Ray& inRay, RayHit* outHit);
    virtual void doExtraInformation(const Ray& inRay, double inT, RayHit* outData);
    // Java-parity legacy band: per-axis |p_i| within half_i +/- tolerance. It is
    // intentionally not the Euclidean band of classifyPoint.
    virtual int doContainmentTest(const Vector3Dd& p, double distanceTolerance);
    virtual double* getMinMax();
    virtual PolyhedralBoundedSolid* exportToPolyhedralBoundedSolid();

    Vector3Dd getSize() const { return bounds[1].subtract(bounds[0]); }
    void setSize(double dx, double dy, double dz);
    void setSize(const Vector3Dd& s);

private:
    PolyhedralBoundedSolid* buildPolyhedralBoundedSolid();
};

inline BOOLEAN Box::containsBounds(const Vector3Dd& point, const Vector3Dd& minimum,
    const Vector3Dd& maximum)
{
    return !(point.x() < minimum.x() || point.x() > maximum.x() ||
        point.y() < minimum.y() || point.y() > maximum.y() ||
        point.z() < minimum.z() || point.z() > maximum.z());
}

inline Vector3Dd Box::normalForBounds(const Vector3Dd& point, const Vector3Dd& minimum,
    const Vector3Dd& maximum, double tolerance)
{
    if (Math::abs(point.x() - maximum.x()) < tolerance) return Vector3Dd(1,0,0);
    if (Math::abs(point.x() - minimum.x()) < tolerance) return Vector3Dd(-1,0,0);
    if (Math::abs(point.y() - maximum.y()) < tolerance) return Vector3Dd(0,1,0);
    if (Math::abs(point.y() - minimum.y()) < tolerance) return Vector3Dd(0,-1,0);
    if (Math::abs(point.z() - maximum.z()) < tolerance) return Vector3Dd(0,0,1);
    if (Math::abs(point.z() - minimum.z()) < tolerance) return Vector3Dd(0,0,-1);
    return Vector3Dd(0,0,0);
}

inline RayFirstHitStatus::Value Box::clipAxis(double origin, double direction,
    double minimum, double maximum, double parallelTolerance, BOOLEAN checkArithmetic,
    double& entry, double& exit, BOOLEAN& faceOverlap)
{
    if (direction < -parallelTolerance || direction > parallelTolerance) {
        const double farBound = direction < 0 ? minimum : maximum;
        const double nearBound = direction < 0 ? maximum : minimum;
        const double farT = (farBound - origin) / direction;
        if (!checkArithmetic && farT < entry) return RayFirstHitStatus::Miss;
        const double nearT = (nearBound - origin) / direction;
        if (checkArithmetic && (!Double::isFinite(farT) ||
            !Double::isFinite(nearT) || farT == nearT))
            return RayFirstHitStatus::Indeterminate;
        if (farT < entry) return RayFirstHitStatus::Miss;
        if (farT <= exit) exit = farT;
        if (nearT >= entry) {
            if (nearT > exit) return RayFirstHitStatus::Miss;
            entry = nearT;
        }
        return RayFirstHitStatus::Hit;
    }
    if (origin < minimum || origin > maximum) return RayFirstHitStatus::Miss;
    if (origin == minimum || origin == maximum) faceOverlap = TRUE;
    return RayFirstHitStatus::Hit;
}

inline RayFirstHitStatus::Value Box::intersectBounds(const Vector3Dd& origin,
    const Vector3Dd& direction, const Vector3Dd& minimum, const Vector3Dd& maximum,
    double parallelTolerance, BOOLEAN checkArithmetic, double& entry, double& exit,
    BOOLEAN& faceOverlap)
{
    faceOverlap = FALSE;
    if (checkArithmetic &&
        (!Double::isFinite(origin.x()) || !Double::isFinite(origin.y()) ||
         !Double::isFinite(origin.z()) || !Double::isFinite(direction.x()) ||
         !Double::isFinite(direction.y()) || !Double::isFinite(direction.z()) ||
         (direction.x() == 0 && direction.y() == 0 && direction.z() == 0) ||
         !Double::isFinite(minimum.x()) || !Double::isFinite(minimum.y()) ||
         !Double::isFinite(minimum.z()) || !Double::isFinite(maximum.x()) ||
         !Double::isFinite(maximum.y()) || !Double::isFinite(maximum.z()) ||
         minimum.x() >= maximum.x() || minimum.y() >= maximum.y() || minimum.z() >= maximum.z() ||
         !Double::isFinite(parallelTolerance) || parallelTolerance < 0 ||
         Double::isNaN(entry) || Double::isNaN(exit) || entry > exit))
        return RayFirstHitStatus::InvalidInput;
    RayFirstHitStatus::Value result = clipAxis(origin.x(), direction.x(), minimum.x(), maximum.x(),
        parallelTolerance, checkArithmetic, entry, exit, faceOverlap);
    if (result != RayFirstHitStatus::Hit) return result;
    result = clipAxis(origin.y(), direction.y(), minimum.y(), maximum.y(),
        parallelTolerance, checkArithmetic, entry, exit, faceOverlap);
    if (result != RayFirstHitStatus::Hit) return result;
    return clipAxis(origin.z(), direction.z(), minimum.z(), maximum.z(),
        parallelTolerance, checkArithmetic, entry, exit, faceOverlap);
}

#endif
