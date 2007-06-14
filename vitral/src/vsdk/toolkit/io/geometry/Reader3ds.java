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
import vsdk.toolkit.environment.Background;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.TriangleMeshGroup;
import vsdk.toolkit.environment.scene.SimpleBody;

import vsdk.toolkit.io.PersistenceElement;

class _Reader3dsChunk extends PersistenceElement
{
    //=======================================================================
    public int chunkId;
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
    public static final int ID_OBJECT_BLOCK            = 0x4000;
    public static final int ID_TRIANGLE_MESH           = 0x4100;
    public static final int ID_VERTEXT_LIST            = 0x4110;
    public static final int ID_FACE_LIST               = 0x4120;
    public static final int ID_FACE_MATERIAL           = 0x4130;
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
    public static final int ID_DISABLED                = 0x0010;
    public static final int ID_BOGUS                   = 0x0011;
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
            chunkname = "ID_MAIN"; break;
          case ID_MESH:
            chunkname = "ID_MESH"; break;
          case ID_BACKGROUND_COLOR:
            chunkname = "ID_BACKGROUND_COLOR"; break;
          case ID_AMBIENT_COLOR:
            chunkname = "ID_AMBIENT_COLOR"; break;
          case ID_MATERIAL:
            chunkname = "ID_MATERIAL"; break;
          case ID_MATERIAL_NAME:
            chunkname = "ID_MATERIAL_NAME"; break;
          case ID_OBJECT_BLOCK:
            chunkname = "ID_OBJECT_BLOCK"; break;
          case ID_TRIANGLE_MESH:
            chunkname = "ID_TRIANGLE_MESH"; break;
          case ID_VERTEXT_LIST:
            chunkname = "ID_VERTEXT_LIST"; break;
          case ID_FACE_LIST:
            chunkname = "ID_FACE_LIST"; break;
          case ID_FACE_MATERIAL:
            chunkname = "ID_FACE_MATERIAL"; break;
          case ID_SMOOTH_LIST:
            chunkname = "ID_SMOOTH_LIST"; break;
          case ID_MAP_LIST:
            chunkname = "ID_MAP_LIST"; break;
          case ID_MATRIX:
            chunkname = "ID_MATRIX"; break;
          case ID_TRI_MAPPING_STANDARD:
            chunkname = "ID_TRI_MAPPING_STANDARD"; break;
          case ID_TRI_VISIBLE:
            chunkname = "ID_TRI_VISIBLE"; break;
          case ID_TRI_VERTEX_OPTIONS:
            chunkname = "ID_TRI_VERTEX_OPTIONS"; break;
          case ID_LIGHT:
            chunkname = "ID_LIGHT"; break;
          case ID_SPOTLIGHT:
            chunkname = "ID_SPOTLIGHT"; break;
          case ID_LIT_OFF:
            chunkname = "ID_LIT_OFF"; break;
          case ID_LIT_UNKNOWN_01:
            chunkname = "ID_LIT_UNKNOWN_01"; break;
          case ID_CAMERA:
            chunkname = "ID_CAMERA"; break;
          case ID_OBJECT_UNKNOWN_01:
            chunkname = "ID_OBJECT_UNKNOWN_01"; break;
          case ID_OBJECT_UNKNOWN_02:
            chunkname = "ID_OBJECT_UNKNOWN_02"; break;
          case ID_EDIT_CONFIG1:
            chunkname = "ID_EDIT_CONFIG1"; break;
          case ID_EDIT_CONFIG2:
            chunkname = "ID_EDIT_CONFIG2"; break;
          case ID_EDIT_VIEW_P1:
            chunkname = "ID_EDIT_VIEW_P1"; break;
          case ID_EDIT_VIEW_P2:
            chunkname = "ID_EDIT_VIEW_P2"; break;
          case ID_EDIT_VIEW_P3:
            chunkname = "ID_EDIT_VIEW_P3"; break;
          case ID_TOP:
            chunkname = "ID_TOP"; break;
          case ID_BOTTOM:
            chunkname = "ID_BOTTOM"; break;
          case ID_LEFT:
            chunkname = "ID_LEFT"; break;
          case ID_RIGHT:
            chunkname = "ID_RIGHT"; break;
          case ID_FRONT:
            chunkname = "ID_FRONT"; break;
          case ID_BACK:
            chunkname = "ID_BACK"; break;
          case ID_USER:
            chunkname = "ID_USER"; break;
          case ID_CAMERA_VIEW:
            chunkname = "ID_CAMERA_VIEW"; break;
          case ID_LIGHT_VIEW:
            chunkname = "ID_LIGHT_VIEW"; break;
          case ID_DISABLED:
            chunkname = "ID_DISABLED"; break;
          case ID_BOGUS:
            chunkname = "ID_BOGUS"; break;
          case ID_VIEWPORT:
            chunkname = "ID_VIEWPORT"; break;
          case ID_EDIT_UNKNOWN_01:
            chunkname = "ID_EDIT_UNKNOWN_01"; break;
          case ID_EDIT_UNKNOWN_02:
            chunkname = "ID_EDIT_UNKNOWN_02"; break;
          case ID_EDIT_UNKNOWN_03:
            chunkname = "ID_EDIT_UNKNOWN_03"; break;
          case ID_EDIT_UNKNOWN_04:
            chunkname = "ID_EDIT_UNKNOWN_04"; break;
          case ID_EDIT_UNKNOWN_05:
            chunkname = "ID_EDIT_UNKNOWN_05"; break;
          case ID_EDIT_UNKNOWN_06:
            chunkname = "ID_EDIT_UNKNOWN_06"; break;
          case ID_EDIT_UNKNOWN_07:
            chunkname = "ID_EDIT_UNKNOWN_07"; break;
          case ID_EDIT_UNKNOWN_08:
            chunkname = "ID_EDIT_UNKNOWN_08"; break;
          case ID_EDIT_UNKNOWN_09:
            chunkname = "ID_EDIT_UNKNOWN_09"; break;
          case ID_EDIT_UNKNOWN_10:
            chunkname = "ID_EDIT_UNKNOWN_10"; break;
          case ID_EDIT_UNKNOWN_11:
            chunkname = "ID_EDIT_UNKNOWN_11"; break;
          case ID_EDIT_UNKNOWN_12:
            chunkname = "ID_EDIT_UNKNOWN_12"; break;
          case ID_EDIT_UNKNOWN_13:
            chunkname = "ID_EDIT_UNKNOWN_13"; break;
          case ID_KEYFRAMER:
            chunkname = "ID_KEYFRAMER"; break;
          case ID_KEYFRAMER_FRAMES:
            chunkname = "ID_KEYFRAMER_FRAMES"; break;
          case ID_KEYFRAMER_UNKNOWN_01:
            chunkname = "ID_KEYFRAMER_UNKNOWN_01"; break;
          case ID_KEYFRAMER_UNKNOWN_02:
            chunkname = "ID_KEYFRAMER_UNKNOWN_02"; break;
          case ID_KEYFRAMER_OBJDES:
            chunkname = "ID_KEYFRAMER_OBJDES"; break;
          case ID_KEYFRAMER_OBJHIERARCH:
            chunkname = "ID_KEYFRAMER_OBJHIERARCH"; break;
          case ID_KEYFRAMER_OBJDUMMYNAME:
            chunkname = "ID_KEYFRAMER_OBJDUMMYNAME"; break;
          case ID_KEYFRAMER_OBJUNKNOWN_01:
            chunkname = "ID_KEYFRAMER_OBJUNKNOWN_01"; break;
          case ID_KEYFRAMER_OBJUNKNOWN_02:
            chunkname = "ID_KEYFRAMER_OBJUNKNOWN_02"; break;
          case ID_KEYFRAMER_OBJUNKNOWN_03:
            chunkname = "ID_KEYFRAMER_OBJUNKNOWN_03"; break;
          case ID_KEYFRAMER_OBJPIVOT:
            chunkname = "ID_KEYFRAMER_OBJPIVOT"; break;
          case ID_KEYFRAMER_OBJUNKNOWN_04:
            chunkname = "ID_KEYFRAMER_OBJUNKNOWN_04"; break;
          case ID_KEYFRAMER_OBJUNKNOWN_05:
            chunkname = "ID_KEYFRAMER_OBJUNKNOWN_05"; break;
    }
        return chunkname;
    }

    public _Reader3dsChunk()
    {
        chunkId = 0x0000;
        length = 0;
    }

    public String toString()
    {
    String msg;
        msg = "CHUNK3DS type [" +  chunkToString(chunkId) + "], length ["
            + length + "]";
    return msg;
    }

    public void readHeader(InputStream is) throws Exception
    {
        byte[] bytesBuffer;

        bytesBuffer = new byte[4];

        readBytes(is, bytesBuffer);
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
    //=======================================================================
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

        //-----------------------------------------------------------------
        System.out.println("Processing 3DS file...");

        chunk.readHeader(is);
    System.out.println(chunk);

        //-----------------------------------------------------------------
        is.close();
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
