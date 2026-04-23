package vsdk.toolkit.render.jogl;

import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.SimpleBackground;

// JOGL classes
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2GL3;

public class Jogl2SimpleBackgroundRenderer extends Jogl2Renderer 
{
    public static void draw(GL2 gl, SimpleBackground background)
    {
        Vector3D d = new Vector3D(1, 0, 0);
        ColorRgb color;

        color = background.colorInDireccion(d);

        //-----------------------------------------------------------------
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glMatrixMode(GL.GL_TEXTURE);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();

        //-----------------------------------------------------------------
        //gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
        gl.glShadeModel(GL2.GL_FLAT);
        gl.glDisable(GL.GL_BLEND);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL.GL_DEPTH_TEST);
        gl.glDisable(GL.GL_TEXTURE_2D);
        gl.glColor3d(color.r, color.g, color.b);
        gl.glFrontFace(GL.GL_CCW);
        gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2GL3.GL_FILL);

        gl.glBegin(GL2.GL_QUADS);
            gl.glVertex2f(1, 1);
            gl.glVertex2f(-1, 1);
            gl.glVertex2f(-1, -1);
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
    }
}
