package vsdk.toolkit.render.jogl.volume;

import java.util.List;

import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.volume.Torus;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.render.jogl.Jogl4GeometryRenderer;
import vsdk.toolkit.render.jogl.Jogl4MeshBuilder;
import vsdk.toolkit.render.jogl.Jogl4MeshRenderer;
import vsdk.toolkit.render.jogl.Jogl4Renderer;

/**
Renders a `Torus` with the GL4 pipeline: it is tessellated by `buildMesh`
into a grid of quads along its two angles, and the mesh is cached by
`Jogl4GeometryRenderer`, identified by the radii of the torus.
*/
public class Jogl4TorusRenderer extends Jogl4Renderer {
    /// Divisions along the major circle
    public static final int MAJOR_SEGMENTS = 48;
    /// Divisions along the minor circle
    public static final int MINOR_SEGMENTS = 24;

    private Jogl4TorusRenderer() {
    }

    /**
    Draws the torus, with the passes selected by the configuration (see
    `Jogl4MeshRenderer`).

    @param gl OpenGL context
    @param torus torus to draw
    @param camera camera that views the torus
    @param lights lights of the scene, or null or empty to use a light at the camera
    @param material material of the torus
    @param quality bits of rendering configuration
    @param textureMap texture, or null
    @param normalMap normal (bump) map, or null
    @param localTransform transformation from torus space to world space
    */
    public static void draw(
        GL4 gl,
        Torus torus,
        Camera camera,
        List<Light> lights,
        SimpleMaterial material,
        RendererConfiguration quality,
        RGBImageUncompressed textureMap,
        RGBImageUncompressed normalMap,
        Matrix4x4d localTransform)
    {
        Jogl4GeometryRenderer.draw(gl, torus, camera, lights, material, quality,
            textureMap, normalMap, localTransform);
    }

    /**
    @param torus a torus
    @return a string that identifies the mesh of the torus
    */
    public static String meshKey(Torus torus)
    {
        return "torus/" + torus.getMajorRadius() + "/" + torus.getMinorRadius();
    }

    /**
    @param torus torus to tessellate
    @return the mesh of the surface of the torus
    */
    public static Jogl4MeshRenderer.Mesh buildMesh(Torus torus)
    {
        double bigR = torus.getMajorRadius();
        double smallR = torus.getMinorRadius();
        Jogl4MeshBuilder builder = new Jogl4MeshBuilder(bigR + smallR);
        int nw = MAJOR_SEGMENTS;
        int nv = MINOR_SEGMENTS;

        for ( int i = 0; i < nw; i++ ) {
            double w0 = 2 * Math.PI * i / nw;
            double w1 = 2 * Math.PI * (i + 1) / nw;

            for ( int j = 0; j < nv; j++ ) {
                double v0 = 2 * Math.PI * j / nv;
                double v1 = 2 * Math.PI * (j + 1) / nv;
                Vector3Dd p00 = point(bigR, smallR, w0, v0);
                Vector3Dd p01 = point(bigR, smallR, w0, v1);
                Vector3Dd p11 = point(bigR, smallR, w1, v1);
                Vector3Dd p10 = point(bigR, smallR, w1, v0);
                Vector3Dd n00 = normal(w0, v0);
                Vector3Dd n01 = normal(w0, v1);
                Vector3Dd n11 = normal(w1, v1);
                Vector3Dd n10 = normal(w1, v0);

                builder.addQuad(
                    p00, n00, (double)i / nw, (double)j / nv,
                    p10, n10, (double)(i + 1) / nw, (double)j / nv,
                    p11, n11, (double)(i + 1) / nw, (double)(j + 1) / nv,
                    p01, n01, (double)i / nw, (double)(j + 1) / nv);
            }
        }
        return builder.build();
    }

    private static Vector3Dd point(double bigR, double smallR, double w, double v)
    {
        return new Vector3Dd(
            (bigR + smallR * Math.cos(v)) * Math.cos(w),
            (bigR + smallR * Math.cos(v)) * Math.sin(w),
            smallR * Math.sin(v));
    }

    private static Vector3Dd normal(double w, double v)
    {
        return new Vector3Dd(Math.cos(v) * Math.cos(w), Math.cos(v) * Math.sin(w), Math.sin(v));
    }
}
