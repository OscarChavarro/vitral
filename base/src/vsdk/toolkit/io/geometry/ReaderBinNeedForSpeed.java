//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - June 4 2008 - Oscar Chavarro: Original base version                   =
//===========================================================================

package vsdk.toolkit.io.geometry;

// Java basic classes
import java.io.File;
import java.io.InputStream;
import java.io.BufferedInputStream;
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
import vsdk.toolkit.environment.geometry.TriangleStripMesh;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.io.PersistenceElement;
import vsdk.toolkit.io.image.ImagePersistence;

public class ReaderBinNeedForSpeed extends PersistenceElement
{
    private static final long MAGIC_ZEROES = 0x00000000;
    private static final long MAGIC_OBJECTSTART = 0x0012F800;
    private static final long MAGIC_ENDNAME = 0x00134012;
    private static final long MAGIC_MESHVERTICES = 0x00134B01;
    private static final long MAGIC_MESHTRIANGLES = 0x00134B03;

    private static final int KEY_BUFFER_SIZE = 8;
    private static int currentCircularKeyBufferIndex;
    private static long circularKeyBuffer[];

    private static void addToCircularBuffer(long i)
    {
        if ( currentCircularKeyBufferIndex >= KEY_BUFFER_SIZE ) {
            currentCircularKeyBufferIndex = 0;
        }
        circularKeyBuffer[currentCircularKeyBufferIndex] = i;
        currentCircularKeyBufferIndex++;
    }

    private static long getCircularBufferValueAt(int back)
    {
        int i;
        int c;

        for ( c = 0, i = currentCircularKeyBufferIndex; c < back; c++ ) {
            i--;
            if ( i < 0 ) {
                i = KEY_BUFFER_SIZE-1;
            }
        }
        return circularKeyBuffer[i];
    }

    private static boolean isFill(byte arr[])
    {
        int i;
        for ( i = 0; i < arr.length; i++ ) {
            if ( arr[i] != 0x11 ) {
                return false;
            }
        }
        return true;
    }

    private static Material defaultMaterial()
    {
        Material m = new Material();

        m.setAmbient(new ColorRgb(0.2, 0.2, 0.2));
        m.setDiffuse(new ColorRgb(0.5, 0.9, 0.5));
        m.setSpecular(new ColorRgb(1, 1, 1));
        m.setDoubleSided(false);
        return m;
    }

    private static SimpleBody
    readBody(InputStream is) throws Exception
    {
        //-----------------------------------------------------------------
        if ( !skipKeysUntil(is, MAGIC_OBJECTSTART) ||
             !skipKeysUntil(is, MAGIC_ZEROES) ) {
            System.out.println("Bad body start! Aborting.");
            System.exit(0);
        }

        String name;
        name = readAsciiFixedSizeString(is, 28);

        long key;

        key = readLongLE(is);

        if ( key != MAGIC_ENDNAME ) {
            System.out.println("Bad name end! Aborting.");
            System.exit(0);
        }

        if ( !skipKeysUntil(is, MAGIC_MESHVERTICES) ) {
            System.out.println("No mesh found! Aborting.");
            System.exit(0);
        }

        long nv = getCircularBufferValueAt(4);
        long nt = getCircularBufferValueAt(8);

        skipKeys(is, 1); // ??

        //-----------------------------------------------------------------
        TriangleMesh mesh = new TriangleMesh();
        mesh.initVertexPositionsArray((int)nv);
        mesh.initVertexUvsArray();
        double vp[];
        double vt[];
        vp = mesh.getVertexPositions();
        vt = mesh.getVertexUvs();
        byte arr[] = new byte[4];

        do {
            readBytes(is, arr);
        } while ( isFill(arr) );

        vp[0] = byteArray2floatLE(arr, 0);

        int i;

        for ( i = 0; i < nv; i++ ) {
            // x
            if ( i != 0 ) {
                vp[3*i+0] = readFloatLE(is);
            }
            vp[3*i+1] = readFloatLE(is); // y
            vp[3*i+2] = readFloatLE(is); // z
            readFloatLE(is);
            readFloatLE(is);
            readFloatLE(is);
            readFloatLE(is);
            vt[2*i+0] = readFloatLE(is); // u
            vt[2*i+1] = readFloatLE(is); // v
        }

        //-----------------------------------------------------------------
        if ( !skipKeysUntil(is, MAGIC_MESHTRIANGLES) ) {
            System.out.println("No mesh found! Aborting.");
            System.exit(0);
        }
        skipKeys(is, 1); // ??

        //-----------------------------------------------------------------
        mesh.initTriangleArrays((int)nt);
        int t[];

        t = mesh.getTriangleIndexes();

        do {
            readBytes(is, arr);
        } while ( isFill(arr) );

        t[0] = byteArray2intLE(arr, 0);
        t[1] = byteArray2intLE(arr, 2);

        for ( i = 0; i < nt; i++ ) {
            if ( i != 0 ) {
                t[3*i+0] = readIntLE(is);
                t[3*i+1] = readIntLE(is);
            }
            t[3*i+2] = readIntLE(is);
        }

        if ( (nt % 2) != 0 ) {
            readIntLE(is);
        }

        mesh.calculateNormals();

        //-----------------------------------------------------------------
        SimpleBody thing;
        Vector3D position = new Vector3D();
        Matrix4x4 R = new Matrix4x4();
        Material material = defaultMaterial();

        thing = new SimpleBody();
        thing.setName(name);
        thing.setPosition(position);
        thing.setRotation(R);
        thing.setRotationInverse(R.inverse());
        thing.setMaterial(material);
        thing.setGeometry(mesh);

        return thing;
    }

    private static boolean
    skipKeysUntil(InputStream is, long key) throws Exception
    {
        long i;

        do {
            try {
                i = readLongLE(is);
                addToCircularBuffer(i);
            }
            catch ( Exception e ) {
                return false;
            }
        } while ( i != key );
        return true;
    }

    private static void
    skipKeys(InputStream is, int n) throws Exception
    {
        int i;

        for ( i = 0; i < n; i++ ) {
            readLongLE(is);
        }
    }

    private static long processHeader(InputStream is) throws Exception
    {
        skipKeys(is, 11);
        long n;
        n = readLongLE(is);
        return n;
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
        BufferedInputStream bis = new BufferedInputStream(fis);

        circularKeyBuffer = new long[KEY_BUFFER_SIZE];
        currentCircularKeyBufferIndex = 0;

        //-----------------------------------------------------------------
        long n = processHeader(bis);
        long i;
        SimpleBody thing;

        for ( i = 0; i < n; i++ ) {
            if ( skipKeysUntil(bis, MAGIC_OBJECTSTART) ) {
                thing = readBody(bis);
                if ( thing != null ) {
                    simpleBodiesArray.add(thing);
                }
            }
        }

        //-----------------------------------------------------------------
        bis.close();
        fis.close();
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
