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
import javax.media.opengl.GL2;
import javax.media.opengl.GLProfile;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLPbuffer;
import javax.media.opengl.GLDrawableFactory;

// Vitral classes
import vsdk.framework.Component;

public class JoglShapeMatchingOfflineRenderer extends Component implements GLEventListener {
    private GLPbuffer  pbuffer;
    private boolean ready;
    private boolean pbufferSupported;
    private JoglShapeMatchingOfflineRenderable target;

    public JoglShapeMatchingOfflineRenderer(int imageWidth, int imageHeight, JoglShapeMatchingOfflineRenderable target) {

        this.target = target;
        ready = false;

        // Create a GLCapabilities object for the pbuffer
        GLCapabilities pbCaps = new GLCapabilities(GLProfile.get(GLProfile.GL3));
        GLDrawableFactory drawableFactory;

        pbCaps.setDoubleBuffered(false);
        drawableFactory = GLDrawableFactory.getFactory(GLProfile.get(GLProfile.GL3));

        if ( !drawableFactory.canCreateGLPbuffer(null) ) {
              pbufferSupported = false;
              return;
        }

        try {
            pbuffer = drawableFactory.createGLPbuffer(null, pbCaps, null, imageWidth, imageHeight, null);
            pbufferSupported = true;
            pbuffer.addGLEventListener(this);
          }
          catch ( Exception e ) {
              System.err.println("Error creating OpenGL Pbuffer. This program requires a 3D accelerator card.");
              pbuffer = null;
              pbufferSupported = false;
              return;
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
                ;
            }
        }
    }

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

    public void init( GLAutoDrawable drawable ) {
    }
  
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height ) 
    {
        GL2 gl = drawable.getGL().getGL2();
        gl.glViewport(0, 0, width, height);
    }

    public void dispose(GLAutoDrawable drawable)
    {
        ;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
