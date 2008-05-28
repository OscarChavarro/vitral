//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - May 27 2008 - Oscar Chavarro: Original base version                   =
//===========================================================================

package vsdk.toolkit.io.geometry;

// Java basic classes
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.util.ArrayList;
import java.util.StringTokenizer;

// VSDK Classes
import vsdk.toolkit.common.ArrayListOfInts;
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.Triangle;
import vsdk.toolkit.common.Vertex;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.Matrix4x4;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.TriangleMesh;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.io.PersistenceElement;

class _ReaderPlyElement
{
    private static final int TYPE_SIGNED_CHARACTER = 1;
    private static final int TYPE_UNSIGNED_CHARACTER = 2;
    private static final int TYPE_SIGNED_SHORT_INTEGER = 3;
    private static final int TYPE_UNSIGNED_SHORT_INTEGER = 4;
    private static final int TYPE_SIGNED_INTEGER = 5;
    private static final int TYPE_UNSIGNED_INTEGER = 6;
    private static final int TYPE_FLOAT = 7;
    private static final int TYPE_DOUBLE = 8;
    private static final int TYPE_INVALID = 0;

    private String elementName;
    private int elementCount;
    private int elementType; // One of the "TYPE_*" constants
    private int listCountType; // One of the "TYPE_*" constants
    private int elementFamily;

    private TriangleMesh mesh;
    private int currentPropertyIndex;
    private int xindex;
    private int yindex;
    private int zindex;
    private int listindex;
    private ArrayListOfInts skipTypes;

    public _ReaderPlyElement(StringTokenizer headerLine, TriangleMesh mesh)
    {
        this.mesh = mesh;
        elementName = new String(headerLine.nextToken());
        elementCount = Integer.parseInt(headerLine.nextToken());
        currentPropertyIndex = 0;
        xindex = -1;
        yindex = -1;
        zindex = -1;
        listindex = -1;
        skipTypes = new ArrayListOfInts(10);
    }

    private int getType(String t)
    {
        if ( t.equals("float32") || t.equals("float") ) {
            return TYPE_FLOAT;
        }
        else if ( t.equals("uint8") || t.equals("uchar") ) {
            return TYPE_UNSIGNED_CHARACTER;
        }
        else if ( t.equals("int32") || t.equals("int") ) {
            return TYPE_SIGNED_INTEGER;
        }
        return TYPE_INVALID;
    }

    public boolean addProperty(StringTokenizer line)
    {
        String var;
        String type;
        int skiptype;

        type = line.nextToken();
        skiptype = getType(type);

        if ( elementName.equals("vertex") ) {
            elementType = skiptype;
            var = line.nextToken();
            if ( var.equals("x") ) {
                xindex = currentPropertyIndex;
            }
            else if ( var.equals("y") ) {
                yindex = currentPropertyIndex;
            }
            else if ( var.equals("z") ) {
                zindex = currentPropertyIndex;
            }
        }
        if ( elementName.equals("face") ) {
            if ( type.equals("list") ) {
                type = line.nextToken();
                listCountType = getType(type);
                type = line.nextToken();
                elementType = getType(type);
                listindex = currentPropertyIndex;
            }
        }
        skipTypes.append(skiptype);

        currentPropertyIndex++;
        return true;
    }

    /**
    This method is used to read data from file, while ignoring its unknown
    contents.
    */
    private void skipRead(_ReaderPlyElementReader reader, int type) throws Exception
    {
        switch ( type ) {
          case TYPE_SIGNED_CHARACTER:
            reader.readSignedCharacter();
            break;
          case TYPE_UNSIGNED_CHARACTER:
            reader.readUnsignedCharacter();
            break;
          case TYPE_SIGNED_SHORT_INTEGER:
            reader.readSignedShortInteger();
            break;
          case TYPE_UNSIGNED_SHORT_INTEGER:
            reader.readUnsignedShortInteger();
            break;
          case TYPE_SIGNED_INTEGER:
            reader.readSignedInteger();
            break;
          case TYPE_UNSIGNED_INTEGER:
            reader.readUnsignedInteger();
            break;
          case TYPE_FLOAT:
            reader.readFloat();
            break;
          case TYPE_DOUBLE:
            reader.readDouble();
            break;
        }
    }

    private boolean readVertexData(_ReaderPlyElementReader reader, int i, double v[]) throws Exception
    {
        int j;
        double val;

        for ( j = 0; j < currentPropertyIndex; j++ ) {
            if ( elementType == TYPE_FLOAT ) {
                val = reader.readFloat();
                if ( j == xindex ) {
                    v[3*i+0] = val;
                }
                if ( j == yindex ) {
                    v[3*i+1] = val;
                }
                if ( j == zindex ) {
                    v[3*i+2] = val;
                }
            }
            else {
                VSDK.reportMessage(this, VSDK.FATAL_ERROR, "readVertexData",
                "Wrong element type!");
                return false;
            }
        }
        return true;
    }

    private boolean readPolygonData(_ReaderPlyElementReader reader, int i, ArrayListOfInts triangles) throws Exception
    {
        int n;
        int j;
        int val;
        int p0 = 0;
        int p1 = 0;
        int p2 = 0;

        for ( j = 0; j < currentPropertyIndex; j++ ) {
            if ( j == listindex ) {
                n = 0;
                if ( listCountType == TYPE_UNSIGNED_CHARACTER ) {
                    n = reader.readUnsignedCharacter();
                }
                else {
                    VSDK.reportMessage(this, VSDK.FATAL_ERROR, "readPolygonData",
                    "Wrong list count element type!");
                    return false;
                }
                for ( j = 0; j < n; j++ ) {
                    if ( elementType == TYPE_SIGNED_INTEGER ) {
                        val = reader.readSignedInteger();
    
                        if( j == 0 ) {
                            p0 = val;
                        }
                        else if( j == 1 ) {
                            p1 = val;
                        }
                        else {
                            p2 = val;
                            // Add a triangle over <p0, p1, p2>
                            triangles.append(p0);
                            triangles.append(p1);
                            triangles.append(p2);
                            //
                            p1 = val;
                        }
                    }
                    else {
                        VSDK.reportMessage(this, VSDK.FATAL_ERROR, "readPolygonData",  
                            "Wrong list element type!");
                        return false;
                    }
                }
            }
            else {
                skipRead(reader, skipTypes.array[j]);
            }
        }
        return true;
    }

    public boolean processInput(_ReaderPlyElementReader reader) throws Exception
    {
        //-----------------------------------------------------------------
        int i;

        if ( elementName.equals("vertex") ) {
            mesh.initVertexPositionsArray(elementCount);
            double v[];
            v = mesh.getVertexPositions();

            for ( i = 0; i < elementCount; i++ ) {
                if ( !readVertexData(reader, i, v) ) {
                    return false;
                }
            }

        }
        //-----------------------------------------------------------------
        else if ( elementName.equals("face") ) {
            ArrayListOfInts triangles;
            triangles = new ArrayListOfInts(1024);

            for ( i = 0; i < elementCount; i++ ) {
                if ( !readPolygonData(reader, i, triangles) ) {
                    return false;
                }
            }

            //-----------------------------------------------------------------
            int t[];

            mesh.initTriangleArrays(triangles.size/3);
            t = mesh.getTriangleIndexes();

            for ( i = 0; i < triangles.size; i++ ) {
                t[i] = triangles.array[i];
            }
            triangles.array = null;
            triangles = null;
        }

        return true;
    }
}

abstract class _ReaderPlyElementReader extends PersistenceElement
{
    protected InputStream parentInputStream;
    public _ReaderPlyElementReader(InputStream is)
    {
        parentInputStream = is;
    }
    public abstract int readSignedCharacter() throws Exception;
    public abstract int readUnsignedCharacter() throws Exception;
    public abstract int readSignedShortInteger() throws Exception;
    public abstract int readUnsignedShortInteger() throws Exception;
    public abstract int readSignedInteger() throws Exception;
    public abstract int readUnsignedInteger() throws Exception;
    public abstract float readFloat() throws Exception;
    public abstract float readDouble() throws Exception;
}

class _ReaderPlyElementReaderAscii extends _ReaderPlyElementReader
{
    private static String separators;
    public _ReaderPlyElementReaderAscii(InputStream is)
    {
        super(is);
        separators = " \t\n\r";
    }
    public int readSignedCharacter() throws Exception
    {
        VSDK.reportMessage(this, VSDK.FATAL_ERROR, "readSignedCharacter",
            "Operation not implemented!");
        return 0;
    }

    public int readUnsignedCharacter() throws Exception
    {
        String token;
        do {
            token = readAsciiToken(parentInputStream, separators);
        } while( token == null || token.length() < 1 );
        return Integer.parseInt(token);
    }

    public int readSignedShortInteger() throws Exception
    {
        String token;
        do {
            token = readAsciiToken(parentInputStream, separators);
        } while( token == null || token.length() < 1 );
        return Integer.parseInt(token);
    }

    public int readUnsignedShortInteger() throws Exception
    {
        String token;
        do {
            token = readAsciiToken(parentInputStream, separators);
        } while( token == null || token.length() < 1 );
        return Integer.parseInt(token);
    }

    public int readSignedInteger() throws Exception
    {
        String token;
        do {
            token = readAsciiToken(parentInputStream, separators);
        } while( token == null || token.length() < 1 );
        return Integer.parseInt(token);
    }

    public int readUnsignedInteger() throws Exception
    {
        String token;
        do {
            token = readAsciiToken(parentInputStream, separators);
        } while( token == null || token.length() < 1 );
        return Integer.parseInt(token);
    }

    public float readFloat() throws Exception
    {
        String token;
        do {
            token = readAsciiToken(parentInputStream, separators);
        } while( token == null || token.length() < 1 );
        return Float.parseFloat(token);
    }

    public float readDouble() throws Exception
    {
        VSDK.reportMessage(this, VSDK.FATAL_ERROR, "readDouble",
            "Operation not implemented!");
        return 0;
    }
}

class _ReaderPlyElementReaderBinaryBigEndian extends _ReaderPlyElementReader
{
    public _ReaderPlyElementReaderBinaryBigEndian(InputStream is)
    {
        super(is);
    }
    public int readSignedCharacter() throws Exception
    {
        byte arr[] = new byte[1];
        readBytes(parentInputStream, arr);
        return VSDK.signedByte2unsignedInteger(arr[0]);
    }

    public int readUnsignedCharacter() throws Exception
    {
        byte arr[] = new byte[1];
        readBytes(parentInputStream, arr);
        return (int)arr[0];
    }

    public int readSignedShortInteger() throws Exception
    {
        VSDK.reportMessage(this, VSDK.FATAL_ERROR, "readSignedShortInteger",
            "Operation not implemented!");
        return 0;
    }

    public int readUnsignedShortInteger() throws Exception
    {
        VSDK.reportMessage(this, VSDK.FATAL_ERROR, "readUnsignedShortInteger",
            "Operation not implemented!");
        return 0;
    }

    public int readSignedInteger() throws Exception
    {
        return (int)readLongBE(parentInputStream);
    }

    public int readUnsignedInteger() throws Exception
    {
        VSDK.reportMessage(this, VSDK.FATAL_ERROR, "readUnsignedInteger",
            "Operation not implemented!");
        return 0;
    }

    public float readFloat() throws Exception
    {
        return readFloatBE(parentInputStream);
    }

    public float readDouble() throws Exception
    {
        VSDK.reportMessage(this, VSDK.FATAL_ERROR, "readDouble",
            "Operation not implemented!");
        return 0;
    }
}

class _ReaderPlyElementReaderBinaryLittleEndian extends _ReaderPlyElementReader
{
    public _ReaderPlyElementReaderBinaryLittleEndian(InputStream is)
    {
        super(is);
    }
    public int readSignedCharacter() throws Exception
    {
        VSDK.reportMessage(this, VSDK.FATAL_ERROR, "readSignedCharacter",
            "Operation not implemented!");
        return 0;
    }

    public int readUnsignedCharacter() throws Exception
    {
        VSDK.reportMessage(this, VSDK.FATAL_ERROR, "readUnsignedCharacter",
            "Operation not implemented!");
        return 0;
    }

    public int readSignedShortInteger() throws Exception
    {
        VSDK.reportMessage(this, VSDK.FATAL_ERROR, "readSignedShortInteger",
            "Operation not implemented!");
        return 0;
    }

    public int readUnsignedShortInteger() throws Exception
    {
        VSDK.reportMessage(this, VSDK.FATAL_ERROR, "readUnsignedShortInteger",
            "Operation not implemented!");
        return 0;
    }

    public int readSignedInteger() throws Exception
    {
        VSDK.reportMessage(this, VSDK.FATAL_ERROR, "readSignedInteger",
            "Operation not implemented!");
        return 0;
    }

    public int readUnsignedInteger() throws Exception
    {
        VSDK.reportMessage(this, VSDK.FATAL_ERROR, "readUnsignedInteger",
            "Operation not implemented!");
        return 0;
    }

    public float readFloat() throws Exception
    {
        return readFloatLE(parentInputStream);
    }

    public float readDouble() throws Exception
    {
        VSDK.reportMessage(this, VSDK.FATAL_ERROR, "readDouble",
            "Operation not implemented!");
        return 0;
    }
}

public class ReaderPly extends PersistenceElement
{
    private static _ReaderPlyElementReader elementReader = null;
    private static ArrayList<_ReaderPlyElement> elements = null;

    private static Material defaultMaterial()
    {
        Material m = new Material();

        m.setAmbient(new ColorRgb(0.2, 0.2, 0.2));
        m.setDiffuse(new ColorRgb(0.5, 0.9, 0.5));
        m.setSpecular(new ColorRgb(1, 1, 1));
        m.setDoubleSided(false);
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

    private static boolean processHeader(InputStream is, TriangleMesh internalGeometry) throws Exception
    {
        String line, token;
        StringTokenizer auxStringTokenizer;

        //-----------------------------------------------------------------
        line = readAsciiLine(is).toLowerCase();

        if ( !line.equals("ply") ) {
            VSDK.reportMessage(null, VSDK.ERROR,
                "ReaderPly.processHeader",
                "Invalid PLY header: wrong magic line. Should be \"ply\".");
            return false;
        }

        //-----------------------------------------------------------------
        boolean headerDone = false;
        _ReaderPlyElement currentElement = null;

        elements = new ArrayList<_ReaderPlyElement>();

        do {
            line = readAsciiLine(is);

            auxStringTokenizer = new StringTokenizer(line, " \t");
            token = auxStringTokenizer.nextToken().toLowerCase();

            if ( token.equals("format") ) {
                token = auxStringTokenizer.nextToken().toLowerCase();
    
                if ( token.equals("ascii") ) {
                    elementReader = new _ReaderPlyElementReaderAscii(is);
                }
                else if ( token.equals("binary_big_endian") ) {
                    elementReader = new _ReaderPlyElementReaderBinaryBigEndian(is);
                }
                else if ( token.equals("binary_little_endian") ) {
                    elementReader = new _ReaderPlyElementReaderBinaryLittleEndian(is);
                }
                else {
                    VSDK.reportMessage(null, VSDK.ERROR,
                        "ReaderPly.processHeader",
                        "Invalid PLY header: unsupported PLY subformat \"" + token + "\".");
                    return false;
                }

                double version;
                version = Double.parseDouble(auxStringTokenizer.nextToken());
                if ( version > 1.0 + VSDK.EPSILON ) {
                    VSDK.reportMessage(null, VSDK.WARNING,
                        "ReaderPly.processHeader",
                                       "Untested PLY file version " + VSDK.formatDouble(version) + ", reading could fail.");
                }

            }
            else if ( token.equals("comment") || token.equals("obj_info") ) {
                // Skip line
                ;
            }
            else if ( token.equals("end_header") ) {
                headerDone = true;
            }
            else if ( token.equals("element") ) {
                currentElement = new _ReaderPlyElement(auxStringTokenizer, internalGeometry);
                elements.add(currentElement);
            }
            else if ( token.equals("property") ) {
                if ( currentElement != null ) {
                    if ( !currentElement.addProperty(auxStringTokenizer) ) {
                        return false;
                    }
                }
            }
            else {
                VSDK.reportMessage(null, VSDK.WARNING,
                    "ReaderPly.processHeader",
                    "Unknown header line \"" + token + "\", ignoring.");
            }
        } while( !headerDone );
        //-----------------------------------------------------------------
        return true;
    }

    public static void
    importEnvironment(File inSceneFileFd, SimpleScene inoutSimpleScene)
        throws Exception
    {
        //-----------------------------------------------------------------
        TriangleMesh internalGeometry;
        internalGeometry = new TriangleMesh();

        FileInputStream fis;
        BufferedInputStream bis;

        fis = new FileInputStream(inSceneFileFd);
        bis = new BufferedInputStream(fis);

        try {
            if ( !processHeader(bis, internalGeometry) ) {
                VSDK.reportMessage(null, VSDK.ERROR,
                    "ReaderPly.importEnvironment", "Invalid PLY header!");
            }
            int i;
            for ( i = 0; i < elements.size(); i++ ) {
                if ( !elements.get(i).processInput(elementReader) ) {
                    return;
                }
            }
        }
        catch ( Exception e ) {
            VSDK.reportMessage(null, VSDK.ERROR,
                               "ReaderPly.importEnvironment", "Error reading PLY data!");                  e.printStackTrace();
        }

        //-----------------------------------------------------------------
        bis.close();
        fis.close();

        //-----------------------------------------------------------------
        internalGeometry.calculateNormals();
        addThing(internalGeometry, inoutSimpleScene.getSimpleBodies());
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
