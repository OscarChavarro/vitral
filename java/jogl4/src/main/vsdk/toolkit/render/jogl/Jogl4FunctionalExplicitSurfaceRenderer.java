package vsdk.toolkit.render.jogl;

import java.util.List;

import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.surface.FunctionalExplicitSurface;
import vsdk.toolkit.environment.geometry.surface.TriangleMesh;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.media.RGBImageUncompressed;

/**
Renders a `FunctionalExplicitSurface` (a height field z = f(x, y)) with the GL4
pipeline. As in `Jogl2FunctionalExplicitSurfaceRenderer`, the surface is drawn
from its internal triangle mesh, which the surface rebuilds each time its
function, bounds or tessellation change. The mesh is visible from both sides,
and its texture coordinates span the (x, y) bounds of the surface.
*/
public class Jogl4FunctionalExplicitSurfaceRenderer extends Jogl4Renderer {
    private Jogl4FunctionalExplicitSurfaceRenderer() {
    }

    /**
    Draws the surface, with the passes selected by the configuration (see
    `Jogl4MeshRenderer`).

    @param gl OpenGL context
    @param surface surface to draw
    @param camera camera that views the surface
    @param lights lights of the scene, or null or empty to use a light at the camera
    @param material material of the surface
    @param quality bits of rendering configuration
    @param textureMap texture, or null
    @param normalMap normal (bump) map, or null
    @param localTransform transformation from surface space to world space
    */
    public static void draw(
        GL4 gl,
        FunctionalExplicitSurface surface,
        Camera camera,
        List<Light> lights,
        SimpleMaterial material,
        RendererConfiguration quality,
        RGBImageUncompressed textureMap,
        RGBImageUncompressed normalMap,
        Matrix4x4d localTransform)
    {
        Jogl4GeometryRenderer.draw(gl, surface, camera, lights, material, quality,
            textureMap, normalMap, localTransform);
    }

    /**
    @param surface a surface
    @return a string that identifies its current tessellation: the internal
    mesh is replaced by the surface whenever it changes
    */
    static String meshKey(FunctionalExplicitSurface surface)
    {
        return "functionalexplicitsurface/" + System.identityHashCode(surface) + "/" +
            System.identityHashCode(surface.getInternalTriangleMesh());
    }

    /**
    @param surface surface to tessellate
    @return the mesh of the surface, or null if it has no valid tessellation
    (i.e. its function could not be evaluated)
    */
    static Jogl4MeshRenderer.Mesh buildMesh(FunctionalExplicitSurface surface)
    {
        TriangleMesh mesh = surface.getInternalTriangleMesh();

        if ( mesh == null ) {
            return null;
        }
        double[] p = mesh.getVertexPositions();
        double[] n = mesh.getVertexNormals();
        int[] idx = mesh.getTriangleIndexes();

        if ( p == null || idx == null || idx.length < 3 ) {
            return null;
        }
        double sizeX = surface.getMaxXBound() - surface.getMinXBound();
        double sizeY = surface.getMaxYBound() - surface.getMinYBound();
        double sizeZ = surface.getMaxZBound() - surface.getMinZBound();
        Jogl4MeshBuilder builder = new Jogl4MeshBuilder(
            Math.max(Math.abs(sizeX), Math.max(Math.abs(sizeY), Math.abs(sizeZ))));
        Vector3Dd[] pos = new Vector3Dd[3];
        Vector3Dd[] nor = new Vector3Dd[3];
        double[] u = new double[3];
        double[] v = new double[3];

        builder.setDoubleSided(true);
        for ( int t = 0; t + 2 < idx.length; t += 3 ) {
            for ( int k = 0; k < 3; k++ ) {
                int i = idx[t + k];

                pos[k] = new Vector3Dd(p[3 * i], p[3 * i + 1], p[3 * i + 2]);
                nor[k] = (n != null && n.length >= 3 * i + 3)
                    ? new Vector3Dd(n[3 * i], n[3 * i + 1], n[3 * i + 2])
                    : null;
                u[k] = sizeX != 0 ? (pos[k].x() - surface.getMinXBound()) / sizeX : 0;
                v[k] = sizeY != 0 ? (pos[k].y() - surface.getMinYBound()) / sizeY : 0;
            }
            Vector3Dd face = pos[1].subtract(pos[0]).crossProduct(pos[2].subtract(pos[0]));

            if ( face.length() < 1e-15 ) {
                continue;
            }
            for ( int k = 0; k < 3; k++ ) {
                if ( nor[k] == null || !(nor[k].length() > 1e-12) ) {
                    nor[k] = face;
                }
            }
            // The front side is the one the normals point to
            if ( nor[0].add(nor[1]).add(nor[2]).dotProduct(face) >= 0 ) {
                builder.addTriangle(pos[0], nor[0], u[0], v[0], pos[1], nor[1], u[1], v[1],
                    pos[2], nor[2], u[2], v[2]);
            }
            else {
                builder.addTriangle(pos[2], nor[2], u[2], v[2], pos[1], nor[1], u[1], v[1],
                    pos[0], nor[0], u[0], v[0]);
            }
        }
        return builder.build();
    }
}
