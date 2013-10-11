//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - March 17 2006 - Oscar Chavarro: Original base version                 =
//===========================================================================

package vsdk.toolkit.render.jogl;

// JOGL classes
import javax.media.opengl.GL;
import javax.media.opengl.GL2;

// VSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.environment.Material;

public class JoglMaterialRenderer extends JoglRenderer {
    private static boolean errorReported = false;
    private static boolean disablingTransparency = false;

    public static void activate(GL2 gl, Material m)
    {
        //-----------------------------------------------------------------
        if ( m == null ) {
            if ( errorReported == false ) {
                VSDK.reportMessage(null, VSDK.WARNING, 
                    "JoglMaterialRenderer.activate", 
                    "Trying to activate null reference to Material." + 
                    " Avoiding further reporting.");
                errorReported = true;
            }
            return;
        }

        float opacity = (float)m.getOpacity();

        if ( opacity > 1.0f ) opacity = 1.0f;
        if ( opacity < 0.0f ) opacity = 0.0f;
        if ( opacity < 1.0f - Float.MIN_VALUE ) {
            gl.glEnable(GL.GL_BLEND);
            gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
            disablingTransparency = true;
          }
          else {
            // Note: Transparency blending is not compatible with
            // blending technique for anaglyphs...
            if ( disablingTransparency ) {
                gl.glDisable(GL.GL_BLEND);
            }
        }

        float phongExp = (float)m.getPhongExponent();
        float ambient[] = m.getAmbient().exportToFloatArrayVect();
        ambient[3] = opacity;
        float diffuse[] = m.getDiffuse().exportToFloatArrayVect();
        diffuse[3] = opacity;
        float specular[]  = m.getSpecular().exportToFloatArrayVect();
        specular[3] = opacity;
        //float emission[] = m.getEmission().exportToFloatArrayVect();
        //emission[3] = opacity;

        if ( m.isDoubleSided() ) {
            gl.glDisable(GL.GL_CULL_FACE);
          }
          else {
            gl.glEnable(GL.GL_CULL_FACE);
            gl.glCullFace(GL.GL_BACK);
        }

        gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL2.GL_AMBIENT, ambient, 0);
        gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL2.GL_DIFFUSE, diffuse, 0);
        gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL2.GL_SPECULAR, specular, 0);
        //gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL2.GL_EMISSION, emission, 0); // Do not set! take care!
        gl.glMaterialf(GL.GL_FRONT_AND_BACK, GL2.GL_SHININESS, phongExp);
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
