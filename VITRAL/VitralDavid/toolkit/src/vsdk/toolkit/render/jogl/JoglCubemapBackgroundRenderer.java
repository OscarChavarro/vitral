//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - November 28 2005 - Oscar Chavarro: Original base version              =
//===========================================================================

package vsdk.toolkit.render.jogl;

import javax.media.opengl.GL;

import vsdk.toolkit.media.RGBAImage;
import vsdk.toolkit.environment.CubemapBackground;
import vsdk.toolkit.render.jogl.JoglCameraRenderer;

public class JoglCubemapBackgroundRenderer
{
    public static void draw(GL gl, CubemapBackground background)
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
        gl.glClear(gl.GL_DEPTH_BUFFER_BIT);
        gl.glShadeModel(gl.GL_FLAT);
        gl.glDisable(gl.GL_BLEND);
        gl.glDisable(gl.GL_LIGHTING);
        gl.glDisable(gl.GL_DEPTH_TEST);
        gl.glDisable(gl.GL_TEXTURE_2D);
        gl.glFrontFace(gl.GL_CCW);
        gl.glPolygonMode(gl.GL_FRONT_AND_BACK, gl.GL_FILL);

        // Not needed when image background is implemented ...
        gl.glColor3d(0, 0, 0);
        gl.glBegin(gl.GL_QUADS);
            gl.glVertex2f(1, 1);
            gl.glVertex2f(-1, 1);
            gl.glVertex2f(-1, -1);
            gl.glVertex2f(1, -1);
        gl.glEnd();

        //- Put image background ------------------------------------------
        RGBAImage images[] = background.getImages();

        JoglCameraRenderer.activateGLCenter(gl, background.getCamera());
    
        gl.glColor3d(1, 1, 1);

        gl.glEnable(gl.GL_TEXTURE_2D);


        // Front
        JoglRGBAImageRenderer.activateGL(gl, images[0]);
        gl.glTexParameteri(gl.GL_TEXTURE_2D, 
                           gl.GL_TEXTURE_MAG_FILTER, gl.GL_NEAREST);
        gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_MIN_FILTER, 
                           gl.GL_NEAREST);
        gl.glLoadIdentity();
        gl.glBegin(gl.GL_QUADS);
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
        JoglRGBAImageRenderer.activateGL(gl, images[1]);
        gl.glTexParameteri(gl.GL_TEXTURE_2D, 
                           gl.GL_TEXTURE_MAG_FILTER, gl.GL_NEAREST);
        gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_MIN_FILTER, 
                           gl.GL_NEAREST);
        gl.glLoadIdentity();
        gl.glRotated(-90, 0, 0, 1);
        gl.glBegin(gl.GL_QUADS);
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
        JoglRGBAImageRenderer.activateGL(gl, images[2]);
        gl.glTexParameteri(gl.GL_TEXTURE_2D, 
                           gl.GL_TEXTURE_MAG_FILTER, gl.GL_NEAREST);
        gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_MIN_FILTER, 
                           gl.GL_NEAREST);
        gl.glLoadIdentity();
        gl.glRotated(180, 0, 0, 1);
        gl.glBegin(gl.GL_QUADS);
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
        JoglRGBAImageRenderer.activateGL(gl, images[3]);
        gl.glTexParameteri(gl.GL_TEXTURE_2D, 
                           gl.GL_TEXTURE_MAG_FILTER, gl.GL_NEAREST);
        gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_MIN_FILTER, 
                           gl.GL_NEAREST);
        gl.glLoadIdentity();
        gl.glRotated(90, 0, 0, 1);
        gl.glBegin(gl.GL_QUADS);
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
        JoglRGBAImageRenderer.activateGL(gl, images[4]);
        gl.glTexParameteri(gl.GL_TEXTURE_2D, 
                           gl.GL_TEXTURE_MAG_FILTER, gl.GL_NEAREST);
        gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_MIN_FILTER, 
                           gl.GL_NEAREST);
        gl.glLoadIdentity();
        gl.glRotated(-90, 1, 0, 0);
        gl.glBegin(gl.GL_QUADS);
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
        JoglRGBAImageRenderer.activateGL(gl, images[5]);
        gl.glTexParameteri(gl.GL_TEXTURE_2D, 
                           gl.GL_TEXTURE_MAG_FILTER, gl.GL_NEAREST);
        gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_MIN_FILTER, 
                           gl.GL_NEAREST);
        gl.glLoadIdentity();
        gl.glRotated(90, 1, 0, 0);
        gl.glBegin(gl.GL_QUADS);
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
