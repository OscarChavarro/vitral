#ifndef __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_SURFACE_FUNCTIONALEXPLICITSURFACE_H__
#define __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_SURFACE_FUNCTIONALEXPLICITSURFACE_H__

#include "vsdk/toolkit/environment/geometry/surface/Surface.h"
#include <string>

class TriangleMesh;
class Ray;
class RayHit;
class VoxelVolume;
class Matrix4x4d;
class ProgressMonitor;

class FunctionalExplicitSurface : public Surface {
private:
    std::string functionExpression;
    double minx;
    double miny;
    double minz;
    double maxx;
    double maxy;
    double maxz;
    int nx;
    int ny;
    TriangleMesh* internalGeometry;

    void init(const std::string& fxy);
    int coord(int nx, int ny, int ix, int iy);
    void updateInternalGeometry();
    double evalExpression(double x, double y, bool& ok) const;

public:
    FunctionalExplicitSurface(const std::string& fxy);
    virtual ~FunctionalExplicitSurface();

    std::string getFunctionExpression() const;

    void setBounds(double minx, double miny, double minz,
                   double maxx, double maxy, double maxz);
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

    Ray* doIntersection(const Ray& inOut_Ray);
    virtual bool doIntersection(const Ray& inRay, RayHit* outHit);
    virtual void doExtraInformation(const Ray& inRay, double inT, RayHit* outData);
    virtual int doContainmentTest(const Vector3Dd& p, double distanceTolerance);
    void doVoxelization(VoxelVolume& vv, const Matrix4x4d& M, ProgressMonitor* reporter);
};

#endif
