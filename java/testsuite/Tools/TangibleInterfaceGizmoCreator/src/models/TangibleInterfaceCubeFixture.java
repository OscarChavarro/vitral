package models;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.curve.ParametricCurve;
import vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidModeler;
import vsdk.toolkit.environment.geometry.volume.Cone;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidEulerOperators;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidTopologyEditing;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;
import vsdk.toolkit.render.awt.AwtFontReader;

public class TangibleInterfaceCubeFixture {
    private static final double BASE_PATTERN_SCALE = 0.01;
    private static final double BASE_POSITION_TOLERANCE = 1.0e-9;
    private static final double BASE_DEFAULT_EXTRUSION_HEIGHT = 0.03;
    private static final int GLYPH_MODEL_PART_1 = 0;
    private static final int GLYPH_MODEL_PART_2 = 1;
    private static final int GLYPH_MODEL_PART_3 = 2;
    private static final int GLYPH_MODEL_PART_4 = 3;
    private static final int GLYPH_MODEL_PART_5 = 4;
    private static final int BASE_ONLY_STICK = 5;
    private static final int BASE_ONLY_STICK_HOLED = 6;
    private static final int GLYPH_MODEL_PART_6 = 7;
    private static final int BASE_PATH_INDEX_PART_5 = 4;
    private static final int BASE_PATH_INDEX_PART_6 = 5;
    private static final int BASE_CYLINDER_SIDES = 32;
    private static final int BASE_CYLINDER_HEIGHT_DIVISIONS = 1;
    private static final double GLYPH_TEXT_EXTRUSION_HEIGHT = 0.027;
    private static final double GLYPH_TEXT_TARGET_EXTENT = 0.144;
    private static final double GLYPH_TEXT_Z_OFFSET = 0.01;
    private static final String[] GLYPH_TEXT_FONT_CANDIDATES = {
        "../../../../etc/fonts/cyrvetic.ttf"
    };
    private static final String[] GLYPH_CUBE_PART_CHARACTERS = {
        "A", "B", "C", "D", "E", "F"
    };

    private final String[] basePathLoops = new String[] {
        "M 0 40 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -4 h -3 v -3 h 6 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 4 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 4 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -7 Z",
        "M 3 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 4 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 4 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -4 v -4 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 Z",
        "M 0 33 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -6 h 3 v 3 h 4 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 6 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 7 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -4 v 3 h -3 Z",
        "M 3 37 v -4 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h 4 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 6 v 4 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -6 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 Z",
        "M 33 40 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v -4 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h 4 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 7 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -7 Z",
        "M 37 40 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -4 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -4 h 3 v -3 h 4 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 6 v 3 h -3 v 4 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 6 Z",
    };

    public PolyhedralBoundedSolid buildGizmoModel(int index) {
        return buildGizmoModel(index, 0.0, 0.0, 0.0);
    }

    public PolyhedralBoundedSolid buildGizmoModel(
        int index,
        double innerRadius,
        double outer,
        double baseHeight)
    {
        return switch ( index ) {
            case GLYPH_MODEL_PART_1, GLYPH_MODEL_PART_2, GLYPH_MODEL_PART_3,
                 GLYPH_MODEL_PART_4, GLYPH_MODEL_PART_5 ->
                glyphBuildModel(index, index);
            case GLYPH_MODEL_PART_6 ->
                glyphBuildModel(BASE_PATH_INDEX_PART_6, BASE_PATH_INDEX_PART_6);
            case BASE_ONLY_STICK ->
                baseBuildStickModel(innerRadius/100, outer/100, baseHeight/10);
            case BASE_ONLY_STICK_HOLED ->
                baseBuildStickHoledModel(innerRadius/100);
            default -> throw new IllegalArgumentException(
                "Unsupported gizmo model index: " + index);
        };
    }

    private PolyhedralBoundedSolid glyphBuildModel(int baseIndex, int glyphIndex)
    {
        PolyhedralBoundedSolid baseSolid =
            baseCreatePart(baseIndex, BASE_DEFAULT_EXTRUSION_HEIGHT);
        PolyhedralBoundedSolid glyphSolid = glyphCreateCenteredTextSolid(
            glyphForIndex(glyphIndex), baseSolid, GLYPH_TEXT_Z_OFFSET);

        PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(
            baseSolid, glyphSolid, PolyhedralBoundedSolidModeler.UNION, false);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(result);
        return result;
    }

    private PolyhedralBoundedSolid baseBuildStickModel(
        double innerRadius,
        double outterRadius,
        double baseHeight)
    {
        PolyhedralBoundedSolid baseSolid = baseCreatePart(
            BASE_PATH_INDEX_PART_6, BASE_DEFAULT_EXTRUSION_HEIGHT);
        double[] baseMinMax = baseSolid.getMinMax();
        Vector3Dd baseCenter = new Vector3Dd(
            (baseMinMax[0] + baseMinMax[3]) * 0.5,
            (baseMinMax[1] + baseMinMax[4]) * 0.5,
            0.0);
        PolyhedralBoundedSolid tubeSolid = baseCreateStickTube(
            innerRadius, outterRadius, baseHeight, baseCenter);

        PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(
            baseSolid, tubeSolid, PolyhedralBoundedSolidModeler.UNION, false);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(result);
        return result;
    }

    private PolyhedralBoundedSolid baseBuildStickHoledModel(double innerRadius)
    {
        PolyhedralBoundedSolid baseSolid = baseCreatePart(
            BASE_PATH_INDEX_PART_5, BASE_DEFAULT_EXTRUSION_HEIGHT);
        double[] baseMinMax = baseSolid.getMinMax();
        Vector3Dd baseCenter = new Vector3Dd(
            (baseMinMax[0] + baseMinMax[3]) * 0.5,
            (baseMinMax[1] + baseMinMax[4]) * 0.5,
            0.0);
        PolyhedralBoundedSolid holeCylinder = baseCreateCylinder(
            baseResolveInnerRadius(innerRadius, 0.45),
            BASE_DEFAULT_EXTRUSION_HEIGHT * 3.0,
            new Vector3Dd(
                baseCenter.x(),
                baseCenter.y(),
                -BASE_DEFAULT_EXTRUSION_HEIGHT));

        PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(
            baseSolid, holeCylinder, PolyhedralBoundedSolidModeler.SUBTRACT, false);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(result);
        return result;
    }

    private PolyhedralBoundedSolid baseCreatePart(int index, double extrusionHeight) {
        if ( index < 0 || index >= basePathLoops.length ) {
            throw new IllegalArgumentException("Pattern index out of range: " + index);
        }

        List<Vector3Dd> polygon = baseParsePattern(basePathLoops[index]);
        if ( polygon.size() < 3 ) {
            throw new IllegalArgumentException("Pattern " + index + " does not define a valid polygon");
        }

        PolyhedralBoundedSolid solid = new PolyhedralBoundedSolid();
        PolyhedralBoundedSolidEulerOperators.mvfs(solid, polygon.get(0), 1, 1);

        for ( int i = 1; i < polygon.size(); i++ ) {
            PolyhedralBoundedSolidEulerOperators.smev(
                solid, 1, i, i + 1, polygon.get(i));
        }
        PolyhedralBoundedSolidEulerOperators.smef(
            solid, 1, polygon.size(), 1, 2);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);

        Matrix4x4d sweep = new Matrix4x4d();
        sweep = sweep.translation(0.0, 0.0, extrusionHeight);
        PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(
            solid, solid.findFace(1), sweep);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);
        return solid;
    }

    private double baseResolveExtrusionHeight(double baseHeight)
    {
        if ( baseHeight > 0.0 ) {
            return baseHeight;
        }
        return BASE_DEFAULT_EXTRUSION_HEIGHT;
    }

    private PolyhedralBoundedSolid baseCreateStickTube(
        double innerRadius,
        double outterRadius,
        double baseHeight,
        Vector3Dd baseCenter)
    {
        double resolvedBaseHeight = baseResolveExtrusionHeight(baseHeight);
        double resolvedOuterRadius = baseResolveOuterRadius(outterRadius);
        double resolvedInnerRadius = baseResolveInnerRadius(
            innerRadius, resolvedOuterRadius);

        PolyhedralBoundedSolid cylinderA = baseCreateCylinder(
            resolvedOuterRadius,
            resolvedBaseHeight + BASE_DEFAULT_EXTRUSION_HEIGHT / 3.0,
            new Vector3Dd(
                baseCenter.x(),
                baseCenter.y(),
                2.0 * BASE_DEFAULT_EXTRUSION_HEIGHT / 3.0));
        PolyhedralBoundedSolid cylinderB = baseCreateCylinder(
            resolvedInnerRadius,
            resolvedBaseHeight + BASE_DEFAULT_EXTRUSION_HEIGHT,
            new Vector3Dd(
                baseCenter.x(),
                baseCenter.y(),
                BASE_DEFAULT_EXTRUSION_HEIGHT / 3.0));

        PolyhedralBoundedSolid tube = PolyhedralBoundedSolidModeler.setOp(
            cylinderA, cylinderB, PolyhedralBoundedSolidModeler.SUBTRACT, false);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(tube);
        return tube;
    }

    private PolyhedralBoundedSolid baseCreateCylinder(
        double radius,
        double height,
        Vector3Dd translation)
    {
        PolyhedralBoundedSolid solid = new Cone(radius, radius, height)
            .exportToPolyhedralBoundedSolid(
                BASE_CYLINDER_SIDES, BASE_CYLINDER_HEIGHT_DIVISIONS);
        Matrix4x4d move = new Matrix4x4d();
        move = move.translation(translation);
        PolyhedralBoundedSolidModeler.applyTransformation(solid, move);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);
        return solid;
    }

    private double baseResolveOuterRadius(double outterRadius)
    {
        if ( outterRadius > 0.0 ) {
            return outterRadius;
        }
        return 0.45;
    }

    private double baseResolveInnerRadius(
        double innerRadius,
        double outterRadius)
    {
        if ( innerRadius > 0.0 ) {
            return Math.min(innerRadius, outterRadius);
        }
        return outterRadius * 0.75;
    }

    private String glyphForIndex(int index)
    {
        if ( index < 0 || index >= GLYPH_CUBE_PART_CHARACTERS.length ) {
            throw new IllegalArgumentException("Glyph index out of range: " + index);
        }
        return GLYPH_CUBE_PART_CHARACTERS[index];
    }

    private PolyhedralBoundedSolid glyphCreateCenteredTextSolid(
        String glyph,
        PolyhedralBoundedSolid referenceSolid,
        double zOffset)
    {
        String fontPath = glyphResolveTextFontPath();
        AwtFontReader fontReader = new AwtFontReader();
        ParametricCurve curve = fontReader.extractGlyph(fontPath, glyph);
        if ( curve == null ) {
            throw new IllegalStateException("Unable to extract glyph " + glyph
                + " from font " + fontPath);
        }
        curve.setApproximationSteps(8);
        glyphNormalizeTextCurve(curve, referenceSolid, zOffset);

        PolyhedralBoundedSolid solid =
            PolyhedralBoundedSolidModeler.createBrepFromParametricCurve(curve);
        glyphSanitizeSolid(solid);

        Matrix4x4d sweep = new Matrix4x4d();
        sweep = sweep.translation(0.0, 0.0, GLYPH_TEXT_EXTRUSION_HEIGHT);
        PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(
            solid, solid.findFace(1), sweep);
        glyphSanitizeSolid(solid);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);
        return solid;
    }

    private void glyphNormalizeTextCurve(
        ParametricCurve curve,
        PolyhedralBoundedSolid referenceSolid,
        double zOffset)
    {
        double[] minMax = glyphCalculateCurveMinMax(curve);
        double width = minMax[3] - minMax[0];
        double height = minMax[4] - minMax[1];
        double maxExtent = Math.max(width, height);
        if ( maxExtent <= 0.0 ) {
            throw new IllegalStateException("Text glyph has invalid extent");
        }

        double scaleFactor = GLYPH_TEXT_TARGET_EXTENT / maxExtent;
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
        glyphTransformCurve(curve, translation.multiply(scale));
    }

    private void glyphSanitizeSolid(PolyhedralBoundedSolid solid)
    {
        PolyhedralBoundedSolidTopologyEditing.weldCoincidentVertices(
            solid, PolyhedralBoundedSolidNumericPolicy.forSolid(solid));
        PolyhedralBoundedSolidTopologyEditing.maximizeFaces(solid);
        PolyhedralBoundedSolidTopologyEditing.compactIds(solid);
    }

    private double[] glyphCalculateCurveMinMax(ParametricCurve curve)
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

    private void glyphTransformCurve(ParametricCurve curve, Matrix4x4d transform)
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

    private String glyphResolveTextFontPath()
    {
        for ( String candidate : GLYPH_TEXT_FONT_CANDIDATES ) {
            File file = new File(candidate);
            if ( file.isFile() ) {
                return file.getPath();
            }
        }
        throw new IllegalStateException("Font file not found in etc/fonts");
    }

    private List<Vector3Dd> baseParsePattern(String pattern)
    {
        String[] tokens = pattern.trim().split("\\s+");
        if ( tokens.length < 4 || !"M".equals(tokens[0]) ) {
            throw new IllegalArgumentException("Pattern must start with an SVG M command");
        }

        ArrayList<Vector3Dd> polygon = new ArrayList<Vector3Dd>();
        double x = baseParseScaledCoordinate(tokens[1]);
        double y = baseParseScaledCoordinate(tokens[2]);
        polygon.add(new Vector3Dd(x, y, 0.0));

        int i = 3;
        while ( i < tokens.length ) {
            String command = tokens[i++];
            if ( "Z".equals(command) ) {
                break;
            }
            if ( i >= tokens.length ) {
                throw new IllegalArgumentException("Missing operand for SVG command " + command);
            }

            double delta = baseParseScaledCoordinate(tokens[i++]);
            if ( "h".equals(command) ) {
                x += delta;
            }
            else if ( "v".equals(command) ) {
                y += delta;
            }
            else {
                throw new IllegalArgumentException("Unsupported SVG command: " + command);
            }
            polygon.add(new Vector3Dd(x, y, 0.0));
        }

        baseRemoveRepeatedClosure(polygon);
        return polygon;
    }

    private double baseParseScaledCoordinate(String token)
    {
        return Double.parseDouble(token) * BASE_PATTERN_SCALE;
    }

    private void baseRemoveRepeatedClosure(List<Vector3Dd> polygon)
    {
        if ( polygon.size() < 2 ) {
            return;
        }

        Vector3Dd first = polygon.get(0);
        Vector3Dd last = polygon.get(polygon.size() - 1);
        if ( Vector3Dd.distance(first, last) < BASE_POSITION_TOLERANCE ) {
            polygon.remove(polygon.size() - 1);
        }
    }
}
