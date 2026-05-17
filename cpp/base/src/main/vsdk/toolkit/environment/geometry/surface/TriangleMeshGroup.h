#ifndef __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_SURFACE_TRIANGLEMESHGROUP_H__
#define __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_SURFACE_TRIANGLEMESHGROUP_H__

#include "Surface.h"
#include "TriangleMesh.h"
#include <string>
#include <vector>

class Ray;
class RayHit;
class VoxelVolume;
class Matrix4x4d;
class ProgressMonitor;

class TriangleMeshGroup : public Surface {
private:
    std::vector<TriangleMesh> meshes;
    int intersectionMeshIndex;
    int intersectionTriangleIndex;

    double* computeMinMaxPositions();

public:
    TriangleMeshGroup();
    TriangleMeshGroup(const std::vector<TriangleMesh>& meshes);
    TriangleMeshGroup(const TriangleMeshGroup& group);

    std::vector<TriangleMesh>& getMeshes();
    void setMeshes(const std::vector<TriangleMesh>& meshes);
    void addMesh(const TriangleMesh& mesh);
    TriangleMesh& getMeshAt(int index);

    virtual double* getMinMax();
    void calculateMinMaxPositions();

    Ray* doIntersection(const Ray& inOut_Ray);
    virtual bool doIntersection(const Ray& inRay, RayHit* outHit);
    virtual void doExtraInformation(const Ray& inRay, double inT, RayHit* outData);

    int* doIntersectionInformation();

    virtual int doContainmentTest(const Vector3Dd& p, double distanceTolerance);

    std::string toString() const;

    void doVoxelization(VoxelVolume& vv, const Matrix4x4d& M, ProgressMonitor* reporter);

    TriangleMeshGroup* exportToTriangleMeshGroup();
};

#endif
