package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidEulerConstructions;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidEulerOperators;

/**
Prisms built with the Euler operators and the translational sweep, for the
tests that need operands other than boxes: regular prisms of N sides (the
polyhedral stand-in for a cylinder) and star prisms (non-convex, with N
reflex edges).
 */
final class PrismFixtures
{
    private PrismFixtures()
    {
    }

    /** Prism over the polygon `xy` (counterclockwise), from z=0 to z=height. */
    static PolyhedralBoundedSolid prism(double[][] xy, double height)
    {
        PolyhedralBoundedSolid solid = new PolyhedralBoundedSolid();
        int i;

        PolyhedralBoundedSolidEulerOperators.mvfs(solid,
            new Vector3Dd(xy[0][0], xy[0][1], 0.0), 1, 1);
        for ( i = 1; i < xy.length; i++ ) {
            PolyhedralBoundedSolidEulerOperators.smev(solid, 1, i, i + 1,
                new Vector3Dd(xy[i][0], xy[i][1], 0.0));
        }
        PolyhedralBoundedSolidEulerOperators.smef(solid, 1, xy.length, 1, 2);
        Matrix4x4d t = new Matrix4x4d();
        t = t.translation(0.0, 0.0, height);
        PolyhedralBoundedSolidEulerConstructions
            .translationalSweepExtrudeFacePlanar(solid, solid.findFace(1), t);
        return solid;
    }

    /** Regular polygon of `sides` sides inscribed in a circle. */
    static double[][] regularPolygon(int sides, double radius)
    {
        double[][] xy = new double[sides][2];
        int i;

        for ( i = 0; i < sides; i++ ) {
            double a = 2.0 * Math.PI * i / sides;
            xy[i][0] = radius * Math.cos(a);
            xy[i][1] = radius * Math.sin(a);
        }
        return xy;
    }

    /** Star of `points` points: vertices alternate between two radii. */
    static double[][] starPolygon(int points, double outerRadius,
        double innerRadius)
    {
        double[][] xy = new double[2 * points][2];
        int i;

        for ( i = 0; i < 2 * points; i++ ) {
            double a = Math.PI * i / points;
            double r = i % 2 == 0 ? outerRadius : innerRadius;
            xy[i][0] = r * Math.cos(a);
            xy[i][1] = r * Math.sin(a);
        }
        return xy;
    }

    /** Area of a simple polygon (positive when counterclockwise). */
    static double polygonArea(double[][] xy)
    {
        double area = 0.0;
        int i;

        for ( i = 0; i < xy.length; i++ ) {
            double[] p = xy[i];
            double[] q = xy[(i + 1) % xy.length];
            area += p[0] * q[1] - q[0] * p[1];
        }
        return area / 2.0;
    }
}
