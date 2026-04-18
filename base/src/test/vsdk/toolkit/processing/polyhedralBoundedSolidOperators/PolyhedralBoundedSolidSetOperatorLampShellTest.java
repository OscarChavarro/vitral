package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.geometry.Box;
import vsdk.toolkit.environment.geometry.Sphere;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.processing.GeometricModeler;

import static org.assertj.core.api.Assertions.assertThat;

class PolyhedralBoundedSolidSetOperatorLampShellTest
{
    private static final double EPSILON = 1.0e-6;
    private static final int EXPECTED_FACE_COUNT = 13;
    private static final double OUTER_RADIUS = 0.5;
    private static final double INNER_RADIUS = 0.45;
    private static final double CUBE_TOP_Z = 0.325 + 1.05 / 2.0;

    @Test
    void given_csgLampShell_defaultExampleConfiguration_when_intersectingShellAndCube_then_topCapIsOneFaceWithTwoRingsAndNoExtraTopFaces()
    {
        int subdivisionCircumference = 3;
        int subdivisionHeight = 1;

        PolyhedralBoundedSolid outerSphere = createSphere(
            OUTER_RADIUS, subdivisionCircumference, subdivisionHeight);
        PolyhedralBoundedSolid innerSphere = createSphere(
            INNER_RADIUS, subdivisionCircumference, subdivisionHeight);

        PolyhedralBoundedSolid sphericalShell = GeometricModeler.setOp(
            outerSphere, innerSphere, GeometricModeler.SUBTRACT, false);

        Box clipCubeGeometry = new Box(new Vector3D(1.4, 1.4, 1.05));
        PolyhedralBoundedSolid clipCube = clipCubeGeometry
            .exportToPolyhedralBoundedSolid();
        Matrix4x4 cubeMove = new Matrix4x4();
        cubeMove.translation(0.55, 0.55, 0.325);
        clipCube.applyTransformation(cubeMove);

        PolyhedralBoundedSolid result = GeometricModeler.setOp(
            sphericalShell, clipCube, GeometricModeler.INTERSECTION, false);

        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(result)).isTrue();
        assertThat(result.polygonsList.size()).isEqualTo(EXPECTED_FACE_COUNT);

        ArrayList<_PolyhedralBoundedSolidFace> topPlaneFaces;
        _PolyhedralBoundedSolidFace topFace;
        int i;

        topPlaneFaces = new ArrayList<_PolyhedralBoundedSolidFace>();
        for ( i = 0; i < result.polygonsList.size(); i++ ) {
            _PolyhedralBoundedSolidFace face = result.polygonsList.get(i);
            if ( isFaceOnTopPlane(face) ) {
                topPlaneFaces.add(face);
            }
        }

        assertThat(topPlaneFaces).hasSize(1);
        topFace = topPlaneFaces.get(0);
        assertThat(faceNormalZ(topFace)).isBetween(1.0 - EPSILON,
            1.0 + EPSILON);
        assertThat(topFace.boundariesList.size()).isEqualTo(2);

        double firstLoopRadius = maxLoopRadius(topFace.boundariesList.get(0));
        double secondLoopRadius = maxLoopRadius(topFace.boundariesList.get(1));
        double outerLoopRadius = Math.max(firstLoopRadius, secondLoopRadius);
        double innerLoopRadius = Math.min(firstLoopRadius, secondLoopRadius);
        assertThat(outerLoopRadius).isGreaterThan(innerLoopRadius);
    }

    private static PolyhedralBoundedSolid createSphere(
        double radius, int subdivisionCircumference, int subdivisionHeight)
    {
        Matrix4x4 move = new Matrix4x4();
        move.translation(0.55, 0.55, 0.55);

        Sphere sphere = new Sphere(radius);
        PolyhedralBoundedSolid solid = sphere.exportToPolyhedralBoundedSolid(
            subdivisionCircumference, subdivisionHeight);
        solid.applyTransformation(move);
        return solid;
    }

    private static boolean isFaceOnTopPlane(_PolyhedralBoundedSolidFace face)
    {
        if ( Math.abs(Math.abs(faceNormalZ(face)) - 1.0) > EPSILON ) {
            return false;
        }

        int i;
        for ( i = 0; i < face.boundariesList.size(); i++ ) {
            if ( !isLoopOnZPlane(face.boundariesList.get(i), CUBE_TOP_Z) ) {
                return false;
            }
        }
        return true;
    }

    private static double faceNormalZ(_PolyhedralBoundedSolidFace face)
    {
        if ( face.containingPlane == null ) {
            face.calculatePlane();
        }
        if ( face.containingPlane == null ) {
            return 0.0;
        }

        Vector3D normal = face.containingPlane.getNormal();
        return normal.z;
    }

    private static boolean isLoopOnZPlane(
        _PolyhedralBoundedSolidLoop loop, double expectedZ)
    {
        _PolyhedralBoundedSolidHalfEdge start = loop.boundaryStartHalfEdge;
        _PolyhedralBoundedSolidHalfEdge he = start;

        do {
            if ( Math.abs(he.startingVertex.position.z - expectedZ) >
                 EPSILON ) {
                return false;
            }
            he = he.next();
        } while ( he != start );

        return true;
    }

    private static double maxLoopRadius(_PolyhedralBoundedSolidLoop loop)
    {
        _PolyhedralBoundedSolidHalfEdge start = loop.boundaryStartHalfEdge;
        _PolyhedralBoundedSolidHalfEdge he = start;
        double maxRadius = 0.0;

        do {
            Vector3D p = he.startingVertex.position;
            double dx = p.x - 0.55;
            double dy = p.y - 0.55;
            double radius = Math.sqrt(dx*dx + dy*dy);
            if ( radius > maxRadius ) {
                maxRadius = radius;
            }
            he = he.next();
        } while ( he != start );

        return maxRadius;
    }
}
