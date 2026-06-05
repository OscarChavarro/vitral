package models;

import java.util.ArrayList;
import java.util.List;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidModeler;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidEulerOperators;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;

public class TangibleInterfaceCubeFixture {
    private static final double PATTERN_SCALE = 0.01;
    private static final double POSITION_TOLERANCE = 1.0e-9;

    String[] patterns = new String[] {
        "M 0 40 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -4 h -3 v -3 h 6 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 4 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 4 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -7 Z",
        "M 90 7 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 6 h -3 v -3 h -4 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -6 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -7 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 4 v -3 h 3 Z",
        "M 103 37 v -4 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h 4 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 6 v 4 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -6 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 Z",
        "M 0 87 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -4 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 4 v 3 h 3 v 4 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 6 h -3 v -3 h -4 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -6 Z",
        "M 90 57 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h -4 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v -4 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 7 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 7 Z",
        "M 103 87 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -3 h -3 v -3 h 3 v -4 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 3 v -3 h 3 v 3 h 4 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 3 h 3 v 3 h -3 v 4 h -4 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 v 3 h -3 v -3 h -3 Z"
    };

    PolyhedralBoundedSolid createPart(int index, double extrusionHeight) {
        if ( index < 0 || index >= patterns.length ) {
            throw new IllegalArgumentException("Pattern index out of range: " + index);
        }

        List<Vector3Dd> polygon = parsePattern(patterns[index]);
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

    private List<Vector3Dd> parsePattern(String pattern)
    {
        String[] tokens = pattern.trim().split("\\s+");
        if ( tokens.length < 4 || !"M".equals(tokens[0]) ) {
            throw new IllegalArgumentException("Pattern must start with an SVG M command");
        }

        ArrayList<Vector3Dd> polygon = new ArrayList<Vector3Dd>();
        double x = parseScaledCoordinate(tokens[1]);
        double y = parseScaledCoordinate(tokens[2]);
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

            double delta = parseScaledCoordinate(tokens[i++]);
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

        removeRepeatedClosure(polygon);
        return polygon;
    }

    private double parseScaledCoordinate(String token)
    {
        return Double.parseDouble(token) * PATTERN_SCALE;
    }

    private void removeRepeatedClosure(List<Vector3Dd> polygon)
    {
        if ( polygon.size() < 2 ) {
            return;
        }

        Vector3Dd first = polygon.get(0);
        Vector3Dd last = polygon.get(polygon.size() - 1);
        if ( Vector3Dd.distance(first, last) < POSITION_TOLERANCE ) {
            polygon.remove(polygon.size() - 1);
        }
    }
}
