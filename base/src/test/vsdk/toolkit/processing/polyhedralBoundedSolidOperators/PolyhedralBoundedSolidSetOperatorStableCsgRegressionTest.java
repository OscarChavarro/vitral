package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
Hard regression corpus for boolean fixtures currently considered stable.

<p>Traceability: [MANT1988] Ch. 15 set-operation algorithm; these tests
lock topology and bounding geometry after union, intersection, and
difference so generate/classify/connect/finish changes cannot drift silently.</p>
 */
class PolyhedralBoundedSolidSetOperatorStableCsgRegressionTest
{
    @ParameterizedTest
    @MethodSource("stableRegressionCorpus")
    void given_stableCsgFixture_when_runningBoolean_then_resultKeepsExactTopologyAndStableGeometry(
        RegressionExpectation expected)
    {
        PolyhedralBoundedSolid result = runOperation(expected.sample, expected.op);
        double epsilon = PolyhedralBoundedSolidNumericPolicy.forSolid(result)
            .bigEpsilon();
        double[] actualMinMax = result.getMinMax();

        assertThat(result).isNotNull();
        assertThat(result.polygonsList.size()).isEqualTo(expected.faceCount);
        assertThat(result.edgesList.size()).isEqualTo(expected.edgeCount);
        assertThat(result.verticesList.size()).isEqualTo(expected.vertexCount);
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(result)).isTrue();
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateStrict(result)).isTrue();

        int i;
        for ( i = 0; i < expected.minMax.length; i++ ) {
            assertThat(actualMinMax[i]).isCloseTo(expected.minMax[i],
                within(epsilon));
        }
    }

    private static Stream<Arguments> stableRegressionCorpus()
    {
        return Stream.of(
            Arguments.of(new RegressionExpectation(
                CsgSampleCorpus.MANT1986_2, StableOp.UNION,
                12, 30, 20,
                new double[] {
                    0.000000000, -0.180000000, 0.000000000,
                    1.240000000, 0.500000000, 1.020000000
                })),
            Arguments.of(new RegressionExpectation(
                CsgSampleCorpus.MANT1986_2, StableOp.INTERSECTION,
                6, 12, 8,
                new double[] {
                    0.240000000, 0.000000000, 0.420000000,
                    1.000000000, 0.320000000, 0.600000000
                })),
            Arguments.of(new RegressionExpectation(
                CsgSampleCorpus.MANT1986_2, StableOp.SUBTRACT_AB,
                9, 21, 14,
                new double[] {
                    0.000000000, 0.000000000, 0.000000000,
                    1.000000000, 0.500000000, 0.600000000
                })),
            Arguments.of(new RegressionExpectation(
                CsgSampleCorpus.MANT1986_2, StableOp.SUBTRACT_BA,
                9, 21, 14,
                new double[] {
                    0.240000000, -0.180000000, 0.420000000,
                    1.240000000, 0.320000000, 1.020000000
                })),
            Arguments.of(new RegressionExpectation(
                CsgSampleCorpus.STACKED_BLOCKS, StableOp.UNION,
                14, 32, 20,
                new double[] {
                    0.000000000, 0.000000000, 0.000000000,
                    1.000000000, 1.000000000, 0.600000000
                })),
            Arguments.of(new RegressionExpectation(
                CsgSampleCorpus.STACKED_BLOCKS, StableOp.INTERSECTION,
                2, 4, 4,
                new double[] {
                    0.250000000, 0.250000000, 0.300000000,
                    0.750000000, 0.750000000, 0.300000000
                })),
            Arguments.of(new RegressionExpectation(
                CsgSampleCorpus.STACKED_BLOCKS, StableOp.SUBTRACT_AB,
                6, 12, 8,
                new double[] {
                    0.000000000, 0.250000000, 0.000000000,
                    1.000000000, 0.750000000, 0.300000000
                })),
            Arguments.of(new RegressionExpectation(
                CsgSampleCorpus.STACKED_BLOCKS, StableOp.SUBTRACT_BA,
                6, 12, 8,
                new double[] {
                    0.250000000, 0.000000000, 0.300000000,
                    0.750000000, 1.000000000, 0.600000000
                })),
            Arguments.of(new RegressionExpectation(
                CsgSampleCorpus.MOON_BLOCK, StableOp.UNION,
                76, 222, 148,
                new double[] {
                    0.050000000, 0.050000000, -0.450000000,
                    1.325000000, 1.050000000, 1.550000000
                })),
            Arguments.of(new RegressionExpectation(
                CsgSampleCorpus.MOON_BLOCK, StableOp.INTERSECTION,
                34, 96, 64,
                new double[] {
                    0.325000000, 0.071174693, 0.050000000,
                    1.050000000, 1.028825307, 1.050000000
                })),
            Arguments.of(new RegressionExpectation(
                CsgSampleCorpus.MOON_BLOCK, StableOp.SUBTRACT_AB,
                40, 114, 76,
                new double[] {
                    0.050000000, 0.050000000, 0.050000000,
                    0.687500000, 1.050000000, 1.050000000
                })),
            Arguments.of(new RegressionExpectation(
                CsgSampleCorpus.MOON_BLOCK, StableOp.SUBTRACT_BA,
                70, 204, 136,
                new double[] {
                    0.325000000, 0.050000000, -0.450000000,
                    1.325000000, 1.050000000, 1.550000000
                }))
        );
    }

    private static PolyhedralBoundedSolid runOperation(CsgSampleCorpus sample,
                                                       StableOp op)
    {
        PolyhedralBoundedSolid[] operands = CsgSampleCorpusFixtures
            .createPair(sample);

        switch ( op ) {
            case UNION:
                return PolyhedralBoundedSolidModeler.setOp(
                    operands[0], operands[1], PolyhedralBoundedSolidModeler.UNION,
                    false);
            case INTERSECTION:
                return PolyhedralBoundedSolidModeler.setOp(
                    operands[0], operands[1],
                    PolyhedralBoundedSolidModeler.INTERSECTION, false);
            case SUBTRACT_AB:
                return PolyhedralBoundedSolidModeler.setOp(
                    operands[0], operands[1], PolyhedralBoundedSolidModeler.SUBTRACT,
                    false);
            case SUBTRACT_BA:
            default:
                return PolyhedralBoundedSolidModeler.setOp(
                    operands[1], operands[0], PolyhedralBoundedSolidModeler.SUBTRACT,
                    false);
        }
    }

    private enum StableOp
    {
        UNION,
        INTERSECTION,
        SUBTRACT_AB,
        SUBTRACT_BA
    }

    private static final class RegressionExpectation
    {
        private final CsgSampleCorpus sample;
        private final StableOp op;
        private final int faceCount;
        private final int edgeCount;
        private final int vertexCount;
        private final double[] minMax;

        private RegressionExpectation(CsgSampleCorpus sample, StableOp op,
                                      int faceCount, int edgeCount,
                                      int vertexCount, double[] minMax)
        {
            this.sample = sample;
            this.op = op;
            this.faceCount = faceCount;
            this.edgeCount = edgeCount;
            this.vertexCount = vertexCount;
            this.minMax = minMax;
        }

        @Override
        public String toString()
        {
            return sample.name() + "_" + op.name();
        }
    }
}
