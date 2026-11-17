//= References:                                                             =
//= [BLOO1988] Bloomenthal, Jules. "Polygonization of implicit surfaces".   =
//=     Computer Aided Geometric Design 5(4), 1988.                         =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.ToDoubleFunction;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;

/**
Polyhedral approximations of solids given by a scalar field: the solid is the
region where the field is negative. The field is sampled on a regular grid,
and its zero level is polygonized by marching tetrahedra [BLOO1988]: each
grid cell is split into six tetrahedra sharing its main diagonal (the same
split in all the cells, so neighboring cells agree on their common faces),
and the field is taken as linear inside each tetrahedron. That gives one
triangle or one planar quadrilateral per tetrahedron crossed by the surface,
and a closed 2-manifold surface: the nodes on the border of the grid are taken
as outside, so a solid that does not fit in the grid is cut by it (with flat
caps half a cell inside the border).

The polygons are given to `PolyhedralBoundedSolidMeshConstructions`, so the
boundary representation is built only with Euler operators [MANT1988], for
any genus of the surface.
*/
public class PolyhedralBoundedSolidFieldConstructions
{
    /// Interpolated vertices are kept this fraction of an edge away from its
    /// nodes, so polygons never degenerate to points or lines
    private static final double MINIMUM_EDGE_FRACTION = 0.001;
    /// The grid is shifted this fraction of a cell (an irrational number), so
    /// its nodes do not fall exactly on simple surfaces, which would give
    /// coincident vertices
    private static final double GRID_SHIFT = 0.5 * (Math.sqrt(5.0) - 2.0);
    /// A quadrilateral whose fourth vertex is farther than this (relative to
    /// its size) from the plane of the other three is split in triangles
    private static final double PLANARITY_TOLERANCE = 1.0e-9;

    /// A node whose field is closer to zero than this fraction of the largest
    /// difference with its neighbors gets this value instead (keeping its
    /// sign): the surface stays that fraction of an edge away from it, so no
    /// sliver polygons appear around nodes the surface passes close to
    private static final double NODE_SEPARATION = 0.05;
    /// Offsets to the neighbors of a node along the edges of the tetrahedra
    private static final int[][] TETRAHEDRA_NEIGHBORS = {
        {1, 0, 0}, {0, 1, 0}, {0, 0, 1}, {1, 1, 0}, {1, 0, 1}, {0, 1, 1}, {1, 1, 1},
        {-1, 0, 0}, {0, -1, 0}, {0, 0, -1}, {-1, -1, 0}, {-1, 0, -1}, {0, -1, -1},
        {-1, -1, -1}
    };

    /// The six tetrahedra of a cell, as corners of the cell (bit 0: +x, bit
    /// 1: +y, bit 2: +z): paths from corner 0 to corner 7 along the axes
    private static final int[][] TETRAHEDRA = {
        {0, 1, 3, 7}, {0, 1, 5, 7}, {0, 2, 3, 7},
        {0, 2, 6, 7}, {0, 4, 5, 7}, {0, 4, 6, 7}
    };

    /**
    Samples a field over a box and builds the solid where it is negative.
    @param field scalar field
    @param minimum minimum corner of the box
    @param maximum maximum corner of the box
    @param divisions number of grid cells along the largest side of the box
    (cells are cubes)
    @return the polyhedral approximation of the solid inside the box, or null
    if it is empty
    */
    public static PolyhedralBoundedSolid createFromScalarField(
        ToDoubleFunction<Vector3Dd> field, Vector3Dd minimum, Vector3Dd maximum,
        int divisions)
    {
        Vector3Dd size = maximum.subtract(minimum);
        double largest = Math.max(size.x(), Math.max(size.y(), size.z()));
        if ( !(largest > 0) || divisions < 1 ) {
            return null;
        }
        double h = largest / divisions;
        int nodesX = Math.max(2, (int)Math.ceil(size.x() / h - 1.0e-9) + 1);
        int nodesY = Math.max(2, (int)Math.ceil(size.y() / h - 1.0e-9) + 1);
        int nodesZ = Math.max(2, (int)Math.ceil(size.z() / h - 1.0e-9) + 1);
        // Grid centered on the box, slightly shifted
        Vector3Dd origin = new Vector3Dd(
            minimum.x() + 0.5 * (size.x() - (nodesX - 1) * h) + GRID_SHIFT * h,
            minimum.y() + 0.5 * (size.y() - (nodesY - 1) * h) + GRID_SHIFT * h,
            minimum.z() + 0.5 * (size.z() - (nodesZ - 1) * h) + GRID_SHIFT * h);
        double[] values = new double[nodesX * nodesY * nodesZ];

        for ( int k = 0; k < nodesZ; k++ ) {
            for ( int j = 0; j < nodesY; j++ ) {
                for ( int i = 0; i < nodesX; i++ ) {
                    values[i + nodesX * (j + nodesY * k)] = field.applyAsDouble(
                        new Vector3Dd(origin.x() + i * h, origin.y() + j * h,
                            origin.z() + k * h));
                }
            }
        }
        return createFromSampledField(nodesX, nodesY, nodesZ, origin,
            new Vector3Dd(h, h, h), values);
    }

    /**
    Samples a field over a box enlarged by a margin of a cell and a half on
    each side, so a solid that fits in the box is not cut by the grid.
    @param field scalar field
    @param minimum minimum corner of a box containing the solid
    @param maximum maximum corner of a box containing the solid
    @param divisions number of grid cells along the largest side of the box
    @return the polyhedral approximation of the solid, or null if it is empty
    */
    public static PolyhedralBoundedSolid createFromBoundedScalarField(
        ToDoubleFunction<Vector3Dd> field, Vector3Dd minimum, Vector3Dd maximum,
        int divisions)
    {
        Vector3Dd size = maximum.subtract(minimum);
        double largest = Math.max(size.x(), Math.max(size.y(), size.z()));
        if ( !(largest > 0) || divisions < 1 ) {
            return null;
        }
        double margin = 1.5 * largest / divisions;
        Vector3Dd m = new Vector3Dd(margin, margin, margin);
        return createFromScalarField(field, minimum.subtract(m), maximum.add(m),
            divisions + 3);
    }

    /**
    Builds the solid where a sampled field is negative.
    @param nodesX number of grid nodes along X (at least 2)
    @param nodesY number of grid nodes along Y (at least 2)
    @param nodesZ number of grid nodes along Z (at least 2)
    @param origin position of node (0, 0, 0)
    @param spacing distance between nodes along each axis
    @param values field at each node (i, j, k), at index i + nodesX*(j + nodesY*k);
    nodes on the border of the grid are taken as outside
    @return the polyhedral approximation of the solid, or null if it is empty
    */
    public static PolyhedralBoundedSolid createFromSampledField(
        int nodesX, int nodesY, int nodesZ, Vector3Dd origin, Vector3Dd spacing,
        double[] values)
    {
        return new _Polygonizer(nodesX, nodesY, nodesZ, origin, spacing, values).build();
    }

    private static final class _Polygonizer
    {
        private final int nodesX;
        private final int nodesY;
        private final int nodesZ;
        private final Vector3Dd origin;
        private final Vector3Dd spacing;
        private final double[] values;
        private final ArrayList<Vector3Dd> positions = new ArrayList<Vector3Dd>();
        private final ArrayList<int[]> polygons = new ArrayList<int[]>();
        private final HashMap<Long, Integer> edgeVertices = new HashMap<Long, Integer>();

        _Polygonizer(int nodesX, int nodesY, int nodesZ, Vector3Dd origin,
            Vector3Dd spacing, double[] values)
        {
            this.nodesX = nodesX;
            this.nodesY = nodesY;
            this.nodesZ = nodesZ;
            this.origin = origin;
            this.spacing = spacing;
            this.values = separateFromZero(values);
        }

        /**
        @return the values, with the ones of the nodes too close to zero
        moved away from it (see NODE_SEPARATION)
        */
        private double[] separateFromZero(double[] original)
        {
            double[] separated = original.clone();
            for ( int k = 1; k < nodesZ - 1; k++ ) {
                for ( int j = 1; j < nodesY - 1; j++ ) {
                    for ( int i = 1; i < nodesX - 1; i++ ) {
                        int node = nodeIndex(i, j, k);
                        double v = original[node];
                        if ( !Double.isFinite(v) ) {
                            continue;
                        }
                        double largestDifference = 0;
                        for ( int[] offset : TETRAHEDRA_NEIGHBORS ) {
                            double w = original[nodeIndex(i + offset[0],
                                j + offset[1], k + offset[2])];
                            if ( Double.isFinite(w) ) {
                                largestDifference = Math.max(largestDifference,
                                    Math.abs(w - v));
                            }
                        }
                        double minimum = NODE_SEPARATION * largestDifference;
                        if ( Math.abs(v) < minimum ) {
                            separated[node] = v < 0 ? -minimum : minimum;
                        }
                    }
                }
            }
            return separated;
        }

        private int nodeIndex(int i, int j, int k)
        {
            return i + nodesX * (j + nodesY * k);
        }

        private Vector3Dd nodePosition(int node)
        {
            int i = node % nodesX;
            int j = (node / nodesX) % nodesY;
            int k = node / (nodesX * nodesY);
            return new Vector3Dd(origin.x() + i * spacing.x(),
                origin.y() + j * spacing.y(), origin.z() + k * spacing.z());
        }

        private boolean isBorder(int node)
        {
            int i = node % nodesX;
            int j = (node / nodesX) % nodesY;
            int k = node / (nodesX * nodesY);
            return i == 0 || j == 0 || k == 0 ||
                i == nodesX - 1 || j == nodesY - 1 || k == nodesZ - 1;
        }

        private boolean isInside(int node)
        {
            return !isBorder(node) && values[node] < 0;
        }


        /**
        @return index of the vertex where the zero level crosses the edge
        from an inside node to an outside node
        */
        private int edgeVertex(int inside, int outside)
        {
            long key = ((long)Math.min(inside, outside)) * values.length +
                Math.max(inside, outside);
            Integer index = edgeVertices.get(key);
            if ( index != null ) {
                return index;
            }
            double a = values[inside];
            double b = values[outside];
            double t;
            if ( isBorder(outside) ) {
                // Caps: half way to the border, so they are flat
                t = 0.5;
            }
            else if ( Double.isFinite(a) && Double.isFinite(b) && b >= 0 ) {
                t = a / (a - b);
            }
            else {
                // Not a number or infinite: no reliable interpolation
                t = 0.5;
            }
            t = Math.max(MINIMUM_EDGE_FRACTION,
                Math.min(1.0 - MINIMUM_EDGE_FRACTION, t));
            Vector3Dd p = nodePosition(inside);
            Vector3Dd q = nodePosition(outside);
            positions.add(p.add(q.subtract(p).multiply(t)));
            index = positions.size() - 1;
            edgeVertices.put(key, index);
            return index;
        }

        PolyhedralBoundedSolid build()
        {
            int[] corners = new int[8];

            for ( int k = 0; k < nodesZ - 1; k++ ) {
                for ( int j = 0; j < nodesY - 1; j++ ) {
                    for ( int i = 0; i < nodesX - 1; i++ ) {
                        for ( int c = 0; c < 8; c++ ) {
                            corners[c] = nodeIndex(i + (c & 1),
                                j + ((c >> 1) & 1), k + ((c >> 2) & 1));
                        }
                        for ( int[] tetrahedron : TETRAHEDRA ) {
                            polygonizeTetrahedron(corners[tetrahedron[0]],
                                corners[tetrahedron[1]], corners[tetrahedron[2]],
                                corners[tetrahedron[3]]);
                        }
                    }
                }
            }
            return PolyhedralBoundedSolidMeshConstructions.createFromPolygonMesh(
                positions, polygons);
        }

        private void polygonizeTetrahedron(int n0, int n1, int n2, int n3)
        {
            int[] nodes = {n0, n1, n2, n3};
            int[] in = new int[4];
            int[] out = new int[4];
            int insideCount = 0;
            int outsideCount = 0;

            for ( int node : nodes ) {
                if ( isInside(node) ) {
                    in[insideCount++] = node;
                }
                else {
                    out[outsideCount++] = node;
                }
            }
            if ( insideCount == 0 || outsideCount == 0 ) {
                return;
            }

            int[] polygon;
            if ( insideCount == 1 ) {
                polygon = new int[] {edgeVertex(in[0], out[0]),
                    edgeVertex(in[0], out[1]), edgeVertex(in[0], out[2])};
            }
            else if ( insideCount == 3 ) {
                polygon = new int[] {edgeVertex(in[0], out[0]),
                    edgeVertex(in[1], out[0]), edgeVertex(in[2], out[0])};
            }
            else {
                // Consecutive vertices share a node of the tetrahedron
                polygon = new int[] {edgeVertex(in[0], out[0]),
                    edgeVertex(in[0], out[1]), edgeVertex(in[1], out[1]),
                    edgeVertex(in[1], out[0])};
            }

            // Counterclockwise as seen from outside: the normal points from
            // the inside nodes to the outside ones
            Vector3Dd towardsOutside = centroid(out, outsideCount)
                .subtract(centroid(in, insideCount));
            if ( newellNormal(polygon).dotProduct(towardsOutside) < 0 ) {
                reverse(polygon);
            }

            if ( polygon.length == 4 && !isPlanar(polygon) ) {
                polygons.add(new int[] {polygon[0], polygon[1], polygon[2]});
                polygons.add(new int[] {polygon[0], polygon[2], polygon[3]});
            }
            else {
                polygons.add(polygon);
            }
        }

        private Vector3Dd centroid(int[] nodes, int count)
        {
            Vector3Dd sum = new Vector3Dd(0, 0, 0);
            for ( int i = 0; i < count; i++ ) {
                sum = sum.add(nodePosition(nodes[i]));
            }
            return sum.multiply(1.0 / count);
        }

        private Vector3Dd newellNormal(int[] polygon)
        {
            double x = 0;
            double y = 0;
            double z = 0;
            for ( int i = 0; i < polygon.length; i++ ) {
                Vector3Dd p = positions.get(polygon[i]);
                Vector3Dd q = positions.get(polygon[(i + 1) % polygon.length]);
                x += (p.y() - q.y()) * (p.z() + q.z());
                y += (p.z() - q.z()) * (p.x() + q.x());
                z += (p.x() - q.x()) * (p.y() + q.y());
            }
            return new Vector3Dd(x, y, z);
        }

        private boolean isPlanar(int[] quad)
        {
            Vector3Dd p0 = positions.get(quad[0]);
            Vector3Dd p1 = positions.get(quad[1]);
            Vector3Dd p2 = positions.get(quad[2]);
            Vector3Dd p3 = positions.get(quad[3]);
            Vector3Dd n = p1.subtract(p0).crossProduct(p2.subtract(p0));
            double size = Math.max(p2.subtract(p0).length(), p3.subtract(p1).length());
            double length = n.length();
            if ( !(length > 0) || !(size > 0) ) {
                return false;
            }
            return Math.abs(p3.subtract(p0).dotProduct(n)) / length <=
                PLANARITY_TOLERANCE * size;
        }

        private static void reverse(int[] polygon)
        {
            for ( int i = 0, j = polygon.length - 1; i < j; i++, j-- ) {
                int tmp = polygon[i];
                polygon[i] = polygon[j];
                polygon[j] = tmp;
            }
        }
    }
}
