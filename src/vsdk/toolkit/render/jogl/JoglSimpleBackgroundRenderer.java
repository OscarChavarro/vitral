//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - November 25 2005 - Oscar Chavarro: Original base version              =
//===========================================================================

package vsdk.toolkit.render.jogl;

import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.SimpleBackground;
import javax.media.opengl.GL2;

public class JoglSimpleBackgroundRenderer extends JoglRenderer 
{
    public static void draw(GL2 gl, SimpleBackground background)
    {
        Vector3D d = new Vector3D(1, 0, 0);
        ColorRgb color;

        color = background.colorInDireccion(d);

        //-----------------------------------------------------------------
        gl.glMatrixMode(gl.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glMatrixMode(gl.GL_TEXTURE);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glMatrixMode(gl.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();

        //-----------------------------------------------------------------
        //gl.glClear(gl.GL_DEPTH_BUFFER_BIT);
        gl.glShadeModel(gl.GL_FLAT);
        gl.glDisable(gl.GL_BLEND);
        gl.glDisable(gl.GL_LIGHTING);
        gl.glDisable(gl.GL_DEPTH_TEST);
        gl.glDisable(gl.GL_TEXTURE_2D);
        gl.glColor3d(color.r, color.g, color.b);
        gl.glFrontFace(gl.GL_CCW);
        gl.glPolygonMode(gl.GL_FRONT_AND_BACK, gl.GL_FILL);

        gl.glBegin(gl.GL_QUADS);
            gl.glVertex2f(1, 1);
            gl.glVertex2f(-1, 1);
            gl.glVertex2f(-1, -1);
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
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
