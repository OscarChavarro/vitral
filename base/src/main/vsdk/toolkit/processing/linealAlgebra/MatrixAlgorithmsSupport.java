package vsdk.toolkit.processing.linealAlgebra;

import vsdk.toolkit.common.linealAlgebra.MatrixNxM;
import vsdk.toolkit.common.linealAlgebra.exceptions.MatrixDimensionMismatchException;
import vsdk.toolkit.common.linealAlgebra.exceptions.MatrixNotSquareException;

final class MatrixAlgorithmsSupport
{
    private MatrixAlgorithmsSupport()
    {
    }

    static void requireNonNull(MatrixNxM matrix)
    {
        if ( matrix == null ) {
            throw new IllegalArgumentException("matrix cannot be null");
        }
    }

    static void requireSquare(MatrixNxM matrix)
    {
        requireNonNull(matrix);
        if ( matrix.getNumRows() != matrix.getNumColumns() ) {
            throw new MatrixNotSquareException("Matrix must be square");
        }
    }

    static double[][] toArray(MatrixNxM matrix)
    {
        int rows = matrix.getNumRows();
        int cols = matrix.getNumColumns();
        double[][] values = new double[rows][cols];

        int i;
        int j;
        for ( i = 0; i < rows; i++ ) {
            for ( j = 0; j < cols; j++ ) {
                values[i][j] = matrix.getVal(i, j);
            }
        }

        return values;
    }

    static MatrixNxM fromArray(double[][] values)
    {
        if ( values == null || values.length == 0 || values[0] == null || values[0].length == 0 ) {
            throw new MatrixDimensionMismatchException("values cannot be null or empty");
        }

        int rows = values.length;
        int cols = values[0].length;
        MatrixNxM result = new MatrixNxM(rows, cols);

        int i;
        int j;
        for ( i = 0; i < rows; i++ ) {
            if ( values[i] == null || values[i].length != cols ) {
                throw new MatrixDimensionMismatchException("all rows must have the same length");
            }
            for ( j = 0; j < cols; j++ ) {
                result = result.withVal(i, j, values[i][j]);
            }
        }

        return result;
    }
}
