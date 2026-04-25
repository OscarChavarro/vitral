package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidEulerOperators;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.geometry.volume.Sphere;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;

public class CsgKurlanderBowlFixture
{
    private static final int CYLINDER_SIDES = 30;
    private static final int MOTIF_RING_COUNT = 4;
    private static final int MOTIFS_PER_TYPE_RING = 5;
    private static final int STAR_COUNT =
        MOTIF_RING_COUNT * MOTIFS_PER_TYPE_RING;
    private static final int MOON_COUNT =
        MOTIF_RING_COUNT * MOTIFS_PER_TYPE_RING;
    private static final double[] STAR_Z_VALUES = {
        9.0, 6.5, 14.0, 4.0, 11.5
    };
    private static final double[] STAR_AZIMUTH_OFFSETS = {
        0.0, -22.5, -45.0, -45.0, -67.5
    };
    private static final double[] MOON_Z_VALUES = {
        4.0, 14.0, 11.5, 9.0, 6.5
    };
    private static final double[] MOON_AZIMUTH_OFFSETS = {
        0.0, 0.0, -22.5, -45.0, -67.5
    };
    private static final double OBJECT_SCALE = 0.1;

    private CsgKurlanderBowlFixture()
    {
    }

    private static double scale(double value)
    {
        return value * OBJECT_SCALE;
    }

    private static PolyhedralBoundedSolid booleanOp(
        PolyhedralBoundedSolid a, PolyhedralBoundedSolid b, int op)
    {
        return PolyhedralBoundedSolidModeler.setOp(a, b, op, false);
    }

    private static PolyhedralBoundedSolid booleanOpWithoutFaceMaximization(
        PolyhedralBoundedSolid a, PolyhedralBoundedSolid b, int op)
    {
        return PolyhedralBoundedSolidModeler.setOp(a, b, op, false, false);
    }

    private static PolyhedralBoundedSolid createSphere(double radius,
                                                       Vector3D center)
    {
        PolyhedralBoundedSolid solid = new Sphere(radius)
            .exportToPolyhedralBoundedSolid();
        Matrix4x4 t = new Matrix4x4();
        t = t.translation(center);
        PolyhedralBoundedSolidModeler.applyTransformation(solid, t);
        return solid;
    }

    private static PolyhedralBoundedSolid createCylinder(double radius,
                                                         double height,
                                                         Vector3D translation)
    {
        PolyhedralBoundedSolid solid = PolyhedralBoundedSolidModeler
            .createCircularLamina(0.0, 0.0, radius, 0.0, CYLINDER_SIDES);
        Matrix4x4 sweep = new Matrix4x4();
        sweep = sweep.translation(0.0, 0.0, height);
        PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(
            solid, solid.findFace(1), sweep);

        Matrix4x4 move = new Matrix4x4();
        move = move.translation(translation);
        PolyhedralBoundedSolidModeler.applyTransformation(solid, move);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);
        return solid;
    }

    private static PolyhedralBoundedSolid createExtrudedPolygon(
        Vector3D[] points, double thickness)
    {
        int i;
        PolyhedralBoundedSolid solid = new PolyhedralBoundedSolid();
        PolyhedralBoundedSolidEulerOperators.mvfs(solid, points[0], 1, 1);

        for ( i = 1; i < points.length; i++ ) {
            PolyhedralBoundedSolidEulerOperators.smev(solid, 1, i, i + 1, points[i]);
        }
        PolyhedralBoundedSolidEulerOperators.smef(solid, 1, points.length, 1, 2);

        Matrix4x4 t = new Matrix4x4();
        t = t.translation(0.0, 0.0, thickness);
        PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(
            solid, solid.findFace(1), t);
        return solid;
    }

    private static PolyhedralBoundedSolid createStar()
    {
        int i;
        int n = 10;
        double outerR = scale(2.0);
        double innerR = scale(0.77);
        double start = Math.toRadians(-90.0);
        Vector3D[] points = new Vector3D[n];

        for ( i = 0; i < n; i++ ) {
            double a = start + i * Math.PI / 5.0;
            double r = (i % 2 == 0) ? outerR : innerR;
            points[i] = new Vector3D(r * Math.cos(a), r * Math.sin(a), 0.0);
        }

        return createExtrudedPolygon(points, scale(5.5));
    }

    private static PolyhedralBoundedSolid createMoon()
    {
        PolyhedralBoundedSolid a = createCylinder(
            scale(1.5), scale(5.0), new Vector3D(0, 0, 0));
        PolyhedralBoundedSolid b = createCylinder(
            scale(1.5), scale(5.0), new Vector3D(scale(1.1), 0, scale(0.6)));
        return booleanOp(a, b, PolyhedralBoundedSolidModeler.SUBTRACT);
    }

    private static PolyhedralBoundedSolid placeMotif(
        PolyhedralBoundedSolid motif, double z, double azimuthDeg)
    {
        Matrix4x4 ry = new Matrix4x4();
        Matrix4x4 rz = new Matrix4x4();
        Matrix4x4 t = new Matrix4x4();
        Matrix4x4 m;
        double azimuthRad = Math.toRadians(azimuthDeg);
        double x = scale(6.0 * Math.cos(azimuthRad));
        double y = scale(6.0 * Math.sin(azimuthRad));

        ry = ry.axisRotation(Math.toRadians(90.0), 0, 1, 0);
        rz = rz.axisRotation(Math.toRadians(azimuthDeg), 0, 0, 1);
        t = t.translation(x, y, scale(z));
        m = t.multiply(rz.multiply(ry));
        PolyhedralBoundedSolidModeler.applyTransformation(motif, m);
        return motif;
    }

    public static int getSingleMotifStarCount()
    {
        return STAR_COUNT;
    }

    public static int getSingleMotifMoonCount()
    {
        return MOON_COUNT;
    }

    public static int getSingleMotifCount()
    {
        return STAR_COUNT + MOON_COUNT;
    }

    public static int normalizeSingleMotifIndex(int motifIndex)
    {
        return Math.floorMod(motifIndex, getSingleMotifCount());
    }

    public static String describeSingleMotif(int motifIndex)
    {
        int normalizedIndex = normalizeSingleMotifIndex(motifIndex);
        int lastIndex = getSingleMotifCount() - 1;
        int typeIndex;

        if ( normalizedIndex < STAR_COUNT ) {
            typeIndex = normalizedIndex + 1;
            return "STAR " + typeIndex + "/" + STAR_COUNT +
                " index " + normalizedIndex + "/" + lastIndex;
        }

        typeIndex = normalizedIndex - STAR_COUNT + 1;
        return "MOON " + typeIndex + "/" + MOON_COUNT +
            " index " + normalizedIndex + "/" + lastIndex;
    }

    public static PolyhedralBoundedSolid[] createBowlAndFirstStarOperands()
    {
        return createBowlAndFirstStarOperands(0);
    }

    public static PolyhedralBoundedSolid[] createBowlAndFirstStarOperands(
        int motifIndex)
    {
        PolyhedralBoundedSolid[] operands =
            new PolyhedralBoundedSolid[2];
        PolyhedralBoundedSolid outer = createSphere(
            scale(10.0), new Vector3D(0, 0, scale(10.0)));
        PolyhedralBoundedSolid inner = createSphere(
            scale(9.5), new Vector3D(0, 0, scale(10.0)));
        PolyhedralBoundedSolid shell = booleanOp(
            outer, inner, PolyhedralBoundedSolidModeler.SUBTRACT);
        PolyhedralBoundedSolid bowl = booleanOp(
            shell,
            createCylinder(scale(10.5), scale(16.5), new Vector3D(0, 0, 0)),
            PolyhedralBoundedSolidModeler.INTERSECTION);

        operands[0] = bowl;
        operands[1] = createSingleMotif(motifIndex);
        return operands;
    }

    private static PolyhedralBoundedSolid createSingleMotif(int motifIndex)
    {
        int normalizedIndex = normalizeSingleMotifIndex(motifIndex);
        int motifTypeIndex;

        if ( normalizedIndex < STAR_COUNT ) {
            motifTypeIndex = normalizedIndex;
            return placeMotif(createStar(),
                getStarZ(motifTypeIndex),
                getStarAzimuthDeg(motifTypeIndex));
        }

        motifTypeIndex = normalizedIndex - STAR_COUNT;
        return placeMotif(createMoon(),
            getMoonZ(motifTypeIndex),
            getMoonAzimuthDeg(motifTypeIndex));
    }

    private static double getStarZ(int motifTypeIndex)
    {
        return getMotifValue(motifTypeIndex, STAR_Z_VALUES);
    }

    private static double getMoonZ(int motifTypeIndex)
    {
        return getMotifValue(motifTypeIndex, MOON_Z_VALUES);
    }

    private static double getStarAzimuthDeg(int motifTypeIndex)
    {
        return getMotifAzimuthDeg(motifTypeIndex, STAR_AZIMUTH_OFFSETS);
    }

    private static double getMoonAzimuthDeg(int motifTypeIndex)
    {
        return getMotifAzimuthDeg(motifTypeIndex, MOON_AZIMUTH_OFFSETS);
    }

    private static double getMotifValue(int motifTypeIndex, double[] values)
    {
        int positionIndex = motifTypeIndex % MOTIFS_PER_TYPE_RING;
        return values[positionIndex];
    }

    private static double getMotifAzimuthDeg(
        int motifTypeIndex, double[] offsets)
    {
        int positionIndex = motifTypeIndex % MOTIFS_PER_TYPE_RING;
        int ringIndex = motifTypeIndex / MOTIFS_PER_TYPE_RING + 1;
        double base = -90.0 * ringIndex;

        return base + offsets[positionIndex];
    }

    public static PolyhedralBoundedSolid[] createShellAndFirstMoonOperands()
    {
        PolyhedralBoundedSolid[] operands =
            new PolyhedralBoundedSolid[2];
        PolyhedralBoundedSolid outer = createSphere(
            scale(10.0), new Vector3D(0, 0, scale(10.0)));
        PolyhedralBoundedSolid inner = createSphere(
            scale(9.5), new Vector3D(0, 0, scale(10.0)));

        operands[0] = booleanOp(
            outer, inner, PolyhedralBoundedSolidModeler.SUBTRACT);
        operands[1] = placeMotif(createMoon(), 4.0, -90.0);
        return operands;
    }

    public static PolyhedralBoundedSolid create()
    {
        int i;
        int moonIndex = 0;
        int starIndex = 0;
        int motifIndex = 0;
        int moonCount = getSingleMotifMoonCount();
        int starCount = getSingleMotifStarCount();
        int motifCount = moonCount + starCount;
        printProgressMessage(
            "Processing Kurlander bowl all motifs: starting base shell");
        PolyhedralBoundedSolid outer = createSphere(
            scale(10.0), new Vector3D(0, 0, scale(10.0)));
        PolyhedralBoundedSolid inner = createSphere(
            scale(9.5), new Vector3D(0, 0, scale(10.0)));
        PolyhedralBoundedSolid shell = booleanOp(
            outer, inner, PolyhedralBoundedSolidModeler.SUBTRACT);
        printProgressMessage(
            "Processing Kurlander bowl all motifs: base shell ready");

        for ( i = 0; i < moonCount; i++ ) {
            moonIndex++;
            motifIndex++;
            printMotifProgress("moon", moonIndex, moonCount,
                motifIndex, motifCount);
            shell = booleanOpWithoutFaceMaximization(shell,
                placeMotif(createMoon(), getMoonZ(i), getMoonAzimuthDeg(i)),
                PolyhedralBoundedSolidModeler.SUBTRACT);
        }

        for ( i = 0; i < starCount; i++ ) {
            starIndex++;
            motifIndex++;
            printMotifProgress("star", starIndex, starCount,
                motifIndex, motifCount);
            shell = booleanOpWithoutFaceMaximization(shell,
                placeMotif(createStar(), getStarZ(i), getStarAzimuthDeg(i)),
                PolyhedralBoundedSolidModeler.SUBTRACT);
        }

        PolyhedralBoundedSolid guide = createCylinder(
            scale(10.5), scale(16.5), new Vector3D(0, 0, 0));
        printProgressMessage(
            "Processing Kurlander bowl all motifs: clipping final bowl");
        PolyhedralBoundedSolid result = booleanOpWithoutFaceMaximization(
            shell, guide, PolyhedralBoundedSolidModeler.INTERSECTION);

        PolyhedralBoundedSolidValidationEngine.validateIntermediate(result);
        printProgressMessage(
            "Processing Kurlander bowl all motifs: finished");
        return result;
    }

    private static void printMotifProgress(String motifType,
                                           int typeIndex,
                                           int typeCount,
                                           int motifIndex,
                                           int motifCount)
    {
        printProgressMessage("Processing " + motifType + " " + typeIndex +
            "/" + typeCount + ", motif " + motifIndex + "/" + motifCount);
    }

    private static void printProgressMessage(String message)
    {
        System.out.println(message);
        System.out.flush();
        System.err.println(message);
        System.err.flush();
    }
}
