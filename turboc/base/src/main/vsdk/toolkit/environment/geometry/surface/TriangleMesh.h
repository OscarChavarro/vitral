#ifndef __TRIANGLE_MESH__
#define __TRIANGLE_MESH__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/environment/geometry/surface/Surface.h"
#include "vsdk/toolkit/environment/geometry/element/Vertex.h"
#include "vsdk/toolkit/environment/geometry/element/Triangle.h"
class SimpleMaterial;
class Image;
class Ray;
class RayHit;
class TriangleMeshGroup;
class InfinitePlane;

class TriangleMesh : public Surface {
private:
    String name;

    ArrayList<double> vertexPositions;
    ArrayList<double> vertexNormals;
    ArrayList<double> vertexBinormals;
    ArrayList<double> vertexTangents;
    ArrayList<double> vertexColors;
    ArrayList<BOOLEAN> vertexSelections;
    ArrayList<double> vertexUvs;

    ArrayList< ArrayList<int> > incidentTrianglesPerVertexArray;

    ArrayList<int> triangleIndexes;
    ArrayList<double> triangleNormals;

    ArrayList<SimpleMaterial*> materials;
    ArrayList<Image*> textures;

    ArrayList< ArrayList<int> > textureRanges;
    ArrayList< ArrayList<int> > materialRanges;

    int intersectionTriangleIndex;
    BOOLEAN ownsMaterials;

    static BOOLEAN intersectTriangle(const Ray& ray, const Vector3Dd& v0, const Vector3Dd& v1, const Vector3Dd& v2, double& t, double& u, double& v);
    void interpolateTriangleData(int triangleIndex, double u, double v, RayHit* outHit, const Vector3Dd& rayDirection);
    void fillMaterialAndTexture(int triangleIndex, RayHit* outHit);
    BOOLEAN doIntersectionInternal(const Ray& inRay, RayHit* outHit, int* outTriangleIndex);

    double* calculateMinMaxPositions();

    void appendVertices(const ArrayList<double>& ev);
    void appendTriangles(const ArrayList<int>& et);

    void simpleTriangleCut(InfinitePlane& p, ArrayList<double>& extraVertices,
                           ArrayList<int>& extraTriangles, int nv, int i,
                           const Vector3Dd& p1, const Vector3Dd& p2, const Vector3Dd& p3);
    void halfTriangleCut(InfinitePlane& p, ArrayList<double>& extraVertices,
                         ArrayList<int>& extraTriangles, int nv, int i, int j,
                         const Vector3Dd& p1, const Vector3Dd& p2, const Vector3Dd& p3);
    void doubleTriangleCut(InfinitePlane& p, ArrayList<double>& extraVertices,
                           ArrayList<int>& extraTriangles, int nv, int i, int j,
                           const Vector3Dd& p1, const Vector3Dd& p2, const Vector3Dd& p3);

public:
    static const Class CLASS;
    virtual const Class *getClass() const;

    TriangleMesh();
    TriangleMesh(const TriangleMesh& other);
    ~TriangleMesh();

    TriangleMesh* clone() const;

    String getName() const;
    void setName(const String& name);

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

    void setVertexes(const ArrayList<Vertex>& vertexes, BOOLEAN withNormals, BOOLEAN withBinormals, BOOLEAN withTangents, BOOLEAN withUvs);

    void initTriangleArrays(int n);
    void setTriangles(const ArrayList<Triangle>& triangles);

    void setTextures(const ArrayList<Image*>& textures);
    void setMaterials(const ArrayList<SimpleMaterial*>& materials);
    void setOwnsMaterials(BOOLEAN owns);

    void setVertexAt(int i, const Vertex& vertex);
    void setTriangleAt(int i, const Triangle& triangle);

    int getNumVertices() const;
    int getNumTriangles() const;

    ArrayList<BOOLEAN>& getVertexSelections();

    ArrayList<double>& getVertexPositions();
    ArrayList<double>& getVertexNormals();
    ArrayList<double>& getVertexBinormals();
    ArrayList<double>& getVertexTangents();
    ArrayList<double>& getVertexColors();
    ArrayList<double>& getVertexUvs();
    ArrayList<int>& getTriangleIndexes();
    ArrayList<double>& getTriangleNormals();

    ArrayList<SimpleMaterial*>& getMaterials();
    ArrayList<Image*>& getTextures();
    Image* getTextureAt(int index) const;
    void setTextureAt(int index, Image* image);

    ArrayList< ArrayList<int> >& getTextureRanges();
    ArrayList<int> getTextureRangeAt(int spanRange) const;
    void setTextureRanges(const ArrayList< ArrayList<int> >& ranges);

    ArrayList< ArrayList<int> >& getMaterialRanges();
    ArrayList<int> getMaterialRangeAt(int spanRange) const;
    void setMaterialRanges(const ArrayList< ArrayList<int> >& ranges);

    void calculateNormals();
    void reorientateNormals();

    int doIntersectionInformation() const;

    Ray* doIntersectionFirstHit(const Ray& inOut_Ray);
    virtual BOOLEAN doIntersectionFirstHit(const Ray& inRay, RayHit* outHit);
    BOOLEAN doIntersectionFirstHit(const Ray& inRay, RayHit* outHit, int* outTriangleIndex);
    virtual void doExtraInformation(const Ray& inRay, double inT, RayHit* outData);

    virtual int doContainmentTest(const Vector3Dd& p, double distanceTolerance);
    virtual double* getMinMax();

    TriangleMeshGroup* exportToTriangleMeshGroup();

    void compact();
    String toString() const;

    void removeSelectedVertices();
    void slice(InfinitePlane& p);
};

#endif
