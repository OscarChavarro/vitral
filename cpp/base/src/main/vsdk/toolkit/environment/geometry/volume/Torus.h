#ifndef __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_VOLUME_TORUS_H__
#define __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_VOLUME_TORUS_H__

#include "Solid.h"

class Ray;
class RayHit;

class Torus : public Solid {
private:
    double majorRadius;
    double minorRadius;

    double implicitValue(const Vector3Dd& p) const;

public:
    Torus(double inMajorRadius, double inMinorRadius);
    virtual ~Torus() {}

    double getMajorRadius() const;
    void setMajorRadius(double rMajor);
    double getMinorRadius() const;
    void setMinorRadius(double rMinor);

    Ray* doIntersection(const Ray& inOutRay);
    virtual bool doIntersection(const Ray& inRay, RayHit* outHit);
    virtual void doExtraInformation(const Ray& inRay, double inT, RayHit* outHit);
    virtual double* getMinMax();
};

#endif
