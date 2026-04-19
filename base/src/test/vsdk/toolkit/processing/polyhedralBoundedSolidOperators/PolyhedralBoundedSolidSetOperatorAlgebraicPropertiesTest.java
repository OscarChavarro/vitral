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

class PolyhedralBoundedSolidSetOperatorAlgebraicPropertiesTest
{
    @ParameterizedTest
    @MethodSource("idempotenceCorpus")
    void given_fixtureSolid_when_idempotentOperationsOnKnownFailureCorpus_then_detectsAlgebraicDrift(
        String corpusKey, int solidIndex)
    {
        // Arrange
        PolyhedralBoundedSolid baseline = createPair(corpusKey)[solidIndex];
        double[] baselineMinMax = baseline.getMinMax();
        PolyhedralBoundedSolid[] unionPair = createEquivalentSolidPair(
            corpusKey, solidIndex);
        PolyhedralBoundedSolid[] intersectionPair = createEquivalentSolidPair(
            corpusKey, solidIndex);
        PolyhedralBoundedSolid[] differencePair = createEquivalentSolidPair(
            corpusKey, solidIndex);

        // Action
        PolyhedralBoundedSolid unionResult = PolyhedralBoundedSolidModeler.setOp(
            unionPair[0], unionPair[1], PolyhedralBoundedSolidModeler.UNION, false);
        PolyhedralBoundedSolid intersectionResult = PolyhedralBoundedSolidModeler.setOp(
            intersectionPair[0], intersectionPair[1],
            PolyhedralBoundedSolidModeler.INTERSECTION, false);
        PolyhedralBoundedSolid differenceResult = PolyhedralBoundedSolidModeler.setOp(
            differencePair[0], differencePair[1], PolyhedralBoundedSolidModeler.SUBTRACT,
            false);

        // Assert
        boolean unionMatches = isBoundingBoxClose(unionResult, baselineMinMax);
        boolean intersectionMatches = isBoundingBoxClose(intersectionResult,
            baselineMinMax);
        boolean differenceIsEmpty = differenceResult.polygonsList.size() == 0 &&
            differenceResult.edgesList.size() == 0 &&
            differenceResult.verticesList.size() == 0;
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(unionResult)).isTrue();
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(intersectionResult)).isTrue();
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(differenceResult)).isTrue();
        assertThat(unionMatches && intersectionMatches && differenceIsEmpty)
            .isFalse();
    }

    @ParameterizedTest
    @MethodSource("binaryCorpus")
    void given_fixturePair_when_absorptionOperationsOnKnownFailureCorpus_then_detectsAlgebraicDrift(
        String corpusKey)
    {
        // Arrange
        PolyhedralBoundedSolid baselineLeft = createPair(corpusKey)[0];
        double[] baselineMinMax = baselineLeft.getMinMax();

        PolyhedralBoundedSolid[] pairForIntersection = createPair(corpusKey);
        PolyhedralBoundedSolid[] pairForUnion = createPair(corpusKey);
        PolyhedralBoundedSolid[] pairForFinalUnion = createPair(corpusKey);
        PolyhedralBoundedSolid[] pairForFinalIntersection = createPair(corpusKey);

        // Action
        PolyhedralBoundedSolid aIntersectionB = PolyhedralBoundedSolidModeler.setOp(
            pairForIntersection[0], pairForIntersection[1],
            PolyhedralBoundedSolidModeler.INTERSECTION, false);
        PolyhedralBoundedSolid firstAbsorption = PolyhedralBoundedSolidModeler.setOp(
            pairForFinalUnion[0], aIntersectionB, PolyhedralBoundedSolidModeler.UNION,
            false);

        PolyhedralBoundedSolid aUnionB = PolyhedralBoundedSolidModeler.setOp(
            pairForUnion[0], pairForUnion[1], PolyhedralBoundedSolidModeler.UNION, false);
        PolyhedralBoundedSolid secondAbsorption = PolyhedralBoundedSolidModeler.setOp(
            pairForFinalIntersection[0], aUnionB,
            PolyhedralBoundedSolidModeler.INTERSECTION, false);

        // Assert
        boolean firstMatches = isBoundingBoxClose(firstAbsorption, baselineMinMax);
        boolean secondMatches = isBoundingBoxClose(secondAbsorption,
            baselineMinMax);
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(firstAbsorption)).isTrue();
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(secondAbsorption)).isTrue();
        assertThat(firstMatches && secondMatches).isFalse();
    }

    @ParameterizedTest
    @MethodSource("differenceSwappedCorpus")
    void given_fixturePair_when_differenceUsesSwappedOperands_then_eachOrderRemainsDeterministicAndValid(
        String corpusKey)
    {
        // Arrange
        PolyhedralBoundedSolid[] pairA = createPair(corpusKey);
        PolyhedralBoundedSolid[] pairB = createPair(corpusKey);
        PolyhedralBoundedSolid[] pairC = createPair(corpusKey);
        PolyhedralBoundedSolid[] pairD = createPair(corpusKey);

        // Action
        PolyhedralBoundedSolid differenceABFirst = PolyhedralBoundedSolidModeler.setOp(
            pairA[0], pairA[1], PolyhedralBoundedSolidModeler.SUBTRACT, false);
        PolyhedralBoundedSolid differenceABSecond = PolyhedralBoundedSolidModeler.setOp(
            pairB[0], pairB[1], PolyhedralBoundedSolidModeler.SUBTRACT, false);

        PolyhedralBoundedSolid differenceBAFirst = PolyhedralBoundedSolidModeler.setOp(
            pairC[1], pairC[0], PolyhedralBoundedSolidModeler.SUBTRACT, false);
        PolyhedralBoundedSolid differenceBASecond = PolyhedralBoundedSolidModeler.setOp(
            pairD[1], pairD[0], PolyhedralBoundedSolidModeler.SUBTRACT, false);

        // Assert
        assertThat(differenceABFirst.polygonsList.size())
            .isEqualTo(differenceABSecond.polygonsList.size());
        assertThat(differenceABFirst.edgesList.size())
            .isEqualTo(differenceABSecond.edgesList.size());
        assertThat(differenceABFirst.verticesList.size())
            .isEqualTo(differenceABSecond.verticesList.size());
        assertThat(differenceBAFirst.polygonsList.size())
            .isEqualTo(differenceBASecond.polygonsList.size());
        assertThat(differenceBAFirst.edgesList.size())
            .isEqualTo(differenceBASecond.edgesList.size());
        assertThat(differenceBAFirst.verticesList.size())
            .isEqualTo(differenceBASecond.verticesList.size());
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(differenceABFirst)).isTrue();
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(differenceABSecond)).isTrue();
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(differenceBAFirst)).isTrue();
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(differenceBASecond)).isTrue();
    }

    private static Stream<Arguments> idempotenceCorpus()
    {
        return Stream.of(
            Arguments.of("MANT1986_2", 0),
            Arguments.of("MANT1986_2", 1),
            Arguments.of("MANT1988_15_2_LIMIT", 0),
            Arguments.of("MANT1988_15_2_LIMIT", 1),
            Arguments.of("MANT1988_6_13", 0),
            Arguments.of("MANT1988_6_13", 1)
        );
    }

    private static Stream<Arguments> binaryCorpus()
    {
        return Stream.of(
            Arguments.of("MANT1986_2"),
            Arguments.of("MANT1988_15_2_LIMIT"),
            Arguments.of("MANT1988_6_13")
        );
    }

    private static Stream<Arguments> differenceSwappedCorpus()
    {
        return Stream.of(
            Arguments.of("MANT1986_2"),
            Arguments.of("MANT1988_15_2_LIMIT"),
            Arguments.of("MANT1988_6_13")
        );
    }

    private static PolyhedralBoundedSolid[] createEquivalentSolidPair(
        String corpusKey, int solidIndex)
    {
        PolyhedralBoundedSolid[] pair = createPair(corpusKey);
        PolyhedralBoundedSolid left = pair[solidIndex];
        PolyhedralBoundedSolid right = createPair(corpusKey)[solidIndex];
        return new PolyhedralBoundedSolid[] { left, right };
    }

    private static PolyhedralBoundedSolid[] createPair(String corpusKey)
    {
        if ( "MANT1986_2".equals(corpusKey) ) {
            return PolyhedralBoundedSolidTestFixtures.createMant1986_2Pair();
        }
        if ( "MANT1988_15_2_LIMIT".equals(corpusKey) ) {
            return PolyhedralBoundedSolidTestFixtures.createMant1988_15_2Pair(0);
        }
        if ( "MANT1988_6_13".equals(corpusKey) ) {
            return PolyhedralBoundedSolidTestFixtures.createMant1988_6_13Pair();
        }
        throw new IllegalArgumentException("Unsupported corpus: " + corpusKey);
    }

    private static boolean isBoundingBoxClose(PolyhedralBoundedSolid solid,
                                              double[] baselineMinMax)
    {
        double[] actualMinMax = solid.getMinMax();
        int i;
        for ( i = 0; i < 6; i++ ) {
            if ( Math.abs(actualMinMax[i] - baselineMinMax[i]) > 1.0e-6 ) {
                return false;
            }
        }
        return true;
    }
}
