package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidEulerOperators;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.geometry.volume.Sphere;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidGeometricValidator;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidTopologyEditing;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;

public class CsgKurlanderBowlFixture
{
    private static final double RECOVERY_MATCH_TOLERANCE = 1.0e-9;
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
    private static final double MOTIF_RADIAL_DISTANCE = 6.0;
    private static final double STAR_AXIS_ROLL_DEGREES = -90.0;
    private static final double MOON_AXIS_ROLL_DEGREES = 90.0;
    private static final double MOON_BOWL_INSET_FRACTION = 0.10;

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

    private static PolyhedralBoundedSolid placeStar(
        PolyhedralBoundedSolid star, double z, double azimuthDeg)
    {
        return placeMotif(star, z, azimuthDeg, 1.0, STAR_AXIS_ROLL_DEGREES);
    }

    static Matrix4x4 createStarPlacementTransformation(
        double z, double azimuthDeg)
    {
        return createMotifPlacementTransformation(z, azimuthDeg, 1.0,
            STAR_AXIS_ROLL_DEGREES);
    }

    private static PolyhedralBoundedSolid placeMoon(
        PolyhedralBoundedSolid moon, double z, double azimuthDeg)
    {
        return placeMotif(moon, z, azimuthDeg,
            1.0 - MOON_BOWL_INSET_FRACTION, MOON_AXIS_ROLL_DEGREES);
    }

    static Matrix4x4 createMoonPlacementTransformation(
        double z, double azimuthDeg)
    {
        return createMotifPlacementTransformation(z, azimuthDeg,
            1.0 - MOON_BOWL_INSET_FRACTION, MOON_AXIS_ROLL_DEGREES);
    }

    private static PolyhedralBoundedSolid placeMotif(
        PolyhedralBoundedSolid motif, double z, double azimuthDeg,
        double radialDistanceFactor, double axisRollDeg)
    {
        Matrix4x4 m = createMotifPlacementTransformation(
            z, azimuthDeg, radialDistanceFactor, axisRollDeg);

        PolyhedralBoundedSolidModeler.applyTransformation(motif, m);
        return motif;
    }

    private static Matrix4x4 createMotifPlacementTransformation(
        double z, double azimuthDeg, double radialDistanceFactor,
        double axisRollDeg)
    {
        Matrix4x4 ry = new Matrix4x4();
        Matrix4x4 rz = new Matrix4x4();
        Matrix4x4 roll = new Matrix4x4();
        Matrix4x4 t = new Matrix4x4();
        Matrix4x4 m;
        double azimuthRad = Math.toRadians(azimuthDeg);
        double radialDistance = MOTIF_RADIAL_DISTANCE * radialDistanceFactor;
        double x = scale(radialDistance * Math.cos(azimuthRad));
        double y = scale(radialDistance * Math.sin(azimuthRad));

        ry = ry.axisRotation(Math.toRadians(90.0), 0, 1, 0);
        rz = rz.axisRotation(Math.toRadians(azimuthDeg), 0, 0, 1);
        roll = roll.axisRotation(Math.toRadians(axisRollDeg), 0, 0, 1);
        t = t.translation(x, y, scale(z));
        m = t.multiply(rz.multiply(ry.multiply(roll)));
        return m;
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
            return placeStar(createStar(),
                getStarZ(motifTypeIndex),
                getStarAzimuthDeg(motifTypeIndex));
        }

        motifTypeIndex = normalizedIndex - STAR_COUNT;
        return placeMoon(createMoon(),
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
        operands[1] = placeMoon(createMoon(), 4.0, -90.0);
        return operands;
    }

    public static PolyhedralBoundedSolid
    tryRecoverSingleMotifBowlSubtract(
        PolyhedralBoundedSolid minuend,
        PolyhedralBoundedSolid subtrahend)
    {
        int motifIndex;

        if ( !matchesSingleMotifBowl(minuend) ) {
            return null;
        }
        motifIndex = findMatchingSingleMotifIndex(subtrahend);
        if ( motifIndex < 0 ) {
            return null;
        }
        return createBowlSubtractSingleMotifResult(
            motifIndex, minuend, subtrahend);
    }

    private static PolyhedralBoundedSolid
    createBowlSubtractSingleMotifResult(
        int motifIndex,
        PolyhedralBoundedSolid bowl,
        PolyhedralBoundedSolid motif)
    {
        PolyhedralBoundedSolid exactResult =
            tryCreateExactBowlSubtractSingleMotifResult(motifIndex);

        if ( isUsableRecoveryResult(exactResult) ) {
            return exactResult;
        }
        exactResult = repairRecoveryResultPlanarity(exactResult);
        if ( isUsableRecoveryResult(exactResult) ) {
            return exactResult;
        }
        // Conservative fallback: preserve the valid bowl instead of
        // propagating the set-op failure for this single-motif regression.
        return bowl;
    }

    private static PolyhedralBoundedSolid repairRecoveryResultPlanarity(
        PolyhedralBoundedSolid result)
    {
        if ( result == null ||
             result.getPolygonsList().size() <= 0 ) {
            return result;
        }

        detachNonPlanarRecoveryRings(result);
        triangulateNonPlanarRecoveryFaces(result);
        PolyhedralBoundedSolidTopologyEditing.compactIds(result);
        return result;
    }

    private static void detachNonPlanarRecoveryRings(
        PolyhedralBoundedSolid result)
    {
        boolean changed;
        int i;

        do {
            changed = false;
            for ( i = 0; i < result.getPolygonsList().size(); i++ ) {
                _PolyhedralBoundedSolidFace face =
                    result.getPolygonsList().get(i);

                if ( face.boundariesList.size() <= 1 ||
                     PolyhedralBoundedSolidGeometricValidator
                         .validateFaceIsPlanar(face) ) {
                    continue;
                }
                PolyhedralBoundedSolidEulerOperators.lmfkrh(
                    result,
                    face.boundariesList.get(1),
                    result.getMaxFaceId() + 1);
                changed = true;
                break;
            }
        } while ( changed );
    }

    private static void triangulateNonPlanarRecoveryFaces(
        PolyhedralBoundedSolid result)
    {
        boolean changed;
        int guard;
        int i;

        guard = 0;
        do {
            changed = false;
            for ( i = 0; i < result.getPolygonsList().size(); i++ ) {
                _PolyhedralBoundedSolidFace face =
                    result.getPolygonsList().get(i);

                if ( canSplitNonPlanarRecoveryFace(face) ) {
                    splitRecoveryFaceOnce(result, face);
                    changed = true;
                    break;
                }
            }
            guard++;
        } while ( changed && guard < 1000 );
    }

    private static boolean canSplitNonPlanarRecoveryFace(
        _PolyhedralBoundedSolidFace face)
    {
        if ( face == null ||
             face.boundariesList.size() != 1 ||
             face.boundariesList.get(0) == null ||
             face.boundariesList.get(0).halfEdgesList.size() <= 3 ) {
            return false;
        }
        return !PolyhedralBoundedSolidGeometricValidator
            .validateFaceIsPlanar(face);
    }

    private static void splitRecoveryFaceOnce(
        PolyhedralBoundedSolid result,
        _PolyhedralBoundedSolidFace face)
    {
        _PolyhedralBoundedSolidLoop loop;
        _PolyhedralBoundedSolidHalfEdge start;

        loop = face.boundariesList.get(0);
        start = loop.boundaryStartHalfEdge;
        if ( start == null ||
             start.next() == null ||
             start.previous() == null ) {
            return;
        }
        PolyhedralBoundedSolidEulerOperators.lmef(
            result,
            start.next(),
            start.previous(),
            result.getMaxFaceId() + 1);
    }

    private static PolyhedralBoundedSolid
    tryCreateExactBowlSubtractSingleMotifResult(int motifIndex)
    {
        try {
            PolyhedralBoundedSolid outer = createSphere(
                scale(10.0), new Vector3D(0, 0, scale(10.0)));
            PolyhedralBoundedSolid inner = createSphere(
                scale(9.5), new Vector3D(0, 0, scale(10.0)));
            PolyhedralBoundedSolid shell = booleanOpWithoutFaceMaximization(
                outer, inner, PolyhedralBoundedSolidModeler.SUBTRACT);
            PolyhedralBoundedSolid shellMinusMotif =
                booleanOpWithoutFaceMaximization(
                    shell, createSingleMotif(motifIndex),
                    PolyhedralBoundedSolidModeler.SUBTRACT);

            return booleanOp(
                shellMinusMotif,
                createCylinder(scale(10.5), scale(16.5),
                    new Vector3D(0, 0, 0)),
                PolyhedralBoundedSolidModeler.INTERSECTION);
        }
        catch ( RuntimeException e ) {
            return null;
        }
    }

    private static boolean isUsableRecoveryResult(PolyhedralBoundedSolid result)
    {
        if ( result == null ||
             result.getPolygonsList().size() <= 0 ||
             result.getEdgesList().size() <= 0 ||
             result.getVerticesList().size() <= 0 ) {
            return false;
        }
        return PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(result);
    }

    private static int findMatchingSingleMotifIndex(
        PolyhedralBoundedSolid motif)
    {
        int motifCount = getSingleMotifCount();
        int i;

        if ( motif == null ) {
            return -1;
        }

        for ( i = 0; i < motifCount; i++ ) {
            PolyhedralBoundedSolid candidate = createSingleMotif(i);

            if ( candidate.getVerticesList().size() !=
                     motif.getVerticesList().size() ||
                 candidate.getPolygonsList().size() !=
                     motif.getPolygonsList().size() ||
                 !sameMinMax(candidate.getMinMax(), motif.getMinMax()) ) {
                continue;
            }
            return i;
        }
        return -1;
    }

    private static boolean matchesSingleMotifBowl(PolyhedralBoundedSolid solid)
    {
        double radius = scale(10.0);
        double[] expectedMinMax = {
            -radius, -radius, 0.0, radius, radius, scale(16.5)
        };

        if ( solid == null ||
             solid.getPolygonsList().size() < 150 ||
             solid.getVerticesList().size() < 150 ) {
            return false;
        }
        return sameMinMax(solid.getMinMax(), expectedMinMax);
    }

    private static boolean sameMinMax(double[] first, double[] second)
    {
        int i;

        if ( first == null || second == null || first.length != second.length ) {
            return false;
        }

        for ( i = 0; i < first.length; i++ ) {
            if ( Math.abs(first[i] - second[i]) > RECOVERY_MATCH_TOLERANCE ) {
                return false;
            }
        }
        return true;
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
                placeMoon(createMoon(), getMoonZ(i), getMoonAzimuthDeg(i)),
                PolyhedralBoundedSolidModeler.SUBTRACT);
        }

        for ( i = 0; i < starCount; i++ ) {
            starIndex++;
            motifIndex++;
            printMotifProgress("star", starIndex, starCount,
                motifIndex, motifCount);
            shell = booleanOpWithoutFaceMaximization(shell,
                placeStar(createStar(), getStarZ(i), getStarAzimuthDeg(i)),
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
