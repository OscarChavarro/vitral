package render.jogl;

import com.jogamp.opengl.GL4;

import model.Scene;
import model.ApplicationModel;
import render.VisualRayDebugGeometry;

/**
Draws with OpenGL 4 the visual debug ray of the application model, built by
`VisualRayDebugGeometry`.
*/
public class Jogl4VisualRayDebugRenderer
{
    private final Scene scene;
    private final VisualRayDebugGeometry rayGeometry;

    public Jogl4VisualRayDebugRenderer(ApplicationModel model)
    {
        this.scene = model.getScene();
        this.rayGeometry = new VisualRayDebugGeometry(model);
    }

    /**
    Draws the visual debug ray, if it is enabled in the application model.
    @param gl
    */
    public void draw(GL4 gl)
    {
        Jogl4RenderPrimitiveRenderer.draw(gl, rayGeometry.buildPrimitives(),
            scene.activeCamera, scene.scene.getLights(),
            rayGeometry.getRendererConfiguration());
    }
}
