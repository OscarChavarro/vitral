//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - August 1 2007 - Oscar Chavarro: Original base version                 =
//===========================================================================

package vsdk.toolkit.io.geometry;

// Java basic classes
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.StringTokenizer;

// VSDK Classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.Matrix4x4;
import vsdk.toolkit.common.Vertex;
import vsdk.toolkit.common.Triangle;
import vsdk.toolkit.environment.Background;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.TriangleMeshGroup;
import vsdk.toolkit.environment.geometry.TriangleMesh;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.io.PersistenceElement;
import vsdk.toolkit.io.image.ImagePersistence;

public class ReaderVtk extends PersistenceElement
{
    private static Vector3D points[] = null;
    private static Vector3D normals[] = null;
    private static long stripData[] = null;

    private static boolean
    importVtkFragment(InputStream fis) throws Exception
    {
        String vtkDataFragment;
        int numElements;
        String elementType;
        vtkDataFragment = readAsciiLine(fis);
        int i, j;
        long numPointsInStrip;
        int numIndexesInStripSet;
        StringTokenizer auxStringTokenizer;

        if ( vtkDataFragment != null && vtkDataFragment.startsWith("POINTS ") ) {
            System.out.println("Reading points...");
            auxStringTokenizer = new StringTokenizer(vtkDataFragment, " ");
            auxStringTokenizer.nextToken();
            numElements = Integer.parseInt(auxStringTokenizer.nextToken());
            elementType = auxStringTokenizer.nextToken();
            System.out.println("Num elements: " + numElements);
            System.out.println("Element type: " + elementType);
            if ( elementType.startsWith("float") ) {
                points = new Vector3D[numElements];
                for ( i = 0; i < numElements; i++ ) {
                    points[i] = new Vector3D();
                    points[i].x = readFloatBE(fis);
                    points[i].y = readFloatBE(fis);
                    points[i].z = readFloatBE(fis);
                }
            }
            else {
                VSDK.reportMessage(null, VSDK.WARNING,
                           "ReaderVtk.importVtkFragment",
                           "Current implementation does not implement reading points from type " + elementType);
                return false;
            }
            readAsciiLine(fis); // Closing string
        }
        else if ( vtkDataFragment != null && vtkDataFragment.startsWith("TRIANGLE_STRIPS ") ) {
            System.out.println("Reading triangle strips...");
            auxStringTokenizer = new StringTokenizer(vtkDataFragment, " ");
            auxStringTokenizer.nextToken();
            numElements = Integer.parseInt(auxStringTokenizer.nextToken());
            numIndexesInStripSet = Integer.parseInt(auxStringTokenizer.nextToken());
            System.out.println("Number of strips: " + numElements);

            stripData = new long[numIndexesInStripSet];
            for ( i = 0; i < numIndexesInStripSet; i++ ) {
                stripData[i] = readLongBE(fis);
            }
            readAsciiLine(fis); // Closing string
        }
        else if ( vtkDataFragment != null && vtkDataFragment.startsWith("CELL_DATA ") ) {
            // Just ignore...
        }
        else if ( vtkDataFragment != null && vtkDataFragment.startsWith("POINT_DATA ") ) {
            System.out.println("Reading point data...");
            auxStringTokenizer = new StringTokenizer(vtkDataFragment, " ");
            auxStringTokenizer.nextToken();
            numElements = Integer.parseInt(auxStringTokenizer.nextToken());
            vtkDataFragment = readAsciiLine(fis);
            if ( vtkDataFragment != null && vtkDataFragment.startsWith("NORMALS ") ) {
                auxStringTokenizer = new StringTokenizer(vtkDataFragment, " ");
                auxStringTokenizer.nextToken();
                auxStringTokenizer.nextToken();
                elementType = auxStringTokenizer.nextToken();
                System.out.println("Element type: " + elementType);
                if ( elementType.startsWith("float") ) {
                    System.out.println("Reading " + numElements + " normals... ");
                    normals = new Vector3D[numElements];
                    for ( i = 0; i < numElements; i++ ) {
                        normals[i] = new Vector3D();
                        normals[i].x = readFloatBE(fis);
                        normals[i].y = readFloatBE(fis);
                        normals[i].z = readFloatBE(fis);
                    }
                    readAsciiLine(fis); // Closing string
                }
                else {
                    VSDK.reportMessage(null, VSDK.WARNING,
                           "ReaderVtk.importVtkFragment",
                           "Current implementation can not read normals in data type " + elementType);
                    return false;
                }
            }
            else {
                VSDK.reportMessage(null, VSDK.WARNING,
                           "ReaderVtk.importVtkFragment",
                           "Current implementation does not implement reading point data from " + vtkDataFragment);
                return false;
            }
        }
        else {
            VSDK.reportMessage(null, VSDK.WARNING,
                           "ReaderVtk.importVtkFragment",
                           "Current implementation does not implement reading data from " + vtkDataFragment);
            return false;
        }
        return true;
    }

    public static void
    importEnvironment(File inSceneFileFd, SimpleScene inoutSimpleScene)
        throws Exception
    {
        //-----------------------------------------------------------------
        ArrayList<SimpleBody> simpleBodiesArray = inoutSimpleScene.getSimpleBodies();
        ArrayList<Light> lightsArray = inoutSimpleScene.getLights();
        ArrayList<Background> backgroundsArray = inoutSimpleScene.getBackgrounds();
        ArrayList<Camera> camerasArray = inoutSimpleScene.getCameras();

        System.out.println("Reading " + inSceneFileFd.getAbsolutePath());

        //-----------------------------------------------------------------
        FileInputStream fis = new FileInputStream(inSceneFileFd);

        //-----------------------------------------------------------------
        String header;

        header = readAsciiLine(fis);

        if ( header == null ||
             header.length() < 1 ||
             !header.startsWith("# vtk DataFile Version ") ) {
            VSDK.reportMessage(null, VSDK.WARNING,
                           "ReaderVtk.importEnvironment",
                           "Bad header, not in VTK format.");
            return;
        }

        //-----------------------------------------------------------------
        String vtkHeader;
        String vtkBinaryMode;

        header = readAsciiLine(fis);
        vtkBinaryMode = readAsciiLine(fis);

        if ( vtkBinaryMode == null ||
             vtkBinaryMode.length() < 1 ||
             !vtkBinaryMode.startsWith("BINARY") ) {
            VSDK.reportMessage(null, VSDK.WARNING,
                           "ReaderVtk.importEnvironment",
                           "Current reader implementation only supports BINARY data representation.\n" + vtkBinaryMode + " mode found and not supported.");
            return;
        }

        //-----------------------------------------------------------------
        String vtkDataset;

        vtkDataset = readAsciiLine(fis);
        if ( vtkDataset == null ||
             vtkDataset.length() < 1 ||
             !vtkDataset.startsWith("DATASET") ) {
            VSDK.reportMessage(null, VSDK.WARNING,
                           "ReaderVtk.importEnvironment",
                           "DATASET not defined!");
            return;
        }

        String datasetType;
        datasetType = vtkDataset.substring(8);

        System.out.println("DATASET a procesar: " + datasetType);

        //-----------------------------------------------------------------
        int resting;
        do {
            if ( !importVtkFragment(fis) ) return;
            resting = fis.available();
        } while ( resting > 0 );

        //-----------------------------------------------------------------
        int acum = 0;
        int i, j;
        long deltaTam;

        for ( i = 0; i < stripData.length; i++ ) {
            deltaTam = stripData[i];
            acum++;
            //System.out.printf(" . Tira %d: %d elementos.\n", acum, deltaTam);
            for ( j = 0; j < deltaTam; j++ ) {
                i++;
                //Geometria->anx_indice_tira(stripData[i]);
            }
            //Geometria->anx_tira();
        }

        //-----------------------------------------------------------------
        System.out.println("VTK import done.");
        fis.close();

    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
