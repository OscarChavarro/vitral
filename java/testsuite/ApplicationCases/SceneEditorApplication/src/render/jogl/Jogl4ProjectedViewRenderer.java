package render.jogl;

// JOGL classes
import com.jogamp.opengl.GL4;

// VSDK Classes
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.scene.SimpleBodyGroup;
import vsdk.toolkit.media.ZBuffer;
import vsdk.toolkit.render.jogl.Jogl4FrameBufferReader;

// Application classes
import render.ProjectedViewRenderer;

/**
Renders with OpenGL 4 the projected views used by `ProjectedViewsDebugger`.
*/
public class Jogl4ProjectedViewRenderer implements ProjectedViewRenderer
{
    private final GL4 gl;

    /**
    @param gl context where views are rendered
    */
    public Jogl4ProjectedViewRenderer(GL4 gl)
    {
        this.gl = gl;
    }

    @Override
    public ZBuffer renderDepth(SimpleBodyGroup bodies, Camera camera,
                               RendererConfiguration quality, int xSize, int ySize)
    {
        gl.glViewport(0, 0, xSize, ySize);

        //-----------------------------------------------------------------
        gl.glClearColor(0.5f, 0.5f, 0.9f, 1);
        gl.glClear(GL4.GL_COLOR_BUFFER_BIT | GL4.GL_DEPTH_BUFFER_BIT);
        gl.glEnable(GL4.GL_DEPTH_TEST);
        gl.glDepthMask(true);

        if ( bodies != null ) {
            Jogl4SceneRenderer.drawBodyGroup(gl, bodies, camera, null, quality);
        }

        gl.glFlush();

        //- Obtain ZBuffer ------------------------------------------------
        return Jogl4FrameBufferReader.readDepth(gl);
    }
}
