package vsdk.toolkit.render.jogl.volume;

import java.util.List;

import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.volume.Box;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.render.jogl.Jogl4GeometryRenderer;
import vsdk.toolkit.render.jogl.Jogl4MeshBuilder;
import vsdk.toolkit.render.jogl.Jogl4MeshRenderer;
import vsdk.toolkit.render.jogl.Jogl4Renderer;

/**
Renders a `Box` with the GL4 pipeline: it is tessellated by `buildMesh`
into its six faces, with the texture layout of `Jogl2BoxRenderer`, and the
mesh is cached by `Jogl4GeometryRenderer`, identified by the size of the
box.
*/
public class Jogl4BoxRenderer extends Jogl4Renderer {
    private Jogl4BoxRenderer() {
    }

    /**
    Draws the box, with the passes selected by the configuration (see
    `Jogl4MeshRenderer`).

    @param gl OpenGL context
    @param box box to draw
    @param camera camera that views the box
    @param lights lights of the scene, or null or empty to use a light at the camera
    @param material material of the box
    @param quality bits of rendering configuration
    @param textureMap texture, or null
    @param normalMap normal (bump) map, or null
    @param localTransform transformation from box space to world space
    */
    public static void draw(
        GL4 gl,
        Box box,
        Camera camera,
        List<Light> lights,
        SimpleMaterial material,
        RendererConfiguration quality,
        RGBImageUncompressed textureMap,
        RGBImageUncompressed normalMap,
        Matrix4x4d localTransform)
    {
        Jogl4GeometryRenderer.draw(gl, box, camera, lights, material, quality,
            textureMap, normalMap, localTransform);
    }

    /**
    @param box a box
    @return a string that identifies the mesh of the box
    */
    public static String meshKey(Box box)
    {
        Vector3Dd s = box.getSize();
        return "box/" + s.x() + "/" + s.y() + "/" + s.z();
    }

    /**
    @param box box to tessellate
    @return the mesh of the faces of the box
    */
    public static Jogl4MeshRenderer.Mesh buildMesh(Box box)
    {
        Vector3Dd size = box.getSize();
        double hx = size.x() / 2;
        double hy = size.y() / 2;
        double hz = size.z() / 2;
        Jogl4MeshBuilder builder = new Jogl4MeshBuilder(Math.max(hx, Math.max(hy, hz)) * 2);

        // Each face is given counterclockwise seen from outside
        addFace(builder, new Vector3Dd(0, 0, -1),
            new Vector3Dd(-hx, -hy, -hz), new Vector3Dd(-hx, hy, -hz),
            new Vector3Dd(hx, hy, -hz), new Vector3Dd(hx, -hy, -hz));
        addFace(builder, new Vector3Dd(0, 0, 1),
            new Vector3Dd(-hx, -hy, hz), new Vector3Dd(hx, -hy, hz),
            new Vector3Dd(hx, hy, hz), new Vector3Dd(-hx, hy, hz));
        addFace(builder, new Vector3Dd(0, -1, 0),
            new Vector3Dd(-hx, -hy, hz), new Vector3Dd(-hx, -hy, -hz),
            new Vector3Dd(hx, -hy, -hz), new Vector3Dd(hx, -hy, hz));
        addFace(builder, new Vector3Dd(-1, 0, 0),
            new Vector3Dd(-hx, hy, hz), new Vector3Dd(-hx, hy, -hz),
            new Vector3Dd(-hx, -hy, -hz), new Vector3Dd(-hx, -hy, hz));
        addFace(builder, new Vector3Dd(0, 1, 0),
            new Vector3Dd(hx, hy, hz), new Vector3Dd(hx, hy, -hz),
            new Vector3Dd(-hx, hy, -hz), new Vector3Dd(-hx, hy, hz));
        addFace(builder, new Vector3Dd(1, 0, 0),
            new Vector3Dd(hx, -hy, hz), new Vector3Dd(hx, -hy, -hz),
            new Vector3Dd(hx, hy, -hz), new Vector3Dd(hx, hy, hz));
        return builder.build();
    }

    /**
    Adds a face whose vertices are given in the order upper left, lower left,
    lower right, upper right as seen from outside (the Jogl2 texture layout),
    and which is wound so its front side looks outside.
    */
    private static void addFace(
        Jogl4MeshBuilder builder,
        Vector3Dd n,
        Vector3Dd a,
        Vector3Dd b,
        Vector3Dd c,
        Vector3Dd d)
    {
        Vector3Dd winding = b.subtract(a).crossProduct(c.subtract(b));

        if ( winding.dotProduct(n) >= 0 ) {
            builder.addQuad(a, n, 0, 1, b, n, 0, 0, c, n, 1, 0, d, n, 1, 1);
        }
        else {
            builder.addQuad(d, n, 1, 1, c, n, 1, 0, b, n, 0, 0, a, n, 0, 1);
        }
    }
}
