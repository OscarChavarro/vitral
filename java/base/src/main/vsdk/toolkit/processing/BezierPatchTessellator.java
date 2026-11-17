package vsdk.toolkit.processing;

import java.util.ArrayList;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.surface.QuadMesh;
import vsdk.toolkit.environment.geometry.surface.SubdividedBezierPatch;
import vsdk.toolkit.environment.geometry.surface.TriangleMesh;

/**
Drawable triangle mesh of the Bezier surface of a patch's control net:
(uSteps + 1) x (vSteps + 1) points of the exact surface at uniform (u, v),
each with the unit normal S_u x S_v of the Bernstein derivatives (or, where
that vanishes, the mean of its triangles' normals) and (u, v) as texture
coordinates, joined by two triangles per parameter cell facing the u x v
side. It approximates the Bezier surface, not the patch's own subdivision,
which a ray tracer intersects instead: with the patch's uniform steps both
share the grid points, while the adaptive subdivisions place their triangles
elsewhere.
*/
public class BezierPatchTessellator {
    /** Largest accepted steps along u or v. */
    public static final int MAXIMUM_STEPS = 1024;

    // Cubic Bernstein polynomial k at t and its derivative.
    private static double bernstein(int k, double t)
    {
        final double s = 1.0 - t;
        if (k == 0) return s*s*s;
        if (k == 1) return 3.0*t*s*s;
        if (k == 2) return 3.0*t*t*s;
        return t*t*t;
    }

    private static double bernsteinDerivative(int k, double t)
    {
        final double s = 1.0 - t;
        if (k == 0) return -3.0*s*s;
        if (k == 1) return 3.0*s*s - 6.0*t*s;
        if (k == 2) return 6.0*t*s - 3.0*t*t;
        return 3.0*t*t;
    }

    /** Unit S_u x S_v at (u, v), or the zero vector where it vanishes. */
    public static Vector3Dd surfaceNormal(SubdividedBezierPatch patch, double u, double v)
    {
        Vector3Dd su = new Vector3Dd(0, 0, 0), sv = new Vector3Dd(0, 0, 0);
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                final Vector3Dd p = patch.getControlPoint(i, j);
                su = su.add(p.multiply(bernsteinDerivative(i, u)*bernstein(j, v)));
                sv = sv.add(p.multiply(bernstein(i, u)*bernsteinDerivative(j, v)));
            }
        }
        final Vector3Dd n = su.crossProduct(sv);
        final double size = n.length();
        if (!(size > 1e-300) || !Double.isFinite(size)) return new Vector3Dd(0, 0, 0);
        return n.multiply(1.0/size);
    }

    /**
    Returns a new mesh, or null when a step count is outside
    1..MAXIMUM_STEPS.
    */
    public static TriangleMesh tessellate(SubdividedBezierPatch patch, int uSteps, int vSteps)
    {
        if (uSteps < 1 || vSteps < 1 || uSteps > MAXIMUM_STEPS || vSteps > MAXIMUM_STEPS) return null;
        final int columns = vSteps + 1;
        final int vertexCount = (uSteps + 1)*columns;
        final int triangleCount = 2*uSteps*vSteps;
        TriangleMesh mesh = new TriangleMesh();
        mesh.initVertexPositionsArray(vertexCount);
        mesh.initVertexNormalsArray();
        mesh.initVertexUvsArray();
        double[] positions = mesh.getVertexPositions();
        double[] normals = mesh.getVertexNormals();
        double[] uvs = mesh.getVertexUvs();
        for (int i = 0; i <= uSteps; i++) {
            final double u = (double)i/uSteps;
            for (int j = 0; j <= vSteps; j++) {
                final double v = (double)j/vSteps;
                final int index = i*columns + j;
                final Vector3Dd p = patch.evaluate(u, v);
                final Vector3Dd n = surfaceNormal(patch, u, v);
                positions[index*3] = p.x();
                positions[index*3 + 1] = p.y();
                positions[index*3 + 2] = p.z();
                normals[index*3] = n.x();
                normals[index*3 + 1] = n.y();
                normals[index*3 + 2] = n.z();
                uvs[index*2] = u;
                uvs[index*2 + 1] = v;
            }
        }
        mesh.initTriangleArrays(triangleCount);
        int[] indices = mesh.getTriangleIndexes();
        double[] faceNormals = mesh.getTriangleNormals();
        ArrayList<Vector3Dd> faceSums = new ArrayList<Vector3Dd>(vertexCount);
        for (int index = 0; index < vertexCount; index++) faceSums.add(new Vector3Dd(0, 0, 0));
        int t = 0;
        for (int i = 0; i < uSteps; i++) {
            for (int j = 0; j < vSteps; j++) {
                final int a = i*columns + j, b = (i + 1)*columns + j;
                final int c = (i + 1)*columns + j + 1, d = i*columns + j + 1;
                // (a, b, c) runs along +u then +v: its normal is u x v.
                final int[][] corners = {{a, b, c}, {a, c, d}};
                for (int h = 0; h < 2; h++, t++) {
                    Vector3Dd[] p = new Vector3Dd[3];
                    for (int k = 0; k < 3; k++) {
                        final int v = corners[h][k];
                        indices[t*3 + k] = v;
                        p[k] = new Vector3Dd(positions[v*3], positions[v*3 + 1], positions[v*3 + 2]);
                    }
                    Vector3Dd n = p[1].subtract(p[0]).crossProduct(p[2].subtract(p[0]));
                    final double size = n.length();
                    if (size > 0) n = n.multiply(1.0/size);
                    faceNormals[t*3] = n.x();
                    faceNormals[t*3 + 1] = n.y();
                    faceNormals[t*3 + 2] = n.z();
                    for (int k = 0; k < 3; k++) faceSums.set(corners[h][k], faceSums.get(corners[h][k]).add(n));
                }
            }
        }
        // Where S_u x S_v vanishes (a collapsed edge), use the triangles' normals.
        for (int index = 0; index < vertexCount; index++) {
            if (normals[index*3] != 0 || normals[index*3 + 1] != 0 || normals[index*3 + 2] != 0) continue;
            final double size = faceSums.get(index).length();
            if (!(size > 0)) continue;
            final Vector3Dd n = faceSums.get(index).multiply(1.0/size);
            normals[index*3] = n.x();
            normals[index*3 + 1] = n.y();
            normals[index*3 + 2] = n.z();
        }
        return mesh;
    }

    /**
    Same grid of (uSteps + 1) x (vSteps + 1) points of the Bezier surface as
    `tessellate`, joined by one quadrilateral per parameter cell (without the
    diagonals of the triangles), with positions only: the lines of its edges
    are the curves of constant u and v of the surface.
    @return a new mesh, or null when a step count is outside
    1..MAXIMUM_STEPS
    */
    public static QuadMesh tessellateQuads(SubdividedBezierPatch patch, int uSteps, int vSteps)
    {
        if (uSteps < 1 || vSteps < 1 || uSteps > MAXIMUM_STEPS || vSteps > MAXIMUM_STEPS) return null;
        final int columns = vSteps + 1;
        QuadMesh mesh = new QuadMesh();
        mesh.initVertexPositionsArray((uSteps + 1)*columns);
        double[] positions = mesh.getVertexPositions();
        for (int i = 0; i <= uSteps; i++) {
            final double u = (double)i/uSteps;
            for (int j = 0; j <= vSteps; j++) {
                final int index = i*columns + j;
                final Vector3Dd p = patch.evaluate(u, (double)j/vSteps);
                positions[index*3] = p.x();
                positions[index*3 + 1] = p.y();
                positions[index*3 + 2] = p.z();
            }
        }
        mesh.initQuadArrays(uSteps*vSteps);
        int q = 0;
        for (int i = 0; i < uSteps; i++) {
            for (int j = 0; j < vSteps; j++, q++) {
                mesh.setQuadAt(q, i*columns + j, (i + 1)*columns + j,
                    (i + 1)*columns + j + 1, i*columns + j + 1);
            }
        }
        return mesh;
    }
}
