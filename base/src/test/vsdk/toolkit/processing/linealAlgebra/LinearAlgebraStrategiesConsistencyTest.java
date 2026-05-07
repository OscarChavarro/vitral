package vsdk.toolkit.processing.linealAlgebra;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import vsdk.toolkit.common.linealAlgebra.MatrixNxM;

import static org.assertj.core.api.Assertions.assertThat;

class LinearAlgebraStrategiesConsistencyTest
{
    @ParameterizedTest
    @MethodSource("strategies")
    void given_sameMatrix_when_computingDeterminant_then_allStrategiesAgree(
        StrategySelector.ComputeStrategy strategy)
    {
        MatrixNxM matrix = new MatrixNxM(3, 3)
            .withVal(0, 0, 3.0).withVal(0, 1, 2.0).withVal(0, 2, -1.0)
            .withVal(1, 0, 2.0).withVal(1, 1, -2.0).withVal(1, 2, 4.0)
            .withVal(2, 0, -1.0).withVal(2, 1, 0.5).withVal(2, 2, -1.0);

        LinearAlgebraEngine baseline =
            LinearAlgebraEngine.fromStrategy(StrategySelector.ComputeStrategy.NAIVE_COFACTOR_CPU);
        LinearAlgebraEngine engine = LinearAlgebraEngine.fromStrategy(strategy);

        double expected = baseline.determinant(matrix);
        double actual = engine.determinant(matrix);

        assertThat(actual).isCloseTo(expected, org.assertj.core.data.Offset.offset(1.0e-9));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void given_invertibleMatrix_when_computingInverse_then_allStrategiesProduceEquivalentResults(
        StrategySelector.ComputeStrategy strategy)
    {
        MatrixNxM matrix = new MatrixNxM(3, 3)
            .withVal(0, 0, 4.0).withVal(0, 1, 7.0).withVal(0, 2, 2.0)
            .withVal(1, 0, 3.0).withVal(1, 1, 6.0).withVal(1, 2, 1.0)
            .withVal(2, 0, 2.0).withVal(2, 1, 5.0).withVal(2, 2, 3.0);

        LinearAlgebraEngine baseline =
            LinearAlgebraEngine.fromStrategy(StrategySelector.ComputeStrategy.NAIVE_COFACTOR_CPU);
        LinearAlgebraEngine engine = LinearAlgebraEngine.fromStrategy(strategy);

        MatrixNxM expected = baseline.inverse(matrix);
        MatrixNxM actual = engine.inverse(matrix);

        assertThat(actual.epsilonEquals(expected, 1.0e-7)).isTrue();

        MatrixNxM identity = matrix.multiply(actual);
        assertThat(identity.epsilonEquals(new MatrixNxM(3, 3), 1.0e-6)).isTrue();
    }

    private static Stream<Arguments> strategies()
    {
        return Stream.of(
            Arguments.of(StrategySelector.ComputeStrategy.NAIVE_COFACTOR_CPU),
            Arguments.of(StrategySelector.ComputeStrategy.LU_CPU),
            Arguments.of(StrategySelector.ComputeStrategy.GAUSS_CPU)
        );
    }
}
