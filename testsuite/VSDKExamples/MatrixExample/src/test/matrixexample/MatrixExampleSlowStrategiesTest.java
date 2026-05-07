package matrixexample;

import org.junit.jupiter.api.Test;

import matrixexample.config.CommandLineOptions;
import vsdk.toolkit.common.linealAlgebra.MatrixNxM;
import vsdk.toolkit.processing.linealAlgebra.LinearAlgebraEngine;
import vsdk.toolkit.processing.linealAlgebra.StrategySelector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class MatrixExampleSlowStrategiesTest
{
    @Test
    void given_largeMatrix_when_usingNaiveCofactor_then_determinantAndInverseAreComputableAndSlowEnoughForManualProfiling()
    {
        assumeTrue(Boolean.getBoolean("matrixexample.runSlow"),
            "Enable with -Dmatrixexample.runSlow=true");

        CommandLineOptions options = CommandLineOptions.parse(new String[] {
            "--size", "11",
            "--seed", "1234",
            "--operation", "both",
            "--strategy", "NAIVE_COFACTOR_CPU"
        });

        LinearAlgebraEngine engine = LinearAlgebraEngine.fromStrategy(options.strategy());
        MatrixNxM matrix = MatrixExample.createDiagonallyDominantMatrix(options.size(), options.seed());

        long detStart = System.nanoTime();
        double determinant = engine.determinant(matrix);
        long detElapsedMs = (System.nanoTime() - detStart) / 1_000_000L;

        long invStart = System.nanoTime();
        MatrixNxM inverse = engine.inverse(matrix);
        long invElapsedMs = (System.nanoTime() - invStart) / 1_000_000L;

        assertThat(determinant).isNotNaN();
        assertThat(inverse).isNotNull();

        // This threshold is intentionally conservative for CI variability.
        assertThat(detElapsedMs + invElapsedMs).isGreaterThanOrEqualTo(500L);

        MatrixNxM identity = matrix.multiply(inverse);
        assertThat(identity.epsilonEquals(new MatrixNxM(options.size(), options.size()), 1.0e-4)).isTrue();
    }

    @Test
    void given_strategySelection_when_parsingOptions_then_itMatchesRequestedStrategy()
    {
        CommandLineOptions options = CommandLineOptions.parse(new String[] {
            "--strategy", "LU_CPU"
        });

        assertThat(options.strategy()).isEqualTo(StrategySelector.ComputeStrategy.LU_CPU);
    }
}
