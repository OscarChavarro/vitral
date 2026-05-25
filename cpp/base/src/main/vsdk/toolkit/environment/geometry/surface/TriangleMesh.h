#ifndef __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_SURFACE_TRIANGLEMESH_H__
#define __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_SURFACE_TRIANGLEMESH_H__

#include "vsdk/toolkit/environment/geometry/surface/Surface.h"
#include <string>
#include <vector>

class Vertex;
class Triangle;
class SimpleMaterial;
class Image;
class Ray;
class RayHit;
class TriangleMeshGroup;
class InfinitePlane;
class VoxelVolume;
class Matrix4x4d;
class ProgressMonitor;

class TriangleMesh : public Surface {
private:
    std::string name;

    std::vector<double> vertexPositions;
    std::vector<double> vertexNormals;
    std::vector<double> vertexBinormals;
    std::vector<double> vertexTangents;
    std::vector<double> vertexColors;
    std::vector<bool> vertexSelections;
    std::vector<double> vertexUvs;

    std::vector< std::vector<int> > incidentTrianglesPerVertexArray;

    std::vector<int> triangleIndices;
    std::vector<double> triangleNormals;

    std::vector<SimpleMaterial*> materials;
    std::vector<Image*> textures;

    std::vector< std::vector<int> > textureRanges;
    std::vector< std::vector<int> > materialRanges;

    int intersectionTriangleIndex;

    static bool intersectTriangle(const Ray& ray, const Vector3Dd& v0, const Vector3Dd& v1, const Vector3Dd& v2, double& t, double& u, double& v);
    void interpolateTriangleData(int triangleIndex, double u, double v, RayHit* outHit, const Vector3Dd& rayDirection);
    void fillMaterialAndTexture(int triangleIndex, RayHit* outHit);
    bool doIntersectionInternal(const Ray& inRay, RayHit* outHit, int* outTriangleIndex);

    double* calculateMinMaxPositions();

    void appendVertices(const std::vector<double>& ev);
    void appendTriangles(const std::vector<int>& et);

    void simpleTriangleCut(InfinitePlane& p, std::vector<double>& extraVertices,
                           std::vector<int>& extraTriangles, int nv, int i,
                           const Vector3Dd& p1, const Vector3Dd& p2, const Vector3Dd& p3);
    void halfTriangleCut(InfinitePlane& p, std::vector<double>& extraVertices,
                         std::vector<int>& extraTriangles, int nv, int i, int j,
                         const Vector3Dd& p1, const Vector3Dd& p2, const Vector3Dd& p3);
    void doubleTriangleCut(InfinitePlane& p, std::vector<double>& extraVertices,
                           std::vector<int>& extraTriangles, int nv, int i, int j,
                           const Vector3Dd& p1, const Vector3Dd& p2, const Vector3Dd& p3);

public:
    TriangleMesh();
    TriangleMesh(const TriangleMesh& other);

    TriangleMesh* clone() const;

    std::string getName() const;
    void setName(const std::string& name);

    void getVertexAt(int i, Vertex& vertex) const;

    void initVertexPositionsArray(int n);
    void initVertexNormalsArray();
    void initVertexBinormalsArray();
    void initVertexTangentsArray();
    void initVertexColorsArray();
    void initVertexUvsArray();
    void initIncidentTrianglesPerVertexArray();

    void detachColors();
    void detachNormals();
    void detachUvs();

    void setVertexes(const std::vector<Vertex>& vertexes, bool withNormals, bool withBinormals, bool withTangents, bool withUvs);

    void initTriangleArrays(int n);
    void setTriangles(const std::vector<Triangle>& triangles);

    void setTextures(const std::vector<Image*>& textures);
    void setMaterials(const std::vector<SimpleMaterial*>& materials);

    void setVertexAt(int i, const Vertex& vertex);
    void setTriangleAt(int i, const Triangle& triangle);

    int getNumVertices() const;
    int getNumTriangles() const;

    std::vector<bool>& getVertexSelections();

    std::vector<double>& getVertexPositions();
    std::vector<double>& getVertexNormals();
    std::vector<double>& getVertexBinormals();
    std::vector<double>& getVertexTangents();
    std::vector<double>& getVertexColors();
    std::vector<double>& getVertexUvs();
    std::vector<int>& getTriangleIndexes();
    std::vector<double>& getTriangleNormals();

    std::vector<SimpleMaterial*>& getMaterials();
    std::vector<Image*>& getTextures();
    Image* getTextureAt(int index) const;
    void setTextureAt(int index, Image* image);

    std::vector< std::vector<int> >& getTextureRanges();
    std::vector<int> getTextureRangeAt(int spanRange) const;
    void setTextureRanges(const std::vector< std::vector<int> >& ranges);

    std::vector< std::vector<int> >& getMaterialRanges();
    std::vector<int> getMaterialRangeAt(int spanRange) const;
    void setMaterialRanges(const std::vector< std::vector<int> >& ranges);

    void calculateNormals();
    void reorientateNormals();

    int doIntersectionInformation() const;

    Ray* doIntersection(const Ray& inOut_Ray);
    virtual bool doIntersection(const Ray& inRay, RayHit* outHit);
    bool doIntersection(const Ray& inRay, RayHit* outHit, int* outTriangleIndex);
    virtual void doExtraInformation(const Ray& inRay, double inT, RayHit* outData);

    virtual int doContainmentTest(const Vector3Dd& p, double distanceTolerance);
    virtual double* getMinMax();

    void doVoxelization(VoxelVolume& vv, const Matrix4x4d& M, ProgressMonitor* reporter);

    TriangleMeshGroup* exportToTriangleMeshGroup();

    void compact();
    std::string toString() const;

    void removeSelectedVertices();
    void slice(InfinitePlane& p);
};

#endif
