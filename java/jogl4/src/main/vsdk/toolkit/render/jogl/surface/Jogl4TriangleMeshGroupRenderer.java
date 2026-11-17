package vsdk.toolkit.render.jogl.surface;

import java.util.List;

import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.surface.TriangleMesh;
import vsdk.toolkit.environment.geometry.surface.TriangleMeshGroup;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.render.jogl.Jogl4GeometryRenderer;
import vsdk.toolkit.render.jogl.Jogl4MinMaxRenderer;
import vsdk.toolkit.render.jogl.Jogl4Renderer;
import vsdk.toolkit.render.jogl.Jogl4SelectionCornersRenderer;

/**
Renders a `TriangleMeshGroup` with the GL4 pipeline. OBJ imports are
represented as a group of meshes (normally one per material). Raytracing
already traverses this geometry, so rasterizing every member here (each one
with `Jogl4TriangleMeshRenderer`) keeps the two rendering paths equivalent.
The bounding volume and the selection corners are drawn once, around the
whole group.
*/
public class Jogl4TriangleMeshGroupRenderer extends Jogl4Renderer {
    private Jogl4TriangleMeshGroupRenderer() {
    }

    /**
    Draws the meshes of the group, with the passes selected by the
    configuration (see `Jogl4MeshRenderer`).

    @param gl OpenGL context
    @param group group to draw
    @param camera camera that views the group
    @param lights lights of the scene, or null or empty to use a light at the camera
    @param material material of the group
    @param quality bits of rendering configuration
    @param textureMap texture, or null
    @param normalMap normal (bump) map, or null
    @param localTransform transformation from group space to world space
    */
    public static void draw(
        GL4 gl,
        TriangleMeshGroup group,
        Camera camera,
        List<Light> lights,
        SimpleMaterial material,
        RendererConfiguration quality,
        RGBImageUncompressed textureMap,
        RGBImageUncompressed normalMap,
        Matrix4x4d localTransform)
    {
        if ( quality == null ) {
            return;
        }
        RendererConfiguration meshQuality;
        try {
            meshQuality = quality.clone();
        }
        catch ( CloneNotSupportedException e ) {
            return;
        }
        meshQuality.setBoundingVolume(false);
        meshQuality.setSelectionCorners(false);

        for ( TriangleMesh mesh : group.getMeshes() ) {
            Jogl4GeometryRenderer.draw(gl, mesh, camera, lights, material,
                meshQuality, textureMap, normalMap, localTransform);
        }

        if ( quality.isBoundingVolumeSet() ) {
            Jogl4MinMaxRenderer.draw(gl, group, camera, localTransform);
        }
        if ( quality.isSelectionCornersSet() ) {
            Jogl4SelectionCornersRenderer.draw(gl, group, camera, localTransform);
        }
    }
}
