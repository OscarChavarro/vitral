package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import java.util.ArrayList;
import java.util.List;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.volume.Box;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;

/**
Independent test oracle for boolean and split results: it knows nothing
about the half-edge pipeline and only uses (a) the divergence-theorem volume
of a B-Rep and (b) the exact volume of an intersection of convex half-space
sets.

<p>For two convex operands A and B with intersection volume I:
vol(A u B) = vol(A) + vol(B) - I, vol(A n B) = I, vol(A \ B) = vol(A) - I.
This turns every box/box (or convex/convex) boolean into a test with a
known numeric answer, independent of topology counts, which is the missing
complement of the fixture-snapshot tests (faces/edges/vertices/bbox).</p>

<p>Traceability: the B-Rep volume is the integral property of [MANT1988]
Sec. 13.3 (Program 13.9 avolume), generalized to faces with rings.</p>
 */
final class PolyhedralBoundedSolidVolumeOracle
{
    /** One oriented box: half-space set plus the recipe to build its B-Rep. */
    static final class OrientedBox
    {
        final double[] size;
        final Matrix4x4d transform;

        OrientedBox(double sx, double sy, double sz, Matrix4x4d transform)
        {
            this.size = new double[] { sx, sy, sz };
            this.transform = transform;
        }

        PolyhedralBoundedSolid toSolid()
        {
            PolyhedralBoundedSolid solid = new Box(
                new Vector3Dd(size[0], size[1], size[2]))
                    .exportToPolyhedralBoundedSolid();
            PolyhedralBoundedSolidModeler.applyTransformation(solid,
                transform);
            return solid;
        }

        /** Planes as {nx, ny, nz, d} meaning n.x <= d, n unit outward. */
        List<double[]> halfSpaces()
        {
            ArrayList<double[]> planes = new ArrayList<>();
            Vector3Dd origin = transform.multiply(new Vector3Dd(0, 0, 0));
            Vector3Dd[] axes = {
                transform.multiply(new Vector3Dd(1, 0, 0)).subtract(origin),
                transform.multiply(new Vector3Dd(0, 1, 0)).subtract(origin),
                transform.multiply(new Vector3Dd(0, 0, 1)).subtract(origin)
            };
            int i;

            for ( i = 0; i < 3; i++ ) {
                double stretch = axes[i].length();
                Vector3Dd n = axes[i].normalized();
                double center = n.dotProduct(origin);
                double half = stretch * size[i] / 2.0;
                planes.add(new double[] {
                    n.x(), n.y(), n.z(), center + half });
                planes.add(new double[] {
                    -n.x(), -n.y(), -n.z(), -center + half });
            }
            return planes;
        }

        double volume()
        {
            return convexVolume(halfSpaces());
        }
    }

    private PolyhedralBoundedSolidVolumeOracle()
    {
    }

    static Matrix4x4d translation(double x, double y, double z)
    {
        return new Matrix4x4d().translation(x, y, z);
    }

    static Matrix4x4d rotation(double angle, double x, double y, double z)
    {
        return new Matrix4x4d().axisRotation(angle, x, y, z);
    }

    static double intersectionVolume(OrientedBox a, OrientedBox b)
    {
        ArrayList<double[]> all = new ArrayList<>(a.halfSpaces());
        all.addAll(b.halfSpaces());
        return convexVolume(all);
    }

    /** Expected volume for op in {UNION, INTERSECTION, SUBTRACT} (A op B). */
    static double expectedVolume(OrientedBox a, OrientedBox b, int op)
    {
        double i = intersectionVolume(a, b);

        if ( op == PolyhedralBoundedSolidModeler.UNION ) {
            return a.volume() + b.volume() - i;
        }
        if ( op == PolyhedralBoundedSolidModeler.INTERSECTION ) {
            return i;
        }
        return a.volume() - i;
    }

    /**
    Volume of the convex polytope { x : n_k.x <= d_k }: each plane is
    clipped against all others (Sutherland-Hodgman) and the face areas are
    accumulated with the divergence theorem. Duplicate planes are merged.
    */
    static double convexVolume(List<double[]> input)
    {
        ArrayList<double[]> planes = new ArrayList<>();
        double volume = 0.0;
        int k;

        for ( double[] p : input ) {
            boolean duplicate = false;
            for ( double[] q : planes ) {
                if ( Math.abs(p[0] - q[0]) < 1e-12 &&
                     Math.abs(p[1] - q[1]) < 1e-12 &&
                     Math.abs(p[2] - q[2]) < 1e-12 &&
                     Math.abs(p[3] - q[3]) < 1e-12 ) {
                    duplicate = true;
                }
            }
            if ( !duplicate ) {
                planes.add(p);
            }
        }

        for ( k = 0; k < planes.size(); k++ ) {
            double[] p = planes.get(k);
            Vector3Dd n = new Vector3Dd(p[0], p[1], p[2]);
            Vector3Dd helper = Math.abs(p[0]) < 0.9
                ? new Vector3Dd(1, 0, 0) : new Vector3Dd(0, 1, 0);
            Vector3Dd u = n.crossProduct(helper).normalized();
            Vector3Dd v = n.crossProduct(u);
            Vector3Dd c = n.multiply(p[3]);
            double big = 1.0e4;
            ArrayList<Vector3Dd> polygon = new ArrayList<>();
            int j;

            polygon.add(c.add(u.multiply(big)).add(v.multiply(big)));
            polygon.add(c.add(u.multiply(-big)).add(v.multiply(big)));
            polygon.add(c.add(u.multiply(-big)).add(v.multiply(-big)));
            polygon.add(c.add(u.multiply(big)).add(v.multiply(-big)));

            for ( j = 0; j < planes.size() && !polygon.isEmpty(); j++ ) {
                if ( j == k ) {
                    continue;
                }
                double[] q = planes.get(j);
                Vector3Dd qn = new Vector3Dd(q[0], q[1], q[2]);
                ArrayList<Vector3Dd> clipped = new ArrayList<>();
                int i;

                for ( i = 0; i < polygon.size(); i++ ) {
                    Vector3Dd a = polygon.get(i);
                    Vector3Dd b = polygon.get((i + 1) % polygon.size());
                    double da = qn.dotProduct(a) - q[3];
                    double db = qn.dotProduct(b) - q[3];

                    if ( da <= 0 ) {
                        clipped.add(a);
                    }
                    if ( (da < 0 && db > 0) || (da > 0 && db < 0) ) {
                        clipped.add(a.add(
                            b.subtract(a).multiply(da / (da - db))));
                    }
                }
                polygon = clipped;
            }
            if ( polygon.size() < 3 ) {
                continue;
            }

            Vector3Dd areaVector = new Vector3Dd(0, 0, 0);
            int i;
            for ( i = 0; i < polygon.size(); i++ ) {
                areaVector = areaVector.add(
                    polygon.get(i).subtract(c).crossProduct(
                        polygon.get((i + 1) % polygon.size()).subtract(c)));
            }
            volume += p[3] * Math.abs(areaVector.dotProduct(n)) / 6.0;
        }
        return volume;
    }

    /**
    Unsigned volume of a B-Rep by the divergence theorem. Rings contribute
    with their own (opposite) orientation, so faces with holes need no
    triangulation. Zero-area laminae contribute nothing, which is why a
    volume check alone is not sufficient and must be paired with strict
    validation.
    */
    static double solidVolume(PolyhedralBoundedSolid solid)
    {
        double volume = 0.0;
        int i;

        for ( i = 0; i < solid.getPolygonsList().size(); i++ ) {
            _PolyhedralBoundedSolidFace face = solid.getPolygonsList().get(i);
            Vector3Dd areaVector = new Vector3Dd(0, 0, 0);
            Vector3Dd anchor = null;
            int j;

            for ( j = 0; j < face.boundariesList.size(); j++ ) {
                _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(j);
                int n = loop.halfEdgesList.size();
                int k;

                for ( k = 0; k < n; k++ ) {
                    Vector3Dd a =
                        loop.halfEdgesList.get(k).startingVertex.position;
                    Vector3Dd b = loop.halfEdgesList.get((k + 1) % n)
                        .startingVertex.position;
                    if ( anchor == null ) {
                        anchor = a;
                    }
                    areaVector = areaVector.add(a.crossProduct(b));
                }
            }
            if ( anchor != null ) {
                volume += anchor.dotProduct(areaVector) / 6.0;
            }
        }
        return Math.abs(volume);
    }
}
