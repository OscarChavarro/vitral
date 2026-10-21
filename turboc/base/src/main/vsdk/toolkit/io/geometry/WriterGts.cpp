#include <stddef.h>
#include "java/lang/Double.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
#include "vsdk/toolkit/io/geometry/WriterCommon.h"
#include "vsdk/toolkit/io/geometry/WriterGts.h"


class WriterGtsEdge {
public:
    int from;
    int to;
    WriterGtsEdge() : from(0), to(0) {}
};

class WriterGtsTriangle {
public:
    int point0;
    int point1;
    int point2;
    WriterGtsTriangle() : point0(0), point1(0), point2(0) {}
};

static void line(OutputStream& os, const String& text)
{
    PersistenceElement::writeAsciiLine(os, text.c_str());
}

static int addEdge(ArrayList<WriterGtsEdge>& edges, int from, int to)
{
    int i;

    for ( i = 0; i < edges.size(); i++ ) {
        const WriterGtsEdge& e = edges[i];
        if ( (e.from == from && e.to == to) ||
             (e.from == to && e.to == from) ) {
            return i;
        }
    }

    i = (int)edges.size();
    WriterGtsEdge e;
    e.from = from;
    e.to = to;
    edges.add(e);
    return i;
}

static long long exportMesh(OutputStream& os, TriangleMesh* mesh,
                     long long offset)
{
    int nv = mesh->getNumVertices();
    int nt = mesh->getNumTriangles();
    ArrayList<WriterGtsEdge> edges;
    ArrayList<WriterGtsTriangle> triangles;
    int i;

    //- Compute edges -------------------------------------------------
    ArrayList<int>& t = mesh->getTriangleIndexes();
    for ( i = 0; i < nt; i++ ) {
        WriterGtsTriangle tt;
        tt.point0 = addEdge(edges, t[3*i], t[3*i+1]);
        tt.point1 = addEdge(edges, t[3*i+1], t[3*i+2]);
        tt.point2 = addEdge(edges, t[3*i+2], t[3*i]);
        triangles.add(tt);
    }

    //- Write GTS header ----------------------------------------------
    line(os, String::valueOf(nv) + " " +
         String::valueOf((long)edges.size()) + " " +
         String::valueOf((long)triangles.size()) +
         " GtsSurface GtsFace GtsEdge GtsVertex");

    //- Write vertices ------------------------------------------------
    ArrayList<double>& v = mesh->getVertexPositions();
    for ( i = 0; i < nv; i++ ) {
        line(os, Double::toString(v[3*i]) + " " +
             Double::toString(v[3*i+1]) + " " +
             Double::toString(v[3*i+2]));
    }

    //- Write edges ---------------------------------------------------
    for ( i = 0; i < edges.size(); i++ ) {
        const WriterGtsEdge& e = edges[i];
        line(os, String::valueOf(e.from+1) + " " +
             String::valueOf(e.to+1));
    }

    //- Write triangles -----------------------------------------------
    for ( i = 0; i < triangles.size(); i++ ) {
        const WriterGtsTriangle& tt = triangles[i];
        line(os, String::valueOf(tt.point0+1) + " " +
             String::valueOf(tt.point1+1) + " " +
             String::valueOf(tt.point2+1));
    }

    return offset + nv;
}


void WriterGts::exportEnvironment(OutputStream& outputStream,
                                  SimpleScene* scene)
{
    ArrayList<SimpleBody*>& objs = scene->getSimpleBodies();
    long long baseVertexStart = 0;
    int i;

    for ( i = 0; i < objs.size(); i++ ) {
        TriangleMesh* mesh = writerMeshOf(objs.get(i)->getGeometry());

        //-----------------------------------------------------------------
        if ( mesh != NULL ) {
            baseVertexStart += exportMesh(outputStream, mesh, baseVertexStart);
        }
    }
}
