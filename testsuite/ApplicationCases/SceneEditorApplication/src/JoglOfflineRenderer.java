//===========================================================================
//=-------------------------------------------------------------------------=
//= References:                                                             =
//= [FUNK2003], Funkhouser, Thomas.  Min, Patrick. Kazhdan, Michael. Chen,  =
//=     Joyce. Halderman, Alex. Dobkin, David. Jacobs, David. "A Search     =
//=     Engine for 3D Models", ACM Transactions on Graphics, Vol 22. No1.   =
//=     January 2003. Pp. 83-105                                            =
//===========================================================================

// JOGL classes
import javax.media.opengl.GL;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLPbuffer;
import javax.media.opengl.GLDrawableFactory;

public class JoglOfflineRenderer implements GLEventListener {
    private GLPbuffer  pbuffer;
    private boolean ready;
    private JoglProjectedViewRenderer renderer;
    private boolean pbufferSupported;

    public JoglOfflineRenderer(int imageWidth, int imageHeight, JoglProjectedViewRenderer renderer) {

        this.renderer = renderer;
        ready = false;

        // Create a GLCapabilities object for the pbuffer
        GLCapabilities pbCaps = new GLCapabilities();
        pbCaps.setDoubleBuffered(false);

        if ( !GLDrawableFactory.getFactory().canCreateGLPbuffer() ) {
              pbufferSupported = false;
              return;
        }

        try {
            pbuffer = GLDrawableFactory.getFactory().createGLPbuffer(pbCaps, null, imageWidth, imageHeight, null);
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
        GL gl = drawable.getGL();
        renderer.draw(gl);
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
  
    /** Not used method, but needed to instanciate GLEventListener */
    public void dispose(GLAutoDrawable drawable) {
        ;
    }

    public void reshape( GLAutoDrawable drawable, int x, int y, int width, int height ) 
    {
        GL gl = drawable.getGL();
        gl.glViewport(0, 0, width, height);
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
