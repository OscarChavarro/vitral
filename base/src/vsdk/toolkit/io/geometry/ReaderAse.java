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
        StreamTokenizer parser = new StreamTokenizer(new FileReader(inSceneFileFd));

        parser.resetSyntax();
        parser.eolIsSignificant(false);
        parser.quoteChar('\"');
        parser.slashSlashComments(false);
        parser.slashStarComments(false);
        parser.whitespaceChars(' ', ' ');
        parser.whitespaceChars(',', ',');
        parser.whitespaceChars('\t', '\t');
        parser.wordChars('A', 'Z');
        parser.wordChars('a', 'z');
        parser.wordChars('0', '9');
        parser.wordChars('_', '_');
        parser.wordChars('*', '*');
        parser.parseNumbers();

        int tokenType;

        do {
            try {
                tokenType = parser.nextToken();
            }
            catch ( Exception e ) {
                break;
            }
            switch (tokenType) {
              case StreamTokenizer.TT_EOL:
                break;
              case StreamTokenizer.TT_EOF:
                break;
              case StreamTokenizer.TT_NUMBER:
                System.out.println("NUMBER " + parser.nval);
                break;
              case StreamTokenizer.TT_WORD:
                System.out.println("WORD " + parser.sval);
                break;
              default:
                if (parser.ttype == '\"') {
                    System.out.println("STRING " + parser.sval);
                  }
                  else {
                    // Only supposed to contain '{' or '}'
                    String report;
                    report = parser.toString();
                    if (report.length() >= 8) {
                        char content = report.charAt(7);
                        if (content == '{') {
                            System.out.println("{ MARK");
                          }
                          else if (content == '}') {
                            System.out.println("} MARK");
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

    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
