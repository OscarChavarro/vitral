#ifndef __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_SURFACE_QUADMESH_H__
#define __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_SURFACE_QUADMESH_H__

#include "vsdk/toolkit/environment/geometry/surface/Surface.h"
#include <string>
#include <vector>

class Vertex;
class Ray;
class RayHit;
class TriangleMeshGroup;

class QuadMesh : public Surface {
private:
    std::string name;

    std::vector<double> vertexPositions;
    std::vector<double> vertexNormals;
    std::vector<double> vertexBinormals;
    std::vector<double> vertexTangents;
    std::vector<double> vertexColors;
    std::vector<double> vertexUvs;

    std::vector< std::vector<int> > incidentQuadsPerVertexArray;
    std::vector<int> quadIndices;

    double* calculateMinMaxPositions();

public:
    QuadMesh();

    std::string getName() const;
    void setName(const std::string& name);

    void getVertexAt(int i, Vertex& vertex) const;

    void initVertexPositionsArray(int n);
    void initVertexColorsArray();
    void initVertexNormalsArray();

    void setVertexes(const std::vector<Vertex>& vertexes);
    void initQuadArrays(int n);

    void setVertexAt(int i, const Vertex& vertex);
    void setQuadAt(int i, int p0, int p1, int p2, int p3);

    int getNumVertices() const;
    int getNumQuads() const;

    std::vector<double>& getVertexPositions();
    std::vector<double>& getVertexNormals();
    std::vector<double>& getVertexBinormals();
    std::vector<double>& getVertexTangents();
    std::vector<double>& getVertexColors();
    std::vector<double>& getVertexUvs();
    std::vector<int>& getQuadIndices();

    void calculateNormals();

    virtual double* getMinMax();

    TriangleMeshGroup* exportToTriangleMeshGroup();

    Ray* doIntersection(const Ray& inOut_Ray);
    virtual bool doIntersection(const Ray& inRay, RayHit* outHit);
    virtual void doExtraInformation(const Ray& inRay, double inT, RayHit* outData);

    std::string toString() const;
};

#endif
