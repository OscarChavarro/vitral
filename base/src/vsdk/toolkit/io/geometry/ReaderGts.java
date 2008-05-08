//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - October 25 2007 - Oscar Chavarro: Original base version               =
//===========================================================================

package vsdk.toolkit.io.geometry;

// Java basic classes
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

// VSDK Classes
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.Triangle;
import vsdk.toolkit.common.Vertex;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.Matrix4x4;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.TriangleMesh;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.io.PersistenceElement;

public class ReaderGts extends PersistenceElement
{
    private static Material defaultMaterial()
    {
        Material m = new Material();

        m.setAmbient(new ColorRgb(0.2, 0.2, 0.2));
        m.setDiffuse(new ColorRgb(0.5, 0.9, 0.5));
        m.setSpecular(new ColorRgb(1, 1, 1));
        return m;
    }

    private static void addThing(Geometry g,
        ArrayList<SimpleBody> inoutSimpleBodiesArray)
    {
        if ( inoutSimpleBodiesArray == null ) return;

        SimpleBody thing;

        thing = new SimpleBody();
        thing.setGeometry(g);
        thing.setPosition(new Vector3D());
        thing.setRotation(new Matrix4x4());
        thing.setRotationInverse(new Matrix4x4());
        thing.setMaterial(defaultMaterial());
        inoutSimpleBodiesArray.add(thing);
    }

    public static void
    importEnvironment(File inSceneFileFd, SimpleScene inoutSimpleScene)
        throws Exception
    {
        TriangleMesh internalGeometry;
        internalGeometry = new TriangleMesh();

        internalGeometry = new TriangleMesh();

        //-----------------------------------------------------------------
        BufferedReader br;
        String lineOfText;

        br = new BufferedReader(new FileReader(inSceneFileFd));

        int i;
        StringTokenizer auxStringTokenizer;
        int numPoints = 0;
        int numEdges = 0;
        int numTriangles = 0;
        int edges[][] = null; // Ne*2
        int triangles[][] = null; // Nt*3
        double v[] = null;

        for ( i = 0; (lineOfText = br.readLine()) != null; i++ ) {
            if ( i == 0 ) {
                auxStringTokenizer = new StringTokenizer(lineOfText, " ");
                numPoints = Integer.parseInt(auxStringTokenizer.nextToken());
                numEdges = Integer.parseInt(auxStringTokenizer.nextToken());
                numTriangles = Integer.parseInt(auxStringTokenizer.nextToken());
                internalGeometry.initVertexArrays(numPoints);
                v = internalGeometry.getVertexPositions();
                edges = new int[numEdges][2];
                triangles = new int[numTriangles][3];
            }
            else if ( i >= 1 && i < 1+numPoints ) {
                // Reading a point
                double x, y, z;

                auxStringTokenizer = new StringTokenizer(lineOfText, " ");
                x = Double.parseDouble(auxStringTokenizer.nextToken());
                y = Double.parseDouble(auxStringTokenizer.nextToken());
                z = Double.parseDouble(auxStringTokenizer.nextToken());
                v[3*(i-1)+0] = x;
                v[3*(i-1)+1] = y;
                v[3*(i-1)+2] = z;
            }
            else if ( i >= 1+numPoints && i < 1+numPoints+numEdges ) {
                // Reading an edge
                auxStringTokenizer = new StringTokenizer(lineOfText, " ");

                edges[i-(1+numPoints)][0] = Integer.parseInt(auxStringTokenizer.nextToken());
                edges[i-(1+numPoints)][1] = Integer.parseInt(auxStringTokenizer.nextToken());
            }
            else if ( i >= 1+numPoints+numEdges && i < 1+numPoints+numEdges+numTriangles ) {
                // Reading a triangle
                auxStringTokenizer = new StringTokenizer(lineOfText, " ");

                triangles[i-(1+numPoints+numEdges)][0] = Integer.parseInt(auxStringTokenizer.nextToken());
                triangles[i-(1+numPoints+numEdges)][1] = Integer.parseInt(auxStringTokenizer.nextToken());
                triangles[i-(1+numPoints+numEdges)][2] = Integer.parseInt(auxStringTokenizer.nextToken());
            }
        }
        //-----------------------------------------------------------------
        int ie1, ie2, ie3;
        int ip1, ip2 = -2, ip3 = -1;
        int t[];

        internalGeometry.initTriangleArrays(numTriangles);
        t = internalGeometry.getTriangleIndexes();

        for ( i = 0; i < numTriangles; i++ ) {
            ie1 = triangles[i][0]-1;
            ie2 = triangles[i][1]-1;
            ie3 = triangles[i][2]-1;

            ip1 = edges[ie1][0];
            if ( ip1 != ip3 || ip1 == ip2 ) {
                ip1 = edges[ie1][1];
            }

            ip2 = edges[ie2][0];
            if ( ip2 == ip1 || ip2 == ip3 ) {
                ip2 = edges[ie2][1];
            }

            ip3 = edges[ie3][0];
            if ( ip3 == ip2 || ip3 == ip1 ) {
                ip3 = edges[ie3][1];
            }

            t[3*i+0] = ip1-1;
            t[3*i+1] = ip2-1;
            t[3*i+2] = ip3-1;
        }

        //-----------------------------------------------------------------

        internalGeometry.calculateNormals();
        addThing(internalGeometry, inoutSimpleScene.getSimpleBodies());
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
