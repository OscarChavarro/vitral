//===========================================================================

package application.render.jogl;

// JOGL classes
import javax.media.opengl.GL2;
import javax.media.opengl.GLProfile;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLPbuffer;
import javax.media.opengl.GLDrawableFactory;

// VSDK classes
import vsdk.toolkit.common.VSDK;

public class JoglOfflineRenderer implements GLEventListener {
    private GLPbuffer  pbuffer;
    private boolean ready;
    private JoglProjectedViewRenderer renderer;
    private boolean pbufferSupported;

    public JoglOfflineRenderer(int imageWidth, int imageHeight, JoglProjectedViewRenderer renderer) {

        this.renderer = renderer;
        ready = false;

        // Create a GLCapabilities object for the pbuffer
        GLProfile profile;
        profile = GLProfile.get(GLProfile.GL2);
        GLCapabilities pbCaps = new GLCapabilities(profile);
        pbCaps.setDoubleBuffered(false);

        /*
        if ( !GLDrawableFactory.getFactory(profile).canCreateGLPbuffer() ) {
              pbufferSupported = false;
              return;
        }
        */
        pbufferSupported = false;

        try {
            pbuffer = GLDrawableFactory.getFactory(profile).createGLPbuffer(null, pbCaps, null, imageWidth, imageHeight, null);
            pbufferSupported = true;
            pbuffer.addGLEventListener(this);
          }
          catch ( Exception e ) {
              VSDK.reportMessage(this, VSDK.WARNING, "JoglOfflineRenderer", "Error creating OpenGL Pbuffer. This program requires a 3D accelerator card." + e);
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
                ;
            }
        }
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
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

    @Override
    public void init( GLAutoDrawable drawable ) {
    }
  
    /** Not used method, but needed to instanciate GLEventListener */
    @Override
    public void dispose(GLAutoDrawable drawable) {
        ;
    }

    @Override
    public void reshape( GLAutoDrawable drawable, int x, int y, int width, int height ) 
    {
        GL2 gl = drawable.getGL().getGL2();
        gl.glViewport(0, 0, width, height);
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
