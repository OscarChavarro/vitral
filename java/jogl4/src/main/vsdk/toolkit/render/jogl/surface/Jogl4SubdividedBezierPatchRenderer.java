package vsdk.toolkit.render.jogl.surface;

import java.util.List;

import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.surface.SubdividedBezierPatch;
import vsdk.toolkit.environment.geometry.surface.TriangleMesh;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.processing.BezierPatchTessellator;
import vsdk.toolkit.render.jogl.Jogl4GeometryRenderer;
import vsdk.toolkit.render.jogl.Jogl4MeshRenderer;
import vsdk.toolkit.render.jogl.Jogl4Renderer;

/**
Renders a `SubdividedBezierPatch` with the GL4 pipeline: its surface is
tessellated by `BezierPatchTessellator` into a grid of `TESSELLATION_STEPS`
x `TESSELLATION_STEPS` steps of its parameters, visible from both sides, and
the mesh is cached by `Jogl4GeometryRenderer`, identified by the patch.
*/
public class Jogl4SubdividedBezierPatchRenderer extends Jogl4Renderer {
    /// Parameter steps of the tessellated patch along u and v
    public static final int TESSELLATION_STEPS = 24;

    private Jogl4SubdividedBezierPatchRenderer() {
    }

    /**
    Draws the patch, with the passes selected by the configuration (see
    `Jogl4MeshRenderer`).

    @param gl OpenGL context
    @param patch patch to draw
    @param camera camera that views the patch
    @param lights lights of the scene, or null or empty to use a light at the camera
    @param material material of the patch
    @param quality bits of rendering configuration
    @param textureMap texture, or null
    @param normalMap normal (bump) map, or null
    @param localTransform transformation from patch space to world space
    */
    public static void draw(
        GL4 gl,
        SubdividedBezierPatch patch,
        Camera camera,
        List<Light> lights,
        SimpleMaterial material,
        RendererConfiguration quality,
        RGBImageUncompressed textureMap,
        RGBImageUncompressed normalMap,
        Matrix4x4d localTransform)
    {
        Jogl4GeometryRenderer.draw(gl, patch, camera, lights, material, quality,
            textureMap, normalMap, localTransform);
    }

    /**
    @param patch a patch
    @return a string that identifies the mesh of the patch
    */
    public static String meshKey(SubdividedBezierPatch patch)
    {
        return "subdividedbezierpatch/" + System.identityHashCode(patch) + "/" +
            TESSELLATION_STEPS;
    }

    /**
    @param patch patch to tessellate
    @return the mesh of the patch, or null if it can not be tessellated
    */
    public static Jogl4MeshRenderer.Mesh buildMesh(SubdividedBezierPatch patch)
    {
        TriangleMesh mesh = BezierPatchTessellator.tessellate(patch,
            TESSELLATION_STEPS, TESSELLATION_STEPS);
        if ( mesh == null ) {
            return null;
        }
        return Jogl4TriangleMeshRenderer.buildMesh(mesh, true);
    }
}
