#ifndef __TRIANGLE_STRIP_MESH__
#define __TRIANGLE_STRIP_MESH__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/environment/geometry/surface/Surface.h"
#include "vsdk/toolkit/environment/geometry/element/Vertex.h"
class Ray;
class RayHit;
class TriangleMesh;

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

    /**
    The triangles of the strips, as a triangle mesh sharing the vertex
    positions: in each strip, every vertex after the first two closes a
    triangle with the two before it, alternating their order so all the
    triangles keep the orientation of the first one. Triangles with a
    repeated vertex (used to join strips) are skipped.
    @return a new mesh with positions and triangles only, owned by the
    caller, or null if there are no vertexes or strips
    */
    TriangleMesh* exportToTriangleMesh() const;

    Ray* doIntersectionFirstHit(const Ray& inOut_Ray);
    virtual BOOLEAN doIntersectionFirstHit(const Ray& inRay, RayHit* outHit);
    virtual void doExtraInformation(const Ray& inRay, double inT, RayHit* outData);
};

#endif
