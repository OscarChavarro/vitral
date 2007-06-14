//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - March 17 2006 - Oscar Chavarro: Original base version                 =
//===========================================================================

package vsdk.toolkit.render.jogl;

import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.environment.Material;
import javax.media.opengl.GL;

public class JoglMaterialRenderer {

    public static void activate(GL gl, Material m)
    {
        float phongExp = (float)m.getPhongExponent();
        float ambient[] = m.getAmbient().toFloatVect();
        ambient[3] = (float)m.getAlpha();
        float diffuse[] = m.getDiffuse().toFloatVect();
        diffuse[3] = (float)m.getAlpha();
        float specular[]  = m.getSpecular().toFloatVect();
        specular[3] = (float)m.getAlpha();
        //float emission[] = m.getEmission().toFloatVect();
        //emission[3] = (float)m.getAlpha();

        gl.glDisable(gl.GL_BLEND);
        gl.glDisable(gl.GL_CULL_FACE);
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
