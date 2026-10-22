#ifndef __TRIANGLE_MESH_GROUP__
#define __TRIANGLE_MESH_GROUP__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/environment/geometry/surface/Surface.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMesh.h"
class Ray;
class RayHit;

class TriangleMeshGroup : public Surface {
private:
    ArrayList<TriangleMesh> meshes;
    int intersectionMeshIndex;
    int intersectionTriangleIndex;

    double* computeMinMaxPositions();

public:
    static const Class CLASS;
    virtual const Class *getClass() const;

    TriangleMeshGroup();
    TriangleMeshGroup(const ArrayList<TriangleMesh>& meshes);
    TriangleMeshGroup(const TriangleMeshGroup& group);

    ArrayList<TriangleMesh>& getMeshes();
    void setMeshes(const ArrayList<TriangleMesh>& meshes);
    void addMesh(const TriangleMesh& mesh);
    TriangleMesh& getMeshAt(int index);

    virtual double* getMinMax();
    void calculateMinMaxPositions();

    Ray* doIntersectionFirstHit(const Ray& inOut_Ray);
    virtual bool doIntersectionFirstHit(const Ray& inRay, RayHit* outHit);
    virtual void doExtraInformation(const Ray& inRay, double inT, RayHit* outData);

    int* doIntersectionInformation();

    virtual int doContainmentTest(const Vector3Dd& p, double distanceTolerance);

    String toString() const;

    TriangleMeshGroup* exportToTriangleMeshGroup();
};

#endif
