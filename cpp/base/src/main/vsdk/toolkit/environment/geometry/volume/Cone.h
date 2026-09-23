#ifndef __CONE__
#define __CONE__

#include "vsdk/toolkit/environment/geometry/volume/Solid.h"
class Ray;
class RayHit;

class Cone : public Solid {
private:
    double bottomRadius; // Radius at the base
    double topRadius;    // Radius at the top
    double height;       // Height

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
    Cone(double bottomRadius, double topRadius, double height);
    virtual ~Cone() {}

    double getBottomRadius() const;
    double getTopRadius() const;
    double getHeight() const;
    void setBottomRadius(double value);
    void setTopRadius(double value);
    void setHeight(double value);

    Ray* doIntersectionFirstHit(const Ray& inOutRay);
    virtual bool doIntersectionFirstHit(const Ray& inRay, RayHit* outHit);
    virtual void doExtraInformation(const Ray& inRay, double inT, RayHit* outData);
    virtual double* getMinMax();

};

#endif
