#ifndef __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_VOLUME_CONE_H__
#define __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_VOLUME_CONE_H__

#include "vsdk/toolkit/environment/geometry/volume/Solid.h"
class Ray;
class RayHit;

class Cone : public Solid {
private:
    double r1;
    double r2;
    double h;

    static const int DEFAULT_CIRCUMFERENCE_DIVISIONS = 36;
    static const int DEFAULT_HEIGHT_DIVISIONS = 1;
    static const int MIN_CIRCUMFERENCE_DIVISIONS = 3;
    static const int MIN_HEIGHT_DIVISIONS = 1;

    static double sq(double v);
    static bool approxEq(double a, double b);

    Ray* doIntersectionCylinder(const Ray& inOutRay, double inR, double inH, RayHit* outInfo);
    Ray* doIntersectionCone(const Ray& inOutRay, double inR, double inH, RayHit* outInfo);
    Ray* doIntersectionTap(const Ray& inOutRay, double inR, double inH, RayHit* outInfo);

public:
    Cone(double inR1, double inR2, double inH);
    virtual ~Cone() {}

    double getBaseRadius() const;
    double getTopRadius() const;
    double getHeight() const;
    void setBaseRadius(double val);
    void setTopRadius(double val);
    void setHeight(double val);

    Ray* doIntersectionFirstHit(const Ray& inOutRay);
    virtual bool doIntersectionFirstHit(const Ray& inRay, RayHit* outHit);
    virtual void doExtraInformation(const Ray& inRay, double inT, RayHit* outData);
    virtual double* getMinMax();

};

#endif
