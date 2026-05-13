package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidTestFixtures;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;
import vsdk.toolkit.processing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidModeler;

import static org.assertj.core.api.Assertions.assertThat;

/**
Policy tests for no-intersection, touching-only, and containment boolean
cases.

<p>Traceability: [MANT1988] Ch. 15.1 set-operation statement and special
cases where boundaries do not properly intersect before the normal pipeline.</p>
 */
class PolyhedralBoundedSolidSetOperatorTest
{
    @ParameterizedTest
    @MethodSource("disjointSetOperationSamples")
    void given_disjointSolids_when_setOperation_then_matchesNoIntersectionPolicy(
        int op, int expectedFaceCount, int expectedEdgeCount,
        int expectedVertexCount)
    {
        // Arrange
        PolyhedralBoundedSolid[] operands =
            PolyhedralBoundedSolidTestFixtures.createDisjointBoxPair();
        PolyhedralBoundedSolid solidA = operands[0];
        PolyhedralBoundedSolid solidB = operands[1];

        // Action
        PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(solidA, solidB,
            op, false);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPolygonsList().size()).isEqualTo(expectedFaceCount);
        assertThat(result.getEdgesList().size()).isEqualTo(expectedEdgeCount);
        assertThat(result.getVerticesList().size()).isEqualTo(expectedVertexCount);
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(result)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("touchingSetOperationSamples")
    void given_touchingOnlySolids_when_setOperation_then_matchesTouchingPolicy(
        int op)
    {
        // Arrange
        PolyhedralBoundedSolid[] operands =
            PolyhedralBoundedSolidTestFixtures.createTouchingBoxPair();
        PolyhedralBoundedSolid solidA = operands[0];
        PolyhedralBoundedSolid solidB = operands[1];

        // Action
        PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(solidA, solidB,
            op, false);

        // Assert
        assertThat(result).isNotNull();
        if ( op == PolyhedralBoundedSolidModeler.INTERSECTION ) {
            assertThat(result.getPolygonsList().size()).isEqualTo(0);
            assertThat(result.getEdgesList().size()).isEqualTo(0);
            assertThat(result.getVerticesList().size()).isEqualTo(0);
        }
        else if ( op == PolyhedralBoundedSolidModeler.UNION ) {
            assertThat(result.getPolygonsList().size()).isGreaterThanOrEqualTo(1);
            assertThat(result.getEdgesList().size()).isGreaterThanOrEqualTo(1);
            assertThat(result.getVerticesList().size()).isGreaterThanOrEqualTo(1);
        }
        else {
            assertThat(result.getPolygonsList().size()).isGreaterThanOrEqualTo(0);
            assertThat(result.getEdgesList().size()).isGreaterThanOrEqualTo(0);
            assertThat(result.getVerticesList().size()).isGreaterThanOrEqualTo(0);
        }
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(result)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("containmentSetOperationSamples")
    void given_containmentWithoutBoundaryIntersection_when_setOperation_then_matchesContainmentPolicy(
        int op, int expectedFaceCount, int expectedEdgeCount,
        int expectedVertexCount)
    {
        // Arrange
        PolyhedralBoundedSolid[] operands =
            PolyhedralBoundedSolidTestFixtures.createContainmentBoxPair();
        PolyhedralBoundedSolid inner = operands[0];
        PolyhedralBoundedSolid outer = operands[1];

        // Action
        PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(inner, outer,
            op, false);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPolygonsList().size()).isEqualTo(expectedFaceCount);
        assertThat(result.getEdgesList().size()).isEqualTo(expectedEdgeCount);
        assertThat(result.getVerticesList().size()).isEqualTo(expectedVertexCount);
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(result)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("commutativeOperations")
    void given_sameOperandsSwapped_when_unionOrIntersection_then_resultsKeepEquivalentSizes(
        int op)
    {
        // Arrange
        PolyhedralBoundedSolid[] disjointPairA =
            PolyhedralBoundedSolidTestFixtures.createDisjointBoxPair();
        PolyhedralBoundedSolid[] disjointPairB =
            PolyhedralBoundedSolidTestFixtures.createDisjointBoxPair();

        // Action
        PolyhedralBoundedSolid resultAB = PolyhedralBoundedSolidModeler.setOp(
            disjointPairA[0], disjointPairA[1], op, false);
        PolyhedralBoundedSolid resultBA = PolyhedralBoundedSolidModeler.setOp(
            disjointPairB[1], disjointPairB[0], op, false);

        // Assert
        assertThat(resultAB.getPolygonsList().size())
            .isEqualTo(resultBA.getPolygonsList().size());
        assertThat(resultAB.getEdgesList().size())
            .isEqualTo(resultBA.getEdgesList().size());
        assertThat(resultAB.getVerticesList().size())
            .isEqualTo(resultBA.getVerticesList().size());
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(resultAB)).isTrue();
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(resultBA)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("appe1967CornerUnionSamples")
    void given_coplanarOverlappingBars_when_union_then_itProducesTheExpectedLPrism(
        PolyhedralBoundedSolid solidA,
        PolyhedralBoundedSolid solidB,
        int expectedFaceCount, int expectedEdgeCount, int expectedVertexCount)
    {
        // Arrange

        // Action
        PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(
            solidA, solidB, PolyhedralBoundedSolidModeler.UNION, false);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPolygonsList().size()).isEqualTo(expectedFaceCount);
        assertThat(result.getEdgesList().size()).isEqualTo(expectedEdgeCount);
        assertThat(result.getVerticesList().size()).isEqualTo(expectedVertexCount);
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(result)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("mant1988StrictOperationSamples")
    void given_mant1988Section15_2Scenarios_when_strictOperations_then_resultIsReturnedAndStrictValid(
        int situation, int op)
    {
        // Arrange
        PolyhedralBoundedSolid[] operands =
            PolyhedralBoundedSolidTestFixtures.createMant1988_15_2Pair(
                situation);
        PolyhedralBoundedSolid solidA = operands[0];
        PolyhedralBoundedSolid solidB = operands[1];

        // Action
        PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(solidA, solidB,
            op, false);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPolygonsList().size()).isGreaterThanOrEqualTo(0);
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(result)).isTrue();
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateStrict(result)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("mant1988IntermediateOnlySamples")
    void given_mant1988Section15_2Scenarios_when_pseudomanifoldOperations_then_resultRemainsIntermediateButNotStrict(
        int situation, int op)
    {
        // Arrange
        PolyhedralBoundedSolid[] operands =
            PolyhedralBoundedSolidTestFixtures.createMant1988_15_2Pair(
                situation);
        PolyhedralBoundedSolid solidA = operands[0];
        PolyhedralBoundedSolid solidB = operands[1];

        // Action
        PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(
            solidA, solidB, op, false);

        // Assert
        assertThat(result).isNotNull();
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(result)).isTrue();
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateStrict(result)).isFalse();
    }

    @org.junit.jupiter.api.Test
    void given_mant1988Section15_2HoledIntersection_when_finalMaximizeFacesEnabled_then_resultRemainsIntermediateValidButNotStrict()
    {
        // Arrange
        PolyhedralBoundedSolid[] operands =
            PolyhedralBoundedSolidTestFixtures.createMant1988_15_2Pair(
                -1);
        PolyhedralBoundedSolid solidA = operands[0];
        PolyhedralBoundedSolid solidB = operands[1];

        // Action
        PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(
            solidA, solidB, PolyhedralBoundedSolidModeler.INTERSECTION, false,
            true);

        // Assert
        assertThat(result).isNotNull();
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(result)).isTrue();
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateStrict(result)).isFalse();
    }

    @org.junit.jupiter.api.Test
    void given_mant1988Section15_2HoledUnion_when_currentKernelRuns_then_resultBecomesStrictAndKeepsBlockWithTriangularWings()
    {
        // Arrange
        PolyhedralBoundedSolid[] operands =
            PolyhedralBoundedSolidTestFixtures.createMant1988_15_2Pair(-1);

        // Action
        PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(
            operands[0], operands[1], PolyhedralBoundedSolidModeler.UNION,
            false);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPolygonsList().size()).isEqualTo(14);
        assertThat(result.getEdgesList().size()).isEqualTo(30);
        assertThat(result.getVerticesList().size()).isEqualTo(20);
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(result)).isTrue();
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateStrict(result)).isTrue();
        assertThat(result.findFace(1)).isNotNull();
        assertThat(result.findFace(1).boundariesList.size()).isEqualTo(2);
        assertThat(result.getMinMax()).containsExactly(
            0.0, 0.0, 0.0, 0.775, 1.0, 0.6);
    }

    @org.junit.jupiter.api.Test
    void given_mant1988Section15_1DifferenceBA_when_currentKernelRuns_then_resultClosesDoubleLoop()
    {
        // Arrange
        PolyhedralBoundedSolid[] operands =
            PolyhedralBoundedSolidTestFixtures.createMant1988_15_1Pair();

        // Action
        PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(
            operands[1], operands[0], PolyhedralBoundedSolidModeler.SUBTRACT,
            false);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPolygonsList().size()).isEqualTo(8);
        assertThat(result.getEdgesList().size()).isEqualTo(18);
        assertThat(result.getVerticesList().size()).isEqualTo(12);
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(result)).isTrue();
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateStrict(result)).isTrue();
        assertThat(result.getMinMax()).containsExactly(
            1.0 / 3.0, 0.0, 0.25, 1.0, 1.0, 1.0);
    }

    @org.junit.jupiter.api.Test
    void given_mant1988Section15_1DifferenceBA_when_flexibleBilateralConnectRuns_then_resultClosesDoubleLoop()
    {
        // Arrange
        String[] properties = new String[] {
            "vsdk.setop.connect.keepInsertionOrder",
            "vsdk.setop.connect.flexibleEndpointChains",
            "vsdk.setop.connect.flexibleAllowSamePointSelfClosure",
            "vsdk.setop.connect.flexibleKeepOnlyPairedCutFaces",
            "vsdk.setop.connect.flexibleRejectOneSidedMatches"
        };
        String[] previousValues = new String[properties.length];
        int i;

        for ( i = 0; i < properties.length; i++ ) {
            previousValues[i] = System.getProperty(properties[i]);
            System.setProperty(properties[i], "true");
        }

        // Action
        try {
            PolyhedralBoundedSolid[] operands =
                PolyhedralBoundedSolidTestFixtures.createMant1988_15_1Pair();
            PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(
                operands[1], operands[0], PolyhedralBoundedSolidModeler.SUBTRACT,
                false);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getPolygonsList().size()).isEqualTo(8);
            assertThat(result.getEdgesList().size()).isEqualTo(18);
            assertThat(result.getVerticesList().size()).isEqualTo(12);
            assertThat(PolyhedralBoundedSolidValidationEngine
                .validateIntermediate(result)).isTrue();
            assertThat(PolyhedralBoundedSolidValidationEngine
                .validateStrict(result)).isTrue();
            assertThat(result.getMinMax()).containsExactly(
                1.0 / 3.0, 0.0, 0.25, 1.0, 1.0, 1.0);
        }
        finally {
            for ( i = 0; i < properties.length; i++ ) {
                if ( previousValues[i] == null ) {
                    System.clearProperty(properties[i]);
                }
                else {
                    System.setProperty(properties[i], previousValues[i]);
                }
            }
        }
    }

    @org.junit.jupiter.api.Test
    void given_mant1988Section15_2HoledIntersection_when_togglingFinalMaximizeFaces_then_resultTopologyIsPreserved()
    {
        // Arrange
        PolyhedralBoundedSolid[] operandsWithMax =
            PolyhedralBoundedSolidTestFixtures.createMant1988_15_2Pair(-1);
        PolyhedralBoundedSolid[] operandsWithoutMax =
            PolyhedralBoundedSolidTestFixtures.createMant1988_15_2Pair(-1);

        // Action
        PolyhedralBoundedSolid withMax = PolyhedralBoundedSolidModeler.setOp(
            operandsWithMax[0], operandsWithMax[1],
            PolyhedralBoundedSolidModeler.INTERSECTION, false, true);
        PolyhedralBoundedSolid withoutMax = PolyhedralBoundedSolidModeler.setOp(
            operandsWithoutMax[0], operandsWithoutMax[1],
            PolyhedralBoundedSolidModeler.INTERSECTION, false, false);

        // Assert
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(withMax)).isTrue();
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(withoutMax)).isTrue();
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateStrict(withMax)).isFalse();
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateStrict(withoutMax)).isFalse();
        assertThat(withMax.getPolygonsList().size())
            .isEqualTo(withoutMax.getPolygonsList().size());
        assertThat(withMax.getEdgesList().size())
            .isEqualTo(withoutMax.getEdgesList().size());
        assertThat(withMax.getVerticesList().size())
            .isEqualTo(withoutMax.getVerticesList().size());
    }

    private static Stream<Arguments> disjointSetOperationSamples()
    {
        PolyhedralBoundedSolid[] operands =
            PolyhedralBoundedSolidTestFixtures.createDisjointBoxPair();
        PolyhedralBoundedSolid solidA = operands[0];
        PolyhedralBoundedSolid solidB = operands[1];

        int facesA = solidA.getPolygonsList().size();
        int edgesA = solidA.getEdgesList().size();
        int verticesA = solidA.getVerticesList().size();
        int facesB = solidB.getPolygonsList().size();
        int edgesB = solidB.getEdgesList().size();
        int verticesB = solidB.getVerticesList().size();

        return Stream.of(
            Arguments.of(PolyhedralBoundedSolidModeler.UNION,
                facesA + facesB, edgesA + edgesB, verticesA + verticesB),
            Arguments.of(PolyhedralBoundedSolidModeler.INTERSECTION, 0, 0, 0),
            Arguments.of(PolyhedralBoundedSolidModeler.SUBTRACT, facesA, edgesA, verticesA)
        );
    }

    private static Stream<Arguments> touchingSetOperationSamples()
    {
        return Stream.of(
            Arguments.of(PolyhedralBoundedSolidModeler.UNION),
            Arguments.of(PolyhedralBoundedSolidModeler.INTERSECTION),
            Arguments.of(PolyhedralBoundedSolidModeler.SUBTRACT)
        );
    }

    private static Stream<Arguments> containmentSetOperationSamples()
    {
        PolyhedralBoundedSolid[] operands =
            PolyhedralBoundedSolidTestFixtures.createContainmentBoxPair();
        PolyhedralBoundedSolid inner = operands[0];
        PolyhedralBoundedSolid outer = operands[1];

        int facesInner = inner.getPolygonsList().size();
        int edgesInner = inner.getEdgesList().size();
        int verticesInner = inner.getVerticesList().size();
        int facesOuter = outer.getPolygonsList().size();
        int edgesOuter = outer.getEdgesList().size();
        int verticesOuter = outer.getVerticesList().size();

        return Stream.of(
            Arguments.of(PolyhedralBoundedSolidModeler.UNION,
                facesOuter, edgesOuter, verticesOuter),
            Arguments.of(PolyhedralBoundedSolidModeler.INTERSECTION,
                facesInner, edgesInner, verticesInner),
            Arguments.of(PolyhedralBoundedSolidModeler.SUBTRACT, 0, 0, 0)
        );
    }

    private static Stream<Arguments> commutativeOperations()
    {
        return Stream.of(
            Arguments.of(PolyhedralBoundedSolidModeler.UNION),
            Arguments.of(PolyhedralBoundedSolidModeler.INTERSECTION)
        );
    }

    private static Stream<Arguments> mant1988StrictOperationSamples()
    {
        return Stream.of(
            Arguments.of(-1, PolyhedralBoundedSolidModeler.UNION),
            Arguments.of(-1, PolyhedralBoundedSolidModeler.SUBTRACT),
            Arguments.of(0, PolyhedralBoundedSolidModeler.INTERSECTION),
            Arguments.of(1, PolyhedralBoundedSolidModeler.UNION),
            Arguments.of(1, PolyhedralBoundedSolidModeler.INTERSECTION),
            Arguments.of(1, PolyhedralBoundedSolidModeler.SUBTRACT)
        );
    }

    private static Stream<Arguments> mant1988IntermediateOnlySamples()
    {
        return Stream.of(
            Arguments.of(-1, PolyhedralBoundedSolidModeler.INTERSECTION),
            Arguments.of(0, PolyhedralBoundedSolidModeler.UNION),
            Arguments.of(0, PolyhedralBoundedSolidModeler.SUBTRACT)
        );
    }

    private static Stream<Arguments> appe1967CornerUnionSamples()
    {
        return Stream.of(
            Arguments.of(
                PolyhedralBoundedSolidTestFixtures.createBoxSolid(
                    1.0, 0.2, 0.2, 0.5, 0.1, 0.1),
                PolyhedralBoundedSolidTestFixtures.createBoxSolid(
                    0.2, 1.0, 0.2, 0.1, 0.5, 0.1),
                8, 18, 12)
        );
    }
}
