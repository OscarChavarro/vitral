package vsdk.toolkit.render.jogl.volume;

import java.util.List;

import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.volume.Tube;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.render.jogl.Jogl4GeometryRenderer;
import vsdk.toolkit.render.jogl.Jogl4MeshBuilder;
import vsdk.toolkit.render.jogl.Jogl4MeshRenderer;
import vsdk.toolkit.render.jogl.Jogl4Renderer;

/**
Renders a `Tube` with the GL4 pipeline: it is tessellated by `buildMesh` into
its external wall, its internal wall (with normals towards the axis) and its
annular caps, and the mesh is cached by `Jogl4GeometryRenderer`, identified by
the parameters of the tube. A washer (height 0) is a double sided annulus.
*/
public class Jogl4TubeRenderer extends Jogl4Renderer {
    /// Divisions around the axis
    public static final int SLICES = 32;

    private Jogl4TubeRenderer() {
    }

    /**
    Draws the tube, with the passes selected by the configuration (see
    `Jogl4MeshRenderer`).

    @param gl OpenGL context
    @param tube tube to draw
    @param camera camera that views the tube
    @param lights lights of the scene, or null or empty to use a light at the camera
    @param material material of the tube
    @param quality bits of rendering configuration
    @param textureMap texture, or null
    @param normalMap normal (bump) map, or null
    @param localTransform transformation from tube space to world space
    */
    public static void draw(
        GL4 gl,
        Tube tube,
        Camera camera,
        List<Light> lights,
        SimpleMaterial material,
        RendererConfiguration quality,
        RGBImageUncompressed textureMap,
        RGBImageUncompressed normalMap,
        Matrix4x4d localTransform)
    {
        Jogl4GeometryRenderer.draw(gl, tube, camera, lights, material, quality,
            textureMap, normalMap, localTransform);
    }

    /**
    @param tube a tube
    @return a string that identifies the mesh of the tube
    */
    public static String meshKey(Tube tube)
    {
        return "tube/" + tube.getInternalRadius() + "/" +
            tube.getExternalRadius() + "/" + tube.getHeight();
    }

    /**
    @param tube tube to tessellate
    @return the mesh of the walls and the caps of the tube
    */
    public static Jogl4MeshRenderer.Mesh buildMesh(Tube tube)
    {
        double ri = tube.getInternalRadius();
        double re = tube.getExternalRadius();
        double h = tube.getHeight();
        Jogl4MeshBuilder builder = new Jogl4MeshBuilder(Math.max(re, h));

        if ( tube.isWasher() ) {
            builder.addDisk(0, ri, re, true, SLICES);
            builder.setDoubleSided(true);
            return builder.build();
        }
        builder.addFrustum(0, re, h, re, SLICES);
        if ( ri > 0.0 ) {
            addInternalWall(builder, ri, h);
        }
        builder.addDisk(0, ri, re, false, SLICES);
        builder.addDisk(h, ri, re, true, SLICES);
        return builder.build();
    }

    /**
    The internal wall faces the axis: its quads go clockwise seen from
    outside the hole, with normals pointing to the axis.
    */
    private static void addInternalWall(Jogl4MeshBuilder builder, double r,
        double h)
    {
        for ( int i = 0; i < SLICES; i++ ) {
            double a0 = 2 * Math.PI * i / SLICES;
            double a1 = 2 * Math.PI * (i + 1) / SLICES;
            double c0 = Math.cos(a0);
            double s0 = Math.sin(a0);
            double c1 = Math.cos(a1);
            double s1 = Math.sin(a1);
            Vector3Dd n0 = new Vector3Dd(-c0, -s0, 0);
            Vector3Dd n1 = new Vector3Dd(-c1, -s1, 0);
            double u0 = (double)i / SLICES;
            double u1 = (double)(i + 1) / SLICES;

            builder.addQuad(
                new Vector3Dd(r * c1, r * s1, 0), n1, u1, 0,
                new Vector3Dd(r * c0, r * s0, 0), n0, u0, 0,
                new Vector3Dd(r * c0, r * s0, h), n0, u0, 1,
                new Vector3Dd(r * c1, r * s1, h), n1, u1, 1);
        }
    }
}
