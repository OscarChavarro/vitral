package render.jogl;

import java.util.List;

import com.jogamp.opengl.GL4;

import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.render.jogl.Jogl4GeometryRenderer;

import render.RenderPrimitive;

/**
Draws technology independent `RenderPrimitive`s with OpenGL 4: the
implementation side, for this technology, of the feedback geometry described
by editors and debugging tools (see `BodyEditFeedbackProvider`).
*/
public class Jogl4RenderPrimitiveRenderer
{
    private Jogl4RenderPrimitiveRenderer()
    {
    }

    /**
    @param gl OpenGL context
    @param primitive primitive to draw
    @param camera camera that views the primitive
    @param lights lights of the scene, or null or empty for a light at the camera
    @param quality bits of rendering configuration
    */
    public static void draw(GL4 gl, RenderPrimitive primitive, Camera camera,
                            List<Light> lights, RendererConfiguration quality)
    {
        Jogl4GeometryRenderer.draw(gl, primitive.getGeometry(), camera, lights,
            primitive.getMaterial(), quality, null, null, primitive.getTransform());
    }

    /**
    @param gl OpenGL context
    @param primitives primitives to draw
    @param camera camera that views the primitives
    @param lights lights of the scene, or null or empty for a light at the camera
    @param quality bits of rendering configuration
    */
    public static void draw(GL4 gl, List<RenderPrimitive> primitives, Camera camera,
                            List<Light> lights, RendererConfiguration quality)
    {
        for ( RenderPrimitive primitive : primitives ) {
            draw(gl, primitive, camera, lights, quality);
        }
    }
}
