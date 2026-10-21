#include <stddef.h>
#include <math.h>

#include "java/lang/Double.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
#include "vsdk/toolkit/io/geometry/WriterCommon.h"
#include "vsdk/toolkit/io/geometry/WriterObj.h"


static void line(OutputStream& os, const String& text)
{
    PersistenceElement::writeAsciiLine(os, text.c_str());
}

static String d(double value)
{
    return Double::toString(value);
}

static long long exportMesh(OutputStream& os, TriangleMesh* mesh,
                     long long offset)
{
    int nv = mesh->getNumVertices();
    int nt = mesh->getNumTriangles();
    ArrayList<double>& vp = mesh->getVertexPositions();
    ArrayList<double>& vn = mesh->getVertexNormals();
    ArrayList<double>& vuv = mesh->getVertexUvs();
    bool withNormals = vn.size() > 0;
    bool withUvs = vuv.size() > 0;
    int i;
    Vector3Dd p;
    Vector3Dd n;
    Matrix4x4d R;

    R = R.axisRotation(-M_PI / 2.0, Vector3Dd(1, 0, 0));

    //-----------------------------------------------------------------
    line(os, String("# ") + String::valueOf(nv) +
         " vertex positions");
    for ( i = 0; i < nv; i++ ) {
        p = R.multiply(Vector3Dd(vp[3*i], vp[3*i+1], vp[3*i+2]));
        line(os, String("v ") + d(p.x()) + " " + d(p.y()) + " " +
             d(p.z()));
    }

    //-----------------------------------------------------------------
    line(os, String("# ") + String::valueOf(nv) +
         " vertex texture coordinates");
    for ( i = 0; i < nv && withUvs; i++ ) {
        line(os, String("vt ") + d(vuv[2*i]) + " " + d(vuv[2*i+1]));
    }

    //-----------------------------------------------------------------
    line(os, String("# ") + String::valueOf(nv) +
         " vertex normals");
    for ( i = 0; i < nv && withNormals; i++ ) {
        n = R.multiply(Vector3Dd(vn[3*i], vn[3*i+1], vn[3*i+2]));
        line(os, String("vn ") + d(n.x()) + " " + d(n.y()) + " " +
             d(n.z()));
    }

    //-----------------------------------------------------------------
    ArrayList<int>& t = mesh->getTriangleIndexes();

    line(os, String("# ") + String::valueOf(nt) + " triangles");
    long long n0;
    long long n1;
    long long n2;
    line(os, "o NewObject");
    for ( i = 0; i < nt; i++ ) {
        n0 = t[3*i] + offset + 1;
        n1 = t[3*i+1] + offset + 1;
        n2 = t[3*i+2] + offset + 1;
        String a = String::valueOf(n0);
        String b = String::valueOf(n1);
        String c = String::valueOf(n2);
        if ( withUvs && withNormals ) {
            line(os, String("f ") +
                 a + "/" + a + "/" + a + " " +
                 b + "/" + b + "/" + b + " " +
                 c + "/" + c + "/" + c);
        }
        else {
            line(os, String("f ") + a + " " + b + " " + c);
        }
    }

    return offset + nv;
}


void WriterObj::exportEnvironment(OutputStream& outputStream,
                                  SimpleScene* scene)
{
    //-----------------------------------------------------------------
    line(outputStream, "# OBJ File generated with VitralSDK.");
    line(outputStream, "# http://sophia.javeriana.edu.co/~ochavarr");
    //-----------------------------------------------------------------
    ArrayList<SimpleBody*>& objs = scene->getSimpleBodies();
    long long baseVertexStart = 0;
    int i;

    for ( i = 0; i < objs.size(); i++ ) {
        TriangleMesh* mesh = writerMeshOf(objs.get(i)->getGeometry());

        //-----------------------------------------------------------------
        if ( mesh != NULL ) {
            // As in the Java version, the offset returned by exportMesh
            // already includes the previous one
            baseVertexStart += exportMesh(outputStream, mesh, baseVertexStart);
        }
    }
}
