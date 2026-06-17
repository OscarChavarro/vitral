#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/logging/Logger.h"
#include "vsdk/toolkit/common/symbolicAlgebra/AlgebraicExpression.h"
#include "vsdk/toolkit/common/symbolicAlgebra/AlgebraicExpressionException.h"
#include "vsdk/toolkit/environment/geometry/element/Ray.h"
#include "vsdk/toolkit/environment/geometry/element/RayHit.h"
#include "vsdk/toolkit/environment/geometry/surface/FunctionalExplicitSurface.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMesh.h"
FunctionalExplicitSurface::FunctionalExplicitSurface(const java::String& fxy) : internalGeometry(0)
{
    init(fxy);
}

FunctionalExplicitSurface::~FunctionalExplicitSurface()
{
    if (internalGeometry != 0) delete internalGeometry;
}

void FunctionalExplicitSurface::init(const java::String& fxy)
{
    functionExpression = fxy;
    minx = miny = minz = -1.0;
    maxx = maxy = maxz = 1.0;
    nx = 10;
    ny = 10;
    updateInternalGeometry();
}

java::String FunctionalExplicitSurface::getFunctionExpression() const { return functionExpression; }

void FunctionalExplicitSurface::setBounds(double minx, double miny, double minz,
                                          double maxx, double maxy, double maxz)
{
    this->minx = minx; this->miny = miny; this->minz = minz;
    this->maxx = maxx; this->maxy = maxy; this->maxz = maxz;
    updateInternalGeometry();
}

void FunctionalExplicitSurface::setTesselationHint(int tesx, int tesy) { nx = tesx; ny = tesy; updateInternalGeometry(); }
int FunctionalExplicitSurface::getTesselationHintX() const { return nx; }
int FunctionalExplicitSurface::getTesselationHintY() const { return ny; }
double FunctionalExplicitSurface::getMinXBound() const { return minx; }
double FunctionalExplicitSurface::getMinYBound() const { return miny; }
double FunctionalExplicitSurface::getMinZBound() const { return minz; }
double FunctionalExplicitSurface::getMaxXBound() const { return maxx; }
double FunctionalExplicitSurface::getMaxYBound() const { return maxy; }
double FunctionalExplicitSurface::getMaxZBound() const { return maxz; }

int FunctionalExplicitSurface::coord(int nx, int, int ix, int iy) { return ((nx+1)*iy) + ix; }

double FunctionalExplicitSurface::evalExpression(double x, double y, bool& ok) const
{
    AlgebraicExpression xyFunction;
    try {
        xyFunction.setExpression(functionExpression);
        xyFunction.defineValue("x", x);
        xyFunction.defineValue("y", y);
        ok = true;
        return xyFunction.eval();
    }
    catch (const AlgebraicExpressionException&) {
        ok = false;
        return 0.0;
    }
}

void FunctionalExplicitSurface::updateInternalGeometry()
{
    if (nx <= 0 || ny <= 0) return;
    if (internalGeometry != 0) delete internalGeometry;
    internalGeometry = new TriangleMesh();

    double dx = (maxx - minx) / ((double)nx);
    double dy = (maxy - miny) / ((double)ny);

    internalGeometry->initVertexPositionsArray((nx+1)*(ny+1));
    java::ArrayList<double>& v = internalGeometry->getVertexPositions();

    int index = 0;
    for (int iy = 0; iy <= ny; iy++) {
        double y = miny + ((double)iy)*dy;
        for (int ix = 0; ix <= nx; ix++) {
            double x = minx + ((double)ix)*dx;
            bool ok = true;
            double z = evalExpression(x, y, ok);
            if (!ok) {
                Logger::reportMessage("FunctionalExplicitSurface", Logger::WARNING, "updateInternalGeometry", "Cannot evaluate algebraic expression!");
                return;
            }
            if (z > maxz) z = maxz;
            if (z < minz) z = minz;
            v[3*index+0] = x;
            v[3*index+1] = y;
            v[3*index+2] = z;
            index++;
        }
    }

    internalGeometry->initTriangleArrays(nx*ny*2);
    java::ArrayList<int>& t = internalGeometry->getTriangleIndexes();

    index = 0;
    for (int iy = 0; iy < ny; iy++) {
        for (int ix = 0; ix < nx; ix++) {
            t[3*index+0] = coord(nx, ny, ix, iy);
            t[3*index+1] = coord(nx, ny, ix+1, iy);
            t[3*index+2] = coord(nx, ny, ix+1, iy+1);
            index++;

            t[3*index+0] = coord(nx, ny, ix, iy);
            t[3*index+1] = coord(nx, ny, ix+1, iy+1);
            t[3*index+2] = coord(nx, ny, ix, iy+1);
            index++;
        }
    }

    internalGeometry->calculateNormals();
}

TriangleMesh* FunctionalExplicitSurface::getInternalTriangleMesh() const { return internalGeometry; }

/*
Check the general interface contract in superclass method
Geometry.getMinMax.
@return a new 6 valued double array containing the coordinates of a min-max
bounding box for current geometry.
*/
double* FunctionalExplicitSurface::getMinMax() { return internalGeometry ? internalGeometry->getMinMax() : 0; }

/*
Check the general interface contract in superclass method
Geometry.doIntersection.

\todo  Should not delegate work over tesselated geometry version. Should
evaluate directly from algebraic function surface!
@param inOut_Ray
@return true if given ray intersects current FunctionalExplicitSurface
*/
Ray* FunctionalExplicitSurface::doIntersection(const Ray& inOut_Ray)
{
    if (internalGeometry == 0) return 0;
    RayHit hit;
    if (internalGeometry->doIntersection(inOut_Ray, &hit) && hit.ray() != 0) return new Ray(*hit.ray());
    return 0;
}

bool FunctionalExplicitSurface::doIntersection(const Ray& inRay, RayHit* outHit)
{
    return internalGeometry ? internalGeometry->doIntersection(inRay, outHit) : false;
}

/*
Check the general interface contract in superclass method
Geometry.doExtraInformation.
@param inRay
@param inT
@param outData
*/
void FunctionalExplicitSurface::doExtraInformation(const Ray& inRay, double inT, RayHit* outData)
{
    if (internalGeometry == 0 || outData == 0) return;
    RayHit hit;
    if (internalGeometry->doIntersection(inRay.withT(inT), &hit)) outData->clone(hit);
}

/*
Check the general interface contract in superclass method
Geometry.doContainmentTest.
\todo  Check efficiency for this implementation. Note that for the
special application of volume rendering generation, it is better
to provide another method, to add voxels after a path following
over the line.
@return INSIDE, OUTSIDE or LIMIT constant value
*/
int FunctionalExplicitSurface::doContainmentTest(const Vector3Dd& p, double distanceTolerance)
{
    return internalGeometry ? internalGeometry->doContainmentTest(p, distanceTolerance) : OUTSIDE;
}
