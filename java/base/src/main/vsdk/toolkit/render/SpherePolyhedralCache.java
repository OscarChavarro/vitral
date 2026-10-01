package vsdk.toolkit.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.volume.Sphere;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;

/**
Tessellation cache shared by the `*SphereRenderer`s of every rendering
technology (JOGL2, JOGL4, software...), independent of all of them.

A `Sphere` is kept by its renderers as a parametric surface; to draw it, it is
converted, in code, into a `PolyhedralBoundedSolid` with the parametric
construction of `Sphere.exportToPolyhedralBoundedSolid(meridians, parallels)`,
and that solid is the cached representation: besides being drawn by the
rasterizers, it can be used by other drawing algorithms that are not
OpenGL based (hidden line removal, software renderers...), which a plain mesh
does not allow.

For the rasterizers, each entry also derives from the (triangular) faces of
the solid the per vertex attributes a smooth, textured and bump mapped sphere
needs, which the faceted solid does not carry: for each vertex, its spherical
coordinates (theta, phi) are recovered from its position, and the normal,
tangent and binormal are evaluated on the parametric surface
(`Sphere.sphereNormal`, `sphereTangent`, `sphereBinormal`), with texture
coordinates (1 - theta / (2 PI), (phi + PI / 2) / PI). Along the seam of the
parametrization (theta = 0) the texture coordinates of a triangle are made
continuous, and at the poles (where theta is undefined) the vertex takes the
mean theta of the other corners of its triangle.

Entries are kept by radius and resolution, so many spheres of a scene share
them.
*/
public final class SpherePolyhedralCache
{
    /// Maximum number of entries kept; when reached, the cache is emptied
    private static final int MAX_CACHED_ENTRIES = 256;
    /// Relative distance to a pole under which a vertex is taken as a pole
    private static final double POLE_TOLERANCE = 1.0e-9;

    private static final Map<String, Entry> ENTRIES = new HashMap<String, Entry>();

    /**
    A sphere converted into a polyhedral bounded solid, with the attributes
    of its triangles for the rasterizers: three consecutive vertices make a
    triangle, counterclockwise seen from outside.
    */
    public static final class Entry
    {
        private final double radius;
        private final int meridians;
        private final int parallels;
        private final PolyhedralBoundedSolid solid;
        private final float[] positions;
        private final float[] normals;
        private final float[] uvs;
        private final float[] tangents;
        private final float[] biNormals;

        private Entry(double radius, int meridians, int parallels,
                      PolyhedralBoundedSolid solid, float[] positions,
                      float[] normals, float[] uvs, float[] tangents,
                      float[] biNormals)
        {
            this.radius = radius;
            this.meridians = meridians;
            this.parallels = parallels;
            this.solid = solid;
            this.positions = positions;
            this.normals = normals;
            this.uvs = uvs;
            this.tangents = tangents;
            this.biNormals = biNormals;
        }

        /**
        @return radius of the sphere
        */
        public double getRadius()
        {
            return radius;
        }

        /**
        @return number of meridians of the tessellation
        */
        public int getMeridians()
        {
            return meridians;
        }

        /**
        @return number of parallels (bands between the poles) of the
        tessellation
        */
        public int getParallels()
        {
            return parallels;
        }

        /**
        @return the sphere as a polyhedral bounded solid, shared by every
        user of the entry: it must not be modified
        */
        public PolyhedralBoundedSolid getSolid()
        {
            return solid;
        }

        /**
        @return number of vertices of the triangles
        */
        public int getVertexCount()
        {
            return positions.length / 3;
        }

        /**
        @return x, y, z of each vertex of the triangles
        */
        public float[] getPositions()
        {
            return positions;
        }

        /**
        @return x, y, z of the normal of the sphere at each vertex
        */
        public float[] getNormals()
        {
            return normals;
        }

        /**
        @return u, v texture coordinates of each vertex
        */
        public float[] getUvs()
        {
            return uvs;
        }

        /**
        @return x, y, z of the tangent of the sphere at each vertex
        */
        public float[] getTangents()
        {
            return tangents;
        }

        /**
        @return x, y, z of the binormal of the sphere at each vertex
        */
        public float[] getBiNormals()
        {
            return biNormals;
        }
    }

    private SpherePolyhedralCache()
    {
    }

    /**
    @param sphere sphere to tessellate
    @param meridians number of meridians
    @param parallels number of bands between the poles
    @return the cached tessellation of a sphere of that radius, created the
    first time it is asked for
    */
    public static synchronized Entry obtain(Sphere sphere, int meridians, int parallels)
    {
        String key = sphere.getRadius() + "/" + meridians + "/" + parallels;
        Entry entry = ENTRIES.get(key);

        if ( entry == null ) {
            entry = build(sphere, meridians, parallels);
            if ( ENTRIES.size() >= MAX_CACHED_ENTRIES ) {
                ENTRIES.clear();
            }
            ENTRIES.put(key, entry);
        }
        return entry;
    }

    /**
    Forgets every entry.
    */
    public static synchronized void clear()
    {
        ENTRIES.clear();
    }

    private static Entry build(Sphere sphere, int meridians, int parallels)
    {
        PolyhedralBoundedSolid solid =
            sphere.exportToPolyhedralBoundedSolid(meridians, parallels);
        List<Vector3Dd> triangles = collectTriangles(solid);
        int vertexCount = triangles.size();
        float[] positions = new float[vertexCount * 3];
        float[] normals = new float[vertexCount * 3];
        float[] uvs = new float[vertexCount * 2];
        float[] tangents = new float[vertexCount * 3];
        float[] biNormals = new float[vertexCount * 3];
        double radius = sphere.getRadius();

        for ( int t = 0; t + 2 < vertexCount; t += 3 ) {
            double[] theta = new double[3];
            double[] phi = new double[3];
            boolean[] pole = new boolean[3];

            for ( int k = 0; k < 3; k++ ) {
                Vector3Dd p = triangles.get(t + k);
                double z = radius > 0 ? p.z() / radius : 0;

                phi[k] = Math.asin(Math.max(-1.0, Math.min(1.0, z)));
                pole[k] = Math.abs(Math.abs(z) - 1.0) < POLE_TOLERANCE;
                theta[k] = Math.atan2(-p.y(), p.x());
                if ( theta[k] < 0 ) {
                    theta[k] += 2 * Math.PI;
                }
            }
            makeContinuous(theta, pole);

            for ( int k = 0; k < 3; k++ ) {
                int i = t + k;
                Vector3Dd p = triangles.get(i);
                Vector3Dd n = sphere.sphereNormal(theta[k], phi[k]);
                Vector3Dd tangent = sphere.sphereTangent(theta[k], phi[k]);
                Vector3Dd biNormal = sphere.sphereBinormal(theta[k], phi[k]);

                put(positions, i, p);
                put(normals, i, n);
                put(tangents, i, tangent);
                put(biNormals, i, biNormal);
                uvs[2 * i] = (float)(1.0 - theta[k] / (2 * Math.PI));
                uvs[2 * i + 1] = (float)((phi[k] + Math.PI / 2) / Math.PI);
            }
        }
        return new Entry(radius, meridians, parallels, solid, positions,
            normals, uvs, tangents, biNormals);
    }

    /**
    Makes the angles of the corners of a triangle continuous across the seam
    (a triangle whose angles span more than half a turn crosses it, so the
    small ones are taken one turn further), and gives the poles the mean
    angle of the other corners.
    */
    private static void makeContinuous(double[] theta, boolean[] pole)
    {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;

        for ( int k = 0; k < 3; k++ ) {
            if ( !pole[k] ) {
                min = Math.min(min, theta[k]);
                max = Math.max(max, theta[k]);
            }
        }
        if ( max - min > Math.PI ) {
            for ( int k = 0; k < 3; k++ ) {
                if ( !pole[k] && theta[k] < Math.PI ) {
                    theta[k] += 2 * Math.PI;
                }
            }
        }

        double sum = 0;
        int count = 0;

        for ( int k = 0; k < 3; k++ ) {
            if ( !pole[k] ) {
                sum += theta[k];
                count++;
            }
        }
        for ( int k = 0; k < 3; k++ ) {
            if ( pole[k] ) {
                theta[k] = count > 0 ? sum / count : 0;
            }
        }
    }

    /**
    @return the vertices of the faces of the solid as triangles (a fan per
    face, from its outer loop), three consecutive per triangle, each one
    counterclockwise seen from outside the sphere
    */
    private static List<Vector3Dd> collectTriangles(PolyhedralBoundedSolid solid)
    {
        ArrayList<Vector3Dd> triangles = new ArrayList<Vector3Dd>();

        for ( int f = 0; f < solid.getPolygonsList().size(); f++ ) {
            _PolyhedralBoundedSolidFace face = solid.getPolygonsList().get(f);

            if ( face.boundariesList.size() < 1 ) {
                continue;
            }
            _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(0);
            _PolyhedralBoundedSolidHalfEdge start = loop.boundaryStartHalfEdge;
            ArrayList<Vector3Dd> polygon = new ArrayList<Vector3Dd>();

            if ( start == null ) {
                continue;
            }
            _PolyhedralBoundedSolidHalfEdge he = start;
            do {
                polygon.add(he.startingVertex.position);
                he = he.next();
            } while ( he != start && he != null );

            for ( int i = 1; i + 1 < polygon.size(); i++ ) {
                Vector3Dd a = polygon.get(0);
                Vector3Dd b = polygon.get(i);
                Vector3Dd c = polygon.get(i + 1);
                Vector3Dd normal = b.subtract(a).crossProduct(c.subtract(a));
                Vector3Dd center = a.add(b).add(c);

                if ( normal.dotProduct(center) >= 0 ) {
                    triangles.add(a);
                    triangles.add(b);
                    triangles.add(c);
                }
                else {
                    triangles.add(a);
                    triangles.add(c);
                    triangles.add(b);
                }
            }
        }
        return triangles;
    }

    private static void put(float[] array, int index, Vector3Dd v)
    {
        array[3 * index] = (float)v.x();
        array[3 * index + 1] = (float)v.y();
        array[3 * index + 2] = (float)v.z();
    }
}
