//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - November 28 2005 - Oscar Chavarro: Original base version              =
//===========================================================================

package vsdk.toolkit.render.jogl;

import javax.media.opengl.GL2;

import vsdk.toolkit.media.RGBAImage;
import vsdk.toolkit.environment.FixedBackground;
import vsdk.toolkit.render.jogl.JoglCameraRenderer;

public class JoglFixedBackgroundRenderer extends JoglRenderer 
{
    /**
    @todo in the case of equal size, polygon rendering should not be used,
    direct texel to pixel copy should be used...
    */
    public static void draw(GL2 gl, FixedBackground background)
    {
        //-----------------------------------------------------------------
        gl.glMatrixMode(gl.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glMatrixMode(gl.GL_TEXTURE);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glMatrixMode(gl.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();

        //- Clear background ----------------------------------------------
        //gl.glClear(gl.GL_DEPTH_BUFFER_BIT);
        gl.glShadeModel(gl.GL_FLAT);
        gl.glDisable(gl.GL_BLEND);
        gl.glDisable(gl.GL_LIGHTING);
        gl.glDisable(gl.GL_DEPTH_TEST);
        gl.glDisable(gl.GL_TEXTURE_2D);
        gl.glFrontFace(gl.GL_CCW);
        gl.glPolygonMode(gl.GL_FRONT_AND_BACK, gl.GL_FILL);

        //- Put image background ------------------------------------------
        gl.glEnable(gl.GL_TEXTURE_2D);
        JoglRGBAImageRenderer.activate(gl, background.getImage());

        gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_GENERATE_MIPMAP,
            gl.GL_TRUE);
        gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_MIN_FILTER,
            gl.GL_LINEAR_MIPMAP_LINEAR);
        gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_MAG_FILTER,
            gl.GL_LINEAR);
        gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_WRAP_S,
            gl.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_WRAP_T,
            gl.GL_CLAMP_TO_EDGE);
        gl.glTexEnvf(gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_MODE,
            gl.GL_REPLACE);

        gl.glColor3d(1, 1, 1);
        gl.glBegin(gl.GL_QUADS);
            gl.glTexCoord2f(1, 1);
            gl.glVertex2f(1, 1);
            gl.glTexCoord2f(0, 1);
            gl.glVertex2f(-1, 1);
            gl.glTexCoord2f(0, 0);
            gl.glVertex2f(-1, -1);
            gl.glTexCoord2f(1, 0);
            gl.glVertex2f(1, -1);
        gl.glEnd();

        //-----------------------------------------------------------------
        gl.glPopMatrix();
        gl.glMatrixMode(gl.GL_TEXTURE);
        gl.glPopMatrix();
        gl.glMatrixMode(gl.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(gl.GL_MODELVIEW);
        gl.glEnable(gl.GL_DEPTH_TEST);
        gl.glDisable(gl.GL_TEXTURE_2D);
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
