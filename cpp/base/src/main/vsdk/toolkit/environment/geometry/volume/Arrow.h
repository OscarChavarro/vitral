#ifndef __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_VOLUME_ARROW_H__
#define __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_VOLUME_ARROW_H__

#include "Solid.h"

class Cone;
class Ray;
class RayHit;

class Arrow : public Solid {
private:
    static const double NO_HIT;

    double baseLength;
    double headLength;
    double baseRadius;
    double headRadius;

    Cone* baseCylinder;
    Cone* headCone;
    Cone* lastElement;

    bool doIntersectionDistanceOnly(const Ray& inRay, RayHit* outHit);

public:
    Arrow(double baseLength, double headLength, double baseRadius, double headRadius);
    virtual ~Arrow();

    double getBaseLength() const;
    void setBaseLength(double val);
    double getHeadLength() const;
    void setHeadLength(double val);
    double getBaseRadius() const;
    void setBaseRadius(double val);
    double getHeadRadius() const;
    void setHeadRadius(double val);

    Ray* doIntersection(const Ray& inOutRay);
    virtual bool doIntersection(const Ray& inRay, RayHit* outHit);
    virtual void doExtraInformation(const Ray& inRay, double inT, RayHit* outData);
    virtual double* getMinMax();
};

#endif
