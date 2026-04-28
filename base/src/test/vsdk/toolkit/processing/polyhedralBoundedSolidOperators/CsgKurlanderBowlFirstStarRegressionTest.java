package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidEulerOperators;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidVertex;

import static org.assertj.core.api.Assertions.assertThat;

class CsgKurlanderBowlFirstStarRegressionTest
{
    private static final int STAR_VERTEX_COUNT = 10;

    @Test
    void given_kurlanderSingleMotifSelector_whenIndexing_thenStarsComeBeforeMoonsAndWrapsCircularly()
    {
        int starCount = CsgKurlanderBowlFixture.getSingleMotifStarCount();
        int moonCount = CsgKurlanderBowlFixture.getSingleMotifMoonCount();
        int motifCount = CsgKurlanderBowlFixture.getSingleMotifCount();

        assertThat(starCount).isEqualTo(20);
        assertThat(moonCount).isEqualTo(20);
        assertThat(motifCount).isEqualTo(40);
        assertThat(CsgKurlanderBowlFixture.normalizeSingleMotifIndex(0))
            .isZero();
        assertThat(CsgKurlanderBowlFixture.normalizeSingleMotifIndex(
            motifCount)).isZero();
        assertThat(CsgKurlanderBowlFixture.normalizeSingleMotifIndex(-1))
            .isEqualTo(motifCount - 1);
        assertThat(CsgKurlanderBowlFixture.describeSingleMotif(0))
            .startsWith("STAR 1/20");
        assertThat(CsgKurlanderBowlFixture.describeSingleMotif(starCount))
            .startsWith("MOON 1/20");
    }

    @Test
    void given_kurlanderBowlAndFirstStar_when_subtractingStarFromBowl_then_resultStaysNonEmptyAndIntermediateValid()
    {
        PolyhedralBoundedSolid[] operands =
            CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(0);
        PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(
            operands[0], operands[1], PolyhedralBoundedSolidModeler.SUBTRACT,
            false);

        assertThat(result).isNotNull();
        assertThat(result.getPolygonsList().size()).isGreaterThan(0);
        assertThat(result.getEdgesList().size()).isGreaterThan(0);
        assertThat(result.getVerticesList().size()).isGreaterThan(0);
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(result)).isTrue();
    }

    @Test
    void given_kurlanderBowlAndFirstStar_when_subtractingStarFromBowl_then_connectStageClosesAllStarEdges()
    {
        PolyhedralBoundedSolid[] operands =
            CsgKurlanderBowlFixture.createBowlAndFirstStarOperands();
        PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(
            operands[0], operands[1], PolyhedralBoundedSolidModeler.SUBTRACT,
            false);

        assertThat(result).isNotNull();
        assertThat(_PolyhedralBoundedSolidSetNullEdgesConnector
            .getLastLooseACount()).isZero();
        assertThat(_PolyhedralBoundedSolidSetNullEdgesConnector
            .getLastLooseBCount()).isZero();
    }

    @Test
    void given_kurlanderBowlAndFirstStar_when_subtractingStarFromBowl_then_noZeroAreaLoopWalksTwiceAroundClosedStar()
    {
        PolyhedralBoundedSolid[] operands =
            CsgKurlanderBowlFixture.createBowlAndFirstStarOperands();
        PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(
            operands[0], operands[1], PolyhedralBoundedSolidModeler.SUBTRACT,
            false);
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext =
            PolyhedralBoundedSolidNumericPolicy.forSolid(result);

        assertThat(countZeroAreaClosedStarDoubleWalkLoops(result,
            numericContext)).isZero();
    }

    @Test
    void given_kurlanderBowlAndFifthStar_when_inspectingPartialOperandB_then_twoDoubleBoundaryContoursAreClosed()
    {
        PolyhedralBoundedSolid[] operands =
            CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(4);
        PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(
            operands[0], operands[1], PolyhedralBoundedSolidModeler.SUBTRACT,
            false);
        PolyhedralBoundedSolid partialOperandB = operands[1];
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext =
            PolyhedralBoundedSolidNumericPolicy.forSolid(partialOperandB);

        assertThat(result).isNotNull();
        assertThat(countClosedDoubleBoundaryContours(partialOperandB,
            numericContext))
            .as("The fifth Kurlander star partial operand B should preserve " +
                "the two closed duplicated intersection contours seen in " +
                "the preceding star motifs")
            .isEqualTo(2);
    }

    @Test
    void given_closedStarLoopWalkedForwardAndBackward_whenScanningLoops_then_zeroAreaDoubleWalkIsDetected()
    {
        PolyhedralBoundedSolid solid = createClosedDoubleWalkStarLamina();
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext =
            PolyhedralBoundedSolidNumericPolicy.forSolid(solid);

        assertThat(countZeroAreaClosedStarDoubleWalkLoops(solid,
            numericContext)).isGreaterThan(0);
    }

    private static PolyhedralBoundedSolid createClosedDoubleWalkStarLamina()
    {
        PolyhedralBoundedSolid solid = new PolyhedralBoundedSolid();
        Vector3D[] starPoints = createStarPoints();
        int vertexId = 1;
        int i;

        PolyhedralBoundedSolidEulerOperators.mvfs(solid, starPoints[0], 1, 1);
        for ( i = 1; i < starPoints.length; i++ ) {
            vertexId++;
            PolyhedralBoundedSolidEulerOperators.smev(solid, 1, vertexId - 1,
                vertexId, starPoints[i]);
        }
        for ( i = starPoints.length - 2; i >= 1; i-- ) {
            vertexId++;
            PolyhedralBoundedSolidEulerOperators.smev(solid, 1, vertexId - 1,
                vertexId, starPoints[i]);
        }
        PolyhedralBoundedSolidEulerOperators.smef(solid, 1, vertexId, 1, 2);
        return solid;
    }

    private static Vector3D[] createStarPoints()
    {
        Vector3D[] points = new Vector3D[STAR_VERTEX_COUNT];
        double outerRadius = 2.0;
        double innerRadius = 0.77;
        double start = Math.toRadians(-90.0);
        int i;

        for ( i = 0; i < points.length; i++ ) {
            double angle = start + i * Math.PI / 5.0;
            double radius = (i % 2 == 0) ? outerRadius : innerRadius;
            points[i] = new Vector3D(
                radius * Math.cos(angle),
                radius * Math.sin(angle),
                0.0);
        }
        return points;
    }

    private static int countZeroAreaClosedStarDoubleWalkLoops(
        PolyhedralBoundedSolid solid,
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext)
    {
        int count = 0;
        int i;
        int j;

        for ( i = 0; i < solid.getPolygonsList().size(); i++ ) {
            _PolyhedralBoundedSolidFace face = solid.getPolygonsList().get(i);
            for ( j = 0; j < face.boundariesList.size(); j++ ) {
                _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(j);
                if ( isZeroAreaClosedStarDoubleWalkLoop(loop, numericContext) ) {
                    count++;
                }
            }
        }
        return count;
    }

    private static boolean isZeroAreaClosedStarDoubleWalkLoop(
        _PolyhedralBoundedSolidLoop loop,
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext)
    {
        if ( loop == null ||
             loop.boundaryStartHalfEdge == null ||
             loop.halfEdgesList.size() < (STAR_VERTEX_COUNT - 1) * 2 ) {
            return false;
        }
        if ( loopAreaMagnitude(loop) > numericContext.bigEpsilon() ) {
            return false;
        }
        return countUniqueLoopPositions(loop, numericContext) <= STAR_VERTEX_COUNT;
    }

    private static int countClosedDoubleBoundaryContours(
        PolyhedralBoundedSolid solid,
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext)
    {
        int count = 0;
        int i;
        int j;

        for ( i = 0; i < solid.getPolygonsList().size(); i++ ) {
            _PolyhedralBoundedSolidFace face = solid.getPolygonsList().get(i);
            for ( j = 0; j < face.boundariesList.size(); j++ ) {
                _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(j);
                if ( isClosedDoubleBoundaryContour(solid, loop,
                     numericContext) ) {
                    count++;
                }
            }
        }
        return count;
    }

    private static boolean isClosedDoubleBoundaryContour(
        PolyhedralBoundedSolid solid,
        _PolyhedralBoundedSolidLoop loop,
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext)
    {
        int i;

        if ( loop == null ||
             loop.boundaryStartHalfEdge == null ||
             loop.halfEdgesList.size() < STAR_VERTEX_COUNT ) {
            return false;
        }

        for ( i = 0; i < loop.halfEdgesList.size(); i++ ) {
            _PolyhedralBoundedSolidHalfEdge halfEdge = loop.halfEdgesList.get(i);
            if ( halfEdge == null ||
                 halfEdge.next() == null ||
                 halfEdge.parentEdge == null ||
                 !hasCoincidentPartnerVertex(solid, halfEdge.startingVertex,
                     numericContext) ) {
                return false;
            }
        }
        return loopWalkReturnsToStart(loop);
    }

    private static boolean loopWalkReturnsToStart(
        _PolyhedralBoundedSolidLoop loop)
    {
        _PolyhedralBoundedSolidHalfEdge halfEdge =
            loop.boundaryStartHalfEdge;
        int i;

        for ( i = 0; i < loop.halfEdgesList.size(); i++ ) {
            if ( halfEdge == null ) {
                return false;
            }
            halfEdge = halfEdge.next();
        }
        return halfEdge == loop.boundaryStartHalfEdge;
    }

    private static boolean hasCoincidentPartnerVertex(
        PolyhedralBoundedSolid solid,
        _PolyhedralBoundedSolidVertex vertex,
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext)
    {
        int i;

        for ( i = 0; i < solid.getVerticesList().size(); i++ ) {
            _PolyhedralBoundedSolidVertex candidate =
                solid.getVerticesList().get(i);
            if ( candidate != vertex &&
                 PolyhedralBoundedSolidNumericPolicy.pointsCoincident(
                     candidate.position,
                     vertex.position,
                     numericContext) ) {
                return true;
            }
        }
        return false;
    }

    private static double loopAreaMagnitude(_PolyhedralBoundedSolidLoop loop)
    {
        _PolyhedralBoundedSolidHalfEdge he = loop.boundaryStartHalfEdge;
        _PolyhedralBoundedSolidHalfEdge start = he;
        Vector3D normalAccumulator = new Vector3D();

        do {
            Vector3D p = he.startingVertex.position;
            Vector3D q = he.next().startingVertex.position;

            normalAccumulator = normalAccumulator.add(new Vector3D(
                (p.y() - q.y()) * (p.z() + q.z()),
                (p.z() - q.z()) * (p.x() + q.x()),
                (p.x() - q.x()) * (p.y() + q.y())));
            he = he.next();
        } while ( he != start );
        return normalAccumulator.length();
    }

    private static int countUniqueLoopPositions(
        _PolyhedralBoundedSolidLoop loop,
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext)
    {
        int uniqueCount = 0;
        int i;

        for ( i = 0; i < loop.halfEdgesList.size(); i++ ) {
            _PolyhedralBoundedSolidHalfEdge he = loop.halfEdgesList.get(i);
            if ( !hasPreviousMatchingPosition(loop, i, he, numericContext) ) {
                uniqueCount++;
            }
        }
        return uniqueCount;
    }

    private static boolean hasPreviousMatchingPosition(
        _PolyhedralBoundedSolidLoop loop,
        int currentIndex,
        _PolyhedralBoundedSolidHalfEdge current,
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext)
    {
        int i;

        for ( i = 0; i < currentIndex; i++ ) {
            _PolyhedralBoundedSolidHalfEdge previous = loop.halfEdgesList.get(i);
            if ( PolyhedralBoundedSolidNumericPolicy.pointsCoincident(
                     previous.startingVertex.position,
                     current.startingVertex.position,
                     numericContext) ) {
                return true;
            }
        }
        return false;
    }
}
