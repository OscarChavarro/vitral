#ifndef __BOX__
#define __BOX__

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/environment/geometry/volume/Solid.h"
class Ray;
class RayHit;

class Box : public Solid {
private:
    Vector3Dd size;

    static Vector3Dd planeNormal(int hitPlane);
    static Vector3Dd planeTangent(int hitPlane);
    int classifyHitPlane(double x, double y, double z) const;

public:
    static const Class CLASS;
    virtual const Class *getClass() const;

    Box(double dx, double dy, double dz);
    explicit Box(const Vector3Dd& s);
    virtual ~Box() {}

    Ray* doIntersectionFirstHit(const Ray& inOutRay);
    virtual BOOLEAN doIntersectionFirstHit(const Ray& inRay, RayHit* outHit);
    virtual void doExtraInformation(const Ray& inRay, double inT, RayHit* outData);
    virtual int doContainmentTest(const Vector3Dd& p, double distanceTolerance);
    virtual double* getMinMax();
    virtual PolyhedralBoundedSolid* exportToPolyhedralBoundedSolid();

    Vector3Dd getSize() const;
    void setSize(double dx, double dy, double dz);
    void setSize(const Vector3Dd& s);

private:
    PolyhedralBoundedSolid* buildPolyhedralBoundedSolid();
};

#endif
