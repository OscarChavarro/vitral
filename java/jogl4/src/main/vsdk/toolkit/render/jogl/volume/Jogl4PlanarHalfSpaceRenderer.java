package vsdk.toolkit.render.jogl.volume;

import java.util.List;

import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.environment.geometry.volume.PlanarHalfSpace;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.render.jogl.Jogl4Renderer;
import vsdk.toolkit.render.jogl.surface.Jogl4InfinitePlaneRenderer;

/**
Renders a `PlanarHalfSpace` with the GL4 pipeline. The half space is
unbounded: it is shown by its boundary plane, as the square that
`Jogl4InfinitePlaneRenderer` shows for it.
*/
public class Jogl4PlanarHalfSpaceRenderer extends Jogl4Renderer {
    private Jogl4PlanarHalfSpaceRenderer() {
    }

    /**
    Draws the boundary of the half space, with the passes selected by the
    configuration (see `Jogl4MeshRenderer`).

    @param gl OpenGL context
    @param halfSpace half space to draw
    @param camera camera that views the half space
    @param lights lights of the scene, or null or empty to use a light at the camera
    @param material material of the half space
    @param quality bits of rendering configuration
    @param textureMap texture, or null
    @param normalMap normal (bump) map, or null
    @param localTransform transformation from half space space to world space
    */
    public static void draw(
        GL4 gl,
        PlanarHalfSpace halfSpace,
        Camera camera,
        List<Light> lights,
        SimpleMaterial material,
        RendererConfiguration quality,
        RGBImageUncompressed textureMap,
        RGBImageUncompressed normalMap,
        Matrix4x4d localTransform)
    {
        Jogl4InfinitePlaneRenderer.draw(gl, boundary(halfSpace), camera, lights,
            material, quality, textureMap, normalMap, localTransform);
    }

    /**
    @param halfSpace a half space
    @return a new plane with the equation of its boundary
    */
    public static InfinitePlane boundary(PlanarHalfSpace halfSpace)
    {
        return new InfinitePlane(halfSpace.getA(), halfSpace.getB(),
            halfSpace.getC(), halfSpace.getD());
    }
}
