#ifndef __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_SURFACE_TRIANGLESTRIPMESH_H__
#define __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_SURFACE_TRIANGLESTRIPMESH_H__

#include "Surface.h"
#include <string>
#include <vector>

class Vertex;
class Ray;
class RayHit;

class TriangleStripMesh : public Surface {
private:
    // Basic mesh data model
    std::string name;
    std::vector<Vertex> vertexes;
    std::vector< std::vector<int> > strips;

    double* calculateMinMaxPositions();

public:
    TriangleStripMesh();

    virtual double* getMinMax();

    const std::vector<Vertex>& getVertexes() const;
    const Vertex& getVertexAt(int index) const;
    void setVertexes(const std::vector<Vertex>& vertexes);

    void setStrips(const std::vector< std::vector<int> >& indexes);
    const std::vector< std::vector<int> >& getStrips() const;

    Ray* doIntersection(const Ray& inOut_Ray);
    virtual bool doIntersection(const Ray& inRay, RayHit* outHit);
    virtual void doExtraInformation(const Ray& inRay, double inT, RayHit* outData);
};

#endif
