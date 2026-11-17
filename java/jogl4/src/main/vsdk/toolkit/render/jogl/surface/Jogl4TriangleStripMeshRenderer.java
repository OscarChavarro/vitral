package vsdk.toolkit.render.jogl.surface;

import java.util.List;

import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.surface.TriangleMesh;
import vsdk.toolkit.environment.geometry.surface.TriangleStripMesh;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.render.jogl.Jogl4GeometryRenderer;
import vsdk.toolkit.render.jogl.Jogl4MeshBuilder;
import vsdk.toolkit.render.jogl.Jogl4MeshRenderer;
import vsdk.toolkit.render.jogl.Jogl4Renderer;

/**
Renders a `TriangleStripMesh` with the GL4 pipeline: the triangles of its
strips (see `TriangleStripMesh.exportToTriangleMesh`) are copied by
`buildMesh`, visible from both sides, with the normals of its vertexes or,
when they have none, the normal of each triangle. The mesh is cached by
`Jogl4GeometryRenderer`, identified by the strip mesh, as they are not edited
while shown.
*/
public class Jogl4TriangleStripMeshRenderer extends Jogl4Renderer {
    private Jogl4TriangleStripMeshRenderer() {
    }

    /**
    Draws the strip mesh, with the passes selected by the configuration (see
    `Jogl4MeshRenderer`).

    @param gl OpenGL context
    @param strips strip mesh to draw
    @param camera camera that views the strip mesh
    @param lights lights of the scene, or null or empty to use a light at the camera
    @param material material of the strip mesh
    @param quality bits of rendering configuration
    @param textureMap texture, or null
    @param normalMap normal (bump) map, or null
    @param localTransform transformation from strip mesh space to world space
    */
    public static void draw(
        GL4 gl,
        TriangleStripMesh strips,
        Camera camera,
        List<Light> lights,
        SimpleMaterial material,
        RendererConfiguration quality,
        RGBImageUncompressed textureMap,
        RGBImageUncompressed normalMap,
        Matrix4x4d localTransform)
    {
        Jogl4GeometryRenderer.draw(gl, strips, camera, lights, material, quality,
            textureMap, normalMap, localTransform);
    }

    /**
    @param strips a strip mesh
    @return a string that identifies the mesh
    */
    public static String meshKey(TriangleStripMesh strips)
    {
        return "trianglestripmesh/" + System.identityHashCode(strips);
    }

    /**
    @param strips strip mesh to copy
    @return its mesh, or null if it has no triangles
    */
    public static Jogl4MeshRenderer.Mesh buildMesh(TriangleStripMesh strips)
    {
        TriangleMesh mesh = strips.exportToTriangleMesh();
        if ( mesh == null || mesh.getNumTriangles() <= 0 ) {
            return null;
        }
        double[] p = mesh.getVertexPositions();
        int[] idx = mesh.getTriangleIndexes();
        Jogl4MeshBuilder builder = new Jogl4MeshBuilder(
            Jogl4MeshBuilder.characteristicSize(strips.getMinMax()));
        builder.setDoubleSided(true);
        for ( int t = 0; t + 2 < idx.length; t += 3 ) {
            Vector3Dd[] pos = new Vector3Dd[3];
            Vector3Dd[] nor = new Vector3Dd[3];

            for ( int k = 0; k < 3; k++ ) {
                int i = idx[t + k];
                pos[k] = new Vector3Dd(p[3 * i], p[3 * i + 1], p[3 * i + 2]);
                nor[k] = strips.getVertexAt(i).getNormal();
            }
            Vector3Dd face = pos[1].subtract(pos[0]).crossProduct(
                pos[2].subtract(pos[0]));
            if ( !(face.length() > 0) ) {
                continue;
            }
            for ( int k = 0; k < 3; k++ ) {
                if ( nor[k] == null || !(nor[k].length() > 0) ) {
                    nor[k] = face;
                }
            }
            builder.addTriangle(pos[0], nor[0], 0, 0, pos[1], nor[1], 0, 0,
                pos[2], nor[2], 0, 0);
        }
        return builder.build();
    }
}
