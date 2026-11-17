package vsdk.toolkit.render.jogl.surface;

import java.util.List;

import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.surface.QuadMesh;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.render.jogl.Jogl4GeometryRenderer;
import vsdk.toolkit.render.jogl.Jogl4MeshBuilder;
import vsdk.toolkit.render.jogl.Jogl4MeshRenderer;
import vsdk.toolkit.render.jogl.Jogl4Renderer;

/**
Renders a `QuadMesh` with the GL4 pipeline: its quads are copied by
`buildMesh` (as two triangles each), visible from both sides, with the
normals of its vertices or, when it has none, the normal of each quad. The
mesh is cached by `Jogl4GeometryRenderer`, identified by the quad mesh, as
they are not edited while shown.
*/
public class Jogl4QuadMeshRenderer extends Jogl4Renderer {
    private Jogl4QuadMeshRenderer() {
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
        QuadMesh mesh,
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
    @param mesh a quad mesh
    @return a string that identifies the mesh
    */
    public static String meshKey(QuadMesh mesh)
    {
        return "quadmesh/" + System.identityHashCode(mesh);
    }

    /**
    @param mesh quad mesh to copy
    @return its mesh, or null if it has no vertices or quads
    */
    public static Jogl4MeshRenderer.Mesh buildMesh(QuadMesh mesh)
    {
        double[] p = mesh.getVertexPositions();
        double[] n = mesh.getVertexNormals();
        double[] uv = mesh.getVertexUvs();
        int[] idx = mesh.getQuadIndices();
        int numVertices = p != null ? p.length / 3 : 0;

        if ( p == null || idx == null || mesh.getNumQuads() <= 0 ) {
            return null;
        }
        Jogl4MeshBuilder builder = new Jogl4MeshBuilder(
            Jogl4MeshBuilder.characteristicSize(mesh.getMinMax()));
        builder.setDoubleSided(true);
        for ( int q = 0; q < mesh.getNumQuads() && 4 * q + 3 < idx.length; q++ ) {
            Vector3Dd[] pos = new Vector3Dd[4];
            Vector3Dd[] nor = new Vector3Dd[4];
            double[] u = new double[4];
            double[] v = new double[4];
            boolean valid = true;

            for ( int k = 0; k < 4; k++ ) {
                int i = idx[4 * q + k];
                if ( i < 0 || i >= numVertices ) {
                    valid = false;
                    break;
                }
                pos[k] = new Vector3Dd(p[3 * i], p[3 * i + 1], p[3 * i + 2]);
                nor[k] = (n != null && n.length >= 3 * i + 3)
                    ? new Vector3Dd(n[3 * i], n[3 * i + 1], n[3 * i + 2])
                    : null;
                if ( uv != null && uv.length >= 2 * i + 2 ) {
                    u[k] = uv[2 * i];
                    v[k] = uv[2 * i + 1];
                }
            }
            if ( !valid ) {
                continue;
            }
            // Normal of the quad: cross product of its diagonals
            Vector3Dd face = pos[2].subtract(pos[0]).crossProduct(
                pos[3].subtract(pos[1]));
            if ( !(face.length() > 0) ) {
                face = new Vector3Dd(0, 0, 1);
            }
            for ( int k = 0; k < 4; k++ ) {
                if ( nor[k] == null || !(nor[k].length() > 0) ) {
                    nor[k] = face;
                }
            }
            builder.addQuad(pos[0], nor[0], u[0], v[0], pos[1], nor[1], u[1], v[1],
                pos[2], nor[2], u[2], v[2], pos[3], nor[3], u[3], v[3]);
        }
        return builder.build();
    }
}
