#ifndef __FUNCTIONAL_EXPLICIT_SURFACE__
#define __FUNCTIONAL_EXPLICIT_SURFACE__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/environment/geometry/surface/Surface.h"
class AlgebraicExpression;
class QuadMesh;
class TriangleMesh;
class Ray;
class RayHit;

class FunctionalExplicitSurface : public Surface {
private:
    String functionExpression;
    AlgebraicExpression* xyFunction;
    double minXBound;
    double minYBound;
    double minZBound;
    double maxXBound;
    double maxYBound;
    double maxZBound;
    int tesselationHintX;
    int tesselationHintY;
    TriangleMesh* internalTriangleMesh;

    void init(const String& fxy);
    int coord(int tesselationHintX, int tesselationHintY, int ix, int iy);
    void updateInternalGeometry();

public:
    static const Class CLASS;
    virtual const Class *getClass() const;

    FunctionalExplicitSurface(const String& fxy);
    virtual ~FunctionalExplicitSurface();

    String getFunctionExpression() const;

    void setBounds(double minXBound, double minYBound, double minZBound,
                   double maxXBound, double maxYBound, double maxZBound);
    void setTesselationHint(int tesx, int tesy);

    int getTesselationHintX() const;
    int getTesselationHintY() const;

    double getMinXBound() const;
    double getMinYBound() const;
    double getMinZBound() const;
    double getMaxXBound() const;
    double getMaxYBound() const;
    double getMaxZBound() const;

    TriangleMesh* getInternalTriangleMesh() const;

    virtual double* getMinMax();

    Ray* doIntersectionFirstHit(const Ray& inOut_Ray);
    virtual BOOLEAN doIntersectionFirstHit(const Ray& inRay, RayHit* outHit);
    virtual void doExtraInformation(const Ray& inRay, double inT, RayHit* outData);
    virtual int doContainmentTest(const Vector3Dd& p, double distanceTolerance);

    /**
    Exports the tessellation of the surface as a mesh of quadrilaterals: the
    same grid of `getTesselationHintX()` x `getTesselationHintY()` cells of
    the internal triangle mesh, without the diagonals that split each cell
    in two triangles, so the lines of the mesh are the lines of constant x
    and y of the surface.
    @return a new mesh, owned by the caller, or null if the surface has no
    valid tessellation (i.e. its function could not be evaluated)
    */
    QuadMesh* exportToQuadMesh();
};

#endif
