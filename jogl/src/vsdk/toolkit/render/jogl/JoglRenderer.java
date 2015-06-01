//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - July 25 2006 - Oscar Chavarro: Original base version                  =
//= - December 28 2006 - Oscar Chavarro: Added Nvidia Cg support            =
//===========================================================================

package vsdk.toolkit.render.jogl;

// Java base classes
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

// JOGL classes
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.GLBuffers;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.media.Image;

// VitralSDK classes
import vsdk.toolkit.render.RenderingElement;
import static vsdk.toolkit.render.jogl.JoglHudIconRenderer.activateDefaultTextureParameters;

/**
The JoglRenderer abstract class provides an interface for Jogl*Renderer
style classes. This serves two purposes:
  - To help in design level organization of Jogl renderers (this eases the
    study of the class hierarchy)
  - To provide a place to locate operations common to all Jogl renderers
    classes and Jogl renderers' private utility/supporting classes. In this
    moment, this operations include the global framework for managing
    general JOGL initialization, as such verifying correct availavility of
    native OpenGL libraries.
*/

public abstract class JoglRenderer extends RenderingElement {
    /**
    Note that this static block is automatically called for the first
    instanciation or the first static call to any of current subclasses.
    This is used to check correct JOGL environment availability, and
    report if not.
    Check why it is good to have this here disabled
    */
/*
    static {
        // What happens when this is executed from an applet?
        //verifyOpenGLAvailability();
    }
*/

    /**
    @return 
    */
    public static boolean verifyOpenGLAvailability()
    {
/*
        if ( !PersistenceElement.verifyLibrary("jogl_gl2es12") ) {
            VSDK.reportMessage(null, VSDK.FATAL_ERROR, 
                "JoglRenderer.verifyOpenGLAvailability",
                "JOGL Library not found.  Check your installation.");
            return false;
        }
*/
/*
        if ( !PersistenceElement.verifyLibrary("jogl_awt") ) {
            VSDK.reportMessage(null, VSDK.FATAL_ERROR, 
                "JoglRenderer.verifyOpenGLAvailability",
                "JOGL-AWT Library not found.  Check your installation.");
            return false;
        }
*/
/*
        if ( !PersistenceElement.verifyLibrary("jogl_cg") ) {
            VSDK.reportMessage(null, VSDK.FATAL_ERROR, 
                "JoglRenderer.verifyOpenGLAvailability",
                "JOGL-CG Library not found.  Check your installation.");
            return false;
        }
*/
        return true;
    }

    protected static FloatBuffer
    cloneDoubleArrayToFloatBuffer(double v[])
    {
        FloatBuffer buffer;
        int i;

        //buffer = BufferUtil.newFloatBuffer(v.length);
        //buffer = ByteBuffer.allocateDirect(v.length*4).asFloatBuffer();
        buffer = (FloatBuffer)GLBuffers.newDirectGLBuffer(GL.GL_FLOAT, v.length);

        for ( i = 0; i < v.length; i++ ) {
            buffer.put((float)v[i]);
        }
        buffer.rewind();

        return buffer;
    }

    protected static FloatBuffer
    cloneDoubleArrayToInvertedFloatBuffer(double v[])
    {
        FloatBuffer buffer;
        int i;

        //buffer = BufferUtil.newFloatBuffer(v.length);
        //buffer = ByteBuffer.allocateDirect(v.length*4).asFloatBuffer();
        buffer = (FloatBuffer)GLBuffers.newDirectGLBuffer(GL.GL_FLOAT, v.length);

        for ( i = 0; i < v.length; i++ ) {
            buffer.put((float)(v[i]*-1));
        }
        buffer.rewind();

        return buffer;
    }

    protected static IntBuffer
    cloneIntArrayToIntBuffer(int v[])
    {
        IntBuffer buffer;
        int i;

        //buffer = BufferUtil.newIntBuffer(v.length);
        //buffer = ByteBuffer.allocateDirect(v.length*4).asIntBuffer();
        buffer = (IntBuffer)GLBuffers.newDirectGLBuffer(GL2.GL_INT, v.length);

        for ( i = 0; i < v.length; i++ ) {
            buffer.put(v[i]);
        }
        buffer.rewind();

        return buffer;
    }
    
    public static void drawUnitSquare(GL2 gl) {
        drawUnitSquare(gl, 1.0);
    }
    
    public static void drawUnitSquare(GL2 gl, double alpha) {
        gl.glColor4d(1, 1, 1, alpha);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_TEXTURE_2D);
        gl.glBegin(GL2.GL_QUADS);
        gl.glTexCoord2d(0, 0);
        gl.glVertex2d(-0.5, -0.5);

        gl.glTexCoord2d(1, 0);
        gl.glVertex2d(0.5, -0.5);

        gl.glTexCoord2d(1, 1);
        gl.glVertex2d(0.5, 0.5);

        gl.glTexCoord2d(0, 1);
        gl.glVertex2d(-0.5, 0.5);
        gl.glEnd();

    }

    public static void drawImageOn2DWindow(GL2 gl, Image img, Camera c, int x, int y) {
        drawImageOn2DWindow(gl, img, c, x, y, 1.0);
    }
    
    public static void drawImageOn2DWindow(GL2 gl, Image img, Camera c, int x, int y, double alpha) {
        double fx, fy;
        double dx, dy;

        if ( x < 0 ) {
            x = -x;
            x = (int) c.getViewportXSize() - img.getXSize() - x;
        }
        if ( y < 0 ) {
            y = -y;
            y = (int) c.getViewportYSize() - img.getYSize() - y;
        }

        if ( img == null ) {
            return;
        }

        fx = (((double) img.getXSize()) * 2.0) / c.getViewportXSize();
        fy = (((double) img.getYSize()) * 2.0) / c.getViewportYSize();
        dx = ((double) (x) * 2.0 + 
            ((double) img.getXSize())) / c.getViewportXSize();
        dy = ((double) (y) * 2.0 + 
            ((double) img.getYSize())) / c.getViewportYSize();

        gl.glEnable(GL2.GL_TEXTURE_2D);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glTranslated(dx - 1, 1.0 - dy, 0);
        gl.glScaled(fx, fy, 1.0);
        gl.glActiveTexture(GL2.GL_TEXTURE0);
        JoglImageRenderer.activate(gl, img);
        activateDefaultTextureParameters(gl);
        drawUnitSquare(gl, alpha);
        
        gl.glPopMatrix();
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(GL2.GL_MODELVIEW);
    }

    public static void activateDefaultTextureParameters(GL2 gl) {
        gl.glTexParameteri(
            GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
        gl.glTexParameteri(
            GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
        gl.glTexParameterf(
            GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_MIRRORED_REPEAT);
        gl.glTexParameterf(
            GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_MIRRORED_REPEAT);
        //gl.glTexEnvf(gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_MODE, gl.GL_DECAL);
    }


}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
