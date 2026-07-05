#ifndef __SPHERE__
#define __SPHERE__

#include "vsdk/toolkit/environment/geometry/volume/Solid.h"
class Ray;
class RayHit;
class PolyhedralBoundedSolid;

class Sphere : public Solid {
private:
    double radius_;
    double radiusSquared_;

    static const int DEFAULT_PARALLELS = 8;
    static const int DEFAULT_MERIDIANS = 16;
    static const int MIN_PARALLELS = 2;
    static const int MIN_MERIDIANS = 3;

    static Vector3Dd spherePosition(double theta, double t, double r);
    PolyhedralBoundedSolid* buildPolyhedralBoundedSolid(int nmeridians, int nparalels);

public:
    explicit Sphere(double r);
    virtual ~Sphere() {}

    Ray* doIntersectionFirstHit(const Ray& inoutRay);
    virtual bool doIntersectionFirstHit(const Ray& inRay, RayHit* outHit);
    virtual void doExtraInformation(const Ray& inRay, double inT, RayHit* outData);
    virtual int doContainmentTest(const Vector3Dd& p, double distanceTolerance);
    virtual double* getMinMax();

    double getRadius() const;
    double getRadiusSquared() const;
    void setRadius(double r);

    virtual PolyhedralBoundedSolid* exportToPolyhedralBoundedSolid();
    PolyhedralBoundedSolid* exportToPolyhedralBoundedSolid(int meridians, int parallels);

    Vector3Dd spherePosition(double theta, double phi);
    Vector3Dd sphereNormal(double theta, double phi);
    Vector3Dd sphereTangent(double theta, double phi);
    Vector3Dd sphereBinormal(double theta, double phi);
};

#endif
