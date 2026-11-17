package vsdk.toolkit.render.jogl.volume;

import java.util.Arrays;
import java.util.List;

import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.volume.BlobComponent;
import vsdk.toolkit.environment.geometry.volume.BlobSolid;
import vsdk.toolkit.environment.geometry.volume.ImplicitSolid;
import vsdk.toolkit.environment.geometry.volume.PolynomialSurface;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.render.jogl.Jogl4GeometryRenderer;
import vsdk.toolkit.render.jogl.Jogl4MeshRenderer;
import vsdk.toolkit.render.jogl.Jogl4Renderer;

/**
Renders an `ImplicitSolid` (i.e. a `BlobSolid` or a `PolynomialSurface`)
with the GL4 pipeline, from the `SolidBoundaryTessellator` mesh of its bounds
(see `_Jogl4SolidBoundaryMeshBuilder`), cached by `Jogl4GeometryRenderer`.
Blobs and polynomial surfaces are identified by their parameters (so editing
them produces a new mesh), other implicit solids by identity.
*/
public class Jogl4ImplicitSolidRenderer extends Jogl4Renderer {
    private Jogl4ImplicitSolidRenderer() {
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
        ImplicitSolid solid,
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
    @param solid an implicit solid
    @return a string that identifies the mesh of the solid
    */
    public static String meshKey(ImplicitSolid solid)
    {
        if ( solid instanceof BlobSolid b ) {
            StringBuilder key = new StringBuilder("blob/");
            key.append(b.getThreshold());
            for ( long i = 0; i < b.getComponentCount(); i++ ) {
                BlobComponent c = b.getComponent(i);
                key.append('/').append(keyOf(c.getCenter())).append('/')
                    .append(c.getRadius()).append('/').append(c.getStrength());
            }
            return key.append('/').append(_Jogl4SolidBoundaryMeshBuilder.RESOLUTION)
                .toString();
        }
        if ( solid instanceof PolynomialSurface p ) {
            return "polynomial/" + p.getOrder() + "/" +
                Arrays.toString(p.getCoefficients()) + "/" +
                _Jogl4SolidBoundaryMeshBuilder.RESOLUTION;
        }
        return "tessellatedsolid/" + System.identityHashCode(solid) + "/" +
            _Jogl4SolidBoundaryMeshBuilder.RESOLUTION;
    }

    /**
    @param solid solid to tessellate
    @return the mesh of its boundary, or null if it can not be tessellated
    */
    public static Jogl4MeshRenderer.Mesh buildMesh(ImplicitSolid solid)
    {
        return _Jogl4SolidBoundaryMeshBuilder.buildMesh(solid);
    }

    /** Exact text of the coordinates, as `Vector3Dd.toString` rounds them */
    private static String keyOf(Vector3Dd v)
    {
        return v.x() + "," + v.y() + "," + v.z();
    }
}
