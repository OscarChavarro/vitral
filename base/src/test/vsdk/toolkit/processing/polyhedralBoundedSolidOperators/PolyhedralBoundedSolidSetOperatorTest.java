package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.PolyhedralBoundedSolidTestFixtures;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;
import vsdk.toolkit.processing.GeometricModeler;

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
        PolyhedralBoundedSolid result = GeometricModeler.setOp(solidA, solidB,
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
        PolyhedralBoundedSolid result = GeometricModeler.setOp(solidA, solidB,
            op, false);

        // Assert
        assertThat(result).isNotNull();
        if ( op == GeometricModeler.INTERSECTION ) {
            assertThat(result.polygonsList.size()).isEqualTo(0);
            assertThat(result.edgesList.size()).isEqualTo(0);
            assertThat(result.verticesList.size()).isEqualTo(0);
        }
        else if ( op == GeometricModeler.UNION ) {
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
        PolyhedralBoundedSolid result = GeometricModeler.setOp(inner, outer,
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
        PolyhedralBoundedSolid resultAB = GeometricModeler.setOp(
            disjointPairA[0], disjointPairA[1], op, false);
        PolyhedralBoundedSolid resultBA = GeometricModeler.setOp(
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
    @MethodSource("mant1988StableOperationSamples")
    void given_mant1988Section15_2Scenarios_when_stableOperations_then_resultIsReturnedAndValid(
        int situation, int op)
    {
        // Arrange
        PolyhedralBoundedSolid[] operands =
            PolyhedralBoundedSolidTestFixtures.createMant1988_15_2Pair(
                situation);
        PolyhedralBoundedSolid solidA = operands[0];
        PolyhedralBoundedSolid solidB = operands[1];

        // Action
        PolyhedralBoundedSolid result = GeometricModeler.setOp(solidA, solidB,
            op, false);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.polygonsList.size()).isGreaterThanOrEqualTo(0);
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(result)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("mant1988KnownFailureSamples")
    void given_mant1988Section15_2Scenarios_when_knownFailureOperations_then_throwsRuntimeException(
        int situation, int op)
    {
        // Arrange
        PolyhedralBoundedSolid[] operands =
            PolyhedralBoundedSolidTestFixtures.createMant1988_15_2Pair(
                situation);
        PolyhedralBoundedSolid solidA = operands[0];
        PolyhedralBoundedSolid solidB = operands[1];

        // Action
        Throwable thrown = null;
        try {
            GeometricModeler.setOp(solidA, solidB, op, false);
        }
        catch ( Throwable ex ) {
            thrown = ex;
        }

        // Assert
        assertThat(thrown).isInstanceOf(RuntimeException.class);
    }

    @ParameterizedTest
    @MethodSource("mant1988LimitSamples")
    void given_mant1988Section15_2Scenarios_when_union_then_returnsValidResult(
        int situation)
    {
        // Arrange
        PolyhedralBoundedSolid[] operands =
            PolyhedralBoundedSolidTestFixtures.createMant1988_15_2Pair(
                situation);
        PolyhedralBoundedSolid solidA = operands[0];
        PolyhedralBoundedSolid solidB = operands[1];

        // Action
        PolyhedralBoundedSolid result = GeometricModeler.setOp(solidA, solidB,
            GeometricModeler.UNION, false);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.polygonsList.size()).isGreaterThanOrEqualTo(0);
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(result)).isTrue();
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
            Arguments.of(GeometricModeler.UNION,
                facesA + facesB, edgesA + edgesB, verticesA + verticesB),
            Arguments.of(GeometricModeler.INTERSECTION, 0, 0, 0),
            Arguments.of(GeometricModeler.DIFFERENCE, facesA, edgesA, verticesA)
        );
    }

    private static Stream<Arguments> mant1988LimitSamples()
    {
        return Stream.of(
            Arguments.of(-1),
            Arguments.of(0),
            Arguments.of(1)
        );
    }

    private static Stream<Arguments> touchingSetOperationSamples()
    {
        return Stream.of(
            Arguments.of(GeometricModeler.UNION),
            Arguments.of(GeometricModeler.INTERSECTION),
            Arguments.of(GeometricModeler.DIFFERENCE)
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
            Arguments.of(GeometricModeler.UNION,
                facesOuter, edgesOuter, verticesOuter),
            Arguments.of(GeometricModeler.INTERSECTION,
                facesInner, edgesInner, verticesInner),
            Arguments.of(GeometricModeler.DIFFERENCE, 0, 0, 0)
        );
    }

    private static Stream<Arguments> commutativeOperations()
    {
        return Stream.of(
            Arguments.of(GeometricModeler.UNION),
            Arguments.of(GeometricModeler.INTERSECTION)
        );
    }

    private static Stream<Arguments> mant1988StableOperationSamples()
    {
        return Stream.of(
            Arguments.of(-1, GeometricModeler.UNION),
            Arguments.of(0, GeometricModeler.UNION),
            Arguments.of(1, GeometricModeler.UNION),
            Arguments.of(0, GeometricModeler.INTERSECTION),
            Arguments.of(0, GeometricModeler.DIFFERENCE)
        );
    }

    private static Stream<Arguments> mant1988KnownFailureSamples()
    {
        return Stream.of(
            Arguments.of(1, GeometricModeler.INTERSECTION),
            Arguments.of(-1, GeometricModeler.DIFFERENCE)
        );
    }
}
