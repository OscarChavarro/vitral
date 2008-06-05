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
    private static final long MAGIC_FILESTART = 0x00134002;
    private static final long MAGIC_OBJECTINIT = 0x00134011;
    private static final long MAGIC_OBJECTSTART = 0x0012F800;
    private static final long MAGIC_ENDNAME = 0x00134012;
    private static final long MAGIC_MESHNUMS = 0x00134900;
    private static final long MAGIC_MESHVERTICES = 0x00134B01;
    private static final long MAGIC_MESHTRIANGLES = 0x00134B03;

    private static final long MAGIC_MESHUNKNOWN1 = 0x00134B02;
    private static final long MAGIC_MESHUNKNOWN2 = 0x00134013;
    private static final long MAGIC_MESHUNKNOWN3 = 0x00134017;
    private static final long MAGIC_MESHUNKNOWN4 = 0x0013401A;
    private static final long MAGIC_MESHUNKNOWN5 = 0x00134019;
    private static final long MAGIC_MESHUNKNOWN6 = 0x00134000;
    private static final long MAGIC_MESHUNKNOWN7 = 0x00039202;
    private static final long MAGIC_MESHUNKNOWN8 = 0x00134018;

    private static final long MAGIC_TABLEUNKNOWN1 = 0x00134003;
    private static final long MAGIC_TABLEUNKNOWN2 = 0x00134004;

    private static long skippedKeys;

    private static long readChunkStart(InputStream is, byte arr[]) throws Exception
    {
        long param = readLongLE(is);

        do {
            readBytes(is, arr);
        } while ( isFill(arr) );

        return param;
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
        byte arr[] = new byte[4];
        long skipHeader;
        long skipTriangles;
        long nt;
        long nv;

        //-----------------------------------------------------------------
        if ( !skipKeysUntil(is, MAGIC_OBJECTSTART) ||
             !skipKeysUntil(is, MAGIC_OBJECTSTART) ||
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

        //-----------------------------------------------------------------
        if ( !skipKeysUntil(is, MAGIC_MESHNUMS) ) {
            System.out.println("No mesh found! Aborting.");
            System.exit(0);
        }
        readChunkStart(is, arr);
        skipKeys(is, 8); // ??

        nt = readLongLE(is);
        skipKeys(is, 3); // ??

        nv = readLongLE(is);
        skipKeys(is, 2); // ??

        skippedKeys = 0;
        if ( !skipKeysUntil(is, MAGIC_MESHVERTICES) ) {
            System.out.println("No mesh found! Aborting.");
            System.exit(0);
        }
        skipHeader = skippedKeys;

        //-----------------------------------------------------------------
        TriangleMesh mesh = new TriangleMesh();
        mesh.initVertexPositionsArray((int)nv);
        mesh.initVertexNormalsArray();
        mesh.initVertexUvsArray();
        double vp[];
        double vt[];
        double vn[];
        vp = mesh.getVertexPositions();
        vt = mesh.getVertexUvs();
        vn = mesh.getVertexNormals();

        if ( nv == 0 ) {
            return null;
	}

        readChunkStart(is, arr);
        vp[0] = byteArray2floatLE(arr, 0);

        int i;

        /////
        long val;
        ArrayList<Long> vals = new ArrayList<Long>();
        /////

        for ( i = 0; i < nv; i++ ) {
            // x
            if ( i != 0 ) {
                vp[3*i+0] = readFloatLE(is);
            }
            vp[3*i+1] = readFloatLE(is); // y
            vp[3*i+2] = readFloatLE(is); // z
            vn[3*i+0] = readFloatLE(is); // nx
            vn[3*i+1] = readFloatLE(is); // ny
            vn[3*i+2] = readFloatLE(is); // nz
            val = readLongLE(is); // ??

            /////
            long a, b, c;
            a = (val >> 16) & 0x000000FF;
            b = (val >> 8) & 0x000000FF;
            c = (val) & 0x000000FF;
            if ( a != b || a != c || b != c ) {
                int index;
                Long l = new Long(val);
                index = java.util.Collections.binarySearch(vals, l);
                if ( index < 0 ) {
                    vals.add((-index)-1, l);
                }
            }
            /////

            vt[2*i+0] = readFloatLE(is); // u
            vt[2*i+1] = readFloatLE(is); // v
        }

        /////
        //for ( i = 0; i < vals.size(); i++ ) {
        //    System.err.println(VSDK.formatIntAsHex(vals.get(i).intValue()));
        //}
        /////

        //-----------------------------------------------------------------
        key = readLongLE(is);
        if ( key != MAGIC_MESHUNKNOWN1 ) {
            System.out.print("NV: " + nv + ", NT: " + nt + ", skipHeader: " + skipHeader + ", ");
            System.out.println("Warning: missed binary stream! Check model" + name + "!");
            return null;
        }
/*

        long ntable = readLongLE(is)/2;

        long a = 0;

	for ( i = 0; i < ntable; i++ ) {
	    a = readLongLE(is);
	    //System.out.println(" - " + i + ": 0x" + VSDK.formatIntAsHex(a) + " = " + a);
	}
*/

        //-----------------------------------------------------------------
        skippedKeys = 0;
        if ( !skipKeysUntil(is, MAGIC_MESHTRIANGLES) ) {
            System.out.println("No mesh found! Aborting.");
            System.exit(0);
        }
        skipTriangles = skippedKeys;

        //-----------------------------------------------------------------
        mesh.initTriangleArrays((int)nt);
        int t[];

        t = mesh.getTriangleIndexes();

        readChunkStart(is, arr);
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

        //mesh.calculateNormals();

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

        //-----------------------------------------------------------------
        //System.out.print("NV: " + nv + ", NT: " + nt + ", skipHeader: " + skipHeader + ", skipTriangles: " + skipTriangles + ", ");

        return thing;
    }

    private static boolean
    skipKeysUntil(InputStream is, long key) throws Exception
    {
        long i;

        do {
            try {
                i = readLongLE(is);
                skippedKeys++;
            }
            catch ( Exception e ) {
                return false;
            }
        } while ( i != key );
        return true;
    }

    private static long
    skipUnknownKeys(InputStream is) throws Exception
    {
        long i;

        do {
            try {
                i = readLongLE(is);
                skippedKeys++;
                if (
                    i == MAGIC_OBJECTINIT ||
                    i == MAGIC_OBJECTSTART ||
                    i == MAGIC_ENDNAME ||
                    i == MAGIC_MESHNUMS ||
                    i == MAGIC_MESHVERTICES ||
                    i == MAGIC_MESHUNKNOWN1 ||
                    i == MAGIC_MESHTRIANGLES ||
                    i == MAGIC_MESHUNKNOWN8 ||
                    i == MAGIC_MESHUNKNOWN2 ||
                    i == MAGIC_MESHUNKNOWN3 ||
                    i == MAGIC_MESHUNKNOWN4 ||
                    i == MAGIC_MESHUNKNOWN5 ||
                    i == MAGIC_MESHUNKNOWN6 ||
                    i == MAGIC_MESHUNKNOWN7
                ) {
                    return i;
                }
            }
            catch ( Exception e ) {
                return 0;
            }
        } while ( true );
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
        //-----------------------------------------------------------------
        skipKeys(is, 6);

        long headerChunkKey = readLongLE(is);

        if ( headerChunkKey != MAGIC_FILESTART ) {
            System.out.println("Wrong header!");
            return 0;
        }

        long count = readLongLE(is);
        if ( count != 128 ) {
            System.out.println("Wrong header!");
            return 0;
	}

        skipKeys(is, 3);

        long n;
        n = readLongLE(is);

        //-----------------------------------------------------------------
        if ( !skipKeysUntil(is, MAGIC_TABLEUNKNOWN1) ) {
            System.out.println("Wrong header!");
            return 0;
        }
        long ntable = readLongLE(is)/8;

        if ( n != ntable ) {
            System.out.println("Wrong header!");
            return 0;
        }

        //System.out.println("Skipping unknown table with " + ntable + " elements.");
        int i;
        long a = 0, b = 0;

	for ( i = 0; i < ntable; i++ ) {
	    a = readLongLE(is);
	    b = readLongLE(is);
	    //System.out.println(" - " + i + ": " + VSDK.formatIntAsHex((int)a));
            if ( b != 0 ) {
                System.out.println("Warning: wrong table entry!");
	    }
	}

        //-----------------------------------------------------------------
        long key;

        key = readLongLE(is);
        if ( key != MAGIC_TABLEUNKNOWN2 ) {
            System.out.println("Wrong header!");
            return 0;
	}

        ntable = readLongLE(is)/8;
        //System.out.println("Skipping unknown table with " + ntable + " elements.");

	for ( i = 0; i < ntable; i++ ) {
	    a = readLongLE(is);
	    b = readLongLE(is);
	}

        //-----------------------------------------------------------------

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

        //System.out.println("Reading " + inSceneFileFd.getAbsolutePath());

        //-----------------------------------------------------------------
        FileInputStream fis = new FileInputStream(inSceneFileFd);
        BufferedInputStream bis = new BufferedInputStream(fis);

        //-----------------------------------------------------------------
        long n = processHeader(bis);
        long i;
        SimpleBody thing;
        boolean started = false;

        for ( i = 0; i < n; i++ ) {
            skippedKeys = 0;
            if ( started || skipKeysUntil(bis, MAGIC_OBJECTINIT) ) {
                long s = skippedKeys;
                //System.out.print("Skipped before: " + s + ", ");

                thing = readBody(bis);
                if ( thing != null ) {
                    simpleBodiesArray.add(thing);
                    //System.out.println(thing.getName());
                }
                else {
                    System.out.println("Error!");
                    System.exit(0);                
                }

                started = false;
/*
                long k = skipUnknownKeys(bis);
                if ( k == MAGIC_OBJECTINIT ) {
                    started = true;
                }
                else if ( k == MAGIC_MESHUNKNOWN5 ) {
                    TriangleMesh mesh = (TriangleMesh)thing.getGeometry();
                    double v[] = mesh.getVertexPositions();
                    int t[] = mesh.getTriangleIndexes();
                    int nv = v.length/3;
                    int nt = t.length/3;
                    skippedKeys = 0;
                    k = skipUnknownKeys(bis);
                    boolean selected = skippedKeys > nv; 
                    System.out.println("NV: " + nv + ", NT: " + nt + " skip: " + skippedKeys + ", Criteria: " + (selected?"true":"false"));

                    if ( k == MAGIC_OBJECTINIT ) {
                        started = true;
                    }
                }
                else {
                    System.out.println("MMM... " + VSDK.formatIntAsHex((int)k));
                }
*/
            }
        }

        //-----------------------------------------------------------------
        bis.close();
        fis.close();
        //System.exit(0);
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
