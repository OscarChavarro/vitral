package vsdk.toolkit.processing.linealAlgebra;

import vsdk.toolkit.common.linealAlgebra.MatrixNxM;

public class LinearAlgebraEngine
{
    private static final LinearAlgebraEngine DEFAULT_ENGINE =
        fromStrategy(StrategySelector.ComputeStrategy.NAIVE_COFACTOR_CPU);

    private final LinearAlgebraBackend backend;

    public LinearAlgebraEngine(LinearAlgebraBackend backend)
    {
        if ( backend == null ) {
            throw new IllegalArgumentException("backend cannot be null");
        }
        this.backend = backend;
    }

    public static LinearAlgebraEngine defaultEngine()
    {
        return DEFAULT_ENGINE;
    }

    public static LinearAlgebraEngine fromStrategy(StrategySelector.ComputeStrategy strategy)
    {
        StrategySelector selector = new StrategySelector();
        return new LinearAlgebraEngine(selector.select(strategy));
    }

    public double determinant(MatrixNxM matrix)
    {
        return backend.determinantStrategy().determinant(matrix);
    }

    public MatrixNxM inverse(MatrixNxM matrix)
    {
        return backend.inverseStrategy().inverse(matrix);
    }

    public LinearAlgebraBackend backend()
    {
        return backend;
    }

    static final class DefaultBackend implements LinearAlgebraBackend
    {
        private final DeterminantStrategy determinant;
        private final InverseStrategy inverse;

        DefaultBackend(Object strategy)
        {
            if ( !(strategy instanceof DeterminantStrategy) ) {
                throw new IllegalArgumentException("strategy must implement DeterminantStrategy");
            }
            if ( !(strategy instanceof InverseStrategy) ) {
                throw new IllegalArgumentException("strategy must implement InverseStrategy");
            }
            this.determinant = (DeterminantStrategy) strategy;
            this.inverse = (InverseStrategy) strategy;
        }

        @Override
        public String id()
        {
            return determinant.id();
        }

        @Override
        public DeterminantStrategy determinantStrategy()
        {
            return determinant;
        }

        @Override
        public InverseStrategy inverseStrategy()
        {
            return inverse;
        }
    }
}
