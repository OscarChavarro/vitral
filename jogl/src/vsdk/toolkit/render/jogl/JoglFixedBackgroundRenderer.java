//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - November 28 2005 - Oscar Chavarro: Original base version              =
//===========================================================================

package vsdk.toolkit.render.jogl;

// JOGL classes
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2GL3;

// VSDK classes
import vsdk.toolkit.environment.FixedBackground;

public class JoglFixedBackgroundRenderer extends JoglRenderer 
{
    /**
    \todo  in the case of equal size, polygon rendering should not be used,
    direct texel to pixel copy should be used...
    */
    public static void draw(GL2 gl, FixedBackground background)
    {
        //-----------------------------------------------------------------
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glMatrixMode(GL.GL_TEXTURE);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();

        //- Clear background ----------------------------------------------
        //gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
        gl.glShadeModel(GL2.GL_FLAT);
        gl.glDisable(GL.GL_BLEND);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL.GL_DEPTH_TEST);
        gl.glDisable(GL.GL_TEXTURE_2D);
        gl.glFrontFace(GL.GL_CCW);
        gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2GL3.GL_FILL);

        //- Put image background ------------------------------------------
        gl.glEnable(GL.GL_TEXTURE_2D);
        JoglRGBAImageRenderer.activate(gl, background.getImage());

        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL2.GL_GENERATE_MIPMAP,
            GL.GL_TRUE);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER,
            GL.GL_LINEAR_MIPMAP_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER,
            GL.GL_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S,
            GL.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T,
            GL.GL_CLAMP_TO_EDGE);
        gl.glTexEnvf(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE,
            GL.GL_REPLACE);

        gl.glColor3d(1, 1, 1);
        gl.glBegin(GL2.GL_QUADS);
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
        gl.glMatrixMode(GL.GL_TEXTURE);
        gl.glPopMatrix();
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glDisable(GL.GL_TEXTURE_2D);
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
