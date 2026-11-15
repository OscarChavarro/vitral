package vsdk.toolkit.io.geometry;

import java.util.ArrayList;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.element.Triangle;
import vsdk.toolkit.environment.geometry.surface.TriangleMesh;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleScene;

/**
One MIT triangles block; material changes flush faces while keeping vertices.
*/
public class ReaderMitTriangleGroup {
    private final ArrayList<Vector3Dd> vertices = new ArrayList<Vector3Dd>();

    private final ArrayList<Triangle> triangles = new ArrayList<Triangle>();

    public ReaderMitTriangleGroup() {}

    public void reset() { vertices.clear(); triangles.clear(); }

    public boolean addVertex(Vector3Dd vertex) { return vertices.add(vertex); }

    private static boolean isStrtolSpace(char c)
    {
        return c == ' ' || c == '\t' || c == '\n' || c == '\u000b' || c == '\f' || c == '\r';
    }

    public boolean addFace(String coordinates) {
        ArrayList<Integer> indices = new ArrayList<Integer>();
        int cursor = 0;
        final int n = coordinates.length();
        while (cursor < n) {
            while (cursor < n && (coordinates.charAt(cursor) == ' ' || coordinates.charAt(cursor) == '\t' ||
                   coordinates.charAt(cursor) == '\r' || coordinates.charAt(cursor) == '\n')) ++cursor;
            if (cursor >= n) break;
            // strtol: leading white space, optional sign and decimal digits.
            int start = cursor;
            while (start < n && isStrtolSpace(coordinates.charAt(start))) ++start;
            long[] value = {0};
            final int length = _CScanf.integerPrefix(coordinates, start, value);
            if (length == 0 || value[0] < 0 || value[0] >= vertices.size()) return false;
            if (!indices.add((int)value[0])) return false;
            cursor = start + length;
        }
        if (indices.size() < 3) return false;
        for (int i = 2; i < indices.size(); ++i) {
            final int a = indices.get(0), b = indices.get(i-1), c = indices.get(i);
            if (a == b || b == c || a == c) continue;
            if (!triangles.add(new Triangle(a,b,c))) return false;
        }
        return true;
    }

    /** Adds the pending faces as one body and returns it, or null when none are pending. */
    public SimpleBody flush(SimpleScene scene, SimpleMaterial material) {
        if (triangles.isEmpty()) return null;
        TriangleMesh mesh = new TriangleMesh();
        mesh.initVertexPositionsArray(vertices.size());
        double[] positions = mesh.getVertexPositions();
        for (int i = 0; i < vertices.size(); ++i) {
            positions[i*3] = vertices.get(i).x();
            positions[i*3+1] = vertices.get(i).y();
            positions[i*3+2] = vertices.get(i).z();
        }
        mesh.setTriangles(triangles.toArray(new Triangle[0]));
        // Java only: MIT raytracing scenes intersect triangle surfaces from
        // both sides; keep that behavior when the same scene is rasterized.
        mesh.setDoubleSided(true);
        // Prepare shared read-only normals before parallel ray tracing starts.
        mesh.calculateNormals();
        SimpleBody body = new SimpleBody();
        body.setGeometry(mesh);
        body.setMaterial(new SimpleMaterial(material));
        scene.addBody(body);
        triangles.clear();
        return body;
    }
}
