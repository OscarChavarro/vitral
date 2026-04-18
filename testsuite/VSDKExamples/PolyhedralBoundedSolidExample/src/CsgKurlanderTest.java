// VitralSDK classes
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.geometry.Sphere;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;
import vsdk.toolkit.processing.GeometricModeler;

public class CsgKurlanderTest
{
    private static final int CYLINDER_SIDES = 30;

    private static PolyhedralBoundedSolid booleanOp(
        PolyhedralBoundedSolid a, PolyhedralBoundedSolid b, int op)
    {
        return GeometricModeler.setOp(a, b, op, false);
    }

    private static PolyhedralBoundedSolid createSphere(double radius,
                                                       Vector3D center)
    {
        PolyhedralBoundedSolid solid = new Sphere(radius)
            .exportToPolyhedralBoundedSolid();
        Matrix4x4 t = new Matrix4x4();
        t.translation(center);
        solid.applyTransformation(t);
        return solid;
    }

    private static PolyhedralBoundedSolid createCylinder(double radius,
                                                         double height,
                                                         Vector3D translation)
    {
        // Robust cylinder B-Rep for CSG: polygonal lamina + sweep, instead of
        // relying on cone export with equal radii.
        PolyhedralBoundedSolid solid = GeometricModeler
            .createCircularLamina(0.0, 0.0, radius, 0.0, CYLINDER_SIDES);
        Matrix4x4 sweep = new Matrix4x4();
        sweep.translation(0.0, 0.0, height);
        GeometricModeler.translationalSweepExtrudeFacePlanar(
            solid, solid.findFace(1), sweep);

        Matrix4x4 move = new Matrix4x4();
        move.translation(translation);
        solid.applyTransformation(move);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);
        return solid;
    }

    private static PolyhedralBoundedSolid createExtrudedPolygon(
        Vector3D[] points, double thickness)
    {
        int i;
        PolyhedralBoundedSolid s = new PolyhedralBoundedSolid();
        s.mvfs(points[0], 1, 1);

        for ( i = 1; i < points.length; i++ ) {
            s.smev(1, i, i+1, points[i]);
        }
        s.smef(1, points.length, 1, 2);

        Matrix4x4 t = new Matrix4x4();
        t.translation(0.0, 0.0, thickness);
        GeometricModeler.translationalSweepExtrudeFacePlanar(s, s.findFace(1), t);
        return s;
    }

    private static PolyhedralBoundedSolid createStar()
    {
        int i;
        int n = 10;
        double outerR = 2.0;
        double innerR = 0.77;
        double start = Math.toRadians(-90.0);
        Vector3D[] pts = new Vector3D[n];

        for ( i = 0; i < n; i++ ) {
            double a = start + i * Math.PI / 5.0;
            double r = (i % 2 == 0) ? outerR : innerR;
            pts[i] = new Vector3D(r * Math.cos(a), r * Math.sin(a), 0.0);
        }

        return createExtrudedPolygon(pts, 5.5);
    }

    private static PolyhedralBoundedSolid createMoon()
    {
        PolyhedralBoundedSolid a = createCylinder(1.5, 5.0, new Vector3D(0, 0, 0));
        PolyhedralBoundedSolid b = createCylinder(1.5, 5.0, new Vector3D(1.1, 0, 0.6));
        return booleanOp(a, b, GeometricModeler.DIFFERENCE);
    }

    private static PolyhedralBoundedSolid placeMotif(
        PolyhedralBoundedSolid motif, double z, double azimuthDeg)
    {
        Matrix4x4 t = new Matrix4x4();
        Matrix4x4 ry = new Matrix4x4();
        Matrix4x4 rz = new Matrix4x4();
        Matrix4x4 m;

        t.translation(6.0, 0.0, z);
        ry.axisRotation(Math.toRadians(90.0), 0, 1, 0);
        rz.axisRotation(Math.toRadians(azimuthDeg), 0, 0, 1);
        m = rz.multiply(ry.multiply(t));
        motif.applyTransformation(m);
        return motif;
    }

    public static PolyhedralBoundedSolid create()
    {
        int i;
        PolyhedralBoundedSolid outer = createSphere(10.0, new Vector3D(0, 0, 10.0));
        PolyhedralBoundedSolid inner = createSphere(9.5, new Vector3D(0, 0, 10.0));
        PolyhedralBoundedSolid shell = booleanOp(
            outer, inner, GeometricModeler.DIFFERENCE);

        for ( i = 1; i <= 4; i++ ) {
            double base = -90.0 * i;
            shell = booleanOp(shell,
                placeMotif(createMoon(), 4.0, base),
                GeometricModeler.DIFFERENCE);
            shell = booleanOp(shell,
                placeMotif(createMoon(), 14.0, base),
                GeometricModeler.DIFFERENCE);
            shell = booleanOp(shell,
                placeMotif(createMoon(), 11.5, base - 22.5),
                GeometricModeler.DIFFERENCE);
            shell = booleanOp(shell,
                placeMotif(createMoon(), 9.0, base - 45.0),
                GeometricModeler.DIFFERENCE);
            shell = booleanOp(shell,
                placeMotif(createMoon(), 6.5, base - 67.5),
                GeometricModeler.DIFFERENCE);
        }

        for ( i = 1; i <= 4; i++ ) {
            double base = -90.0 * i;
            shell = booleanOp(shell,
                placeMotif(createStar(), 9.0, base),
                GeometricModeler.DIFFERENCE);
            shell = booleanOp(shell,
                placeMotif(createStar(), 6.5, base - 22.5),
                GeometricModeler.DIFFERENCE);
            shell = booleanOp(shell,
                placeMotif(createStar(), 14.0, base - 45.0),
                GeometricModeler.DIFFERENCE);
            shell = booleanOp(shell,
                placeMotif(createStar(), 4.0, base - 45.0),
                GeometricModeler.DIFFERENCE);
            shell = booleanOp(shell,
                placeMotif(createStar(), 11.5, base - 67.5),
                GeometricModeler.DIFFERENCE);
        }

        PolyhedralBoundedSolid guide = createCylinder(
            10.5, 16.5, new Vector3D(0, 0, 0));
        PolyhedralBoundedSolid result = booleanOp(
            shell, guide, GeometricModeler.INTERSECTION);

        PolyhedralBoundedSolidValidationEngine.validateIntermediate(result);
        return result;
    }
}
