package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.PolyhedralBoundedSolidTestFixtures;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;
import vsdk.toolkit.processing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidModeler;

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(result.polygonsList.size()).isEqualTo(expectedFaceCount);
        assertThat(result.edgesList.size()).isEqualTo(expectedEdgeCount);
        assertThat(result.verticesList.size()).isEqualTo(expectedVertexCount);
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
            assertThat(result.polygonsList.size()).isEqualTo(0);
            assertThat(result.edgesList.size()).isEqualTo(0);
            assertThat(result.verticesList.size()).isEqualTo(0);
        }
        else if ( op == PolyhedralBoundedSolidModeler.UNION ) {
            assertThat(result.polygonsList.size()).isGreaterThanOrEqualTo(1);
            assertThat(result.edgesList.size()).isGreaterThanOrEqualTo(1);
            assertThat(result.verticesList.size()).isGreaterThanOrEqualTo(1);
        }
        else {
            assertThat(result.polygonsList.size()).isGreaterThanOrEqualTo(0);
            assertThat(result.edgesList.size()).isGreaterThanOrEqualTo(0);
            assertThat(result.verticesList.size()).isGreaterThanOrEqualTo(0);
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
        assertThat(result.polygonsList.size()).isEqualTo(expectedFaceCount);
        assertThat(result.edgesList.size()).isEqualTo(expectedEdgeCount);
        assertThat(result.verticesList.size()).isEqualTo(expectedVertexCount);
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
        assertThat(resultAB.polygonsList.size())
            .isEqualTo(resultBA.polygonsList.size());
        assertThat(resultAB.edgesList.size())
            .isEqualTo(resultBA.edgesList.size());
        assertThat(resultAB.verticesList.size())
            .isEqualTo(resultBA.verticesList.size());
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
        PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(
            solidA, solidB, PolyhedralBoundedSolidModeler.UNION, false);

        assertThat(result).isNotNull();
        assertThat(result.polygonsList.size()).isEqualTo(expectedFaceCount);
        assertThat(result.edgesList.size()).isEqualTo(expectedEdgeCount);
        assertThat(result.verticesList.size()).isEqualTo(expectedVertexCount);
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
        assertThat(result.polygonsList.size()).isGreaterThanOrEqualTo(0);
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
        assertThat(withMax.polygonsList.size())
            .isEqualTo(withoutMax.polygonsList.size());
        assertThat(withMax.edgesList.size())
            .isEqualTo(withoutMax.edgesList.size());
        assertThat(withMax.verticesList.size())
            .isEqualTo(withoutMax.verticesList.size());
    }

    private static Stream<Arguments> disjointSetOperationSamples()
    {
        PolyhedralBoundedSolid[] operands =
            PolyhedralBoundedSolidTestFixtures.createDisjointBoxPair();
        PolyhedralBoundedSolid solidA = operands[0];
        PolyhedralBoundedSolid solidB = operands[1];

        int facesA = solidA.polygonsList.size();
        int edgesA = solidA.edgesList.size();
        int verticesA = solidA.verticesList.size();
        int facesB = solidB.polygonsList.size();
        int edgesB = solidB.edgesList.size();
        int verticesB = solidB.verticesList.size();

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

        int facesInner = inner.polygonsList.size();
        int edgesInner = inner.edgesList.size();
        int verticesInner = inner.verticesList.size();
        int facesOuter = outer.polygonsList.size();
        int edgesOuter = outer.edgesList.size();
        int verticesOuter = outer.verticesList.size();

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
            Arguments.of(0, PolyhedralBoundedSolidModeler.INTERSECTION),
            Arguments.of(1, PolyhedralBoundedSolidModeler.UNION),
            Arguments.of(1, PolyhedralBoundedSolidModeler.INTERSECTION),
            Arguments.of(1, PolyhedralBoundedSolidModeler.SUBTRACT)
        );
    }

    private static Stream<Arguments> mant1988IntermediateOnlySamples()
    {
        return Stream.of(
            Arguments.of(-1, PolyhedralBoundedSolidModeler.UNION),
            Arguments.of(-1, PolyhedralBoundedSolidModeler.INTERSECTION),
            Arguments.of(-1, PolyhedralBoundedSolidModeler.SUBTRACT),
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
