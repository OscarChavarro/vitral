//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - July 22 2007 - Oscar Chavarro: Original base version                  =
//===========================================================================

package vsdk.toolkit.io.geometry;

// Java basic classes
import java.io.File;
import java.io.FileReader;
import java.io.StreamTokenizer;
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
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.io.PersistenceElement;
import vsdk.toolkit.io.image.ImagePersistence;

public class ReaderAse extends PersistenceElement
{
    private static ArrayList <String> ids = new ArrayList<String>();

// Identifiers found inside a test set of 6500 .ase files imported from .max
/*
*3DSMAX_ASCIIEXPORT
*BITMAP
*BITMAP_FILTER
*BITMAP_INVERT
*BOUNDINGBOX_MAX
*BOUNDINGBOX_MIN
*CAMERA_FAR
*CAMERA_FOV
*CAMERA_HITHER
*CAMERA_NEAR
*CAMERAOBJECT
*CAMERA_SETTINGS
*CAMERA_TDIST
*CAMERA_TYPE
*CAMERA_YON
*COMMENT
*CONTROL_BEZIER_POS_KEY
*CONTROL_BEZIER_SCALE_KEY
*CONTROL_FLOAT_KEY
*CONTROL_FLOAT_SAMPLE
*CONTROL_POS_BEZIER
*CONTROL_POS_KEY
*CONTROL_POS_LINEAR
*CONTROL_POS_SAMPLE
*CONTROL_POS_TCB
*CONTROL_POS_TRACK
*CONTROL_ROT_BEZIER
*CONTROL_ROT_KEY
*CONTROL_ROT_LINEAR
*CONTROL_ROT_SAMPLE
*CONTROL_ROT_TCB
*CONTROL_ROT_TRACK
*CONTROL_SCALE_BEZIER
*CONTROL_SCALE_SAMPLE
*CONTROL_SCALE_TCB
*CONTROL_SCALE_TRACK
*CONTROL_TCB_POS_KEY
*CONTROL_TCB_ROT_KEY
*CONTROL_TCB_SCALE_KEY
*GEOMOBJECT
*GROUP
*HELPER_CLASS
*HELPEROBJECT
*INHERIT_POS
*INHERIT_ROT
*INHERIT_SCL
*LIGHT_ABSMAPBIAS
*LIGHT_ASPECT
*LIGHT_ATTNEND
*LIGHT_ATTNSTART
*LIGHT_COLOR
*LIGHT_EXCLUDED
*LIGHT_EXCLUDED_AFFECT_ILLUM
*LIGHT_EXCLUDED_AFFECT_SHADOW
*LIGHT_EXCLUDED_INCLUDE
*LIGHT_EXCLUDELIST
*LIGHT_FALLOFF
*LIGHT_HOTSPOT
*LIGHT_INTENS
*LIGHT_MAPBIAS
*LIGHT_MAPRANGE
*LIGHT_MAPSIZE
*LIGHT_NUMEXCLUDED
*LIGHTOBJECT
*LIGHT_OVERSHOOT
*LIGHT_RAYBIAS
*LIGHT_SETTINGS
*LIGHT_SHADOWS
*LIGHT_SPOTSHAPE
*LIGHT_TDIST
*LIGHT_TYPE
*LIGHT_USEGLOBAL
*LIGHT_USELIGHT
*MAP_AMBIENT
*MAP_AMOUNT
*MAP_BUMP
*MAP_CLASS
*MAP_DIFFUSE
*MAP_FILTERCOLOR
*MAP_GENERIC
*MAP_NAME
*MAP_OPACITY
*MAP_REFLECT
*MAP_REFRACT
*MAP_SELFILLUM
*MAP_SHINE
*MAP_SHINESTRENGTH
*MAP_SPECULAR
*MAP_SUBNO
*MAP_TYPE
*MATERIAL
*MATERIAL_AMBIENT
*MATERIAL_CLASS
*MATERIAL_COUNT
*MATERIAL_DIFFUSE
*MATERIAL_FACEMAP
*MATERIAL_FALLOFF
*MATERIAL_LIST
*MATERIAL_NAME
*MATERIAL_REF
*MATERIAL_SELFILLUM
*MATERIAL_SHADING
*MATERIAL_SHINE
*MATERIAL_SHINESTRENGTH
*MATERIAL_SOFTEN
*MATERIAL_SPECULAR
*MATERIAL_TRANSPARENCY
*MATERIAL_TWOSIDED
*MATERIAL_WIRE
*MATERIAL_WIRESIZE
*MATERIAL_WIREUNITS
*MATERIAL_XP_FALLOFF
*MATERIAL_XP_TYPE
*MESH
*MESH_FACE
*MESH_FACE_LIST
*MESH_MTLID
*MESH_NUMFACES
*MESH_NUMVERTEX
*MESH_SMOOTHING
*MESH_VERTEX
*MESH_VERTEX_LIST
*NODE_NAME
*NODE_PARENT
*NODE_TM
*NODE_VISIBILITY_TRACK
*NUMSUBMTLS
*PROP_CASTSHADOW
*PROP_MOTIONBLUR
*PROP_RECVSHADOW
*SCENE
*SCENE_AMBIENT_STATIC
*SCENE_BACKGROUND_STATIC
*SCENE_ENVMAP
*SCENE_FILENAME
*SCENE_FIRSTFRAME
*SCENE_FRAMESPEED
*SCENE_LASTFRAME
*SCENE_TICKSPERFRAME
*SHAPE_CLOSED
*SHAPE_LINE
*SHAPE_LINECOUNT
*SHAPEOBJECT
*SHAPE_VERTEXCOUNT
*SHAPE_VERTEX_INTERP
*SHAPE_VERTEX_KNOT
*SUBMATERIAL
*TIMEVALUE
*TM_ANIMATION
*TM_POS
*TM_ROTANGLE
*TM_ROTAXIS
*TM_ROW0
*TM_ROW1
*TM_ROW2
*TM_ROW3
*TM_SCALE
*TM_SCALEAXIS
*TM_SCALEAXISANG
*UVW_ANGLE
*UVW_BLUR
*UVW_BLUR_OFFSET
*UVW_NOISE_LEVEL
*UVW_NOISE_PHASE
*UVW_NOISE_SIZE
*UVW_NOUSE_AMT
*UVW_U_OFFSET
*UVW_U_TILING
*UVW_V_OFFSET
*UVW_V_TILING
*WIREFRAME_COLOR
*/

    private static void printList()
    {
        int i;
        for ( i = 0; i < ids.size(); i++ ) {
            System.out.println(ids.get(i));
        }
    }

    private static void addToList(String n)
    {
        int i;
        String cad;

        for ( i = 0; i < ids.size(); i++ ) {
            cad = ids.get(i);
            if ( cad.equals(n) ) {
                return;
            }
        }
        ids.add(new String(n));
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
        StreamTokenizer parser = new StreamTokenizer(new FileReader(inSceneFileFd));

        parser.resetSyntax();
        parser.eolIsSignificant(true);
        parser.quoteChar('\"');
        parser.slashSlashComments(false);
        parser.slashStarComments(false);
        parser.whitespaceChars(' ', ' ');
        parser.whitespaceChars(',', ',');
        parser.whitespaceChars('\t', '\t');
        parser.whitespaceChars('\n', '\n');
        parser.whitespaceChars('\r', '\r');
        parser.parseNumbers();
        parser.wordChars('*', '*');
        parser.wordChars('0', '9');
        parser.wordChars('.', '.');
        parser.wordChars('A', 'Z');
        parser.wordChars('a', 'z');
        parser.wordChars('_', '_');
        parser.wordChars('`', '`');
        parser.wordChars('(', '(');
        parser.wordChars(')', ')');
        parser.wordChars('\'', '\'');
        parser.wordChars('+', '+');
        parser.wordChars('?', '?');
        parser.wordChars('!', '!');
        parser.wordChars('=', '=');
        parser.wordChars('&', '&');
        parser.wordChars('/', '/');
        parser.wordChars('#', '#');
        parser.wordChars('\\', '\\');
        parser.wordChars(':', ':');
        parser.wordChars('-', '-');

        int tokenType;
        long line = 0;
        int group = 0;
        int level = 0;

        do {
            try {
                tokenType = parser.nextToken();
            }
            catch ( Exception e ) {
                break;
            }
            switch (tokenType) {
              case StreamTokenizer.TT_EOL:
/*
                if ( line % 1000000L == 0 ) {
                    group++;
                    System.out.print("\n" + group + " ");
                }
                line++;
                if ( line % 100000L == 0 ) {
                    System.out.print(".");
                }
*/
                break;
              case StreamTokenizer.TT_EOF:
                break;
              case StreamTokenizer.TT_NUMBER:
                //System.out.println("NUMBER " + parser.nval);
                break;
              case StreamTokenizer.TT_WORD:
                if ( parser.sval.startsWith("*") ) {
                    addToList(parser.sval);
                }
                else {
                    //System.out.println("WORD " + parser.sval);
                }
                break;
              default:
                if (parser.ttype == '\"') {
                    //System.out.println("STRING " + parser.sval);
                  }
                  else {
                    // Only supposed to contain '{' or '}'
                    String report;
                    report = parser.toString();
                    if ( report.length() >= 8 ) {
                        char content = report.charAt(7);
                        if (content == '{') {
                            //System.out.println("{ MARK");
                            level++;
                          }
                          else if (content == '}') {
                            //System.out.println("} MARK");
                              level--;
                          } else {
                            // Nothing is done, as this is and unknown token,
                            // posibly corresponding to an empty token (i.e.
                            // a comment line with no real information)
                        }
                    }
                }
                break;
            }
        } while ( tokenType != StreamTokenizer.TT_EOF );

        //printList();
        System.out.print(inSceneFileFd.getAbsolutePath());

        if ( level == 0 ) { 
            System.out.println(" OKOK!");
        }
        else {
            System.out.println(" BADBAD: Final level " + level);
        }
        System.exit(0);
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
