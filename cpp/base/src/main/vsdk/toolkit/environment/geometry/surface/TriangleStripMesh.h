#ifndef __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_SURFACE_TRIANGLESTRIPMESH_H__
#define __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_SURFACE_TRIANGLESTRIPMESH_H__

#include "vsdk/toolkit/environment/geometry/surface/Surface.h"
#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "java/lang/String.h"
#include "java/lang/String.h"
#include "java/lang/String.h"

class Vertex;
class Ray;
class RayHit;

class TriangleStripMesh : public Surface {
private:
    // Basic mesh data model
    java::String name;
    java::ArrayList<Vertex> vertexes;
    java::ArrayList< java::ArrayList<int> > strips;

    double* calculateMinMaxPositions();

public:
    TriangleStripMesh();

    virtual double* getMinMax();

    const java::ArrayList<Vertex>& getVertexes() const;
    Vertex getVertexAt(int index) const;
    void setVertexes(const java::ArrayList<Vertex>& vertexes);

    void setStrips(const java::ArrayList< java::ArrayList<int> >& indexes);
    java::ArrayList< java::ArrayList<int> >& getStrips();

    Ray* doIntersection(const Ray& inOut_Ray);
    virtual bool doIntersection(const Ray& inRay, RayHit* outHit);
    virtual void doExtraInformation(const Ray& inRay, double inT, RayHit* outData);
};

#endif
