//===========================================================================
//=-------------------------------------------------------------------------=
//= References:                                                             =
//= [FUNK2003], Funkhouser, Thomas.  Min, Patrick. Kazhdan, Michael. Chen,  =
//=     Joyce. Halderman, Alex. Dobkin, David. Jacobs, David. "A Search     =
//=     Engine for 3D Models", ACM Transactions on Graphics, Vol 22. No1.   =
//=     January 2003. Pp. 83-105                                            =
//===========================================================================

package vsdk.framework.shapeMatching;

// JOGL classes
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLOffscreenAutoDrawable;
import com.jogamp.opengl.GLDrawableFactory;

// Vitral classes
import vsdk.framework.Component;

public class JoglShapeMatchingOfflineRenderer extends Component implements GLEventListener {
    private GLOffscreenAutoDrawable pbuffer;
    private boolean ready;
    private boolean pbufferSupported;
    private JoglShapeMatchingOfflineRenderable target;

    public JoglShapeMatchingOfflineRenderer(int imageWidth, int imageHeight, JoglShapeMatchingOfflineRenderable target) {
        init(imageWidth, imageHeight, target);
    }

    private void init(int imageWidth, int imageHeight, JoglShapeMatchingOfflineRenderable target)
    {
        this.target = target;
        ready = false;

        // Create a GLCapabilities object for the pbuffer
        GLProfile profile;
        profile = GLProfile.get(GLProfile.GL2);
        GLCapabilities pbCaps = new GLCapabilities(profile);
        GLDrawableFactory drawableFactory;

        pbCaps.setDoubleBuffered(false);
        drawableFactory = GLDrawableFactory.getFactory(GLProfile.get(GLProfile.GL3));

        if ( !drawableFactory.canCreateGLPbuffer(null, profile) ) {
              pbufferSupported = false;
              return;
        }

        try {
            pbuffer = drawableFactory.createOffscreenAutoDrawable(
                null, pbCaps, null, imageWidth, imageHeight);


            pbufferSupported = true;
            pbuffer.addGLEventListener(this);
          }
          catch ( Exception e ) {
              System.err.println("Error creating OpenGL Pbuffer. This program requires a 3D accelerator card.");
              pbuffer = null;
              pbufferSupported = false;
        }
    }


    public boolean isPbufferSupported()
    {
        return pbufferSupported;
    }

    public void waitForMe()
    {
        if ( pbuffer == null ) {
            return;
        }
        while ( !ready ) {
            System.out.print("*");
            try {
                Thread.sleep(100);
            }
            catch ( Exception e ) {
            }
        }
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        target.executeRendering(gl);
        ready = true;
    }

    public void execute()
    {
        ready = false;
        if ( pbuffer == null ) return;

        pbuffer.display();
    }

    public void displayChanged(GLAutoDrawable gLDrawable, boolean modeChanged, boolean deviceChanged) {
    }

    @Override
    public void init( GLAutoDrawable drawable ) {
    }
  
    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height ) 
    {
        GL2 gl = drawable.getGL().getGL2();
        gl.glViewport(0, 0, width, height);
    }

    @Override
    public void dispose(GLAutoDrawable drawable)
    {
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
