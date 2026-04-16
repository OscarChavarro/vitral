package application.render.jogl;

// JOGL classes
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLOffscreenAutoDrawable;
import com.jogamp.opengl.GLDrawableFactory;

// VSDK classes
import vsdk.toolkit.common.VSDK;

public class JoglOfflineRenderer implements GLEventListener {
    private GLOffscreenAutoDrawable pbuffer;
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
            pbuffer = GLDrawableFactory.getFactory(profile)
                .createOffscreenAutoDrawable(null, pbCaps, null, imageWidth, imageHeight);
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
