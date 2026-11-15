#ifndef __SPHERE__
#define __SPHERE__

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/environment/geometry/volume/Solid.h"
class Ray;
class RayHit;
#ifndef __TURBOC__
class PolyhedralBoundedSolid;
#endif

class Sphere : public Solid {
private:
    double radius;
    double radiusSquared;
    BOOLEAN validateSurfaceQuery(const Ray& ray, const RayCrossingQuery& query,
        RayFirstHitStatus::Value& failure) const;
    void fillSurfaceDetail(const Ray& ray, double t, RayHit* result) const;

#ifndef __TURBOC__
    enum {
        DEFAULT_PARALLELS = 8,
        DEFAULT_MERIDIANS = 16,
        MIN_PARALLELS = 2,
        MIN_MERIDIANS = 3
    };

    static Vector3Dd spherePosition(double theta, double t, double r);
    PolyhedralBoundedSolid* buildPolyhedralBoundedSolid(int nmeridians, int nparalels);
#endif

public:
    static const Class CLASS;
    virtual const Class *getClass() const;
    explicit Sphere(double r);
    virtual Geometry* copyGeometry() const { return new Sphere(radius); }
    virtual RayCrossingSupport::Value emitRayCrossings(const Ray& ray,
        const RayCrossingQuery& query, RayCrossingSink& sink) const;

    virtual RaySolidQueryStatus::Value classifyRay(const Ray& ray, const RayCrossingQuery& query,
        RaySolidClassification& result) const;

    virtual RayFirstHitStatus::Value queryFirstHit(const Ray& ray, const RayCrossingQuery& query,
        RayHit* result, RayBoundary* boundary = NULL) const;

    // Returns zero, one or two roots in increasing order without allocation.
    static int intersectionRoots(const Vector3Dd& origin, const Vector3Dd& direction,
        double radius, const RayCrossingQuery& query, double& nearT, double& farT);

    virtual ~Sphere() {}

    Ray* doIntersectionFirstHit(const Ray& inoutRay);
    virtual BOOLEAN doIntersectionFirstHit(const Ray& inRay, RayHit* outHit);
    // Explicit inclusive interval; the legacy overload queries [0,+infinity).
    BOOLEAN doIntersectionFirstHit(const Ray& inRay, const RayCrossingQuery& query,
        RayHit* outHit);
    virtual void doExtraInformation(const Ray& inRay, double inT, RayHit* outData);
    virtual GeometryContainment::Value classifyPoint(const Vector3Dd& p, double distanceTolerance) const;
    virtual GeometryContainment::Value classifyPointCertified(const Vector3Dd& p, double distanceTolerance) const;
    virtual GeometryContainment::Value classifyBoxCertified(const Vector3Dd& center, const Vector3Dd& halfWidths) const;
    virtual int doContainmentTest(const Vector3Dd& p, double distanceTolerance);
    virtual void getBounds(GeometryBounds& result) const;
    virtual double* getMinMax();

    double getRadius() const;
    // Borrowed reference to this object's intrinsic radius; valid while it lives.
    const double& getRadiusReference() const { return radius; }
    // Signed squared implicit function: negative inside, positive outside.
    double implicitValue(const Vector3Dd& point) const { return point.dotProduct(point) - radiusSquared; }
    // Fast normal for a known nonzero surface point in local coordinates.
    Vector3Dd surfaceNormalAtNonzeroPoint(const Vector3Dd& point) const { return point.normalizedFast(); }
    double getRadiusSquared() const;
    void setRadius(double value);

#ifndef __TURBOC__
    virtual PolyhedralBoundedSolid* exportToPolyhedralBoundedSolid();
    PolyhedralBoundedSolid* exportToPolyhedralBoundedSolid(int meridians, int parallels);
#endif

    Vector3Dd spherePosition(double theta, double phi);
    Vector3Dd sphereNormal(double theta, double phi);
    Vector3Dd sphereTangent(double theta, double phi);
    Vector3Dd sphereBinormal(double theta, double phi);
};

#endif
