//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - October 15 2007 - Oscar Chavarro: Original base version               =
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

public class WriterObj extends PersistenceElement {

    private static long
    exportMesh(OutputStream inOutputStream, TriangleMesh mesh, long offset)
        throws Exception
    {
        Vertex v[] = mesh.getVertexes();
        Triangle t[] = mesh.getTriangles();
        int i;
        Vector3D p, n;
        Matrix4x4 R = new Matrix4x4();

        R.axisRotation(Math.toRadians(-90), new Vector3D(1, 0, 0));

        writeAsciiLine(inOutputStream, "# " + v.length + " vertex positions");
        for ( i = 0; i < v.length; i++ ) {
            p = R.multiply(v[i].position);
            writeAsciiLine(inOutputStream, "v " + p.x + " " + p.y + " " + p.z);
        }

        writeAsciiLine(inOutputStream, "# " + v.length + " vertex texture coordinates");
        for ( i = 0; i < v.length; i++ ) {
            writeAsciiLine(inOutputStream, "vt " + v[i].u + " " + v[i].v);
        }

        writeAsciiLine(inOutputStream, "# " + v.length + " vertex normals");
        for ( i = 0; i < v.length; i++ ) {
            n = R.multiply(v[i].normal);
            writeAsciiLine(inOutputStream, "vn " + n.x + " " + n.y + " " + n.z);
        }

        writeAsciiLine(inOutputStream, "# " + t.length + " triangles");
            long n0, n1, n2;
        writeAsciiLine(inOutputStream, "o NewObject");
        for ( i = 0; i < t.length; i++ ) {
            n0 = t[i].p0 + offset + 1;
            n1 = t[i].p1 + offset + 1;
            n2 = t[i].p2 + offset + 1;
            writeAsciiLine(inOutputStream, "f " + 
                n0 + "/" + n0 + "/" + n0 + " " +
                n1 + "/" + n1 + "/" + n1 + " " +
                n2 + "/" + n2 + "/" + n2);
        }

        return offset + v.length;
    }

    public static void
    exportEnvironment(OutputStream inOutputStream, SimpleScene inScene)
        throws Exception
    {
        //-----------------------------------------------------------------
        writeAsciiLine(inOutputStream, "# OBJ File generated with VitralSDK.");
        writeAsciiLine(inOutputStream, "# http://sophia.javeriana.edu.co/~ochavarr");
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
