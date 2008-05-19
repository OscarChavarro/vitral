//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - May 19 2008 - Oscar Chavarro: Original base version                   =
//===========================================================================

package vsdk.toolkit.io.geometry;

// Java basic classes
import java.util.ArrayList;
import java.io.OutputStream;

// VSDK Classes
import vsdk.toolkit.common.VSDK;
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

public class WriterVtk extends PersistenceElement {

    private static void
    exportMesh(OutputStream inOutputStream, TriangleMesh mesh)
        throws Exception
    {
        double v[] = mesh.getVertexPositions();
        double n[] = mesh.getVertexNormals();
        int t[] = mesh.getTriangleIndexes();

        String line;
        writeAsciiLine(inOutputStream, "DATASET POLYDATA");

        //-----------------------------------------------------------------
        line = "POINTS " + (v.length/3) + " float";
        writeAsciiLine(inOutputStream, line);
        int i;
        float val;
        for ( i = 0; i < v.length; i++ ) {
            val = (float)(v[i]*1000.0);
            writeFloatBE(inOutputStream, val);
        }
        writeAsciiLine(inOutputStream, "");

        //-----------------------------------------------------------------
        line = "POLYGONS " + (t.length / 3) + " " + ((t.length/3)*4);
        writeAsciiLine(inOutputStream, line);
        int p = 3;
        for ( i = 0; i < t.length/3; i++ ) {
            writeLongBE(inOutputStream, p);
            writeLongBE(inOutputStream, t[3*i+0]);
            writeLongBE(inOutputStream, t[3*i+1]);
            writeLongBE(inOutputStream, t[3*i+2]);
        }
        writeAsciiLine(inOutputStream, "");

        //-----------------------------------------------------------------
        if ( n != null ) {
            line = "CELL_DATA " + (t.length / 3);
            writeAsciiLine(inOutputStream, line);
            line = "POINT_DATA " + (v.length/3);
            writeAsciiLine(inOutputStream, line);
            line = "NORMALS Normals float";
            writeAsciiLine(inOutputStream, line);
            for ( i = 0; i < n.length; i++ ) {
                val = (float)(n[i]);
                writeFloatBE(inOutputStream, val);
            }
            writeAsciiLine(inOutputStream, "");
        }
    }

    public static void
    exportEnvironment(OutputStream inOutputStream, SimpleScene inScene)
        throws Exception
    {
        //-----------------------------------------------------------------
        writeAsciiLine(inOutputStream, "# vtk DataFile Version 3.0");
        writeAsciiLine(inOutputStream, "vtk output");
        writeAsciiLine(inOutputStream, "BINARY");

        //-----------------------------------------------------------------
        ArrayList<SimpleBody> objs;
        Geometry g;
        TriangleMesh mesh;
        boolean exported = false;

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
            else {
                VSDK.reportMessage(null, VSDK.WARNING, "WriterVtk.exportEnvironment", "Current writer implementation only supports writing of triangle meshes. Object skipped.");
            }

            //-----------------------------------------------------------------
            if ( mesh != null ) {
                if ( !exported ) {
                    exportMesh(inOutputStream, mesh);
                    exported = true;
                }
                else {
                    VSDK.reportMessage(null, VSDK.WARNING, "WriterVtk.exportEnvironment", "Current writer implementation only supports writing ONE triangle meshes. Only first mesh exported, remaining meshes skipped.");
                }
            }
        }
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
