#ifndef __TRIANGLE_STRIP_MESH__
#define __TRIANGLE_STRIP_MESH__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/environment/geometry/surface/Surface.h"
class Vertex;
class Ray;
class RayHit;
class TriangleMesh;

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

    /**
    The triangles of the strips, as a triangle mesh sharing the vertex
    positions: in each strip, every vertex after the first two closes a
    triangle with the two before it, alternating their order so all the
    triangles keep the orientation of the first one. Triangles with a
    repeated vertex (used to join strips) are skipped.
    @return a new mesh (owned by the caller) with positions and triangles
    only, or null if there are no vertexes or strips
    */
    TriangleMesh* exportToTriangleMesh();

    Ray* doIntersectionFirstHit(const Ray& inOut_Ray);
    virtual bool doIntersectionFirstHit(const Ray& inRay, RayHit* outHit);
    virtual void doExtraInformation(const Ray& inRay, double inT, RayHit* outData);
};

#endif
