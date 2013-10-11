//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - November 28 2005 - Oscar Chavarro: Original base version              =
//===========================================================================

package vsdk.toolkit.render.jogl;

// JOGL classes
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2GL3;

import vsdk.toolkit.media.RGBAImage;
import vsdk.toolkit.environment.CubemapBackground;

public class JoglCubemapBackgroundRenderer extends JoglRenderer 
{
    private static void setTextureParameters(GL2 gl)
    {
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
    }

    public static void draw(GL2 gl, CubemapBackground background)
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
        RGBAImage images[] = background.getImages();

        JoglCameraRenderer.activateCenter(gl, background.getCamera());
        
        gl.glColor3d(1, 1, 1);

        gl.glEnable(GL.GL_TEXTURE_2D);

        // Front
        JoglRGBAImageRenderer.activate(gl, images[0]);

        setTextureParameters(gl);

        gl.glLoadIdentity();
        gl.glBegin(GL2.GL_QUADS);
            gl.glNormal3d(0, -1, 0);
            gl.glTexCoord2f(1, 1);
            gl.glVertex3d( 1,  1,  1);
            gl.glTexCoord2f(0, 1);
            gl.glVertex3d(-1,  1,  1);
            gl.glTexCoord2f(0, 0);
            gl.glVertex3d(-1,  1, -1);
            gl.glTexCoord2f(1, 0);
            gl.glVertex3d( 1,  1, -1);
        gl.glEnd();

        // Right
        JoglRGBAImageRenderer.activate(gl, images[1]);

        setTextureParameters(gl);

        gl.glLoadIdentity();
        gl.glRotated(-90, 0, 0, 1);
        gl.glBegin(GL2.GL_QUADS);
            gl.glNormal3d(0, -1, 0);
            gl.glTexCoord2f(1, 1);
            gl.glVertex3d( 1,  1,  1);
            gl.glTexCoord2f(0, 1);
            gl.glVertex3d(-1,  1,  1);
            gl.glTexCoord2f(0, 0);
            gl.glVertex3d(-1,  1, -1);
            gl.glTexCoord2f(1, 0);
            gl.glVertex3d( 1,  1, -1);
        gl.glEnd();

        // Left
        JoglRGBAImageRenderer.activate(gl, images[3]);

        setTextureParameters(gl);

        gl.glLoadIdentity();
        gl.glRotated(90, 0, 0, 1);
        gl.glBegin(GL2.GL_QUADS);
            gl.glNormal3d(0, -1, 0);
            gl.glTexCoord2f(1, 1);
            gl.glVertex3d( 1,  1,  1);
            gl.glTexCoord2f(0, 1);
            gl.glVertex3d(-1,  1,  1);
            gl.glTexCoord2f(0, 0);
            gl.glVertex3d(-1,  1, -1);
            gl.glTexCoord2f(1, 0);
            gl.glVertex3d( 1,  1, -1);
        gl.glEnd();

        // Back
        JoglRGBAImageRenderer.activate(gl, images[2]);

        setTextureParameters(gl);

        gl.glLoadIdentity();
        gl.glRotated(180, 0, 0, 1);
        gl.glBegin(GL2.GL_QUADS);
            gl.glNormal3d(0, -1, 0);
            gl.glTexCoord2f(1, 1);
            gl.glVertex3d( 1,  1,  1);
            gl.glTexCoord2f(0, 1);
            gl.glVertex3d(-1,  1,  1);
            gl.glTexCoord2f(0, 0);
            gl.glVertex3d(-1,  1, -1);
            gl.glTexCoord2f(1, 0);
            gl.glVertex3d( 1,  1, -1);
        gl.glEnd();

        // Down
        JoglRGBAImageRenderer.activate(gl, images[4]);

        setTextureParameters(gl);

        gl.glLoadIdentity();
        gl.glRotated(-90, 1, 0, 0);
        gl.glBegin(GL2.GL_QUADS);
            gl.glNormal3d(0, -1, 0);
            gl.glTexCoord2f(1, 1);
            gl.glVertex3d( 1,  1,  1);
            gl.glTexCoord2f(0, 1);
            gl.glVertex3d(-1,  1,  1);
            gl.glTexCoord2f(0, 0);
            gl.glVertex3d(-1,  1, -1);
            gl.glTexCoord2f(1, 0);
            gl.glVertex3d( 1,  1, -1);
        gl.glEnd();

        // Up
        JoglRGBAImageRenderer.activate(gl, images[5]);

        setTextureParameters(gl);

        gl.glLoadIdentity();
        gl.glRotated(90, 1, 0, 0);
        gl.glBegin(GL2.GL_QUADS);
            gl.glNormal3d(0, -1, 0);
            gl.glTexCoord2f(1, 1);
            gl.glVertex3d( 1,  1,  1);
            gl.glTexCoord2f(0, 1);
            gl.glVertex3d(-1,  1,  1);
            gl.glTexCoord2f(0, 0);
            gl.glVertex3d(-1,  1, -1);
            gl.glTexCoord2f(1, 0);
            gl.glVertex3d( 1,  1, -1);
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
