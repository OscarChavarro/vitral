package vsdk.toolkit.processing.linealAlgebra;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.MatrixNxM;
import vsdk.toolkit.common.linealAlgebra.exceptions.MatrixSingularException;

public class NaiveCofactorCpuStrategy implements DeterminantStrategy, InverseStrategy
{
    @Override
    public String id()
    {
        return "naive-cofactor-cpu";
    }

    @Override
    public double determinant(MatrixNxM matrix)
    {
        MatrixAlgorithmsSupport.requireSquare(matrix);
        return determinantArray(MatrixAlgorithmsSupport.toArray(matrix));
    }

    @Override
    public MatrixNxM inverse(MatrixNxM matrix)
    {
        MatrixAlgorithmsSupport.requireSquare(matrix);
        double[][] source = MatrixAlgorithmsSupport.toArray(matrix);
        double det = determinantArray(source);

        if ( Math.abs(det) < VSDK.EPSILON ) {
            throw new MatrixSingularException("Trying to invert a matrix with zero determinant");
        }

        int n = source.length;
        double[][] cofactors = new double[n][n];
        int row;
        int col;
        for ( row = 0; row < n; row++ ) {
            for ( col = 0; col < n; col++ ) {
                double sign = ((row + col) % 2 == 0) ? 1.0 : -1.0;
                cofactors[row][col] = sign * determinantArray(minor(source, row, col));
            }
        }

        double[][] inverse = new double[n][n];
        for ( row = 0; row < n; row++ ) {
            for ( col = 0; col < n; col++ ) {
                inverse[row][col] = cofactors[col][row] / det;
            }
        }

        return MatrixAlgorithmsSupport.fromArray(inverse);
    }

    private double determinantArray(double[][] matrix)
    {
        int n = matrix.length;
        if ( n == 1 ) {
            return matrix[0][0];
        }
        if ( n == 2 ) {
            return matrix[0][0] * matrix[1][1] - matrix[0][1] * matrix[1][0];
        }

        double accum = 0.0;
        int col;
        for ( col = 0; col < n; col++ ) {
            double sign = ((col % 2) == 0) ? 1.0 : -1.0;
            accum += sign * matrix[0][col] * determinantArray(minor(matrix, 0, col));
        }
        return accum;
    }

    private double[][] minor(double[][] matrix, int rowToSkip, int colToSkip)
    {
        int n = matrix.length;
        double[][] minor = new double[n - 1][n - 1];
        int i;
        int j;
        int r = 0;
        int c;

        for ( i = 0; i < n; i++ ) {
            if ( i == rowToSkip ) {
                continue;
            }
            c = 0;
            for ( j = 0; j < n; j++ ) {
                if ( j == colToSkip ) {
                    continue;
                }
                minor[r][c] = matrix[i][j];
                c++;
            }
            r++;
        }

        return minor;
    }
}
