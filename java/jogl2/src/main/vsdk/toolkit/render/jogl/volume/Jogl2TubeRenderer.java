package vsdk.toolkit.render.jogl.volume;

import com.jogamp.opengl.GL2;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.volume.Tube;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.render.jogl.Jogl2GeometryRenderer;
import vsdk.toolkit.render.jogl.Jogl2MeshBuilder;
import vsdk.toolkit.render.jogl.Jogl2MeshRenderer;
import vsdk.toolkit.render.jogl.Jogl2Renderer;

/**
Renders a `Tube` with the fixed function pipeline (OpenGL 2): it is tessellated by `buildMesh` into
its external wall, its internal wall (with normals towards the axis) and its
annular caps, and the mesh is cached by `Jogl2GeometryRenderer`, identified by
the parameters of the tube. A washer (height 0) is a double sided annulus.
*/
public class Jogl2TubeRenderer extends Jogl2Renderer {
    /// Divisions around the axis
    public static final int SLICES = 32;

    private Jogl2TubeRenderer() {
    }

    /**
    Draws the tube, with the passes selected by the configuration (see
    `Jogl2MeshRenderer`), using the lights, material, texture and
    transformations of the current OpenGL state.

    @param gl OpenGL context
    @param tube tube to draw
    @param camera camera that views the tube
    @param quality bits of rendering configuration
    */
    public static void draw(GL2 gl, Tube tube, Camera camera,
        RendererConfiguration quality)
    {
        Jogl2GeometryRenderer.drawMesh(gl, tube, camera, quality);
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
    public static Jogl2MeshRenderer.Mesh buildMesh(Tube tube)
    {
        double ri = tube.getInternalRadius();
        double re = tube.getExternalRadius();
        double h = tube.getHeight();
        Jogl2MeshBuilder builder = new Jogl2MeshBuilder(Math.max(re, h));

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
    private static void addInternalWall(Jogl2MeshBuilder builder, double r,
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
