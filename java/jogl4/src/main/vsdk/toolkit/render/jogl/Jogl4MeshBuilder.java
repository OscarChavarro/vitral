package vsdk.toolkit.render.jogl;

import java.util.ArrayList;
import java.util.List;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;

/**
Accumulates the triangles of a tessellated surface and builds a
`Jogl4MeshRenderer.Mesh` from them. Tangents and binormals are derived from
the normal of each vertex.
*/
final class Jogl4MeshBuilder {
    private final List<float[]> vertices = new ArrayList<>();
    private double characteristicSize;

    Jogl4MeshBuilder(double characteristicSize)
    {
        this.characteristicSize = characteristicSize;
    }

    /**
    Adds one triangle; vertices are given counterclockwise seen from the side
    the normals point to.
    */
    void addTriangle(
        Vector3Dd p0, Vector3Dd n0, double u0, double v0,
        Vector3Dd p1, Vector3Dd n1, double u1, double v1,
        Vector3Dd p2, Vector3Dd n2, double u2, double v2)
    {
        addVertex(p0, n0, u0, v0);
        addVertex(p1, n1, u1, v1);
        addVertex(p2, n2, u2, v2);
    }

    /**
    Adds a quad as two triangles; vertices are given counterclockwise seen from
    the side the normals point to.
    */
    void addQuad(
        Vector3Dd p0, Vector3Dd n0, double u0, double v0,
        Vector3Dd p1, Vector3Dd n1, double u1, double v1,
        Vector3Dd p2, Vector3Dd n2, double u2, double v2,
        Vector3Dd p3, Vector3Dd n3, double u3, double v3)
    {
        addTriangle(p0, n0, u0, v0, p1, n1, u1, v1, p2, n2, u2, v2);
        addTriangle(p0, n0, u0, v0, p2, n2, u2, v2, p3, n3, u3, v3);
    }

    /**
    Adds the side of a frustum of cone around the Z axis.

    @param z0 height of the first ring
    @param r0 radius of the first ring
    @param z1 height of the second ring
    @param r1 radius of the second ring
    @param slices number of divisions around the axis
    */
    void addFrustum(double z0, double r0, double z1, double r1, int slices)
    {
        double dz = z1 - z0;
        double dr = r0 - r1;
        double normalLength = Math.sqrt(dz * dz + dr * dr);

        if ( normalLength < 1e-12 ) {
            return;
        }
        double nz = dr / normalLength;
        double nr = dz / normalLength;

        for ( int i = 0; i < slices; i++ ) {
            double a0 = 2 * Math.PI * i / slices;
            double a1 = 2 * Math.PI * (i + 1) / slices;
            double c0 = Math.cos(a0);
            double s0 = Math.sin(a0);
            double c1 = Math.cos(a1);
            double s1 = Math.sin(a1);
            Vector3Dd n0 = new Vector3Dd(nr * c0, nr * s0, nz);
            Vector3Dd n1 = new Vector3Dd(nr * c1, nr * s1, nz);
            double u0 = (double)i / slices;
            double u1 = (double)(i + 1) / slices;

            if ( r1 < 1e-12 ) {
                Vector3Dd apex = new Vector3Dd(0, 0, z1);
                Vector3Dd nApex = new Vector3Dd(nr * Math.cos((a0 + a1) / 2), nr * Math.sin((a0 + a1) / 2), nz);
                addTriangle(
                    new Vector3Dd(r0 * c0, r0 * s0, z0), n0, u0, 0,
                    new Vector3Dd(r0 * c1, r0 * s1, z0), n1, u1, 0,
                    apex, nApex, (u0 + u1) / 2, 1);
            }
            else {
                addQuad(
                    new Vector3Dd(r0 * c0, r0 * s0, z0), n0, u0, 0,
                    new Vector3Dd(r0 * c1, r0 * s1, z0), n1, u1, 0,
                    new Vector3Dd(r1 * c1, r1 * s1, z1), n1, u1, 1,
                    new Vector3Dd(r1 * c0, r1 * s0, z1), n0, u0, 1);
            }
        }
    }

    /**
    Adds a flat ring (or disk, when the inner radius is zero) perpendicular
    to the Z axis.

    @param z height of the ring
    @param innerRadius inner radius
    @param outerRadius outer radius
    @param facingUp true if the normal is +Z, false for -Z
    @param slices number of divisions around the axis
    */
    void addDisk(double z, double innerRadius, double outerRadius, boolean facingUp, int slices)
    {
        Vector3Dd n = new Vector3Dd(0, 0, facingUp ? 1 : -1);

        for ( int i = 0; i < slices; i++ ) {
            double a0 = 2 * Math.PI * i / slices;
            double a1 = 2 * Math.PI * (i + 1) / slices;
            double c0 = Math.cos(a0);
            double s0 = Math.sin(a0);
            double c1 = Math.cos(a1);
            double s1 = Math.sin(a1);
            Vector3Dd o0 = new Vector3Dd(outerRadius * c0, outerRadius * s0, z);
            Vector3Dd o1 = new Vector3Dd(outerRadius * c1, outerRadius * s1, z);
            double uo0 = 0.5 + 0.5 * c0;
            double vo0 = 0.5 + 0.5 * s0;
            double uo1 = 0.5 + 0.5 * c1;
            double vo1 = 0.5 + 0.5 * s1;

            if ( innerRadius < 1e-12 ) {
                Vector3Dd center = new Vector3Dd(0, 0, z);
                if ( facingUp ) {
                    addTriangle(center, n, 0.5, 0.5, o0, n, uo0, vo0, o1, n, uo1, vo1);
                }
                else {
                    addTriangle(center, n, 0.5, 0.5, o1, n, uo1, vo1, o0, n, uo0, vo0);
                }
            }
            else {
                Vector3Dd i0 = new Vector3Dd(innerRadius * c0, innerRadius * s0, z);
                Vector3Dd i1 = new Vector3Dd(innerRadius * c1, innerRadius * s1, z);
                if ( facingUp ) {
                    addQuad(i0, n, 0.5, 0.5, o0, n, uo0, vo0, o1, n, uo1, vo1, i1, n, 0.5, 0.5);
                }
                else {
                    addQuad(i0, n, 0.5, 0.5, i1, n, 0.5, 0.5, o1, n, uo1, vo1, o0, n, uo0, vo0);
                }
            }
        }
    }

    private void addVertex(Vector3Dd p, Vector3Dd n, double u, double v)
    {
        Vector3Dd normal = n.normalized();
        Vector3Dd reference = Math.abs(normal.z()) < 0.9 ? new Vector3Dd(0, 0, 1) : new Vector3Dd(1, 0, 0);
        Vector3Dd tangent = reference.crossProduct(normal).normalized();
        Vector3Dd binormal = normal.crossProduct(tangent).normalized();

        vertices.add(new float[] {
            (float)p.x(), (float)p.y(), (float)p.z(),
            (float)normal.x(), (float)normal.y(), (float)normal.z(),
            (float)u, (float)v,
            (float)tangent.x(), (float)tangent.y(), (float)tangent.z(),
            (float)binormal.x(), (float)binormal.y(), (float)binormal.z()
        });
    }

    Jogl4MeshRenderer.Mesh build()
    {
        int count = vertices.size();
        float[] positions = new float[count * 3];
        float[] normals = new float[count * 3];
        float[] uvs = new float[count * 2];
        float[] tangents = new float[count * 3];
        float[] biNormals = new float[count * 3];

        for ( int i = 0; i < count; i++ ) {
            float[] v = vertices.get(i);

            System.arraycopy(v, 0, positions, i * 3, 3);
            System.arraycopy(v, 3, normals, i * 3, 3);
            System.arraycopy(v, 6, uvs, i * 2, 2);
            System.arraycopy(v, 8, tangents, i * 3, 3);
            System.arraycopy(v, 11, biNormals, i * 3, 3);
        }
        return new Jogl4MeshRenderer.Mesh(positions, normals, uvs, tangents, biNormals,
            characteristicSize);
    }
}
