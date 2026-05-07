package vsdk.toolkit.processing.linealAlgebra;

import vsdk.toolkit.common.linealAlgebra.MatrixNxM;

public interface DeterminantStrategy
{
    String id();

    double determinant(MatrixNxM matrix);
}
