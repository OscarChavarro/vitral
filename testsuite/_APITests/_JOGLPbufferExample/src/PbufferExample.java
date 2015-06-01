//===========================================================================

// Java basic classes
import java.io.File;

// JOGL classes
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLOffscreenAutoDrawable;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLException;

// VSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.render.jogl.JoglRGBImageRenderer;

/**
This application example is used to test JOGL's / OpenGL's Pbuffer offline
rendering capability. Note that on JOGL library history, several different
ways of performing this have been used. As of october 2013, GLPbuffer class
have been deprecated. Current code contains comments to compare previous
implementation with current one (GLOffscreenAutoDrawable).
*/
public class PbufferExample implements GLEventListener {
    private static final int imageWidth = 320;
    private static final int imageHeight = 240;
    //private GLPbuffer  pbuffer;
    private GLOffscreenAutoDrawable pbuffer;
    private RGBImage  image;

    public PbufferExample() {
        createElements();
    }

    private void createElements() throws GLException 
    {
        GLProfile profile;
        
        profile = GLProfile.get(GLProfile.GL2);
        // Create a GLCapabilities object for the pbuffer
        GLCapabilities pbCaps = new GLCapabilities(profile);
        pbCaps.setDoubleBuffered(false);
        try {
            //pbuffer = GLDrawableFactory.getFactory(profile).
            //    createGLPbuffer(null, pbCaps, null, 
            //    imageWidth, imageHeight, null);
            GLDrawableFactory creator = GLDrawableFactory.getFactory(profile);
            pbuffer = creator.createOffscreenAutoDrawable(
                null, pbCaps, null, imageWidth, imageHeight);
        }
        catch ( Exception e ) {
            VSDK.reportMessageWithException(
                this, 
                VSDK.FATAL_ERROR, 
                "PbufferExample.createElements", 
                "Error creating OpenGL Pbuffer. This program requires a 3D "
                    + "accelerator card.", 
                e);
            System.exit(1);
        }
        pbuffer.addGLEventListener(this);
        pbuffer.display();
    }

    @Override
    public void display(GLAutoDrawable drawable) 
    {
        GL2 gl = drawable.getGL().getGL2();

        gl.glClearColor(0, 0, 0, 1);
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT);
        gl.glColor3d(1, 1, 1); 

        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();

        gl.glBegin(GL2.GL_LINES);
            gl.glVertex3d(0, 0, 0);
            gl.glVertex3d(0.5, 0.5, 0);
        gl.glEnd();
        gl.glFlush();
        
        image=JoglRGBImageRenderer.getImageJOGL(gl);
        ImagePersistence.exportJPG(new File("./output.jpg"), image);
    }

    @Override
    public void init( GLAutoDrawable drawable ) 
    {
    }
  
    /** Not used method, but needed to instanciate GLEventListener
     * @param drawable */
    @Override
    public void dispose(GLAutoDrawable drawable) 
    {
        
    }

    @Override
    public void reshape( 
        GLAutoDrawable drawable, int x, int y, int width, int height ) 
    {
        GL2 gl = drawable.getGL().getGL2();
        gl.glViewport(0, 0, width, height);
    }
  
    public static void main( String[] args ) 
    {
        PbufferExample demo = new PbufferExample();
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
