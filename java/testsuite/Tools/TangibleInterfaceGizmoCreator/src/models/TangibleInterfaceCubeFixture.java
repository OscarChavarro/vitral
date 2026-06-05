package models;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.curve.ParametricCurve;
import vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidModeler;
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
        "M 53 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 4 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 4 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -4 v -4 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 Z",
        "M 100 33 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -6 h 3 v 3 h 4 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 6 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 7 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -4 v 3 h -3 Z",
        "M 3 87 v -4 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h 4 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 6 v 4 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -6 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 Z",
        "M 83 90 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v -4 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h 4 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 7 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -7 Z",
        "M 137 90 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -4 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -4 h 3 v -3 h 4 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 6 v 3 h -3 v 4 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 6 Z",
    };

    public PolyhedralBoundedSolid buildGizmoModel(int index) {
        PolyhedralBoundedSolid baseSolid =
            baseCreatePart(index, BASE_DEFAULT_EXTRUSION_HEIGHT);
        PolyhedralBoundedSolid glyphSolid = glyphCreateCenteredTextSolid(
            glyphForIndex(index), baseSolid, GLYPH_TEXT_Z_OFFSET);

        PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(
            baseSolid, glyphSolid, PolyhedralBoundedSolidModeler.UNION, false);
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
