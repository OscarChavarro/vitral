package vsdk.toolkit.render.jogl.surface;

import java.util.List;

import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.surface.TriangleMesh;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.render.jogl.Jogl4GeometryRenderer;
import vsdk.toolkit.render.jogl.Jogl4MeshBuilder;
import vsdk.toolkit.render.jogl.Jogl4MeshRenderer;
import vsdk.toolkit.render.jogl.Jogl4Renderer;

/**
Renders a `TriangleMesh` with the GL4 pipeline: its triangles are copied
by `buildMesh` with their normals and texture coordinates, and the mesh is
cached by `Jogl4GeometryRenderer`, identified by the triangle mesh, as they
are not edited while shown. It is also the way to draw the triangle meshes
that other geometries are tessellated to.
*/
public class Jogl4TriangleMeshRenderer extends Jogl4Renderer {
    private Jogl4TriangleMeshRenderer() {
    }

    /**
    Draws the mesh, with the passes selected by the configuration (see
    `Jogl4MeshRenderer`).

    @param gl OpenGL context
    @param mesh mesh to draw
    @param camera camera that views the mesh
    @param lights lights of the scene, or null or empty to use a light at the camera
    @param material material of the mesh
    @param quality bits of rendering configuration
    @param textureMap texture, or null
    @param normalMap normal (bump) map, or null
    @param localTransform transformation from mesh space to world space
    */
    public static void draw(
        GL4 gl,
        TriangleMesh mesh,
        Camera camera,
        List<Light> lights,
        SimpleMaterial material,
        RendererConfiguration quality,
        RGBImageUncompressed textureMap,
        RGBImageUncompressed normalMap,
        Matrix4x4d localTransform)
    {
        Jogl4GeometryRenderer.draw(gl, mesh, camera, lights, material, quality,
            textureMap, normalMap, localTransform);
    }

    /**
    @param mesh a triangle mesh
    @return a string that identifies the mesh
    */
    public static String meshKey(TriangleMesh mesh)
    {
        return "trianglemesh/" + System.identityHashCode(mesh);
    }

    /**
    @param mesh triangle mesh to copy, visible from both sides if it is
    double sided
    @return its mesh, or null if it has no vertices or triangles
    */
    public static Jogl4MeshRenderer.Mesh buildMesh(TriangleMesh mesh)
    {
        return buildMesh(mesh, mesh.isDoubleSided());
    }

    /**
    @param mesh triangle mesh to copy
    @param doubleSided true to make it visible from both sides
    @return its mesh, or null if it has no vertices or triangles
    */
    public static Jogl4MeshRenderer.Mesh buildMesh(TriangleMesh mesh,
        boolean doubleSided)
    {
        if ( mesh.getVertexPositions() == null || mesh.getTriangleIndexes() == null ) {
            return null;
        }
        Jogl4MeshBuilder builder = new Jogl4MeshBuilder(
            Jogl4MeshBuilder.characteristicSize(mesh.getMinMax()));
        builder.setDoubleSided(doubleSided);
        addTriangles(builder, mesh);
        return builder.build();
    }

    private static void addTriangles(Jogl4MeshBuilder builder, TriangleMesh mesh)
    {
        double[] p = mesh.getVertexPositions();
        double[] n = mesh.getVertexNormals();
        double[] uv = mesh.getVertexUvs();
        int[] idx = mesh.getTriangleIndexes();

        for ( int t = 0; t + 2 < idx.length; t += 3 ) {
            Vector3Dd[] pos = new Vector3Dd[3];
            Vector3Dd[] nor = new Vector3Dd[3];
            double[] u = new double[3];
            double[] v = new double[3];

            for ( int k = 0; k < 3; k++ ) {
                int i = idx[t + k];
                pos[k] = new Vector3Dd(p[3 * i], p[3 * i + 1], p[3 * i + 2]);
                nor[k] = (n != null && n.length >= 3 * i + 3)
                    ? new Vector3Dd(n[3 * i], n[3 * i + 1], n[3 * i + 2])
                    : new Vector3Dd(0, 0, 1);
                if ( uv != null && uv.length >= 2 * i + 2 ) {
                    u[k] = uv[2 * i];
                    v[k] = uv[2 * i + 1];
                }
            }
            builder.addTriangle(pos[0], nor[0], u[0], v[0], pos[1], nor[1], u[1], v[1],
                pos[2], nor[2], u[2], v[2]);
        }
    }
}
