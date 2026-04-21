package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.geometry.volume.Box;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;

import static org.assertj.core.api.Assertions.assertThat;

/**
Checks that boolean union preserves a rectangular ring on a hollow-brick
fixture.

<p>Traceability: [MANT1988] Ch. 9.2.4 global manipulations for rings/holes
and Ch. 15.8 setopfinish result assembly.</p>
 */
class PolyhedralBoundedSolidSetOperatorHollowBrickUnionTest
{
    private static final double EPSILON = 1.0e-6;
    private static final double TOP_Z = 0.2;
    private static final int EXPECTED_FACE_COUNT = 10;
    private static final int EXPECTED_EDGE_COUNT = 24;
    private static final int EXPECTED_VERTEX_COUNT = 16;

    @Test
    void given_hollowBrickOperands_when_union_then_topCapIsOneFaceWithRectangularRing()
    {
        PolyhedralBoundedSolid[] operands = createHollowBrickOperands();
        PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(
            operands[0], operands[1], PolyhedralBoundedSolidModeler.UNION, false);

        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(result)).isTrue();
        assertThat(result.polygonsList.size()).isEqualTo(EXPECTED_FACE_COUNT);
        assertThat(result.edgesList.size()).isEqualTo(EXPECTED_EDGE_COUNT);
        assertThat(result.verticesList.size()).isEqualTo(EXPECTED_VERTEX_COUNT);
        assertThat(countFacesWithLoopCount(result, 2)).isEqualTo(2);
        assertThat(countFacesWithLoopCount(result, 1)).isEqualTo(8);

        ArrayList<_PolyhedralBoundedSolidFace> topFaces =
            findFacesOnZPlane(result, TOP_Z);

        assertThat(topFaces).hasSize(1);

        _PolyhedralBoundedSolidFace topFace = topFaces.get(0);
        assertThat(faceNormalZ(topFace)).isBetween(1.0 - EPSILON,
            1.0 + EPSILON);
        assertThat(topFace.boundariesList.size()).isEqualTo(2);

        double firstLoopExtent = loopMaxXYExtent(topFace.boundariesList.get(0));
        double secondLoopExtent = loopMaxXYExtent(topFace.boundariesList.get(1));
        assertThat(firstLoopExtent).isGreaterThan(secondLoopExtent);
    }

    private static int countFacesWithLoopCount(
        PolyhedralBoundedSolid solid, int loopCount)
    {
        int i;
        int matches = 0;

        for ( i = 0; i < solid.polygonsList.size(); i++ ) {
            if ( solid.polygonsList.get(i).boundariesList.size() == loopCount ) {
                matches++;
            }
        }
        return matches;
    }

    private static PolyhedralBoundedSolid[] createHollowBrickOperands()
    {
        PolyhedralBoundedSolid[] operands = new PolyhedralBoundedSolid[2];
        PolyhedralBoundedSolid a = createTranslatedBox(1.0, 0.2, 0.2,
            0.5, 0.1, 0.1);
        PolyhedralBoundedSolid b = createTranslatedBox(1.0, 0.2, 0.2,
            0.5, 0.9, 0.1);
        PolyhedralBoundedSolid c = createTranslatedBox(0.2, 1.0, 0.2,
            0.1, 0.5, 0.1);
        PolyhedralBoundedSolid d = createTranslatedBox(0.2, 1.0, 0.2,
            0.9, 0.5, 0.1);

        operands[0] = PolyhedralBoundedSolidModeler.setOp(
            b, c, PolyhedralBoundedSolidModeler.UNION, false);
        operands[1] = PolyhedralBoundedSolidModeler.setOp(
            a, d, PolyhedralBoundedSolidModeler.UNION, false);
        return operands;
    }

    private static PolyhedralBoundedSolid createTranslatedBox(
        double sx, double sy, double sz,
        double tx, double ty, double tz)
    {
        Box box = new Box(new Vector3D(sx, sy, sz));
        PolyhedralBoundedSolid solid = box.exportToPolyhedralBoundedSolid();
        Matrix4x4 translation = new Matrix4x4();
        translation = translation.translation(tx, ty, tz);
        solid.applyTransformation(translation);
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(solid)).isTrue();
        return solid;
    }

    private static ArrayList<_PolyhedralBoundedSolidFace> findFacesOnZPlane(
        PolyhedralBoundedSolid solid, double zPlane)
    {
        ArrayList<_PolyhedralBoundedSolidFace> matches =
            new ArrayList<_PolyhedralBoundedSolidFace>();
        int i;

        for ( i = 0; i < solid.polygonsList.size(); i++ ) {
            _PolyhedralBoundedSolidFace face = solid.polygonsList.get(i);
            if ( isFaceOnZPlane(face, zPlane) ) {
                matches.add(face);
            }
        }
        return matches;
    }

    private static boolean isFaceOnZPlane(
        _PolyhedralBoundedSolidFace face, double zPlane)
    {
        int i;

        if ( Math.abs(Math.abs(faceNormalZ(face)) - 1.0) > EPSILON ) {
            return false;
        }

        for ( i = 0; i < face.boundariesList.size(); i++ ) {
            if ( !isLoopOnZPlane(face.boundariesList.get(i), zPlane) ) {
                return false;
            }
        }
        return true;
    }

    private static boolean isLoopOnZPlane(
        _PolyhedralBoundedSolidLoop loop, double zPlane)
    {
        _PolyhedralBoundedSolidHalfEdge start = loop.boundaryStartHalfEdge;
        _PolyhedralBoundedSolidHalfEdge he = start;

        do {
            if ( Math.abs(he.startingVertex.position.z() - zPlane) > EPSILON ) {
                return false;
            }
            he = he.next();
        } while ( he != start );

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
        return face.containingPlane.getNormal().z();
    }

    private static double loopMaxXYExtent(_PolyhedralBoundedSolidLoop loop)
    {
        _PolyhedralBoundedSolidHalfEdge start = loop.boundaryStartHalfEdge;
        _PolyhedralBoundedSolidHalfEdge he = start;
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        do {
            Vector3D p = he.startingVertex.position;
            minX = Math.min(minX, p.x());
            minY = Math.min(minY, p.y());
            maxX = Math.max(maxX, p.x());
            maxY = Math.max(maxY, p.y());
            he = he.next();
        } while ( he != start );

        return Math.max(maxX - minX, maxY - minY);
    }
}
