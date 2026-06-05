package models;

import java.io.File;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.environment.geometry.curve.ParametricCurve;
import vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidModeler;
import vsdk.toolkit.environment.geometry.volume.Box;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidTopologyEditing;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;
import vsdk.toolkit.render.awt.AwtFontReader;

public final class GeneralModelsBuilder
{
    private static final double DEFAULT_EXTRUSION_HEIGHT = 0.03;
    private static final String[] TEXT_FONT_CANDIDATES = {
        "../../../../etc/fonts/cyrvetic.ttf"
    };
    private static final String[] CUBE_PART_GLYPHS = {
        "A", "B", "C", "D", "E", "F"
    };
    private static final String TEXT_GLYPH = "A";
    private static final double TEXT_TARGET_EXTENT = 0.144;
    private static final double TEXT_Z_OFFSET = 0.01;

    private GeneralModelsBuilder()
    {
    }

    public static PolyhedralBoundedSolid buildSolid(TangibleInterfaceGizmosModel model)
    {
        model.clampSubdivisions();
        switch ( model.getSolidModelName() ) {
            case CUBE_PART_1:
                return createLabeledCubePart(0, CUBE_PART_GLYPHS[0]);
            case CUBE_PART_2:
                return createLabeledCubePart(1, CUBE_PART_GLYPHS[1]);
            case CUBE_PART_3:
                return createLabeledCubePart(2, CUBE_PART_GLYPHS[2]);
            case CUBE_PART_4:
                return createLabeledCubePart(3, CUBE_PART_GLYPHS[3]);
            case CUBE_PART_5:
                return createLabeledCubePart(4, CUBE_PART_GLYPHS[4]);
            case CUBE_PART_6:
                return createLabeledCubePart(5, CUBE_PART_GLYPHS[5]);
            case EXPERIMENTAL:
                return createExperimental();
            default:
                return createLabeledCubePart(0, CUBE_PART_GLYPHS[0]);
        }
    }

    private static PolyhedralBoundedSolid createExperimental()
    {
        return new Box(1, 1, 1).exportToPolyhedralBoundedSolid();
    }

    private static PolyhedralBoundedSolid createLabeledCubePart(
        int partIndex,
        String glyph)
    {
        PolyhedralBoundedSolid solidA =
            new TangibleInterfaceCubeFixture().createPart(
                partIndex, DEFAULT_EXTRUSION_HEIGHT);
        PolyhedralBoundedSolid solidB =
            createCenteredTextSolid(glyph, solidA, TEXT_Z_OFFSET);

        PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(
            solidA, solidB, PolyhedralBoundedSolidModeler.UNION, false);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(result);
        return result;
    }

    private static PolyhedralBoundedSolid createCenteredTextSolid(
        String glyph,
        PolyhedralBoundedSolid referenceSolid,
        double zOffset)
    {
        String fontPath = resolveTextFontPath();
        AwtFontReader fontReader = new AwtFontReader();
        ParametricCurve curve = fontReader.extractGlyph(fontPath, glyph);
        if ( curve == null ) {
            throw new IllegalStateException("Unable to extract glyph " + glyph
                + " from font " + fontPath);
        }
        curve.setApproximationSteps(8);
        normalizeTextCurve(curve, referenceSolid, zOffset);

        PolyhedralBoundedSolid solid =
            PolyhedralBoundedSolidModeler.createBrepFromParametricCurve(curve);
        sanitizeSolid(solid);

        Matrix4x4d sweep = new Matrix4x4d();
        sweep = sweep.translation(0.0, 0.0, DEFAULT_EXTRUSION_HEIGHT);
        PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(
            solid, solid.findFace(1), sweep);
        sanitizeSolid(solid);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);
        return solid;
    }

    private static void normalizeTextCurve(
        ParametricCurve curve,
        PolyhedralBoundedSolid referenceSolid,
        double zOffset)
    {
        double[] minMax = calculateCurveMinMax(curve);
        double width = minMax[3] - minMax[0];
        double height = minMax[4] - minMax[1];
        double maxExtent = Math.max(width, height);
        if ( maxExtent <= 0.0 ) {
            throw new IllegalStateException("Text glyph has invalid extent");
        }

        double scaleFactor = TEXT_TARGET_EXTENT / maxExtent;
        double sourceCenterX = (minMax[0] + minMax[3]) * 0.5;
        double sourceCenterY = (minMax[1] + minMax[4]) * 0.5;

        double targetCenterX = 0.55;
        double targetCenterY = 0.55;
        if ( referenceSolid != null ) {
            double[] referenceMinMax = referenceSolid.getMinMax();
            targetCenterX = (referenceMinMax[0] + referenceMinMax[3]) * 0.5;
            targetCenterY = (referenceMinMax[1] + referenceMinMax[4]) * 0.5;
        }

        Matrix4x4d scale = new Matrix4x4d();
        scale = scale.scale(scaleFactor, scaleFactor, 1.0);
        Matrix4x4d translation = new Matrix4x4d();
        translation = translation.translation(
            targetCenterX - sourceCenterX * scaleFactor,
            targetCenterY - sourceCenterY * scaleFactor,
            zOffset);
        transformCurve(curve, translation.multiply(scale));
    }

    private static void sanitizeSolid(PolyhedralBoundedSolid solid)
    {
        PolyhedralBoundedSolidTopologyEditing.weldCoincidentVertices(
            solid, PolyhedralBoundedSolidNumericPolicy.forSolid(solid));
        PolyhedralBoundedSolidTopologyEditing.maximizeFaces(solid);
        PolyhedralBoundedSolidTopologyEditing.compactIds(solid);
    }

    private static double[] calculateCurveMinMax(ParametricCurve curve)
    {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;

        for ( int i = 0; i < curve.points.size(); i++ ) {
            var controlPoints = curve.points.get(i);
            if ( controlPoints == null ) {
                continue;
            }
            for ( int j = 0; j < controlPoints.length; j++ ) {
                if ( controlPoints[j] == null ) {
                    continue;
                }
                minX = Math.min(minX, controlPoints[j].x());
                minY = Math.min(minY, controlPoints[j].y());
                minZ = Math.min(minZ, controlPoints[j].z());
                maxX = Math.max(maxX, controlPoints[j].x());
                maxY = Math.max(maxY, controlPoints[j].y());
                maxZ = Math.max(maxZ, controlPoints[j].z());
            }
        }
        return new double[] { minX, minY, minZ, maxX, maxY, maxZ };
    }

    private static void transformCurve(ParametricCurve curve, Matrix4x4d transform)
    {
        for ( int i = 0; i < curve.points.size(); i++ ) {
            var controlPoints = curve.points.get(i);
            if ( controlPoints == null ) {
                continue;
            }
            for ( int j = 0; j < controlPoints.length; j++ ) {
                if ( controlPoints[j] != null ) {
                    controlPoints[j] = transform.multiply(controlPoints[j]);
                }
            }
        }
    }

    private static String resolveTextFontPath()
    {
        for ( String candidate : TEXT_FONT_CANDIDATES ) {
            File file = new File(candidate);
            if ( file.isFile() ) {
                return file.getPath();
            }
        }
        throw new IllegalStateException("Font file not found in etc/fonts");
    }
}
