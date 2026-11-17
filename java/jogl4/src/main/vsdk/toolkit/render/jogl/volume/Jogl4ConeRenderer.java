package vsdk.toolkit.render.jogl.volume;

import java.util.List;

import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.volume.Cone;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.render.jogl.Jogl4GeometryRenderer;
import vsdk.toolkit.render.jogl.Jogl4MeshBuilder;
import vsdk.toolkit.render.jogl.Jogl4MeshRenderer;
import vsdk.toolkit.render.jogl.Jogl4Renderer;

/**
Renders a `Cone` (also cylinders and truncated cones) with the GL4
pipeline: it is tessellated by `buildMesh` into its side and its caps, and
the mesh is cached by `Jogl4GeometryRenderer`, identified by the parameters
of the cone.
*/
public class Jogl4ConeRenderer extends Jogl4Renderer {
    /// Divisions around the axis
    public static final int SLICES = 32;

    private Jogl4ConeRenderer() {
    }

    /**
    Draws the cone, with the passes selected by the configuration (see
    `Jogl4MeshRenderer`).

    @param gl OpenGL context
    @param cone cone to draw
    @param camera camera that views the cone
    @param lights lights of the scene, or null or empty to use a light at the camera
    @param material material of the cone
    @param quality bits of rendering configuration
    @param textureMap texture, or null
    @param normalMap normal (bump) map, or null
    @param localTransform transformation from cone space to world space
    */
    public static void draw(
        GL4 gl,
        Cone cone,
        Camera camera,
        List<Light> lights,
        SimpleMaterial material,
        RendererConfiguration quality,
        RGBImageUncompressed textureMap,
        RGBImageUncompressed normalMap,
        Matrix4x4d localTransform)
    {
        Jogl4GeometryRenderer.draw(gl, cone, camera, lights, material, quality,
            textureMap, normalMap, localTransform);
    }

    /**
    @param cone a cone
    @return a string that identifies the mesh of the cone
    */
    public static String meshKey(Cone cone)
    {
        return "cone/" + cone.getBottomRadius() + "/" + cone.getTopRadius() +
            "/" + cone.getHeight();
    }

    /**
    @param cone cone to tessellate
    @return the mesh of the side and the caps of the cone
    */
    public static Jogl4MeshRenderer.Mesh buildMesh(Cone cone)
    {
        double r1 = cone.getBottomRadius();
        double r2 = cone.getTopRadius();
        double h = cone.getHeight();
        Jogl4MeshBuilder builder = new Jogl4MeshBuilder(Math.max(Math.max(r1, r2), h));

        builder.addFrustum(0, r1, h, r2, SLICES);
        builder.addDisk(0, 0, r1, false, SLICES);
        if ( r2 > 0.0 ) {
            builder.addDisk(h, 0, r2, true, SLICES);
        }
        return builder.build();
    }
}
