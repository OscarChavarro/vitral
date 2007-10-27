//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - October 26 2007 - Oscar Chavarro: Original base version               =
//===========================================================================

package vsdk.toolkit.io.geometry;

// Java basic classes
import java.util.ArrayList;
import java.io.OutputStream;

// VSDK Classes
import vsdk.toolkit.common.Matrix4x4;
import vsdk.toolkit.common.Triangle;
import vsdk.toolkit.common.Vertex;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.TriangleMesh;
import vsdk.toolkit.environment.geometry.TriangleMeshGroup;
import vsdk.toolkit.environment.geometry.FunctionalExplicitSurface;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.io.PersistenceElement;

class _WriterGtsEdge
{
    public int from;
    public int to;
}

class _WriterGtsTriangle
{
    public int p0;
    public int p1;
    public int p2;
}

public class WriterGts extends PersistenceElement {

    private static int
    addEdge(ArrayList<_WriterGtsEdge> edges, int from, int to)
    {
        _WriterGtsEdge e;
        int i;

        for ( i = 0; i < edges.size(); i++ ) {
            e = edges.get(i);
            if ( (e.from == from && e.to == to) ||
                 (e.from == to && e.to == from) ) {
                return i;
            }
        }

        i = edges.size();
        e = new _WriterGtsEdge();
        e.from = from;
        e.to = to;
        edges.add(e);
        return i;
    }

    private static long
    exportMesh(OutputStream inOutputStream, TriangleMesh mesh, long offset)
        throws Exception
    {
        Vertex v[] = mesh.getVertexes();
        Triangle t[] = mesh.getTriangles();
        ArrayList<_WriterGtsEdge> edges;
        ArrayList<_WriterGtsTriangle> triangles;
        edges = new ArrayList<_WriterGtsEdge>();
        triangles = new ArrayList<_WriterGtsTriangle>();
        _WriterGtsEdge e;
        _WriterGtsTriangle tt;
        int i;

        for ( i = 0; i < t.length; i++ ) {
            tt = new _WriterGtsTriangle();
            tt.p0 = addEdge(edges, t[i].p0, t[i].p1);
            tt.p1 = addEdge(edges, t[i].p1, t[i].p2);
            tt.p2 = addEdge(edges, t[i].p2, t[i].p0);
            triangles.add(tt);
        }

        writeAsciiLine(inOutputStream, "" + v.length + " " + edges.size() + " " + triangles.size() + " GtsSurface GtsFace GtsEdge GtsVertex");

        for ( i = 0; i < v.length; i++ ) {
            writeAsciiLine(inOutputStream, "" + v[i].position.x + " " +
                v[i].position.y + " " + v[i].position.z);
        }
        for ( i = 0; i < edges.size(); i++ ) {
            e = edges.get(i);
            writeAsciiLine(inOutputStream, "" + (e.from+1) + " " + (e.to+1));
        }
        for ( i = 0; i < triangles.size(); i++ ) {
            tt = triangles.get(i);
            writeAsciiLine(inOutputStream, "" + (tt.p0+1) + " " + (tt.p1+1) + " " + (tt.p2+1));
        }

        return offset + v.length;
    }

    public static void
    exportEnvironment(OutputStream inOutputStream, SimpleScene inScene)
        throws Exception
    {
        //-----------------------------------------------------------------
        ArrayList<SimpleBody> objs;
        Geometry g;
        TriangleMesh mesh;

        objs = inScene.getSimpleBodies();
        long baseVertexStart = 0;

        int i;
        for ( i = 0; i < objs.size(); i++ ) {

            //-----------------------------------------------------------------
            g = objs.get(i).getGeometry();
            mesh = null;
            if ( g instanceof FunctionalExplicitSurface ) {
                mesh = ((FunctionalExplicitSurface)g).getInternalTriangleMesh();
            }
            else if ( g instanceof TriangleMesh ) {
                mesh = (TriangleMesh)g;
            }

            //-----------------------------------------------------------------
            if ( mesh != null ) {
                baseVertexStart += exportMesh(inOutputStream, mesh, baseVertexStart);
            }
        }
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
