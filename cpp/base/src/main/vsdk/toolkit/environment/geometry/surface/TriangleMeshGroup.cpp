#include "vsdk/toolkit/environment/geometry/surface/TriangleMeshGroup.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMesh.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/environment/geometry/elements/Ray.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/environment/geometry/elements/RayHit.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/environment/geometry/volume/Box.h"
#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include "java/lang/String.h"
#include <cstdio>

TriangleMeshGroup::TriangleMeshGroup()
    : intersectionMeshIndex(-1), intersectionTriangleIndex(-1)
{
}

TriangleMeshGroup::TriangleMeshGroup(const java::ArrayList<TriangleMesh>& meshes)
    : meshes(meshes), intersectionMeshIndex(-1), intersectionTriangleIndex(-1)
{
}

TriangleMeshGroup::TriangleMeshGroup(const TriangleMeshGroup& group)
    : meshes(group.meshes),
      intersectionMeshIndex(group.intersectionMeshIndex),
      intersectionTriangleIndex(group.intersectionTriangleIndex)
{
}

java::ArrayList<TriangleMesh>& TriangleMeshGroup::getMeshes()
{
    return meshes;
}

void TriangleMeshGroup::setMeshes(const java::ArrayList<TriangleMesh>& inMeshes)
{
    meshes = inMeshes;
}

void TriangleMeshGroup::addMesh(const TriangleMesh& mesh)
{
    meshes.add(mesh);
}

TriangleMesh& TriangleMeshGroup::getMeshAt(int index)
{
    return meshes[index];
}

/*
@return a new 6 valued double array containing the coordinates of a min-max
bounding box for current geometry.
*/
double* TriangleMeshGroup::computeMinMaxPositions()
{
    double* minMax = new double[6];

    bool first = true;
    double minX = 1e308, minY = 1e308, minZ = 1e308;
    double maxX = -1e308, maxY = -1e308, maxZ = -1e308;

    for (long int i = 0; i < meshes.size(); i++) {
        double* mm = meshes[i].getMinMax();
        double x = mm[0], y = mm[1], z = mm[2];
        double X = mm[3], Y = mm[4], Z = mm[5];

        if (first) {
            minX = x; maxX = X;
            minY = y; maxY = Y;
            minZ = z; maxZ = Z;
            first = false;
        }

        if (x < minX) minX = x;
        if (y < minY) minY = y;
        if (z < minZ) minZ = z;
        if (X > maxX) maxX = X;
        if (Y > maxY) maxY = Y;
        if (Z > maxZ) maxZ = Z;

        delete [] mm;
    }

    if (first) {
        minX = minY = minZ = 0.0;
        maxX = maxY = maxZ = 0.0;
    }

    minMax[0] = minX;
    minMax[1] = minY;
    minMax[2] = minZ;
    minMax[3] = maxX;
    minMax[4] = maxY;
    minMax[5] = maxZ;
    return minMax;
}

double* TriangleMeshGroup::getMinMax()
{
    return computeMinMaxPositions();
}

/*
Kept for source and binary compatibility. MinMax values are calculated
without storing state by getMinMax().
Note that for each position in this array:
    0 - min (x)
    1 - min (y)
    2 - min (z)
    3 - max (x)
    4 - max (y)
    5 - max (z)
*/
void TriangleMeshGroup::calculateMinMaxPositions()
{
    double* mm = computeMinMaxPositions();
    delete [] mm;
}

/*
Check the general interface contract in superclass method
Geometry.doIntersection.

@param inOut_Ray
@return true if given ray intersects current TriangleMeshGroup
*/
Ray* TriangleMeshGroup::doIntersection(const Ray& inOut_Ray)
{
    RayHit hit(RayHit::DETAIL_NONE, true);
    if (doIntersection(inOut_Ray, &hit) && hit.ray() != 0) {
        return new Ray(*hit.ray());
    }
    return 0;
}

bool TriangleMeshGroup::doIntersection(const Ray& inRay, RayHit* outHit)
{
    double* mm = getMinMax();
    Vector3Dd size(mm[3]-mm[0], mm[4]-mm[1], mm[5]-mm[2]);
    Vector3Dd center((mm[3]+mm[0])/2, (mm[4]+mm[1])/2, (mm[5]+mm[2])/2);
    delete [] mm;

    Box bbox(size);
    Ray localRay(inRay.origin().subtract(center), inRay.direction(), inRay.t());
    RayHit coarseHit(RayHit::DETAIL_NONE, false);
    if (!bbox.doIntersection(localRay, &coarseHit)) {
        intersectionMeshIndex = -1;
        intersectionTriangleIndex = -1;
        return false;
    }

    double minT = 1e308;
    RayHit bestHit;
    bool hasBestHit = false;
    int bestMesh = -1;
    int bestTriangle = -1;
    int triangleInformation = -1;

    for (long int i = 0; i < meshes.size(); i++) {
        TriangleMesh& mesh = meshes[i];
        RayHit meshHit;
        if (mesh.doIntersection(inRay, &meshHit, &triangleInformation) &&
            meshHit.ray() != 0 && meshHit.ray()->t() < minT) {
            minT = meshHit.ray()->t();
            bestHit.clone(meshHit);
            bestMesh = (int)i;
            bestTriangle = triangleInformation;
            hasBestHit = true;
        }
    }

    if (!hasBestHit) {
        intersectionMeshIndex = -1;
        intersectionTriangleIndex = -1;
        return false;
    }

    intersectionMeshIndex = bestMesh;
    intersectionTriangleIndex = bestTriangle;

    if (outHit != 0) {
        outHit->clone(bestHit);
    }
    return true;
}

/*
Check the general interface contract in superclass method
Geometry.doExtraInformation.
@param inRay
@param inT
@param outData
*/
void TriangleMeshGroup::doExtraInformation(const Ray& inRay, double inT, RayHit* outData)
{
    if (outData == 0) {
        return;
    }
    doIntersection(inRay.withT(inT), outData);
}

int* TriangleMeshGroup::doIntersectionInformation()
{
    int* info = new int[2];
    info[0] = intersectionMeshIndex;
    info[1] = intersectionTriangleIndex;
    return info;
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
int TriangleMeshGroup::doContainmentTest(const Vector3Dd& p, double distanceTolerance)
{
    for (long int i = 0; i < meshes.size(); i++) {
        int status = meshes[i].doContainmentTest(p, distanceTolerance);
        if (status != OUTSIDE) {
            return status;
        }
    }
    return OUTSIDE;
}

/*
Provides an object to text report convertion, optimized for human
readability and debugging. Do not use for serialization or persistence
purposes.
@return human readable report from current mesh group
*/
static java::String intToStr(int val) {
    char buf[32];
    snprintf(buf, sizeof(buf), "%d", val);
    return java::String(buf);
}

java::String TriangleMeshGroup::toString() const
{
    return java::String("TriangleMeshGroup < #Mesh: ") + intToStr((int)meshes.size()) + " >";
}

/*
Check the general interface contract in superclass method
Geometry.doVoxelization.
*/
void TriangleMeshGroup::doVoxelization(VoxelVolume& vv, const Matrix4x4d& M, ProgressMonitor* reporter)
{
    // Chain of responsability behavior design pattern with TriangleMesh
    for (long int i = 0; i < meshes.size(); i++) {
        meshes[i].doVoxelization(vv, M, reporter);
    }
}

TriangleMeshGroup* TriangleMeshGroup::exportToTriangleMeshGroup()
{
    return this;
}
