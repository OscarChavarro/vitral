//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - December 9 2006 - Oscar Chavarro: Original base version               =
//=-------------------------------------------------------------------------=
//= References:                                                             =
//= [FERC1996] Fercoq, Robin. "3D Studio Material Library File Format",     =
//=     internet document posted at alt.3d and alt.3d-studio                =
//=     (usenet lists), revision 0.1, may 1996                              =
//= [PITT1994] Pitts, Jim. "3D Studio File Format", internet document       =
//=     posted at alt.3d and alt.3d-studio (usenet lists), december 1994    =
//= [VANV1997] van Velsen, Martin. Fercoq, Robin. Szilvasy, Albert.         =
//=     "3D Studio File Format (rewritten)", internet document posted at    =
//=     alt.3d and alt.3d-studio (usenet lists), revision 0.93, january     =
//=     1997.                                                               =
//===========================================================================

package vsdk.toolkit.io.geometry;

// Java basic classes
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;

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

import vsdk.toolkit.io.PersistenceElement;

class _Reader3dsMaterialMapping extends PersistenceElement
{
    public String materialName;
    public int associatedTriangles[];
}

class _Reader3dsChunk extends PersistenceElement
{
    //=======================================================================
    public int id;
    public long length;

    //=======================================================================
    // Main level 3DS Fileformat chunk identifiers
    public static final int ID_MAIN                    = 0x4D4D;

    // Scene element 3DS Fileformat chunk identifiers
    public static final int ID_MESH                    = 0x3D3D;
    public static final int ID_BACKGROUND_COLOR        = 0x1200;
    public static final int ID_AMBIENT_COLOR           = 0x2100;
    public static final int ID_MATERIAL                = 0xAFFF;
    public static final int ID_MATERIAL_NAME           = 0xA000;
    public static final int ID_MATERIAL_AMBIENT        = 0xA010;
    public static final int ID_MATERIAL_DIFFUSE        = 0xA020;
    public static final int ID_MATERIAL_SPECULAR       = 0xA030;
    public static final int ID_MATERIAL_SHININESS      = 0xA040;
    public static final int ID_MATERIAL_TWOSIDED       = 0xA081;
    public static final int ID_MATERIAL_TYPE           = 0xA100;
    public static final int ID_OBJECT_BLOCK            = 0x4000;
    public static final int ID_TRIANGLE_MESH           = 0x4100;
    public static final int ID_VERTEX_LIST             = 0x4110;
    public static final int ID_TRIANGLE_LIST           = 0x4120;
    public static final int ID_MATERIAL_MAPPING_TABLE  = 0x4130;
    public static final int ID_SMOOTH_LIST             = 0x4150;
    public static final int ID_MAP_LIST                = 0x4140;
    public static final int ID_MATRIX                  = 0x4160;
    public static final int ID_TRI_MAPPING_STANDARD    = 0x4170;
    public static final int ID_TRI_VISIBLE             = 0x4165;
    public static final int ID_TRI_VERTEX_OPTIONS      = 0x4111;
    public static final int ID_LIGHT                   = 0x4600;
    public static final int ID_SPOTLIGHT               = 0x4610;
    public static final int ID_LIT_OFF                 = 0x4620;
    public static final int ID_LIT_UNKNOWN_01          = 0x465A;
    public static final int ID_CAMERA                  = 0x4700;
    public static final int ID_OBJECT_UNKNOWN_01       = 0x4710;
    public static final int ID_OBJECT_UNKNOWN_02       = 0x4720;
    public static final int ID_EDIT_CONFIG1            = 0x0100;
    public static final int ID_EDIT_CONFIG2            = 0x3E3D;
    public static final int ID_EDIT_VIEW_P1            = 0x7012;
    public static final int ID_EDIT_VIEW_P2            = 0x7011;
    public static final int ID_EDIT_VIEW_P3            = 0x7020;
    public static final int ID_TOP                     = 0x0001;
    public static final int ID_BOTTOM                  = 0x0002;
    public static final int ID_LEFT                    = 0x0003;
    public static final int ID_RIGHT                   = 0x0004;
    public static final int ID_FRONT                   = 0x0005;
    public static final int ID_BACK                    = 0x0006;
    public static final int ID_USER                    = 0x0007;
    public static final int ID_CAMERA_VIEW             = 0xFFFF;
    public static final int ID_LIGHT_VIEW              = 0x0009;
    public static final int ID_COLOR_RGB1              = 0x0010;
    public static final int ID_COLOR_RGB2              = 0x0011;
    public static final int ID_COLOR_RGB3              = 0x0012;
    public static final int ID_VIEWPORT                = 0x7001;
    public static final int ID_EDIT_UNKNOWN_01         = 0x1100;
    public static final int ID_EDIT_UNKNOWN_02         = 0x1201;
    public static final int ID_EDIT_UNKNOWN_03         = 0x1300;
    public static final int ID_EDIT_UNKNOWN_04         = 0x1400;
    public static final int ID_EDIT_UNKNOWN_05         = 0x1420;
    public static final int ID_EDIT_UNKNOWN_06         = 0x1450;
    public static final int ID_EDIT_UNKNOWN_07         = 0x1500;
    public static final int ID_EDIT_UNKNOWN_08         = 0x2200;
    public static final int ID_EDIT_UNKNOWN_09         = 0x2201;
    public static final int ID_EDIT_UNKNOWN_10         = 0x2210;
    public static final int ID_EDIT_UNKNOWN_11         = 0x2300;
    public static final int ID_EDIT_UNKNOWN_12         = 0x2302;
    public static final int ID_EDIT_UNKNOWN_13         = 0x2000;
    public static final int ID_EDIT_UNKNOWN_14         = 0x3000;

    // Scene control 3DS Fileformat chunk identifiers
    public static final int ID_KEYFRAMER               = 0xB000;
    public static final int ID_KEYFRAMER_FRAMES        = 0xB008;
    public static final int ID_KEYFRAMER_UNKNOWN_01    = 0xB00A;
    public static final int ID_KEYFRAMER_UNKNOWN_02    = 0xB009;
    public static final int ID_KEYFRAMER_OBJDES        = 0xB002;
    public static final int ID_KEYFRAMER_OBJHIERARCH   = 0xB010;
    public static final int ID_KEYFRAMER_OBJDUMMYNAME  = 0xB011;
    public static final int ID_KEYFRAMER_OBJUNKNOWN_01 = 0xB013;
    public static final int ID_KEYFRAMER_OBJUNKNOWN_02 = 0xB014;
    public static final int ID_KEYFRAMER_OBJUNKNOWN_03 = 0xB015;
    public static final int ID_KEYFRAMER_OBJPIVOT      = 0xB020;
    public static final int ID_KEYFRAMER_OBJUNKNOWN_04 = 0xB021;
    public static final int ID_KEYFRAMER_OBJUNKNOWN_05 = 0xB022;

    //=======================================================================
    public static String
    chunkToString(int chunkid)
    {
        String chunkname = "<Unknown chunk id>";

        switch ( chunkid ) {
          case ID_MAIN:
            chunkname = "ID_MAIN";
            break;
          case ID_MESH:
            chunkname = "ID_MESH";
            break;
          case ID_BACKGROUND_COLOR:
            chunkname = "ID_BACKGROUND_COLOR";
            break;
          case ID_AMBIENT_COLOR:
            chunkname = "ID_AMBIENT_COLOR";
            break;
          case ID_MATERIAL:
            chunkname = "ID_MATERIAL";
            break;
          case ID_MATERIAL_NAME:
            chunkname = "ID_MATERIAL_NAME";
            break;
          case ID_MATERIAL_AMBIENT:
            chunkname = "ID_MATERIAL_AMBIENT";
            break;
          case ID_MATERIAL_DIFFUSE:
            chunkname = "ID_MATERIAL_DIFFUSE";
            break;
          case ID_MATERIAL_SPECULAR:
            chunkname = "ID_MATERIAL_SPECULAR";
            break;
          case ID_MATERIAL_SHININESS:
            chunkname = "ID_MATERIAL_SHININESS";
            break;
          case ID_MATERIAL_TWOSIDED:
            chunkname = "ID_MATERIAL_TWOSIDED";
            break;
          case ID_MATERIAL_TYPE:
            chunkname = "ID_MATERIAL_TYPE";
            break;
          case ID_OBJECT_BLOCK:
            chunkname = "ID_OBJECT_BLOCK";
            break;
          case ID_TRIANGLE_MESH:
            chunkname = "ID_TRIANGLE_MESH";
            break;
          case ID_VERTEX_LIST:
            chunkname = "ID_VERTEX_LIST";
            break;
          case ID_TRIANGLE_LIST:
            chunkname = "ID_TRIANGLE_LIST";
            break;
          case ID_MATERIAL_MAPPING_TABLE:
            chunkname = "ID_MATERIAL_MAPPING_TABLE";
            break;
          case ID_SMOOTH_LIST:
            chunkname = "ID_SMOOTH_LIST";
            break;
          case ID_MAP_LIST:
            chunkname = "ID_MAP_LIST";
            break;
          case ID_MATRIX:
            chunkname = "ID_MATRIX";
            break;
          case ID_TRI_MAPPING_STANDARD:
            chunkname = "ID_TRI_MAPPING_STANDARD";
            break;
          case ID_TRI_VISIBLE:
            chunkname = "ID_TRI_VISIBLE";
            break;
          case ID_TRI_VERTEX_OPTIONS:
            chunkname = "ID_TRI_VERTEX_OPTIONS";
            break;
          case ID_LIGHT:
            chunkname = "ID_LIGHT";
            break;
          case ID_SPOTLIGHT:
            chunkname = "ID_SPOTLIGHT";
            break;
          case ID_LIT_OFF:
            chunkname = "ID_LIT_OFF";
            break;
          case ID_LIT_UNKNOWN_01:
            chunkname = "ID_LIT_UNKNOWN_01";
            break;
          case ID_CAMERA:
            chunkname = "ID_CAMERA";
            break;
          case ID_OBJECT_UNKNOWN_01:
            chunkname = "ID_OBJECT_UNKNOWN_01";
            break;
          case ID_OBJECT_UNKNOWN_02:
            chunkname = "ID_OBJECT_UNKNOWN_02";
            break;
          case ID_EDIT_CONFIG1:
            chunkname = "ID_EDIT_CONFIG1";
            break;
          case ID_EDIT_CONFIG2:
            chunkname = "ID_EDIT_CONFIG2";
            break;
          case ID_EDIT_VIEW_P1:
            chunkname = "ID_EDIT_VIEW_P1";
            break;
          case ID_EDIT_VIEW_P2:
            chunkname = "ID_EDIT_VIEW_P2";
            break;
          case ID_EDIT_VIEW_P3:
            chunkname = "ID_EDIT_VIEW_P3";
            break;
          case ID_TOP:
            chunkname = "ID_TOP";
            break;
          case ID_BOTTOM:
            chunkname = "ID_BOTTOM";
            break;
          case ID_LEFT:
            chunkname = "ID_LEFT";
            break;
          case ID_RIGHT:
            chunkname = "ID_RIGHT";
            break;
          case ID_FRONT:
            chunkname = "ID_FRONT";
            break;
          case ID_BACK:
            chunkname = "ID_BACK";
            break;
          case ID_USER:
            chunkname = "ID_USER";
            break;
          case ID_CAMERA_VIEW:
            chunkname = "ID_CAMERA_VIEW";
            break;
          case ID_LIGHT_VIEW:
            chunkname = "ID_LIGHT_VIEW";
            break;
          case ID_COLOR_RGB1:
            chunkname = "ID_COLOR_RGB1";
            break;
          case ID_COLOR_RGB2:
            chunkname = "ID_COLOR_RGB2";
            break;
          case ID_COLOR_RGB3:
            chunkname = "ID_COLOR_RGB3";
            break;
          case ID_VIEWPORT:
            chunkname = "ID_VIEWPORT";
            break;
          case ID_EDIT_UNKNOWN_01:
            chunkname = "ID_EDIT_UNKNOWN_01";
            break;
          case ID_EDIT_UNKNOWN_02:
            chunkname = "ID_EDIT_UNKNOWN_02";
            break;
          case ID_EDIT_UNKNOWN_03:
            chunkname = "ID_EDIT_UNKNOWN_03";
            break;
          case ID_EDIT_UNKNOWN_04:
            chunkname = "ID_EDIT_UNKNOWN_04";
            break;
          case ID_EDIT_UNKNOWN_05:
            chunkname = "ID_EDIT_UNKNOWN_05";
            break;
          case ID_EDIT_UNKNOWN_06:
            chunkname = "ID_EDIT_UNKNOWN_06";
            break;
          case ID_EDIT_UNKNOWN_07:
            chunkname = "ID_EDIT_UNKNOWN_07";
            break;
          case ID_EDIT_UNKNOWN_08:
            chunkname = "ID_EDIT_UNKNOWN_08";
            break;
          case ID_EDIT_UNKNOWN_09:
            chunkname = "ID_EDIT_UNKNOWN_09";
            break;
          case ID_EDIT_UNKNOWN_10:
            chunkname = "ID_EDIT_UNKNOWN_10";
            break;
          case ID_EDIT_UNKNOWN_11:
            chunkname = "ID_EDIT_UNKNOWN_11";
            break;
          case ID_EDIT_UNKNOWN_12:
            chunkname = "ID_EDIT_UNKNOWN_12";
            break;
          case ID_EDIT_UNKNOWN_13:
            chunkname = "ID_EDIT_UNKNOWN_13";
            break;
          case ID_EDIT_UNKNOWN_14:
            chunkname = "ID_EDIT_UNKNOWN_14";
            break;
          case ID_KEYFRAMER:
            chunkname = "ID_KEYFRAMER";
            break;
          case ID_KEYFRAMER_FRAMES:
            chunkname = "ID_KEYFRAMER_FRAMES";
            break;
          case ID_KEYFRAMER_UNKNOWN_01:
            chunkname = "ID_KEYFRAMER_UNKNOWN_01";
            break;
          case ID_KEYFRAMER_UNKNOWN_02:
            chunkname = "ID_KEYFRAMER_UNKNOWN_02";
            break;
          case ID_KEYFRAMER_OBJDES:
            chunkname = "ID_KEYFRAMER_OBJDES";
            break;
          case ID_KEYFRAMER_OBJHIERARCH:
            chunkname = "ID_KEYFRAMER_OBJHIERARCH";
            break;
          case ID_KEYFRAMER_OBJDUMMYNAME:
            chunkname = "ID_KEYFRAMER_OBJDUMMYNAME";
            break;
          case ID_KEYFRAMER_OBJUNKNOWN_01:
            chunkname = "ID_KEYFRAMER_OBJUNKNOWN_01";
            break;
          case ID_KEYFRAMER_OBJUNKNOWN_02:
            chunkname = "ID_KEYFRAMER_OBJUNKNOWN_02";
            break;
          case ID_KEYFRAMER_OBJUNKNOWN_03:
            chunkname = "ID_KEYFRAMER_OBJUNKNOWN_03";
            break;
          case ID_KEYFRAMER_OBJPIVOT:
            chunkname = "ID_KEYFRAMER_OBJPIVOT";
            break;
          case ID_KEYFRAMER_OBJUNKNOWN_04:
            chunkname = "ID_KEYFRAMER_OBJUNKNOWN_04";
            break;
          case ID_KEYFRAMER_OBJUNKNOWN_05:
            chunkname = "ID_KEYFRAMER_OBJUNKNOWN_05";
            break;
          default:
            byte a, b;
            a = (byte)((chunkid & 0xFF00) >> 8);
            b = (byte)((chunkid & 0x00FF));
            chunkname = "<Unknown id 0x" + 
                VSDK.formatByteAsHex(a) + VSDK.formatByteAsHex(b) + ">";
        }
        return chunkname;
    }

    public _Reader3dsChunk()
    {
        id = 0x0000;
        length = 0;
    }

    public String toString()
    {
        String msg;
        msg = "CHUNK3DS type [" +  chunkToString(id) + "], length ["
            + length + "]";
        return msg;
    }

    public void readHeader(InputStream is) throws Exception
    {
        id = readIntLE(is);
        length = readLongLE(is);
    }
}

/**
The class Reader3ds provides 3DStudio loading functionality.  The .3ds
fileformat was the original binary fileformat for ancient 3DStudio program
from Kinetix/Discreet originally deployed for the PC/DOS platform.  The
format was later upgraded to .MAX (not compatible) in windows version of
the program, known as "3DStudio MAX". However, current versions of 3DStudio
MAX support backward compatibility importing and exporting to old 3DStudio
format, and several files exists today persisted in this format.

This is currently a Java/VitralSDK based implementation of the algorithms
and data structures as described in [PITT1994], [FERC1996] and [VANV1997].

@todo Perhaps "Reader3ds" is not the best name for this class, as in the
future should support exporting (writing) operations. It could be renamed
to something as "Persistence3ds".
*/
public class Reader3ds extends PersistenceElement
{
    // Accumulated object parts
    private static ArrayList<Vector3D> currentVertexPositionArray = null;
    private static ArrayList<_Reader3dsMaterialMapping> currentMaterialMappingArray = null;
    private static TriangleMesh currentTriangleMesh = null;
    private static Triangle[] currentTrianglesList;

    // Current environment building elements
    private static Material currentBuildingMaterial = null;
    private static ColorRgb currentColor = null;
    private static ArrayList<Material> currentMaterialArray = null;
    private static ArrayList<SimpleBody> currentSimpleBodiesArray = null;

    private static Material defaultMaterial()
    {
        Material m = new Material();

        m.setAmbient(new ColorRgb(0.2, 0.2, 0.2));
        m.setDiffuse(new ColorRgb(0.5, 0.9, 0.5));
        m.setSpecular(new ColorRgb(1, 1, 1));
        return m;
    }

    private static Material resolveMaterial(String name)
    {
        int i;
        Material m;

        for ( i = 0; i < currentMaterialArray.size(); i++ ) {
            m = currentMaterialArray.get(i);
            if ( name.equals(m.getName()) ) {
                return m;
            }
        }
        return defaultMaterial();
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

    private static String indent(int level) {
        String tab = "";
        switch ( level ) {
          case 0: tab = ""; break;
          case 1: tab = "  - "; break;
          case 2: tab = "    . "; break;
          default:
            tab = "      (" + level + ")-> ";
            break;
        }
        return tab;
    }

    private static boolean checkChunkHierarchy(_Reader3dsChunk son, 
                                               _Reader3dsChunk father)
    {
        if (
(father == null) ||
(father.id == father.ID_MAIN && son.id == son.ID_MESH) ||
(father.id == father.ID_MAIN && son.id == son.ID_BOTTOM) ||
(father.id == father.ID_MAIN && son.id == son.ID_KEYFRAMER) ||
(father.id == father.ID_MESH && son.id == son.ID_TOP) ||
(father.id == father.ID_MESH && son.id == son.ID_MATERIAL) ||
(father.id == father.ID_MESH && son.id == son.ID_OBJECT_BLOCK) ||
(father.id == father.ID_MESH && son.id == son.ID_EDIT_CONFIG1) ||
(father.id == father.ID_MESH && son.id == son.ID_VIEWPORT) ||
(father.id == father.ID_MESH && son.id == son.ID_EDIT_UNKNOWN_01) ||
(father.id == father.ID_MESH && son.id == son.ID_EDIT_UNKNOWN_02) ||
(father.id == father.ID_MESH && son.id == son.ID_EDIT_UNKNOWN_03) ||
(father.id == father.ID_MESH && son.id == son.ID_EDIT_UNKNOWN_04) ||
(father.id == father.ID_MESH && son.id == son.ID_EDIT_UNKNOWN_05) ||
(father.id == father.ID_MESH && son.id == son.ID_EDIT_UNKNOWN_06) ||
(father.id == father.ID_MESH && son.id == son.ID_EDIT_UNKNOWN_07) ||
(father.id == father.ID_MESH && son.id == son.ID_EDIT_UNKNOWN_08) ||
(father.id == father.ID_MESH && son.id == son.ID_EDIT_UNKNOWN_09) ||
(father.id == father.ID_MESH && son.id == son.ID_EDIT_UNKNOWN_10) ||
(father.id == father.ID_MESH && son.id == son.ID_EDIT_UNKNOWN_11) ||
(father.id == father.ID_MESH && son.id == son.ID_EDIT_UNKNOWN_12) ||
(father.id == father.ID_MESH && son.id == son.ID_EDIT_UNKNOWN_13) ||
(father.id == father.ID_MESH && son.id == son.ID_EDIT_UNKNOWN_14) ||
(father.id == father.ID_MESH && son.id == son.ID_AMBIENT_COLOR) ||
(father.id == father.ID_MESH && son.id == son.ID_BACKGROUND_COLOR) ||
(father.id == father.ID_MATERIAL && son.id == son.ID_MATERIAL_NAME) ||
(father.id == father.ID_MATERIAL && son.id == son.ID_MATERIAL_AMBIENT) ||
(father.id == father.ID_MATERIAL && son.id == son.ID_MATERIAL_DIFFUSE) ||
(father.id == father.ID_MATERIAL && son.id == son.ID_MATERIAL_SPECULAR) ||
(father.id == father.ID_MATERIAL && son.id == son.ID_MATERIAL_TYPE) ||
(father.id == father.ID_MATERIAL && son.id == son.ID_MATERIAL_SHININESS) ||
(father.id == father.ID_MATERIAL && son.id == son.ID_MATERIAL_TWOSIDED) ||
(father.id == father.ID_TRIANGLE_LIST && son.id == son.ID_MATERIAL_MAPPING_TABLE) ||
(father.id == father.ID_TRIANGLE_LIST && son.id == son.ID_SMOOTH_LIST) ||
(father.id == father.ID_OBJECT_BLOCK && son.id == son.ID_TRIANGLE_MESH) ||
(father.id == father.ID_OBJECT_BLOCK && son.id == son.ID_LIGHT) ||
(father.id == father.ID_OBJECT_BLOCK && son.id == son.ID_CAMERA) ||
(father.id == father.ID_TRIANGLE_MESH && son.id == son.ID_VERTEX_LIST) ||
(father.id == father.ID_TRIANGLE_MESH && son.id == son.ID_TRIANGLE_LIST) ||
(father.id == father.ID_TRIANGLE_MESH && son.id == son.ID_TRI_VERTEX_OPTIONS) ||(father.id == father.ID_TRIANGLE_MESH && son.id == son.ID_MATRIX) ||
((father.id == father.ID_MATERIAL_AMBIENT || 
  father.id == father.ID_MATERIAL_DIFFUSE || 
  father.id == father.ID_MATERIAL_SPECULAR)  && son.id == son.ID_COLOR_RGB1) ||
((father.id == father.ID_MATERIAL_AMBIENT || 
  father.id == father.ID_MATERIAL_DIFFUSE || 
  father.id == father.ID_MATERIAL_SPECULAR)  && son.id == son.ID_COLOR_RGB2) ||
((father.id == father.ID_MATERIAL_AMBIENT || 
  father.id == father.ID_MATERIAL_DIFFUSE || 
  father.id == father.ID_MATERIAL_SPECULAR)  && son.id == son.ID_COLOR_RGB3)
        ) {
            return true;
        }

        VSDK.reportMessage(null, VSDK.WARNING,
                           "Reader3ds.checkChunkHierarchy",
                           "" + son.chunkToString(son.id) + 
                           " chunk is not supposed to be a level under a " +
                           father.chunkToString(father.id) + " chunk");
        return false;
    }

    private static void processChunk(
        InputStream is, 
        _Reader3dsChunk currentChunk, _Reader3dsChunk parentChunk,
        int level) 
        throws Exception
    {
        //-----------------------------------------------------------------
        boolean skipChunk = false;

        if ( !checkChunkHierarchy(currentChunk, parentChunk) ) {
            skipChunk = true;
        }

        //-----------------------------------------------------------------
        int i;

        if ( (currentChunk.id == currentChunk.ID_MAIN) ||
             (currentChunk.id == currentChunk.ID_MESH) || 
             (currentChunk.id == currentChunk.ID_OBJECT_BLOCK) ||
             (currentChunk.id == currentChunk.ID_MATERIAL) ||
             (currentChunk.id == currentChunk.ID_TRIANGLE_MESH)
           ) {
            //-------------------------------------------------------------
            long internalBytes = 6; // skip current chunk header

            // Build operations preprocessing phase
            if ( currentChunk.id == currentChunk.ID_OBJECT_BLOCK ) {
                // Object block starts with a name string
                //System.out.print(indent(level) + "Reading object \"");
                String cad = readAsciiString(is);
                internalBytes += cad.length()+1;
                //System.out.println(cad + "\"");
            }
            if ( currentChunk.id == currentChunk.ID_TRIANGLE_MESH ) {
                currentTriangleMesh = new TriangleMesh();
                currentMaterialMappingArray =
                    new ArrayList<_Reader3dsMaterialMapping>();
            }
            if ( currentChunk.id == currentChunk.ID_MATERIAL ) {
                currentBuildingMaterial = new Material();
            }

            // Generic recursive block processing
            //System.out.println(indent(level) + currentChunk);

            // Processing of recursive chunks
            _Reader3dsChunk subChunk = new _Reader3dsChunk();

            do {
                subChunk.readHeader(is);
                processChunk(is, subChunk, currentChunk, level+1);
                internalBytes += subChunk.length;
            } while ( is.available() > 0 && 
                      (internalBytes < currentChunk.length) );

            // Build operations postprocessing phase
            if ( currentChunk.id == currentChunk.ID_MATERIAL ) {
                currentMaterialArray.add(currentBuildingMaterial);
            }
            if ( currentChunk.id == currentChunk.ID_TRIANGLE_MESH ) {
                // Vertex processing
                Vertex v[] = new Vertex[currentVertexPositionArray.size()];
                for ( i = 0; i < v.length; i++ ) {
                    v[i] = new Vertex(new Vector3D(currentVertexPositionArray.get(i)));
                }
                currentTriangleMesh.setVertexes(v);

                // Triangle processing
                int numMappedTriangles = 0;
                _Reader3dsMaterialMapping map_i;
                for ( i = 0; i < currentMaterialMappingArray.size(); i++ ) {
                    map_i = currentMaterialMappingArray.get(i);
                    numMappedTriangles += map_i.associatedTriangles.length;
                }

                if ( numMappedTriangles <= 0 ) {
                    currentTriangleMesh.setTriangles(currentTrianglesList);
                }
                else {
                    Triangle newTrianglesList[];
                    Material newMaterials[];
                    int newRanges[][];
                    int j, k;

                    newTrianglesList = new Triangle[numMappedTriangles];
                    newMaterials =
                        new Material[currentMaterialMappingArray.size()];
                    newRanges = new int[currentMaterialMappingArray.size()][2];

                    for ( i = 0, k = 0; 
                          i < currentMaterialMappingArray.size(); i++ ) {
                        map_i = currentMaterialMappingArray.get(i);
                        newMaterials[i] = resolveMaterial(map_i.materialName);
                        for ( j = 0; 
                              j < map_i.associatedTriangles.length; j++ ) {
                            newTrianglesList[k] = 
                            currentTrianglesList[map_i.associatedTriangles[j]];
                            k++;
                        }
                        newRanges[i][0] = k;
                        newRanges[i][1] = i;
                    }
                    currentTriangleMesh.setTriangles(newTrianglesList);
                    currentTriangleMesh.setMaterials(newMaterials);
                    currentTriangleMesh.setMaterialRanges(newRanges);
                    currentTrianglesList = null;
                }

                // Mesh adition to environment
                currentTriangleMesh.calculateNormals();
                addThing(currentTriangleMesh, currentSimpleBodiesArray);
            }
        }
        else if ( currentChunk.id == currentChunk.ID_VERTEX_LIST ) {
            //-------------------------------------------------------------
            //System.out.println(indent(level) + currentChunk);
            int numVertexes = readIntLE(is);
            //System.out.println(indent(level+1) + "  . Reading " + numVertexes +
            //                 " vertexes");

            currentVertexPositionArray = new ArrayList<Vector3D>();
            Vector3D p;
            for ( i = 0; i < numVertexes; i++ ) {
                p = new Vector3D();
                p.x = readFloatLE(is);
                p.y = readFloatLE(is);
                p.z = readFloatLE(is);
                currentVertexPositionArray.add(p);
            }
            is.skip(currentChunk.length-8-numVertexes*12);
        }
        else if ( currentChunk.id == currentChunk.ID_TRIANGLE_LIST ) {
            //-------------------------------------------------------------
            //System.out.println(indent(level) + currentChunk);

            //----
            int numTriangles = readIntLE(is);
            //System.out.println(indent(level+1) + "  . Reading " + numTriangles +
            //                 " triangles");

            int a, b, c, flags;

            currentTrianglesList = new Triangle[numTriangles];
            for ( i = 0; i < numTriangles; i++ ) {
                a = readIntLE(is);
                b = readIntLE(is);
                c = readIntLE(is);
                flags = readIntLE(is);
                currentTrianglesList[i] = new Triangle(a, b, c);
            }

            //----
            // skip current chunk header AND triangles
            long internalBytes = 8+numTriangles*8; 

            // Processing of recursive chunks
            _Reader3dsChunk subChunk = new _Reader3dsChunk();

            do {
                subChunk.readHeader(is);
                processChunk(is, subChunk, currentChunk, level+1);
                internalBytes += subChunk.length;
            } while ( is.available() > 0 && 
                      (internalBytes < currentChunk.length) );
        }
        else if ( currentChunk.id == currentChunk.ID_MATRIX ) {
            //-------------------------------------------------------------
            //System.out.println(indent(level) + currentChunk);
            is.skip(12*4);
        }
        else if ( currentChunk.id == currentChunk.ID_MATERIAL_NAME ) {
            //-------------------------------------------------------------
            //System.out.println(indent(level) + currentChunk);
            String materialName = readAsciiString(is);
            if ( currentBuildingMaterial != null ) {
                currentBuildingMaterial.setName(materialName);
            }
        }
        else if ( currentChunk.id == currentChunk.ID_COLOR_RGB2 ) {
            //-------------------------------------------------------------
            //System.out.println(indent(level) + currentChunk);
            byte r[] = new byte[1];
            byte g[] = new byte[1];
            byte b[] = new byte[1];
            readBytes(is, r);
            readBytes(is, g);
            readBytes(is, b);
            if ( currentColor != null ) {
                currentColor.r = 
                    (double)(VSDK.signedByte2unsignedInteger(r[0])) / 255.0;
                currentColor.g = 
                    (double)(VSDK.signedByte2unsignedInteger(g[0])) / 255.0;
                currentColor.b = 
                    (double)(VSDK.signedByte2unsignedInteger(b[0])) / 255.0;
            }
        }
        else if ( currentChunk.id == currentChunk.ID_MATERIAL_AMBIENT ) {
            //-------------------------------------------------------------
            //System.out.println(indent(level) + currentChunk);
            currentColor = new ColorRgb();
            // Processing of recursive chunks
            _Reader3dsChunk subChunk = new _Reader3dsChunk();
            long internalBytes = 6; // Skip current chunk header
            do {
                subChunk.readHeader(is);
                processChunk(is, subChunk, currentChunk, level+1);
                internalBytes += subChunk.length;
            } while ( is.available() > 0 && 
                      (internalBytes < currentChunk.length) );
            // Set color in the material
            if ( currentBuildingMaterial != null ) {
                currentBuildingMaterial.setAmbient(currentColor);
            }
        }
        else if ( currentChunk.id == currentChunk.ID_MATERIAL_DIFFUSE ) {
            //-------------------------------------------------------------
            //System.out.println(indent(level) + currentChunk);
            currentColor = new ColorRgb();
            // Processing of recursive chunks
            _Reader3dsChunk subChunk = new _Reader3dsChunk();
            long internalBytes = 6; // Skip current chunk header
            do {
                subChunk.readHeader(is);
                processChunk(is, subChunk, currentChunk, level+1);
                internalBytes += subChunk.length;
            } while ( is.available() > 0 && 
                      (internalBytes < currentChunk.length) );
            // Set color in the material
            if ( currentBuildingMaterial != null ) {
                currentBuildingMaterial.setDiffuse(currentColor);
            }
        }
        else if ( currentChunk.id == currentChunk.ID_MATERIAL_SPECULAR ) {
            //-------------------------------------------------------------
            //System.out.println(indent(level) + currentChunk);
            currentColor = new ColorRgb();
            // Processing of recursive chunks
            _Reader3dsChunk subChunk = new _Reader3dsChunk();
            long internalBytes = 6; // Skip current chunk header
            do {
                subChunk.readHeader(is);
                processChunk(is, subChunk, currentChunk, level+1);
                internalBytes += subChunk.length;
            } while ( is.available() > 0 && 
                      (internalBytes < currentChunk.length) );
            // Set color in the material
            if ( currentBuildingMaterial != null ) {
                currentBuildingMaterial.setSpecular(currentColor);
            }
        }
        else if ( currentChunk.id == currentChunk.ID_MATERIAL_MAPPING_TABLE ) {
            //-------------------------------------------------------------
            //System.out.println(indent(level) + currentChunk);
            _Reader3dsMaterialMapping range = new _Reader3dsMaterialMapping();
            range.materialName = readAsciiString(is);
            int numMappings = readIntLE(is);
            range.associatedTriangles = new int[numMappings];
            //System.out.println(indent(level+1) + "  . Reading " +
            //             numMappings + " material mappings for material \"" +
            //             range.materialName + "\"");
            for ( i = 0; i < numMappings; i++ ) {
                range.associatedTriangles[i] = readIntLE(is);
            }
            currentMaterialMappingArray.add(range);
        }
        else {
            //-------------------------------------------------------------
            System.out.println(indent(level) + currentChunk + " (skipped)");
            // Trivial case: unknown chunk
            skipChunk = true;
        }

        //-----------------------------------------------------------------
        if ( skipChunk ) {
            is.skip(currentChunk.length - 6);
        }

    }

    public static void
    importEnvironment(File inSceneFileFd,
                      ArrayList<SimpleBody> inoutSimpleBodiesArray,
                      ArrayList<Light> inoutLightsArray,
                      ArrayList<Background> inoutBackgroundsArray,
                      ArrayList<Camera> inoutCamerasArray
                      ) throws Exception
    {
        //-----------------------------------------------------------------
        _Reader3dsChunk chunk = new _Reader3dsChunk();
        InputStream is = new FileInputStream(inSceneFileFd);

        currentSimpleBodiesArray = inoutSimpleBodiesArray;
        currentMaterialArray = new ArrayList<Material>();

        //- Main level chunk hierarchy processing -------------------------
        chunk.readHeader(is);

        if ( chunk.id != chunk.ID_MAIN ) {
            Exception e = new Exception("\"" + inSceneFileFd.getName() +
                "\" is not a 3DS format file, doesn't start with 0x4D4D " +
                "header chunk.");
            throw e;
        }

        processChunk(is, chunk, null, 0);

        //-----------------------------------------------------------------
        is.close();
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
