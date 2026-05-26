#ifndef __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_SURFACE_TRIANGLEMESHGROUP_H__
#define __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_SURFACE_TRIANGLEMESHGROUP_H__

#include "vsdk/toolkit/environment/geometry/surface/Surface.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMesh.h"
#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "java/lang/String.h"
#include <string>
#include "java/lang/String.h"

class Ray;
class RayHit;
class VoxelVolume;
class Matrix4x4d;
class ProgressMonitor;

class TriangleMeshGroup : public Surface {
private:
    java::ArrayList<TriangleMesh> meshes;
    int intersectionMeshIndex;
    int intersectionTriangleIndex;

    double* computeMinMaxPositions();

public:
    TriangleMeshGroup();
    TriangleMeshGroup(const java::ArrayList<TriangleMesh>& meshes);
    TriangleMeshGroup(const TriangleMeshGroup& group);

    java::ArrayList<TriangleMesh>& getMeshes();
    void setMeshes(const java::ArrayList<TriangleMesh>& meshes);
    void addMesh(const TriangleMesh& mesh);
    TriangleMesh& getMeshAt(int index);

    virtual double* getMinMax();
    void calculateMinMaxPositions();

    Ray* doIntersection(const Ray& inOut_Ray);
    virtual bool doIntersection(const Ray& inRay, RayHit* outHit);
    virtual void doExtraInformation(const Ray& inRay, double inT, RayHit* outData);

    int* doIntersectionInformation();

    virtual int doContainmentTest(const Vector3Dd& p, double distanceTolerance);

    java::String toString() const;

    void doVoxelization(VoxelVolume& vv, const Matrix4x4d& M, ProgressMonitor* reporter);

    TriangleMeshGroup* exportToTriangleMeshGroup();
};

#endif
