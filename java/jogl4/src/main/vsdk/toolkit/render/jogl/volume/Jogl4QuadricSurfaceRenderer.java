package vsdk.toolkit.render.jogl.volume;

import java.util.List;

import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.volume.QuadricSurface;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.render.jogl.Jogl4GeometryRenderer;
import vsdk.toolkit.render.jogl.Jogl4MeshRenderer;
import vsdk.toolkit.render.jogl.Jogl4Renderer;

/**
Renders a `QuadricSurface` with the GL4 pipeline, from the
`SolidBoundaryTessellator` mesh of its bounds (see
`_Jogl4SolidBoundaryMeshBuilder`), cached by `Jogl4GeometryRenderer`,
identified by the coefficients of the quadric.
*/
public class Jogl4QuadricSurfaceRenderer extends Jogl4Renderer {
    private Jogl4QuadricSurfaceRenderer() {
    }

    /**
    Draws the quadric, with the passes selected by the configuration (see
    `Jogl4MeshRenderer`).

    @param gl OpenGL context
    @param quadric quadric to draw
    @param camera camera that views the quadric
    @param lights lights of the scene, or null or empty to use a light at the camera
    @param material material of the quadric
    @param quality bits of rendering configuration
    @param textureMap texture, or null
    @param normalMap normal (bump) map, or null
    @param localTransform transformation from quadric space to world space
    */
    public static void draw(
        GL4 gl,
        QuadricSurface quadric,
        Camera camera,
        List<Light> lights,
        SimpleMaterial material,
        RendererConfiguration quality,
        RGBImageUncompressed textureMap,
        RGBImageUncompressed normalMap,
        Matrix4x4d localTransform)
    {
        Jogl4GeometryRenderer.draw(gl, quadric, camera, lights, material, quality,
            textureMap, normalMap, localTransform);
    }

    /**
    @param quadric a quadric
    @return a string that identifies the mesh of the quadric
    */
    public static String meshKey(QuadricSurface quadric)
    {
        return "quadric/" + keyOf(quadric.squareCoefficients()) + "/" +
            keyOf(quadric.mixedCoefficients()) + "/" +
            keyOf(quadric.linearCoefficients()) + "/" +
            quadric.constantCoefficient() + "/" +
            _Jogl4SolidBoundaryMeshBuilder.RESOLUTION;
    }

    /**
    @param quadric quadric to tessellate
    @return the mesh of its boundary, or null if it can not be tessellated
    */
    public static Jogl4MeshRenderer.Mesh buildMesh(QuadricSurface quadric)
    {
        return _Jogl4SolidBoundaryMeshBuilder.buildMesh(quadric);
    }

    /** Exact text of the coordinates, as `Vector3Dd.toString` rounds them */
    private static String keyOf(Vector3Dd v)
    {
        return v.x() + "," + v.y() + "," + v.z();
    }
}
