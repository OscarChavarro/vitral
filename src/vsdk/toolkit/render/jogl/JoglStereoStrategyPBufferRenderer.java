//===========================================================================

package vsdk.toolkit.render.jogl;

import javax.media.opengl.GL2;
import javax.media.opengl.GLProfile;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLPbuffer;
import javax.media.opengl.GLDrawableFactory;

import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.render.jogl.JoglRGBImageRenderer;

public class JoglStereoStrategyPBufferRenderer extends JoglStereoStrategyRenderer
{
    private GLPbuffer leftPbuffer;
    private GLPbuffer rightPbuffer;
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
            leftPbuffer = GLDrawableFactory.getFactory(profile).createGLPbuffer(null, pbCaps, null, imageWidth, imageHeight, null);
            rightPbuffer = GLDrawableFactory.getFactory(profile).createGLPbuffer(null, pbCaps, null, imageWidth, imageHeight, null);
          }
          catch ( Exception e ) {
              System.err.println("Error creating OpenGL Pbuffer. This program requires a 3D accelerator card.");
              System.exit(1);
        }
        leftPbuffer.addGLEventListener(leftRenderer);
        rightPbuffer.addGLEventListener(rightRenderer);
    }

    public boolean configureDefaultLeftChannel(GL2 gl)
    {
        leftPbuffer.display();
        return false;
    }

    public boolean configureDefaultRightChannel(GL2 gl)
    {
        rightPbuffer.display();
        return false;
    }

    public void activateStereoMode(GL2 gl)
    {
        ;
    }

    private void drawTexturedQuad(GL2 gl, double minx, double miny, double maxx, double maxy)
    {
        gl.glPolygonMode(gl.GL_FRONT, gl.GL_FILL);
        gl.glPolygonMode(gl.GL_BACK, gl.GL_LINE);
        gl.glShadeModel(gl.GL_FLAT);
        gl.glEnable(gl.GL_TEXTURE_2D);

        gl.glTexParameterf(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_MAG_FILTER, gl.GL_LINEAR);
        gl.glTexParameterf(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_MIN_FILTER, gl.GL_LINEAR);
        gl.glTexParameterf(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_WRAP_S, gl.GL_CLAMP);
        gl.glTexParameterf(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_WRAP_T, gl.GL_CLAMP);
        gl.glTexEnvf(gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_MODE, gl.GL_DECAL);

        gl.glColor3d(1, 1, 1);
        gl.glNormal3d(0, 0, 1);
        gl.glBegin(gl.GL_QUADS);
            gl.glTexCoord2d(0, 0);
            gl.glVertex3d(minx, miny, 0);

            gl.glTexCoord2d(1, 0);
            gl.glVertex3d(maxx, miny, 0);

            gl.glTexCoord2d(1, 1);
            gl.glVertex3d(maxx, maxy, 0);

            gl.glTexCoord2d(0, 1);
            gl.glVertex3d(minx, maxy, 0);
        gl.glEnd();
        gl.glDisable(gl.GL_TEXTURE_2D);
    }

    private void drawResultSideBySide(GL2 gl, RGBImage a, RGBImage b)
    {
        JoglRGBImageRenderer.activate(gl, a);
        drawTexturedQuad(gl, -1, -1, 0, 1);

        JoglRGBImageRenderer.activate(gl, b);
        drawTexturedQuad(gl, 0, -1, 1, 1);
    }

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
