#ifndef __CONE__
#define __CONE__

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/environment/geometry/volume/Solid.h"
class PolyhedralBoundedSolid;
class Ray;
class RayHit;

class Cone : public Solid {
private:
    double bottomRadius; // Radius at the base
    double topRadius;    // Radius at the top
    double height;       // Height

    enum {
        DEFAULT_CIRCUMFERENCE_DIVISIONS = 36,
        DEFAULT_HEIGHT_DIVISIONS = 1,
        MIN_CIRCUMFERENCE_DIVISIONS = 3,
        MIN_HEIGHT_DIVISIONS = 1
    };

    static double sq(double v);
    static BOOLEAN approxEq(double a, double b);

    Ray* doIntersectionCylinder(const Ray& inOutRay, double inR, double inH, RayHit* outInfo);
    Ray* doIntersectionCone(const Ray& inOutRay, double inR, double inH, RayHit* outInfo);
    Ray* doIntersectionTap(const Ray& inOutRay, double inR, double inH, RayHit* outInfo);

    static void closeTopFaceToApex(PolyhedralBoundedSolid* solid, double apexZ);
    PolyhedralBoundedSolid* buildPolyhedralBoundedSolid(int nsides,
        int heightDivisions);

public:
    static const Class CLASS;
    virtual const Class *getClass() const;

    Cone(double bottomRadius, double topRadius, double height);
    virtual ~Cone() {}

    double getBottomRadius() const;
    double getTopRadius() const;
    double getHeight() const;
    void setBottomRadius(double value);
    void setTopRadius(double value);
    void setHeight(double value);

    Ray* doIntersectionFirstHit(const Ray& inOutRay);
    virtual BOOLEAN doIntersectionFirstHit(const Ray& inRay, RayHit* outHit);
    virtual void doExtraInformation(const Ray& inRay, double inT, RayHit* outData);
    virtual int doContainmentTest(const Vector3Dd& p, double distanceTolerance);
    virtual double* getMinMax();

    /**
    @return a new boundary representation of the cone with the default
    divisions, owned by the caller
    */
    virtual PolyhedralBoundedSolid* exportToPolyhedralBoundedSolid();

    /**
    @param circumferenceDivisions sides of the cone base (at least 3)
    @param heightDivisions vertical subdivisions (at least 1)
    @return a new boundary representation of the cone, owned by the caller
    */
    PolyhedralBoundedSolid* exportToPolyhedralBoundedSolid(
        int circumferenceDivisions, int heightDivisions);

};

#endif
