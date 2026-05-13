package vsdk.toolkit.processing.linealAlgebra;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.MatrixNxM;
import vsdk.toolkit.common.linealAlgebra.exceptions.MatrixSingularException;

public class GaussCpuStrategy implements DeterminantStrategy, InverseStrategy
{
    @Override
    public String id()
    {
        return "gauss-cpu";
    }

    @Override
    public double determinant(MatrixNxM matrix)
    {
        MatrixAlgorithmsSupport.requireSquare(matrix);
        double[][] a = MatrixAlgorithmsSupport.toArray(matrix);
        int n = a.length;
        int sign = 1;

        int k;
        for ( k = 0; k < n; k++ ) {
            int pivot = k;
            double max = Math.abs(a[k][k]);
            int i;
            for ( i = k + 1; i < n; i++ ) {
                double candidate = Math.abs(a[i][k]);
                if ( candidate > max ) {
                    max = candidate;
                    pivot = i;
                }
            }

            if ( Math.abs(max) < VSDK.EPSILON ) {
                return 0.0;
            }

            if ( pivot != k ) {
                double[] tmp = a[pivot];
                a[pivot] = a[k];
                a[k] = tmp;
                sign = -sign;
            }

            for ( i = k + 1; i < n; i++ ) {
                double factor = a[i][k] / a[k][k];
                int j;
                for ( j = k + 1; j < n; j++ ) {
                    a[i][j] -= factor * a[k][j];
                }
                a[i][k] = 0.0;
            }
        }

        double det = sign;
        int i;
        for ( i = 0; i < n; i++ ) {
            det *= a[i][i];
        }
        return det;
    }

    @Override
    public MatrixNxM inverse(MatrixNxM matrix)
    {
        MatrixAlgorithmsSupport.requireSquare(matrix);
        double[][] a = MatrixAlgorithmsSupport.toArray(matrix);
        int n = a.length;
        double[][] inv = new double[n][n];

        int i;
        for ( i = 0; i < n; i++ ) {
            inv[i][i] = 1.0;
        }

        int col;
        for ( col = 0; col < n; col++ ) {
            int pivot = col;
            double max = Math.abs(a[col][col]);
            for ( i = col + 1; i < n; i++ ) {
                double candidate = Math.abs(a[i][col]);
                if ( candidate > max ) {
                    max = candidate;
                    pivot = i;
                }
            }

            if ( Math.abs(max) < VSDK.EPSILON ) {
                throw new MatrixSingularException("Matrix is singular during Gauss-Jordan elimination");
            }

            if ( pivot != col ) {
                double[] tmp = a[pivot];
                a[pivot] = a[col];
                a[col] = tmp;

                tmp = inv[pivot];
                inv[pivot] = inv[col];
                inv[col] = tmp;
            }

            double pivotValue = a[col][col];
            int j;
            for ( j = 0; j < n; j++ ) {
                a[col][j] /= pivotValue;
                inv[col][j] /= pivotValue;
            }

            for ( i = 0; i < n; i++ ) {
                if ( i == col ) {
                    continue;
                }
                double factor = a[i][col];
                for ( j = 0; j < n; j++ ) {
                    a[i][j] -= factor * a[col][j];
                    inv[i][j] -= factor * inv[col][j];
                }
            }
        }

        return MatrixAlgorithmsSupport.fromArray(inv);
    }
}
