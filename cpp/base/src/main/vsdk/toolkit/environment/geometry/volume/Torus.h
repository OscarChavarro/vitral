#ifndef __TORUS__
#define __TORUS__

#include "vsdk/toolkit/environment/geometry/volume/Solid.h"
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

    Ray* doIntersectionFirstHit(const Ray& inOutRay);
    virtual bool doIntersectionFirstHit(const Ray& inRay, RayHit* outHit);
    virtual void doExtraInformation(const Ray& inRay, double inT, RayHit* outHit);
    virtual double* getMinMax();
};

#endif
