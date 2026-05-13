package vsdk.toolkit.processing.linealAlgebra;

public interface LinearAlgebraBackend
{
    String id();

    DeterminantStrategy determinantStrategy();

    InverseStrategy inverseStrategy();
}
