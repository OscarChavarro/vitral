#ifndef __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_VOLUME_BOX_H__
#define __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_VOLUME_BOX_H__

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
    Box(double dx, double dy, double dz);
    explicit Box(const Vector3Dd& s);
    virtual ~Box() {}

    Ray* doIntersection(const Ray& inOutRay);
    virtual bool doIntersection(const Ray& inRay, RayHit* outHit) override;
    virtual void doExtraInformation(const Ray& inRay, double inT, RayHit* outData) override;
    virtual double* getMinMax() override;
    virtual PolyhedralBoundedSolid* exportToPolyhedralBoundedSolid() override;

    Vector3Dd getSize() const;
    void setSize(double dx, double dy, double dz);
    void setSize(const Vector3Dd& s);

private:
    PolyhedralBoundedSolid* buildPolyhedralBoundedSolid();
};

#endif
