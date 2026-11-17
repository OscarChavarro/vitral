package vsdk.toolkit.render.jogl.volume;

import java.util.List;

import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.volume.HeightFieldSolid;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.render.jogl.Jogl4GeometryRenderer;
import vsdk.toolkit.render.jogl.Jogl4MeshRenderer;
import vsdk.toolkit.render.jogl.Jogl4Renderer;

/**
Renders a `HeightFieldSolid` with the GL4 pipeline, from the
`SolidBoundaryTessellator` mesh of its bounds (see
`_Jogl4SolidBoundaryMeshBuilder`), cached by `Jogl4GeometryRenderer`,
identified by the height field, as they are not edited while shown.
*/
public class Jogl4HeightFieldSolidRenderer extends Jogl4Renderer {
    private Jogl4HeightFieldSolidRenderer() {
    }

    /**
    Draws the solid, with the passes selected by the configuration (see
    `Jogl4MeshRenderer`).

    @param gl OpenGL context
    @param solid solid to draw
    @param camera camera that views the solid
    @param lights lights of the scene, or null or empty to use a light at the camera
    @param material material of the solid
    @param quality bits of rendering configuration
    @param textureMap texture, or null
    @param normalMap normal (bump) map, or null
    @param localTransform transformation from solid space to world space
    */
    public static void draw(
        GL4 gl,
        HeightFieldSolid solid,
        Camera camera,
        List<Light> lights,
        SimpleMaterial material,
        RendererConfiguration quality,
        RGBImageUncompressed textureMap,
        RGBImageUncompressed normalMap,
        Matrix4x4d localTransform)
    {
        Jogl4GeometryRenderer.draw(gl, solid, camera, lights, material, quality,
            textureMap, normalMap, localTransform);
    }

    /**
    @param solid a height field
    @return a string that identifies the mesh of the height field
    */
    public static String meshKey(HeightFieldSolid solid)
    {
        return "tessellatedsolid/" + System.identityHashCode(solid) + "/" +
            _Jogl4SolidBoundaryMeshBuilder.RESOLUTION;
    }

    /**
    @param solid height field to tessellate
    @return the mesh of its boundary, or null if it can not be tessellated
    */
    public static Jogl4MeshRenderer.Mesh buildMesh(HeightFieldSolid solid)
    {
        return _Jogl4SolidBoundaryMeshBuilder.buildMesh(solid);
    }
}
