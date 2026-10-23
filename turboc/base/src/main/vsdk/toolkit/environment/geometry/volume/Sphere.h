#ifndef __SPHERE__
#define __SPHERE__

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/environment/geometry/volume/Solid.h"
class Ray;
class RayHit;
class PolyhedralBoundedSolid;

class Sphere : public Solid {
private:
    double radius;
    double radiusSquared;

    enum {
        DEFAULT_PARALLELS = 8,
        DEFAULT_MERIDIANS = 16,
        MIN_PARALLELS = 2,
        MIN_MERIDIANS = 3
    };

    static Vector3Dd spherePosition(double theta, double t, double r);
    PolyhedralBoundedSolid* buildPolyhedralBoundedSolid(int nmeridians, int nparalels);

public:
    static const Class CLASS;
    virtual const Class *getClass() const;

    explicit Sphere(double r);
    virtual ~Sphere() {}

    Ray* doIntersectionFirstHit(const Ray& inoutRay);
    virtual BOOLEAN doIntersectionFirstHit(const Ray& inRay, RayHit* outHit);
    virtual void doExtraInformation(const Ray& inRay, double inT, RayHit* outData);
    virtual int doContainmentTest(const Vector3Dd& p, double distanceTolerance);
    virtual double* getMinMax();

    double getRadius() const;
    double getRadiusSquared() const;
    void setRadius(double value);

    virtual PolyhedralBoundedSolid* exportToPolyhedralBoundedSolid();
    PolyhedralBoundedSolid* exportToPolyhedralBoundedSolid(int meridians, int parallels);

    Vector3Dd spherePosition(double theta, double phi);
    Vector3Dd sphereNormal(double theta, double phi);
    Vector3Dd sphereTangent(double theta, double phi);
    Vector3Dd sphereBinormal(double theta, double phi);
};

#endif
