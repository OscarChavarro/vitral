package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.volume.Box;
import vsdk.toolkit.environment.geometry.volume.Cone;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidEulerOperators;

/**
Self-contained CSG fixture derived from
{@code TangibleInterfaceCubeFixture.STEPER_MOTOR_GUIDE}.
 */
final class StepperMotorGuideCsgFixture
{
    static final double MILLIMETERS_TO_MODEL_UNITS = 0.01;
    static final double BASE_TOP_Z = mm(3.0);
    static final double TRANSITION_Z = BASE_TOP_Z + mm(4.2);
    static final double SLEEVE_BASE_Z = BASE_TOP_Z - mm(1.0);
    static final double SLEEVE_TOP_Z = BASE_TOP_Z + mm(7.06);

    private static final double D_BORE_DIAMETER = mm(5.05);
    private static final double D_FLAT_AXIS = mm(4.6);
    private static final double SLEEVE_INNER_DIAMETER = mm(9.02);
    private static final double LEGACY_COUPLER_OUTER_DIAMETER = mm(9.02);
    private static final double CORRECTED_COUPLER_OUTER_DIAMETER = mm(10.02);
    private static final double SLEEVE_WALL = mm(1.6);
    private static final double AXIAL_OVERLAP = mm(0.1);
    private static final double CENTER_X = mm(20.0);
    private static final double CENTER_Y = mm(20.0);
    private static final int CYLINDER_SIDES = 32;
    private static final int D_PROFILE_SIDES = 64;

    private StepperMotorGuideCsgFixture()
    {
    }

    static PolyhedralBoundedSolid createLegacyLowerCoupler()
    {
        PolyhedralBoundedSolid exterior = createCylinder(
            LEGACY_COUPLER_OUTER_DIAMETER * 0.5,
            TRANSITION_Z - SLEEVE_BASE_Z,
            SLEEVE_BASE_Z);
        PolyhedralBoundedSolid dCutter = createDProfile(
            SLEEVE_BASE_Z - AXIAL_OVERLAP,
            TRANSITION_Z - SLEEVE_BASE_Z + 2.0 * AXIAL_OVERLAP);
        return strictSetOp(exterior, dCutter,
            PolyhedralBoundedSolidModeler.SUBTRACT);
    }

    static PolyhedralBoundedSolid createLegacyBearingSleeve()
    {
        double innerRadius = SLEEVE_INNER_DIAMETER * 0.5;
        PolyhedralBoundedSolid exterior = createCylinder(
            innerRadius + SLEEVE_WALL,
            SLEEVE_TOP_Z - SLEEVE_BASE_Z,
            SLEEVE_BASE_Z);
        PolyhedralBoundedSolid circularCutter = createCylinder(
            innerRadius,
            SLEEVE_TOP_Z - SLEEVE_BASE_Z + 2.0 * AXIAL_OVERLAP,
            SLEEVE_BASE_Z - AXIAL_OVERLAP);
        return strictSetOp(exterior, circularCutter,
            PolyhedralBoundedSolidModeler.SUBTRACT);
    }

    static PolyhedralBoundedSolid createCorrectedSteppedTube()
    {
        PolyhedralBoundedSolid exterior = createCombinedExterior();
        PolyhedralBoundedSolid upperCutter = createCylinder(
            SLEEVE_INNER_DIAMETER * 0.5,
            SLEEVE_TOP_Z - TRANSITION_Z + AXIAL_OVERLAP,
            TRANSITION_Z);
        PolyhedralBoundedSolid withUpperCavity = strictSetOp(
            exterior, upperCutter, PolyhedralBoundedSolidModeler.SUBTRACT);
        PolyhedralBoundedSolid lowerCutter = createDProfile(
            SLEEVE_BASE_Z - AXIAL_OVERLAP,
            TRANSITION_Z - SLEEVE_BASE_Z + 2.0 * AXIAL_OVERLAP);
        return strictSetOp(withUpperCavity, lowerCutter,
            PolyhedralBoundedSolidModeler.SUBTRACT);
    }

    static PolyhedralBoundedSolid createCorrectedFinalGuide()
    {
        PolyhedralBoundedSolid base = createArrowBase();
        PolyhedralBoundedSolid exterior = createCombinedExterior();
        PolyhedralBoundedSolid baseWithExterior = strictSetOp(
            base, exterior, PolyhedralBoundedSolidModeler.UNION);
        PolyhedralBoundedSolid upperCutter = createCylinder(
            SLEEVE_INNER_DIAMETER * 0.5,
            SLEEVE_TOP_Z - TRANSITION_Z + AXIAL_OVERLAP,
            TRANSITION_Z);
        PolyhedralBoundedSolid withUpperCavity = strictSetOp(
            baseWithExterior, upperCutter,
            PolyhedralBoundedSolidModeler.SUBTRACT);
        PolyhedralBoundedSolid lowerCutter = createDProfile(
            BASE_TOP_Z,
            TRANSITION_Z - BASE_TOP_Z + AXIAL_OVERLAP);
        return strictSetOp(withUpperCavity, lowerCutter,
            PolyhedralBoundedSolidModeler.SUBTRACT);
    }

    static boolean containsMaterialAt(
        PolyhedralBoundedSolid solid, double x, double y, double z)
    {
        double probeSide = mm(0.02);
        PolyhedralBoundedSolid probe = new Box(
            new Vector3Dd(probeSide, probeSide, probeSide))
                .exportToPolyhedralBoundedSolid();
        Matrix4x4d move = new Matrix4x4d();
        move = move.translation(x, y, z);
        PolyhedralBoundedSolidModeler.applyTransformation(probe, move);
        PolyhedralBoundedSolid intersection =
            PolyhedralBoundedSolidModeler.setOp(
                deepClone(solid), probe,
                PolyhedralBoundedSolidModeler.INTERSECTION,
                false, true, false);
        return intersection.getPolygonsList().size() > 0;
    }

    static boolean hasLoopWithFewerThanThreeDistinctEdges(
        PolyhedralBoundedSolid solid)
    {
        for ( int i = 0; i < solid.getPolygonsList().size(); i++ ) {
            for ( int j = 0;
                  j < solid.getPolygonsList().get(i).boundariesList.size();
                  j++ ) {
                if ( solid.getPolygonsList().get(i).boundariesList.get(j)
                        .halfEdgesList.size() < 3 ) {
                    return true;
                }
            }
        }
        return false;
    }

    static double centerX()
    {
        return CENTER_X;
    }

    static double centerY()
    {
        return CENTER_Y;
    }

    private static PolyhedralBoundedSolid createCombinedExterior()
    {
        PolyhedralBoundedSolid lowerExterior = createCylinder(
            CORRECTED_COUPLER_OUTER_DIAMETER * 0.5,
            TRANSITION_Z - (SLEEVE_BASE_Z + AXIAL_OVERLAP),
            SLEEVE_BASE_Z + AXIAL_OVERLAP);
        PolyhedralBoundedSolid sleeveExterior = createCylinder(
            SLEEVE_INNER_DIAMETER * 0.5 + SLEEVE_WALL,
            SLEEVE_TOP_Z - SLEEVE_BASE_Z,
            SLEEVE_BASE_Z);
        return strictSetOp(lowerExterior, sleeveExterior,
            PolyhedralBoundedSolidModeler.UNION);
    }

    private static PolyhedralBoundedSolid createArrowBase()
    {
        List<Vector3Dd> polygon = new ArrayList<Vector3Dd>();
        polygon.add(point(0.0, 0.0, 0.0));
        polygon.add(point(40.0, 0.0, 0.0));
        polygon.add(point(40.0, 15.0, 0.0));
        polygon.add(point(45.0, 15.0, 0.0));
        polygon.add(point(55.0, 20.0, 0.0));
        polygon.add(point(45.0, 25.0, 0.0));
        polygon.add(point(40.0, 25.0, 0.0));
        polygon.add(point(40.0, 40.0, 0.0));
        polygon.add(point(0.0, 40.0, 0.0));
        return extrudePolygon(polygon, BASE_TOP_Z);
    }

    private static PolyhedralBoundedSolid createDProfile(
        double baseZ, double height)
    {
        double radius = D_BORE_DIAMETER * 0.5;
        double flatX = D_FLAT_AXIS - radius;
        List<Vector3Dd> polygon = new ArrayList<Vector3Dd>();
        for ( int i = 0; i < D_PROFILE_SIDES; i++ ) {
            double angleA = 2.0 * Math.PI * i / D_PROFILE_SIDES;
            double angleB = 2.0 * Math.PI * (i + 1) / D_PROFILE_SIDES;
            Vector3Dd a = new Vector3Dd(
                radius * Math.cos(angleA), radius * Math.sin(angleA), baseZ);
            Vector3Dd b = new Vector3Dd(
                radius * Math.cos(angleB), radius * Math.sin(angleB), baseZ);
            boolean aInside = a.x() <= flatX + 1.0e-9;
            boolean bInside = b.x() <= flatX + 1.0e-9;
            if ( aInside ) {
                appendUnique(polygon, new Vector3Dd(
                    CENTER_X + a.x(), CENTER_Y + a.y(), baseZ));
            }
            if ( aInside != bInside ) {
                double factor = (flatX - a.x()) / (b.x() - a.x());
                double y = a.y() + factor * (b.y() - a.y());
                appendUnique(polygon,
                    new Vector3Dd(CENTER_X + flatX, CENTER_Y + y, baseZ));
            }
        }
        return extrudePolygon(polygon, height);
    }

    private static PolyhedralBoundedSolid createCylinder(
        double radius, double height, double baseZ)
    {
        PolyhedralBoundedSolid solid = new Cone(radius, radius, height)
            .exportToPolyhedralBoundedSolid(CYLINDER_SIDES, 1);
        Matrix4x4d move = new Matrix4x4d();
        move = move.translation(CENTER_X, CENTER_Y, baseZ);
        PolyhedralBoundedSolidModeler.applyTransformation(solid, move);
        return solid;
    }

    private static PolyhedralBoundedSolid extrudePolygon(
        List<Vector3Dd> polygon, double height)
    {
        PolyhedralBoundedSolid solid = new PolyhedralBoundedSolid();
        PolyhedralBoundedSolidEulerOperators.mvfs(
            solid, polygon.get(0), 1, 1);
        for ( int i = 1; i < polygon.size(); i++ ) {
            PolyhedralBoundedSolidEulerOperators.smev(
                solid, 1, i, i + 1, polygon.get(i));
        }
        PolyhedralBoundedSolidEulerOperators.smef(
            solid, 1, polygon.size(), 1, 2);
        Matrix4x4d sweep = new Matrix4x4d();
        sweep = sweep.translation(0.0, 0.0, height);
        PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(
            solid, solid.findFace(1), sweep);
        return solid;
    }

    private static PolyhedralBoundedSolid strictSetOp(
        PolyhedralBoundedSolid a, PolyhedralBoundedSolid b, int op)
    {
        return PolyhedralBoundedSolidModeler.setOp(
            a, b, op, false, true, true);
    }

    private static void appendUnique(
        List<Vector3Dd> polygon, Vector3Dd candidate)
    {
        if ( polygon.isEmpty() ||
             polygon.get(polygon.size() - 1).subtract(candidate).length()
                > 1.0e-9 ) {
            polygon.add(candidate);
        }
    }

    private static Vector3Dd point(double xMm, double yMm, double zMm)
    {
        return new Vector3Dd(mm(xMm), mm(yMm), mm(zMm));
    }

    private static double mm(double value)
    {
        return value * MILLIMETERS_TO_MODEL_UNITS;
    }

    private static PolyhedralBoundedSolid deepClone(
        PolyhedralBoundedSolid solid)
    {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ObjectOutputStream output = new ObjectOutputStream(bytes);
            output.writeObject(solid);
            output.close();
            ObjectInputStream input = new ObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray()));
            PolyhedralBoundedSolid clone =
                (PolyhedralBoundedSolid)input.readObject();
            input.close();
            return clone;
        }
        catch ( IOException | ClassNotFoundException e ) {
            throw new IllegalStateException("Unable to clone test solid", e);
        }
    }
}
