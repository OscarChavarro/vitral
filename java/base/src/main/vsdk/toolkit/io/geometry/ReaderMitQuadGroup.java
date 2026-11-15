package vsdk.toolkit.io.geometry;

import java.util.ArrayList;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.element.Vertex;
import vsdk.toolkit.environment.geometry.surface.QuadMesh;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleScene;

/**
One MIT quads block: `v x y z` vertices and `q i0 i1 i2 i3` faces, built as a
QuadMesh; material changes flush faces while keeping vertices.
*/
public class ReaderMitQuadGroup {
    private final ArrayList<Vector3Dd> vertices = new ArrayList<Vector3Dd>();

    private final ArrayList<Integer> quadIndices = new ArrayList<Integer>();

    public ReaderMitQuadGroup() {}

    public void reset() { vertices.clear(); quadIndices.clear(); }

    public boolean addVertex(Vector3Dd vertex) { return vertices.add(vertex); }

    /** Reads four vertex indices; false when malformed or out of range. */
    public boolean addQuad(String indices) {
        int[][] q = {{0}, {0}, {0}, {0}};
        String[] extra = {""};
        if (_CScanf.sscanf(indices, "%d %d %d %d %1s", q[0], q[1], q[2], q[3], extra) != 4) return false;
        for (int i = 0; i < 4; ++i)
            if (q[i][0] < 0 || q[i][0] >= vertices.size()) return false;
        for (int i = 0; i < 4; ++i)
            if (!quadIndices.add(q[i][0])) return false;
        return true;
    }

    /** Adds the pending faces as one body and returns it, or null when none are pending. */
    public SimpleBody flush(SimpleScene scene, SimpleMaterial material) {
        if (quadIndices.isEmpty()) return null;
        ArrayList<Vertex> meshVertices = new ArrayList<Vertex>();
        for (int i = 0; i < vertices.size(); ++i) meshVertices.add(new Vertex(vertices.get(i)));
        QuadMesh mesh = new QuadMesh();
        mesh.setVertexes(meshVertices.toArray(new Vertex[0]));
        final int quadCount = quadIndices.size() / 4;
        mesh.initQuadArrays(quadCount);
        int[] target = mesh.getQuadIndices();
        for (int i = 0; i < quadIndices.size(); ++i) target[i] = quadIndices.get(i);
        SimpleBody body = new SimpleBody();
        body.setGeometry(mesh);
        body.setMaterial(new SimpleMaterial(material));
        scene.addBody(body);
        quadIndices.clear();
        return body;
    }
}
