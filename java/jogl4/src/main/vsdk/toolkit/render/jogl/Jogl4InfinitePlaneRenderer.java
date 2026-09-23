package vsdk.toolkit.render.jogl;

import java.util.List;

import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.media.RGBImageUncompressed;

/**
Renders an `InfinitePlane` with the GL4 pipeline. As in
`Jogl2InfinitePlaneRenderer`, a finite part of the plane is shown: a square
of `SIZE` units centered at the point of the plane closest to the origin,
divided into `TILES` x `TILES` cells. The square is visible from both sides.

The bounding volume of an infinite plane is infinite, so the bounding volume
and the selection corners are drawn around the square shown.
*/
public class Jogl4InfinitePlaneRenderer extends Jogl4Renderer {
    /// Side of the square of the plane that is shown
    public static final double SIZE = 20.0;
    /// Number of cells along each side of the square
    public static final int TILES = 10;

    private Jogl4InfinitePlaneRenderer() {
    }

    /**
    Draws the plane, with the passes selected by the configuration (see
    `Jogl4MeshRenderer`).

    @param gl OpenGL context
    @param plane plane to draw
    @param camera camera that views the plane
    @param lights lights of the scene, or null or empty to use a light at the camera
    @param material material of the plane
    @param quality bits of rendering configuration
    @param textureMap texture, or null
    @param normalMap normal (bump) map, or null
    @param localTransform transformation from plane space to world space
    */
    public static void draw(
        GL4 gl,
        InfinitePlane plane,
        Camera camera,
        List<Light> lights,
        SimpleMaterial material,
        RendererConfiguration quality,
        RGBImageUncompressed textureMap,
        RGBImageUncompressed normalMap,
        Matrix4x4d localTransform)
    {
        Jogl4GeometryRenderer.draw(gl, plane, camera, lights, material, quality,
            textureMap, normalMap, localTransform);
    }

    /**
    @param plane a plane
    @return a string that identifies the square shown for the plane
    */
    static String meshKey(InfinitePlane plane)
    {
        return "infiniteplane/" + plane.getA() + "/" + plane.getB() + "/" +
            plane.getC() + "/" + plane.getD();
    }

    /**
    @param plane a plane
    @return the bounding box of the square shown for the plane, as given by
    `Geometry.getMinMax()`
    */
    public static double[] calculateShownMinMax(InfinitePlane plane)
    {
        Vector3Dd[] frame = calculateFrame(plane);
        double[] minmax = new double[] {
            Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE,
            -Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE
        };
        double h = SIZE / 2;

        for ( int i = -1; i <= 1; i += 2 ) {
            for ( int j = -1; j <= 1; j += 2 ) {
                Vector3Dd p = frame[0].add(frame[1].multiply(i * h)).add(frame[2].multiply(j * h));
                double[] c = new double[] { p.x(), p.y(), p.z() };

                for ( int k = 0; k < 3; k++ ) {
                    minmax[k] = Math.min(minmax[k], c[k]);
                    minmax[k + 3] = Math.max(minmax[k + 3], c[k]);
                }
            }
        }
        return minmax;
    }

    /**
    @param plane plane to tessellate
    @return the mesh of the square shown for the plane
    */
    static Jogl4MeshRenderer.Mesh buildMesh(InfinitePlane plane)
    {
        Vector3Dd[] frame = calculateFrame(plane);
        Vector3Dd center = frame[0];
        Vector3Dd u = frame[1];
        Vector3Dd v = frame[2];
        Vector3Dd n = frame[3];
        double h = SIZE / 2;
        double d = SIZE / TILES;
        Jogl4MeshBuilder builder = new Jogl4MeshBuilder(SIZE);

        builder.setDoubleSided(true);
        for ( int i = 0; i < TILES; i++ ) {
            double a0 = -h + i * d;
            double a1 = a0 + d;

            for ( int j = 0; j < TILES; j++ ) {
                double b0 = -h + j * d;
                double b1 = b0 + d;

                // (u, v, n) is right handed: counterclockwise seen from n
                builder.addQuad(
                    center.add(u.multiply(a0)).add(v.multiply(b0)), n,
                    (double)i / TILES, (double)j / TILES,
                    center.add(u.multiply(a1)).add(v.multiply(b0)), n,
                    (double)(i + 1) / TILES, (double)j / TILES,
                    center.add(u.multiply(a1)).add(v.multiply(b1)), n,
                    (double)(i + 1) / TILES, (double)(j + 1) / TILES,
                    center.add(u.multiply(a0)).add(v.multiply(b1)), n,
                    (double)i / TILES, (double)(j + 1) / TILES);
            }
        }
        return builder.build();
    }

    /**
    @return the point of the plane closest to the origin, two orthonormal
    directions (u, v) on it and its normal n, with (u, v, n) right handed
    */
    private static Vector3Dd[] calculateFrame(InfinitePlane plane)
    {
        Vector3Dd n = plane.getNormal();
        double length = Math.sqrt(plane.getA() * plane.getA() +
            plane.getB() * plane.getB() + plane.getC() * plane.getC());
        // The plane is a x + b y + c z + d = 0; (a, b, c) may not be unitary
        Vector3Dd center = n.multiply(length > 0 ? -plane.getD() / length : 0);
        Vector3Dd reference = Math.abs(n.z()) < 0.9 ? new Vector3Dd(0, 0, 1) : new Vector3Dd(1, 0, 0);
        Vector3Dd u = reference.crossProduct(n).normalized();
        Vector3Dd v = n.crossProduct(u).normalized();

        return new Vector3Dd[] { center, u, v, n };
    }
}
