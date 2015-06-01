//===========================================================================

package vsdk.toolkit.render.jogl;

// JOGL classes
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2ES1;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLOffscreenAutoDrawable;
import com.jogamp.opengl.GLDrawableFactory;

import vsdk.toolkit.media.RGBImage;

public class JoglStereoStrategyPBufferRenderer extends JoglStereoStrategyRenderer
{
    private GLOffscreenAutoDrawable leftPbuffer;
    private GLOffscreenAutoDrawable rightPbuffer;
    private RGBImage leftImage;
    private RGBImage rightImage;

    public JoglStereoStrategyPBufferRenderer(GLEventListener leftRenderer, GLEventListener rightRenderer, RGBImage leftImage, RGBImage rightImage, int imageWidth, int imageHeight)
    {
        super();

        this.leftImage = leftImage;
        this.rightImage = rightImage;

        GLProfile profile = GLProfile.get(GLProfile.GL2);
        GLCapabilities pbCaps = new GLCapabilities(profile);
        pbCaps.setDoubleBuffered(false);

        try {
            GLDrawableFactory creator = GLDrawableFactory.getFactory(profile);
            leftPbuffer = creator.createOffscreenAutoDrawable(
                null, pbCaps, null, imageWidth, imageHeight);
            rightPbuffer = creator.createOffscreenAutoDrawable(
                null, pbCaps, null, imageWidth, imageHeight);
          }
          catch ( Exception e ) {
              System.err.println("Error creating OpenGL Pbuffer. This program requires a 3D accelerator card.");
              System.exit(1);
        }
        leftPbuffer.addGLEventListener(leftRenderer);
        rightPbuffer.addGLEventListener(rightRenderer);
    }

    @Override
    public boolean configureDefaultLeftChannel(GL2 gl)
    {
        leftPbuffer.display();
        return false;
    }

    @Override
    public boolean configureDefaultRightChannel(GL2 gl)
    {
        rightPbuffer.display();
        return false;
    }

    @Override
    public void activateStereoMode(GL2 gl)
    {
    }

    private void drawTexturedQuad(GL2 gl, double minx, double miny, double maxx, double maxy)
    {
        gl.glPolygonMode(GL.GL_FRONT, GL2GL3.GL_FILL);
        gl.glPolygonMode(GL.GL_BACK, GL2GL3.GL_LINE);
        gl.glShadeModel(GL2.GL_FLAT);
        gl.glEnable(GL.GL_TEXTURE_2D);

        gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
        gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
        gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP);
        gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP);
        gl.glTexEnvf(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2ES1.GL_DECAL);

        gl.glColor3d(1, 1, 1);
        gl.glNormal3d(0, 0, 1);
        gl.glBegin(GL2.GL_QUADS);
            gl.glTexCoord2d(0, 0);
            gl.glVertex3d(minx, miny, 0);

            gl.glTexCoord2d(1, 0);
            gl.glVertex3d(maxx, miny, 0);

            gl.glTexCoord2d(1, 1);
            gl.glVertex3d(maxx, maxy, 0);

            gl.glTexCoord2d(0, 1);
            gl.glVertex3d(minx, maxy, 0);
        gl.glEnd();
        gl.glDisable(GL.GL_TEXTURE_2D);
    }

    private void drawResultSideBySide(GL2 gl, RGBImage a, RGBImage b)
    {
        JoglRGBImageRenderer.activate(gl, a);
        drawTexturedQuad(gl, -1, -1, 0, 1);

        JoglRGBImageRenderer.activate(gl, b);
        drawTexturedQuad(gl, 0, -1, 1, 1);
    }

    @Override
    public void deactivateStereoMode(GL2 gl)
    {
        RGBImage a = leftImage;
        RGBImage b = rightImage;

        if ( swapChannels  ) {
            a = rightImage;
            b = leftImage;
        }
        drawResultSideBySide(gl, a, b);
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
