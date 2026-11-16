package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Predicate;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidVolumeOracle.OrientedBox;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;

/**
Exact volume oracle for non-convex operands (Fable5.2Analysis.md Section 8,
"further tests worth writing"). A shape is a union of boxes minus a union
of boxes, all axis-aligned in the shape's own frame, followed by a rigid
motion: L, U and stepped blocks, and a frame with a through hole (genus 1).

<p>The oracle never asks the kernel. Each shape is cut into disjoint
convex cells along the coordinates of its boxes; a cell belongs to the
shape when its centre is in some positive box and in no negative box. The
volume of the points that are in exactly the shapes of a set P is
obtained by inclusion-exclusion from the volumes of the intersections of
shapes, and each intersection is a sum over tuples of cells of the volume
of a convex polytope ({@link PolyhedralBoundedSolidVolumeOracle#convexVolume}).
So the oracle is exact for any boolean expression of up to a few shapes,
chains included.</p>

<p>Traceability: [MANT1988] Sec. 13.3 (volume as integral property),
Appendix B (regularised set operations).</p>
 */
final class CompositeShapeOracle
{
    /** Axis-aligned boxes {x0, y0, z0, x1, y1, z1} in the local frame. */
    static final class Shape
    {
        final String name;
        final List<double[]> positive;
        final List<double[]> negative;
        final Matrix4x4d frame;

        Shape(String name, List<double[]> positive, List<double[]> negative,
            Matrix4x4d frame)
        {
            this.name = name;
            this.positive = positive;
            this.negative = negative;
            this.frame = frame;
        }

        Shape moved(Matrix4x4d motion)
        {
            return new Shape(name, positive, negative,
                motion.multiply(frame));
        }

        /** Disjoint convex cells of the shape, in world coordinates. */
        List<OrientedBox> cells()
        {
            ArrayList<OrientedBox> cells = new ArrayList<>();
            List<Double> xs = coordinates(0);
            List<Double> ys = coordinates(1);
            List<Double> zs = coordinates(2);
            int i;
            int j;
            int k;

            for ( i = 0; i + 1 < xs.size(); i++ ) {
                for ( j = 0; j + 1 < ys.size(); j++ ) {
                    for ( k = 0; k + 1 < zs.size(); k++ ) {
                        double[] c = {
                            (xs.get(i) + xs.get(i + 1)) / 2.0,
                            (ys.get(j) + ys.get(j + 1)) / 2.0,
                            (zs.get(k) + zs.get(k + 1)) / 2.0 };
                        if ( !insideLocal(c) ) {
                            continue;
                        }
                        cells.add(new OrientedBox(
                            xs.get(i + 1) - xs.get(i),
                            ys.get(j + 1) - ys.get(j),
                            zs.get(k + 1) - zs.get(k),
                            frame.multiply(PolyhedralBoundedSolidVolumeOracle
                                .translation(c[0], c[1], c[2]))));
                    }
                }
            }
            return cells;
        }

        private boolean insideLocal(double[] p)
        {
            boolean in = false;

            for ( double[] b : positive ) {
                in |= insideBox(b, p);
            }
            for ( double[] b : negative ) {
                in &= !insideBox(b, p);
            }
            return in;
        }

        private static boolean insideBox(double[] b, double[] p)
        {
            return p[0] > b[0] && p[0] < b[3] && p[1] > b[1] &&
                p[1] < b[4] && p[2] > b[2] && p[2] < b[5];
        }

        private List<Double> coordinates(int axis)
        {
            TreeSet<Double> values = new TreeSet<>();

            for ( double[] b : positive ) {
                values.add(b[axis]);
                values.add(b[axis + 3]);
            }
            for ( double[] b : negative ) {
                values.add(b[axis]);
                values.add(b[axis + 3]);
            }
            return new ArrayList<>(values);
        }

        /**
        Builds the B-Rep with the kernel in the axis-aligned local frame (a
        union of the positive boxes minus the negative ones), then applies
        the rigid motion. Tests compare its volume with {@link #volume()}
        before using it as an operand.
        */
        PolyhedralBoundedSolid toSolid()
        {
            PolyhedralBoundedSolid solid = box(positive.get(0));
            int i;

            for ( i = 1; i < positive.size(); i++ ) {
                solid = PolyhedralBoundedSolidModeler.setOp(solid,
                    box(positive.get(i)), PolyhedralBoundedSolidModeler.UNION);
            }
            for ( double[] b : negative ) {
                solid = PolyhedralBoundedSolidModeler.setOp(solid, box(b),
                    PolyhedralBoundedSolidModeler.SUBTRACT);
            }
            PolyhedralBoundedSolidModeler.applyTransformation(solid, frame);
            return solid;
        }

        double volume()
        {
            return CompositeShapeOracle.volume(List.of(this), m -> m[0]);
        }

        @Override
        public String toString()
        {
            return name;
        }

        private static PolyhedralBoundedSolid box(double[] b)
        {
            return new OrientedBox(b[3] - b[0], b[4] - b[1], b[5] - b[2],
                PolyhedralBoundedSolidVolumeOracle.translation(
                    (b[0] + b[3]) / 2.0, (b[1] + b[4]) / 2.0,
                    (b[2] + b[5]) / 2.0)).toSolid();
        }
    }

    private CompositeShapeOracle()
    {
    }

    /**
    Exact volume of the points whose membership vector (one flag per shape)
    satisfies {@code expression}. The expression must be false when the
    point is in no shape.
    */
    static double volume(List<Shape> shapes,
        Predicate<boolean[]> expression)
    {
        int n = shapes.size();
        ArrayList<List<OrientedBox>> cells = new ArrayList<>();
        double[] intersection = new double[1 << n];
        double total = 0.0;
        int set;

        for ( Shape s : shapes ) {
            cells.add(s.cells());
        }
        for ( set = 1; set < (1 << n); set++ ) {
            intersection[set] = intersectionVolume(cells, set);
        }
        for ( set = 1; set < (1 << n); set++ ) {
            boolean[] member = new boolean[n];
            int i;
            for ( i = 0; i < n; i++ ) {
                member[i] = (set & (1 << i)) != 0;
            }
            if ( !expression.test(member) ) {
                continue;
            }
            // Points in exactly the shapes of `set`: Moebius inversion.
            int superset;
            double exact = 0.0;
            for ( superset = set; superset < (1 << n); superset++ ) {
                if ( (superset & set) != set ) {
                    continue;
                }
                int extra = Integer.bitCount(superset) -
                    Integer.bitCount(set);
                exact += ((extra % 2) == 0 ? 1.0 : -1.0) *
                    intersection[superset];
            }
            total += exact;
        }
        return total;
    }

    private static double intersectionVolume(
        List<List<OrientedBox>> cells, int set)
    {
        ArrayList<List<OrientedBox>> chosen = new ArrayList<>();
        int i;

        for ( i = 0; i < cells.size(); i++ ) {
            if ( (set & (1 << i)) != 0 ) {
                chosen.add(cells.get(i));
            }
        }
        return sumOverTuples(chosen, 0, new ArrayList<>());
    }

    private static double sumOverTuples(List<List<OrientedBox>> chosen,
        int depth, ArrayList<double[]> planes)
    {
        if ( depth == chosen.size() ) {
            return PolyhedralBoundedSolidVolumeOracle.convexVolume(planes);
        }
        double sum = 0.0;
        for ( OrientedBox cell : chosen.get(depth) ) {
            ArrayList<double[]> next = new ArrayList<>(planes);
            next.addAll(cell.halfSpaces());
            sum += sumOverTuples(chosen, depth + 1, next);
        }
        return sum;
    }
}
