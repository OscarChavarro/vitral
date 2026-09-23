package vsdk.toolkit.render.jogl;

import java.util.List;

import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.surface.ParametricBiCubicPatch;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.media.RGBImageUncompressed;

/**
Renders a `ParametricBiCubicPatch` (Bezier, Hermite or Ferguson) with the GL4
pipeline. As in `Jogl2ParametricBiCubicPatchRenderer`, the patch is evaluated
over a regular grid of (s, t) parameters with `getApproximationSteps()`
divisions in each direction, using the analytic normals of the patch, and the
texture space matches the parametric space. The mesh is visible from both
sides.
*/
public class Jogl4ParametricBiCubicPatchRenderer extends Jogl4Renderer {
    private Jogl4ParametricBiCubicPatchRenderer() {
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
        ParametricBiCubicPatch patch,
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
    @return a string that identifies its tessellation: its geometry matrices
    and its number of approximation steps
    */
    static String meshKey(ParametricBiCubicPatch patch)
    {
        return "parametricbicubicpatch/" + System.identityHashCode(patch) + "/" +
            patch.getType() + "/" + patch.getApproximationSteps() + "/" +
            patch.geometryMatrixX.hashCode() + "/" + patch.geometryMatrixY.hashCode() + "/" +
            patch.geometryMatrixZ.hashCode();
    }

    /**
    @param patch patch to tessellate
    @return the mesh of the patch, or null if it can not be tessellated
    */
    static Jogl4MeshRenderer.Mesh buildMesh(ParametricBiCubicPatch patch)
    {
        int steps = Math.max(1, patch.getApproximationSteps());
        int n = steps + 1;
        Vector3Dd[][] points = new Vector3Dd[n][n];
        Vector3Dd[][] normals = new Vector3Dd[n][n];

        for ( int i = 0; i < n; i++ ) {
            double s = (double)i / steps;

            for ( int j = 0; j < n; j++ ) {
                double t = (double)j / steps;
                points[i][j] = patch.evaluate(s, t);
                normals[i][j] = patch.evaluateNormal(s, t);
            }
        }

        double[] minmax = patch.getMinMax();
        double size = 1.0;
        if ( minmax != null && minmax.length >= 6 ) {
            size = Math.max(Math.abs(minmax[3] - minmax[0]),
                Math.max(Math.abs(minmax[4] - minmax[1]), Math.abs(minmax[5] - minmax[2])));
        }
        Jogl4MeshBuilder builder = new Jogl4MeshBuilder(size);

        builder.setDoubleSided(true);
        for ( int i = 0; i < steps; i++ ) {
            for ( int j = 0; j < steps; j++ ) {
                addCell(builder, points, normals, i, j, steps);
            }
        }
        return builder.build();
    }

    /**
    Adds the cell between the grid points (i, j) and (i + 1, j + 1). Normals
    that the patch can not give (degenerate points, i.e. a corner with null
    tangents) are replaced by the normal of the cell, and the cell is wound so
    its front side is the one its normals point to.
    */
    private static void addCell(
        Jogl4MeshBuilder builder,
        Vector3Dd[][] points,
        Vector3Dd[][] normals,
        int i,
        int j,
        int steps)
    {
        Vector3Dd p00 = points[i][j];
        Vector3Dd p10 = points[i + 1][j];
        Vector3Dd p11 = points[i + 1][j + 1];
        Vector3Dd p01 = points[i][j + 1];
        Vector3Dd face = p11.subtract(p00).crossProduct(p01.subtract(p10));

        if ( !(face.length() > 1e-15) ) {
            return;
        }
        Vector3Dd n00 = validNormal(normals[i][j], face);
        Vector3Dd n10 = validNormal(normals[i + 1][j], face);
        Vector3Dd n11 = validNormal(normals[i + 1][j + 1], face);
        Vector3Dd n01 = validNormal(normals[i][j + 1], face);
        double u0 = (double)i / steps;
        double u1 = (double)(i + 1) / steps;
        double v0 = (double)j / steps;
        double v1 = (double)(j + 1) / steps;
        Vector3Dd average = n00.add(n10).add(n11).add(n01);

        if ( average.dotProduct(face) >= 0 ) {
            builder.addQuad(p00, n00, u0, v0, p10, n10, u1, v0,
                p11, n11, u1, v1, p01, n01, u0, v1);
        }
        else {
            builder.addQuad(p01, n01, u0, v1, p11, n11, u1, v1,
                p10, n10, u1, v0, p00, n00, u0, v0);
        }
    }

    private static Vector3Dd validNormal(Vector3Dd normal, Vector3Dd fallback)
    {
        if ( normal == null || !(normal.length() > 1e-12) ) {
            return fallback.normalized();
        }
        return normal;
    }
}
