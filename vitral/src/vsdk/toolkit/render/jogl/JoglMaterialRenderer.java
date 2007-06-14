//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - March 17 2006 - Oscar Chavarro: Original base version                 =
//===========================================================================

package vsdk.toolkit.render.jogl;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.environment.Material;
import javax.media.opengl.GL;

public class JoglMaterialRenderer extends JoglRenderer {
    private static boolean errorReported = false;

    public static void activate(GL gl, Material m)
    {
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
            gl.glEnable(gl.GL_BLEND);
            gl.glBlendFunc(gl.GL_SRC_ALPHA, gl.GL_ONE_MINUS_SRC_ALPHA);
          }
          else {
            gl.glDisable(gl.GL_BLEND);
        }

        float phongExp = (float)m.getPhongExponent();
        float ambient[] = m.getAmbient().toFloatVect();
        ambient[3] = opacity;
        float diffuse[] = m.getDiffuse().toFloatVect();
        diffuse[3] = opacity;
        float specular[]  = m.getSpecular().toFloatVect();
        specular[3] = opacity;
        //float emission[] = m.getEmission().toFloatVect();
        //emission[3] = opacity;

        if ( m.isDoubleSided() ) {
            gl.glDisable(gl.GL_CULL_FACE);
      }
      else {
            gl.glEnable(gl.GL_CULL_FACE);
        gl.glCullFace(gl.GL_BACK);
    }

        gl.glMaterialfv(gl.GL_FRONT_AND_BACK, gl.GL_AMBIENT, ambient, 0);
        gl.glMaterialfv(gl.GL_FRONT_AND_BACK, gl.GL_DIFFUSE, diffuse, 0);
        gl.glMaterialfv(gl.GL_FRONT_AND_BACK, gl.GL_SPECULAR, specular, 0);
        //gl.glMaterialfv(gl.GL_FRONT_AND_BACK, gl.GL_EMISSION, emission, 0); // Do not set! take care!
        gl.glMaterialf(gl.GL_FRONT_AND_BACK, gl.GL_SHININESS, phongExp);
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
