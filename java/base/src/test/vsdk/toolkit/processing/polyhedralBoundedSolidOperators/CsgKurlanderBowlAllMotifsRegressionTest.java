package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class CsgKurlanderBowlAllMotifsRegressionTest
{
    private static final double GEOMETRY_TOLERANCE = 1.0e-9;
    private static final double TOP_MOUTH_TOLERANCE = 1.0e-7;
    private static final int FIRST_MOON_MOTIF_INDEX = 20;
    private static final int THIRD_MOON_MOTIF_INDEX = 22;

    @Test
    void given_kurlanderBowlAndFirstMoon_when_subtractingMoonFromBowl_then_resultStaysNonEmptyAndIntermediateValid()
    {
        PolyhedralBoundedSolid[] operands =
            CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(
                FIRST_MOON_MOTIF_INDEX);
        double topZ = operands[0].getMinMax()[5];
        double innerMouthRadius = findReferenceTopInnerMouthRadius(
            operands[0], topZ);
        PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(
            operands[0], operands[1], PolyhedralBoundedSolidModeler.SUBTRACT,
            false);

        assertThat(result).isNotNull();
        assertThat(result.getPolygonsList().size()).isGreaterThan(0);
        assertThat(result.getEdgesList().size()).isGreaterThan(0);
        assertThat(result.getVerticesList().size()).isGreaterThan(0);
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(result)).isTrue();
        assertThat(countTopMouthCapFaces(result, topZ, innerMouthRadius))
            .as("Subtracting the moon must keep the bowl mouth open; " +
                "no planar cap should be created over the inner top rim")
            .isZero();
    }

    @Test
    void given_kurlanderBowlAndThirdMoon_when_subtractingMoonFromBowl_then_resultStaysNonEmptyAndIntermediateValid()
    {
        PolyhedralBoundedSolid[] operands =
            CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(
                THIRD_MOON_MOTIF_INDEX);
        int originalFaces = operands[0].getPolygonsList().size();
        int originalEdges = operands[0].getEdgesList().size();
        int originalVertices = operands[0].getVerticesList().size();
        double[] originalBounds = operands[0].getMinMax();
        PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(
            operands[0], operands[1], PolyhedralBoundedSolidModeler.SUBTRACT,
            false);

        assertThat(result).isNotNull();
        assertThat(result.getPolygonsList().size()).isGreaterThan(0);
        assertThat(result.getEdgesList().size()).isGreaterThan(0);
        assertThat(result.getVerticesList().size()).isGreaterThan(0);
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(result)).isTrue();
        assertThat(hasSameShapeData(result, originalFaces, originalEdges,
            originalVertices, originalBounds))
            .as("Subtracting the third moon must actually modify the bowl; " +
                "returning the unmodified operand A hides the failed cut")
            .isFalse();
    }

    @Test
    void given_kurlanderShellAndFirstMoon_when_subtractingMoonFromShell_then_resultStaysValid()
    {
        PolyhedralBoundedSolid[] operands =
            CsgKurlanderBowlFixture.createShellAndFirstMoonOperands();
        PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(
            operands[0], operands[1], PolyhedralBoundedSolidModeler.SUBTRACT,
            false, false);

        assertThat(result).isNotNull();
        assertThat(result.getPolygonsList().size()).isGreaterThan(0);
        assertThat(result.getEdgesList().size()).isGreaterThan(0);
        assertThat(result.getVerticesList().size()).isGreaterThan(0);
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(result)).isTrue();
    }

    @Test
    void given_kurlanderFirstStarPlacement_whenCreated_thenTopTipStaysUprightAgainstZ()
    {
        Matrix4x4 placement =
            CsgKurlanderBowlFixture.createStarPlacementTransformation(
                9.0, -90.0);
        Vector3D origin = placement.multiply(new Vector3D());
        Vector3D extrusionAxis = placement.multiply(
            new Vector3D(0.0, 0.0, 0.55)).subtract(origin);
        Vector3D topTip = placement.multiply(
            new Vector3D(0.0, -0.2, 0.0)).subtract(origin);

        assertVectorClose(origin, new Vector3D(0.0, -0.6, 0.9));
        assertVectorClose(extrusionAxis, new Vector3D(0.0, -0.55, 0.0));
        assertVectorClose(topTip, new Vector3D(0.0, 0.0, 0.2));
    }

    @Test
    void given_kurlanderFirstMoonOperand_whenCreated_thenMoonIsRolledAndInsetIntoBowl()
    {
        PolyhedralBoundedSolid[] operands =
            CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(
                FIRST_MOON_MOTIF_INDEX);
        PolyhedralBoundedSolid moon = operands[1];
        double[] minMax = moon.getMinMax();
        Matrix4x4 placement =
            CsgKurlanderBowlFixture.createMoonPlacementTransformation(
                4.0, -90.0);
        Vector3D origin = placement.multiply(new Vector3D());
        Vector3D cylinderAxis = placement.multiply(
            new Vector3D(0.0, 0.0, 0.5)).subtract(origin);
        Vector3D crescentOffset = placement.multiply(
            new Vector3D(0.11, 0.0, 0.06)).subtract(origin);

        assertThat(moon.getVerticesList().size()).isGreaterThan(0);
        assertThat(minMax[1]).isCloseTo(-1.04, within(GEOMETRY_TOLERANCE));
        assertThat(minMax[4]).isCloseTo(-0.54, within(GEOMETRY_TOLERANCE));
        assertVectorClose(origin, new Vector3D(0.0, -0.54, 0.4));
        assertVectorClose(cylinderAxis, new Vector3D(0.0, -0.5, 0.0));
        assertVectorClose(crescentOffset, new Vector3D(0.11, -0.06, 0.0));
    }

    private static void assertVectorClose(Vector3D actual, Vector3D expected)
    {
        assertThat(actual.x()).isCloseTo(expected.x(),
            within(GEOMETRY_TOLERANCE));
        assertThat(actual.y()).isCloseTo(expected.y(),
            within(GEOMETRY_TOLERANCE));
        assertThat(actual.z()).isCloseTo(expected.z(),
            within(GEOMETRY_TOLERANCE));
    }

    private static int countTopMouthCapFaces(
        PolyhedralBoundedSolid result,
        double topZ,
        double innerMouthRadius)
    {
        int count = 0;
        int i;

        for ( i = 0; i < result.getPolygonsList().size(); i++ ) {
            _PolyhedralBoundedSolidFace face =
                result.getPolygonsList().get(i);

            if ( isTopMouthCapFace(face, topZ, innerMouthRadius) ) {
                count++;
            }
        }
        return count;
    }

    private static double findReferenceTopInnerMouthRadius(
        PolyhedralBoundedSolid referenceBowl,
        double topZ)
    {
        double innerRadius = Double.POSITIVE_INFINITY;
        int i;

        for ( i = 0; i < referenceBowl.getPolygonsList().size(); i++ ) {
            _PolyhedralBoundedSolidFace face =
                referenceBowl.getPolygonsList().get(i);

            if ( isTopFace(face, topZ) && face.boundariesList.size() > 1 ) {
                innerRadius = Math.min(innerRadius,
                    findMinimumRadius(face));
            }
        }
        if ( Double.isInfinite(innerRadius) ) {
            throw new AssertionError(
                "Expected reference bowl to expose an open top annulus");
        }
        return innerRadius;
    }

    private static boolean isTopMouthCapFace(
        _PolyhedralBoundedSolidFace face,
        double topZ,
        double innerMouthRadius)
    {
        return isTopFace(face, topZ) &&
               face.boundariesList.size() == 1 &&
               countHalfEdges(face) >= 3 &&
               findMaximumRadius(face) <= innerMouthRadius +
                   TOP_MOUTH_TOLERANCE;
    }

    private static boolean isTopFace(_PolyhedralBoundedSolidFace face,
                                     double topZ)
    {
        int i;

        if ( face == null || face.boundariesList.size() < 1 ) {
            return false;
        }
        for ( i = 0; i < face.boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(i);

            if ( loop == null ||
                 loop.boundaryStartHalfEdge == null ||
                 !loopStaysOnZ(loop, topZ) ) {
                return false;
            }
        }
        return true;
    }

    private static boolean loopStaysOnZ(
        _PolyhedralBoundedSolidLoop loop,
        double z)
    {
        _PolyhedralBoundedSolidHalfEdge halfEdge =
            loop.boundaryStartHalfEdge;
        _PolyhedralBoundedSolidHalfEdge start = halfEdge;

        do {
            if ( Math.abs(halfEdge.startingVertex.position.z() - z) >
                 TOP_MOUTH_TOLERANCE ) {
                return false;
            }
            halfEdge = halfEdge.next();
        } while ( halfEdge != start );
        return true;
    }

    private static double findMinimumRadius(_PolyhedralBoundedSolidFace face)
    {
        return findExtremeRadius(face, true);
    }

    private static double findMaximumRadius(_PolyhedralBoundedSolidFace face)
    {
        return findExtremeRadius(face, false);
    }

    private static double findExtremeRadius(
        _PolyhedralBoundedSolidFace face,
        boolean minimum)
    {
        double radius = minimum ?
            Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
        int i;

        for ( i = 0; i < face.boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(i);
            _PolyhedralBoundedSolidHalfEdge halfEdge =
                loop.boundaryStartHalfEdge;
            _PolyhedralBoundedSolidHalfEdge start = halfEdge;

            do {
                double currentRadius = Math.hypot(
                    halfEdge.startingVertex.position.x(),
                    halfEdge.startingVertex.position.y());

                radius = minimum ?
                    Math.min(radius, currentRadius) :
                    Math.max(radius, currentRadius);
                halfEdge = halfEdge.next();
            } while ( halfEdge != start );
        }
        return radius;
    }

    private static int countHalfEdges(_PolyhedralBoundedSolidFace face)
    {
        int count = 0;
        int i;

        for ( i = 0; i < face.boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(i);

            if ( loop != null ) {
                count += loop.halfEdgesList.size();
            }
        }
        return count;
    }

    private static boolean hasSameShapeData(
        PolyhedralBoundedSolid result,
        int originalFaces,
        int originalEdges,
        int originalVertices,
        double[] originalBounds)
    {
        return result.getPolygonsList().size() == originalFaces &&
               result.getEdgesList().size() == originalEdges &&
               result.getVerticesList().size() == originalVertices &&
               hasSameBounds(result.getMinMax(), originalBounds);
    }

    private static boolean hasSameBounds(double[] actual, double[] expected)
    {
        int i;

        if ( actual == null || expected == null ||
             actual.length != expected.length ) {
            return false;
        }
        for ( i = 0; i < actual.length; i++ ) {
            if ( Math.abs(actual[i] - expected[i]) > GEOMETRY_TOLERANCE ) {
                return false;
            }
        }
        return true;
    }
}
