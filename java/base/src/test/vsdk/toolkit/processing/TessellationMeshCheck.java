package vsdk.toolkit.processing;

import java.util.TreeMap;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.surface.TriangleMesh;

/** Properties of tessellation results checked by the tests. */
final class TessellationMeshCheck {
    private TessellationMeshCheck()
    {
    }

    static Vector3Dd vertex(TriangleMesh mesh, int index)
    {
        final double[] p = mesh.getVertexPositionsView();
        return new Vector3Dd(p[index*3], p[index*3 + 1], p[index*3 + 2]);
    }

    /**
    Every directed edge appears once and its reverse once: the mesh is
    closed and its triangles are consistently wound.
    */
    static boolean isClosedAndOriented(TriangleMesh mesh)
    {
        final int[] t = mesh.getTriangleIndexesView();
        final long n = mesh.getVertexPositionsView().length/3;
        TreeMap<Long, Integer> directed = new TreeMap<Long, Integer>();
        for (int i = 0; i < t.length; i += 3) {
            for (int k = 0; k < 3; k++) {
                final long a = t[i + k], b = t[i + (k + 1)%3];
                if (a == b) return false;
                // A repeated directed edge is a fold or a third sheet on the edge.
                if (directed.put(a*n + b, 1) != null) return false;
            }
        }
        for (Long boxedKey : directed.keySet()) {
            final long key = boxedKey;
            if (!directed.containsKey((key%n)*n + key/n)) return false;
        }
        return !directed.isEmpty();
    }

    /** Signed volume enclosed by the triangles (positive when they face out). */
    static double enclosedVolume(TriangleMesh mesh)
    {
        final int[] t = mesh.getTriangleIndexesView();
        double volume = 0;
        for (int i = 0; i < t.length; i += 3) {
            final Vector3Dd a = vertex(mesh, t[i]), b = vertex(mesh, t[i + 1]), c = vertex(mesh, t[i + 2]);
            volume += a.dotProduct(b.crossProduct(c))/6.0;
        }
        return volume;
    }

    static double area(TriangleMesh mesh)
    {
        final int[] t = mesh.getTriangleIndexesView();
        double total = 0;
        for (int i = 0; i < t.length; i += 3) {
            final Vector3Dd a = vertex(mesh, t[i]), b = vertex(mesh, t[i + 1]), c = vertex(mesh, t[i + 2]);
            total += b.subtract(a).crossProduct(c.subtract(a)).length()/2.0;
        }
        return total;
    }

    /** Vertices minus edges plus triangles. */
    static long eulerCharacteristic(TriangleMesh mesh)
    {
        final int[] t = mesh.getTriangleIndexesView();
        final long n = mesh.getVertexPositionsView().length/3;
        TreeMap<Long, Integer> edges = new TreeMap<Long, Integer>();
        for (int i = 0; i < t.length; i += 3) {
            for (int k = 0; k < 3; k++) {
                long a = t[i + k], b = t[i + (k + 1)%3];
                if (a > b) { final long s = a; a = b; b = s; }
                edges.put(a*n + b, 1);
            }
        }
        return n - edges.size() + (long)(t.length/3);
    }
}
