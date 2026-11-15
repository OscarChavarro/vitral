#ifndef __ARROW__
#define __ARROW__

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/environment/geometry/volume/Solid.h"
class Cone;
class PolyhedralBoundedSolid;
class Ray;
class RayHit;

class Arrow : public Solid {
private:
    static const double NO_HIT;
    enum {
        DEFAULT_CIRCUMFERENCE_DIVISIONS = 36 / 4,
        DEFAULT_HEIGHT_DIVISIONS = 1,
        MIN_CIRCUMFERENCE_DIVISIONS = 3,
        MIN_HEIGHT_DIVISIONS = 1
    };

    double baseLength;
    double headLength;
    double baseRadius;
    double headRadius;

    Cone* baseCylinder;
    Cone* headCone;
    Cone* lastElement;

    BOOLEAN doIntersectionDistanceOnly(const Ray& inRay, RayHit* outHit);
    static void closeTopFaceToApex(
        PolyhedralBoundedSolid* solid, int nsides, double apexZ);
    PolyhedralBoundedSolid* buildPolyhedralBoundedSolid(
        int nsides, int heightDivisions);

public:
    static const Class CLASS;
    virtual const Class *getClass() const;

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

    Ray* doIntersectionFirstHit(const Ray& inOutRay);
    virtual BOOLEAN doIntersectionFirstHit(const Ray& inRay, RayHit* outHit);
    virtual void doExtraInformation(const Ray& inRay, double inT, RayHit* outData);
    virtual double* getMinMax();
    virtual PolyhedralBoundedSolid* exportToPolyhedralBoundedSolid();
    PolyhedralBoundedSolid* exportToPolyhedralBoundedSolid(
        int circumferenceDivisions, int heightDivisions);
};

#endif
