package vsdk.toolkit.processing.linealAlgebra;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.MatrixNxM;
import vsdk.toolkit.common.linealAlgebra.exceptions.MatrixSingularException;

public class LuCpuStrategy implements DeterminantStrategy, InverseStrategy
{
    @Override
    public String id()
    {
        return "lu-cpu";
    }

    @Override
    public double determinant(MatrixNxM matrix)
    {
        MatrixAlgorithmsSupport.requireSquare(matrix);
        LuDecomposition lu = decompose(MatrixAlgorithmsSupport.toArray(matrix));
        int n = lu.lu.length;
        double det = lu.pivotSign;

        int i;
        for ( i = 0; i < n; i++ ) {
            det *= lu.lu[i][i];
        }
        return det;
    }

    @Override
    public MatrixNxM inverse(MatrixNxM matrix)
    {
        MatrixAlgorithmsSupport.requireSquare(matrix);
        LuDecomposition lu = decompose(MatrixAlgorithmsSupport.toArray(matrix));
        int n = lu.lu.length;
        double[][] inv = new double[n][n];

        int col;
        for ( col = 0; col < n; col++ ) {
            double[] e = new double[n];
            e[col] = 1.0;
            double[] x = solve(lu, e);
            int row;
            for ( row = 0; row < n; row++ ) {
                inv[row][col] = x[row];
            }
        }

        return MatrixAlgorithmsSupport.fromArray(inv);
    }

    private LuDecomposition decompose(double[][] source)
    {
        int n = source.length;
        double[][] lu = new double[n][n];
        int i;
        int j;
        for ( i = 0; i < n; i++ ) {
            System.arraycopy(source[i], 0, lu[i], 0, n);
        }

        int[] piv = new int[n];
        for ( i = 0; i < n; i++ ) {
            piv[i] = i;
        }
        int pivSign = 1;

        int k;
        for ( k = 0; k < n; k++ ) {
            int p = k;
            double max = Math.abs(lu[k][k]);
            for ( i = k + 1; i < n; i++ ) {
                double v = Math.abs(lu[i][k]);
                if ( v > max ) {
                    max = v;
                    p = i;
                }
            }

            if ( Math.abs(max) < VSDK.EPSILON ) {
                throw new MatrixSingularException("Matrix is singular during LU decomposition");
            }

            if ( p != k ) {
                double[] tmp = lu[p];
                lu[p] = lu[k];
                lu[k] = tmp;

                int t = piv[p];
                piv[p] = piv[k];
                piv[k] = t;
                pivSign = -pivSign;
            }

            for ( i = k + 1; i < n; i++ ) {
                lu[i][k] /= lu[k][k];
                for ( j = k + 1; j < n; j++ ) {
                    lu[i][j] -= lu[i][k] * lu[k][j];
                }
            }
        }

        return new LuDecomposition(lu, piv, pivSign);
    }

    private double[] solve(LuDecomposition lu, double[] b)
    {
        int n = lu.lu.length;
        double[] x = new double[n];
        int i;
        for ( i = 0; i < n; i++ ) {
            x[i] = b[lu.piv[i]];
        }

        int j;
        for ( i = 0; i < n; i++ ) {
            for ( j = 0; j < i; j++ ) {
                x[i] -= lu.lu[i][j] * x[j];
            }
        }

        for ( i = n - 1; i >= 0; i-- ) {
            for ( j = i + 1; j < n; j++ ) {
                x[i] -= lu.lu[i][j] * x[j];
            }
            x[i] /= lu.lu[i][i];
        }

        return x;
    }

    private static final class LuDecomposition
    {
        private final double[][] lu;
        private final int[] piv;
        private final int pivotSign;

        private LuDecomposition(double[][] lu, int[] piv, int pivotSign)
        {
            this.lu = lu;
            this.piv = piv;
            this.pivotSign = pivotSign;
        }
    }
}
