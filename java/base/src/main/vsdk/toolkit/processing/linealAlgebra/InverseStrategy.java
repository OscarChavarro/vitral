package vsdk.toolkit.processing.linealAlgebra;

import vsdk.toolkit.common.linealAlgebra.MatrixNxM;

public interface InverseStrategy
{
    String id();

    MatrixNxM inverse(MatrixNxM matrix);
}
