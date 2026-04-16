package vsdk.toolkit.render.jogl;

// JOGL classes
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.GLCapabilities;

public class JoglStereoStrategyInterlaceRenderer extends JoglStereoStrategyRenderer
{
    private boolean stencilReady;
    private int viewportXSize;
    private int viewportYSize;

    @Override
    public void requestCapabilities(GLCapabilities caps)
    {
        caps.setStencilBits(8);
    }

    private void
    createHorizontalInterlaceStencil(GL2 gl,
        int viewportXSize, int viewportYSize)
    {
        int i;
        GLU glu = new GLU();

        /* setring screen-corresponding geometry */
        gl.glViewport(0, 0, viewportXSize, viewportYSize);

        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluOrtho2D(0.0, viewportXSize-1, 0.0, viewportYSize - 1);

        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
                
        /* clearing and configuring stencil drawing */
        gl.glDrawBuffer(GL.GL_BACK);
        gl.glEnable(GL.GL_STENCIL_TEST);

        gl.glClearStencil(0);
        gl.glClear(GL.GL_STENCIL_BUFFER_BIT);

        /* Colorbuffer is copied to stencil */
        gl.glStencilOp(GL.GL_REPLACE, GL.GL_REPLACE, GL.GL_REPLACE);
        gl.glDisable(GL.GL_DEPTH_TEST);

        /* to avoid interaction with stencil content */
        gl.glStencilFunc(GL.GL_ALWAYS, 1, 1);
        
        /* drawing stencil pattern */
        gl.glColor4f(1,1,1,0);  /* alfa is 0 not to interfere with alpha tests */
        gl.glLineWidth(1.0f);

        gl.glBegin(GL.GL_LINES);
        for ( i = 0; i < viewportYSize; i += 2 ) {
            gl.glVertex2f(0, i);
            gl.glVertex2f(viewportXSize, i);
        }
        gl.glEnd();     

        /* Disabling changes in stencil buffer */
        gl.glStencilOp(GL.GL_KEEP, GL.GL_KEEP, GL.GL_KEEP);
        gl.glDisable(GL.GL_STENCIL_TEST);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT); // Lines are not visible, they
                                            // are just on the stencil buffer
        gl.glFlush();
    }

    public JoglStereoStrategyInterlaceRenderer()
    {
        super();

        stencilReady = false;
        viewportXSize = -1;
        viewportYSize = -1;
    }

    @Override
    public boolean configureDefaultLeftChannel(GL2 gl)
    {
        /* following comand replace glDrawBuffer(GL_BACK_LEFT); */
        if ( swapChannels  ) {
            gl.glStencilFunc(GL.GL_EQUAL, 1, 1); /* draws if stencil != 1 */
        }
        else {
            gl.glStencilFunc(GL.GL_NOTEQUAL, 1, 1); /* draws if stencil != 1 */
        }
        return true;
    }

    @Override
    public boolean configureDefaultRightChannel(GL2 gl)
    {
        /* following comand replace glDrawBuffer(GL_BACK_RIGHT); */
        if ( swapChannels  ) {
            gl.glStencilFunc(GL.GL_NOTEQUAL, 1, 1); /* draws if stencil == 1 */
        }
        else {
            gl.glStencilFunc(GL.GL_EQUAL, 1, 1); /* draws if stencil == 1 */
        }
        return true;
    }

    @Override
    public void activateStereoMode(GL2 gl)
    {
        /* Generating the pattern in stencil buffer */
        int viewport[] = new int[4];
        gl.glGetIntegerv(GL.GL_VIEWPORT, viewport, 0);
        if ( viewportXSize != viewport[2] ||
             viewportYSize != viewport[3] ) {
            stencilReady = false;
        }

        if ( !stencilReady ) {
            viewportXSize = viewport[2];
            viewportYSize = viewport[3];
            createHorizontalInterlaceStencil(gl, viewportXSize, viewportYSize);

            stencilReady = true;
        }

        gl.glEnable(GL.GL_STENCIL_TEST);
    }

    @Override
    public void deactivateStereoMode(GL2 gl)
    {
        gl.glDisable(GL.GL_STENCIL_TEST);
    }

}
