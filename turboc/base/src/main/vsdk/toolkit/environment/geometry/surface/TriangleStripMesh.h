#ifndef __TRIANGLE_STRIP_MESH__
#define __TRIANGLE_STRIP_MESH__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/environment/geometry/surface/Surface.h"
class Vertex;
class Ray;
class RayHit;

class TriangleStripMesh : public Surface {
private:
    // Basic mesh data model
    String name;
    ArrayList<Vertex> vertexes;
    ArrayList< ArrayList<int> > strips;

    double* calculateMinMaxPositions();

public:
    static const Class CLASS;
    virtual const Class *getClass() const;

    TriangleStripMesh();

    virtual double* getMinMax();

    const ArrayList<Vertex>& getVertexes() const;
    Vertex getVertexAt(int index) const;
    void setVertexes(const ArrayList<Vertex>& vertexes);

    void setStrips(const ArrayList< ArrayList<int> >& indexes);
    ArrayList< ArrayList<int> >& getStrips();

    Ray* doIntersectionFirstHit(const Ray& inOut_Ray);
    virtual bool doIntersectionFirstHit(const Ray& inRay, RayHit* outHit);
    virtual void doExtraInformation(const Ray& inRay, double inT, RayHit* outData);
};

#endif
