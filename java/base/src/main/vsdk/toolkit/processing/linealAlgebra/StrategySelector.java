package vsdk.toolkit.processing.linealAlgebra;

public class StrategySelector
{
    public enum ComputeStrategy
    {
        NAIVE_COFACTOR_CPU,
        LU_CPU,
        GAUSS_CPU
    }

    public LinearAlgebraBackend select(ComputeStrategy strategy)
    {
        if ( strategy == null ) {
            return new LinearAlgebraEngine.DefaultBackend(new NaiveCofactorCpuStrategy());
        }

        switch ( strategy ) {
          case LU_CPU:
              return new LinearAlgebraEngine.DefaultBackend(new LuCpuStrategy());
          case GAUSS_CPU:
              return new LinearAlgebraEngine.DefaultBackend(new GaussCpuStrategy());
          case NAIVE_COFACTOR_CPU:
          default:
              return new LinearAlgebraEngine.DefaultBackend(new NaiveCofactorCpuStrategy());
        }
    }
}
