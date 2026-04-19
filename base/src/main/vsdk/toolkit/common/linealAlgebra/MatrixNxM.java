package vsdk.toolkit.common.linealAlgebra;

import java.io.Serial;
import java.util.Arrays;
import java.util.Objects;

import vsdk.toolkit.common.FundamentalEntity;
import vsdk.toolkit.common.VSDK;

/**
This class is a data structure that represents a NxM matrix.
*/
public final class MatrixNxM extends FundamentalEntity
{
    @Serial
    private static final long serialVersionUID = 20260419L;

    private final int numRows;
    private final int numColumns;
    private final double[][] m;

    /**
    This constructor builds the NxM identity matrix.
    */
    public MatrixNxM(int n, int m) throws Exception
    {
        if ( n <= 0 || m <= 0 ) {
            throw new Exception("Invalid matrix size!");
        }
        this.numRows = n;
        this.numColumns = m;
        this.m = buildIdentityValues(n, m);
    }

    /**
    Copy constructor.
    */
    public MatrixNxM(MatrixNxM other)
    {
        this(Objects.requireNonNull(other, "Matrix to copy cannot be null").numRows,
             other.numColumns,
             other.m,
             true);
    }

    private MatrixNxM(int rows, int cols, double[][] values, boolean deepCopy)
    {
        this.numRows = rows;
        this.numColumns = cols;
        this.m = deepCopy ? deepCopy(values) : values;
    }

    public static MatrixNxM copyOf(MatrixNxM other)
    {
        return new MatrixNxM(other);
    }

    /**
    Returns an identity matrix with the same dimensions.
    */
    public MatrixNxM identity()
    {
        return new MatrixNxM(numRows, numColumns, buildIdentityValues(numRows, numColumns), false);
    }

    public int getNumRows()
    {
        return numRows;
    }

    public int getNumColumns()
    {
        return numColumns;
    }

    public double getVal(int row, int column) throws Exception
    {
        validatePosition(row, column);
        return m[row][column];
    }

    public MatrixNxM withVal(int row, int column, double val) throws Exception
    {
        validatePosition(row, column);
        double[][] r = deepCopy(m);
        r[row][column] = val;
        return new MatrixNxM(numRows, numColumns, r, false);
    }

    public MatrixNxM inverse() throws Exception
    {
        double d = determinant();
        if ( Math.abs(d) < VSDK.EPSILON ) {
            throw new Exception("Trying to invert a matrix with zero determinant!");
        }
        MatrixNxM cof = cofactors();
        MatrixNxM adj = cof.transpose();
        return adj.multiply(1.0 / d);
    }

    public MatrixNxM cofactors() throws Exception
    {
        double[][] r = new double[numRows][numColumns];

        for ( int row = 0; row < numRows; row++ ) {
            for ( int column = 0; column < numColumns; column++ ) {
                MatrixNxM minor = buildMinor(row, column);
                double sign = ((row + column) % 2 == 0) ? 1.0 : -1.0;
                r[row][column] = sign * minor.determinant();
            }
        }
        return new MatrixNxM(numRows, numColumns, r, false);
    }

    public MatrixNxM transpose()
    {
        double[][] r = new double[numColumns][numRows];
        for ( int row = 0; row < numRows; row++ ) {
            for ( int column = 0; column < numColumns; column++ ) {
                r[column][row] = m[row][column];
            }
        }
        return new MatrixNxM(numColumns, numRows, r, false);
    }

    public MatrixNxM multiply(double a)
    {
        double[][] r = new double[numRows][numColumns];
        for ( int row = 0; row < numRows; row++ ) {
            for ( int column = 0; column < numColumns; column++ ) {
                r[row][column] = a * m[row][column];
            }
        }
        return new MatrixNxM(numRows, numColumns, r, false);
    }

    public MatrixNxM multiply(MatrixNxM other) throws Exception
    {
        if ( numColumns != other.numRows ) {
            throw new Exception("When multiplying matrices, first operand number of columns must match second operand number of rows.");
        }

        double[][] r = new double[numRows][other.numColumns];
        for ( int rowA = 0; rowA < numRows; rowA++ ) {
            for ( int columnB = 0; columnB < other.numColumns; columnB++ ) {
                double accum = 0;
                for ( int rowB = 0; rowB < numColumns; rowB++ ) {
                    accum += m[rowA][rowB] * other.m[rowB][columnB];
                }
                r[rowA][columnB] = accum;
            }
        }
        return new MatrixNxM(numRows, other.numColumns, r, false);
    }

    public MatrixNxM buildMinor(int row, int column) throws Exception
    {
        if ( numColumns <= 1 || numRows <= 1 ) {
            throw new Exception("Matrix must be at least of size 2x2 to have a minor matrix!");
        }
        validatePosition(row, column);

        double[][] minor = new double[numRows - 1][numColumns - 1];
        for ( int r1 = 0, r2 = 0; r1 < numRows; r1++ ) {
            if ( r1 == row ) continue;
            for ( int c1 = 0, c2 = 0; c1 < numColumns; c1++ ) {
                if ( c1 == column ) continue;
                minor[r2][c2] = m[r1][c1];
                c2++;
            }
            r2++;
        }

        return new MatrixNxM(numRows - 1, numColumns - 1, minor, false);
    }

    public double determinant() throws Exception
    {
        if ( numColumns != numRows ) {
            throw new Exception("Matrix must be square to have a determinant");
        }
        if ( numColumns == 1 ) {
            return m[0][0];
        }

        double accum = 0;
        int row = 0;
        for ( int col = 0, sign = 1; col < numColumns; col++, sign *= -1 ) {
            MatrixNxM minor = buildMinor(row, col);
            accum += ((double)sign) * minor.determinant() * m[row][col];
        }
        return accum;
    }

    @Override
    public String toString()
    {
        StringBuilder msg = new StringBuilder();
        msg.append("\n------------------------------\n");
        msg.append("  - Matrix of ").append(numRows).append(" rows by ").append(numColumns).append(" columns\n");

        for ( int row = 0; row < numRows; row++ ) {
            for ( int column = 0; column < numColumns; column++ ) {
                msg.append(VSDK.formatDouble(m[row][column])).append(' ');
            }
            msg.append('\n');
        }
        msg.append("------------------------------\n");
        return msg.toString();
    }

    @Override
    public boolean equals(Object obj)
    {
        if ( this == obj ) return true;
        if ( !(obj instanceof MatrixNxM other) ) return false;
        if ( numRows != other.numRows || numColumns != other.numColumns ) return false;
        for ( int i = 0; i < numRows; i++ ) {
            if ( !Arrays.equals(m[i], other.m[i]) ) return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        int result = 31 * numRows + numColumns;
        for ( int i = 0; i < numRows; i++ ) {
            result = 31 * result + Arrays.hashCode(m[i]);
        }
        return result;
    }

    private void validatePosition(int row, int column) throws Exception
    {
        if ( row < 0 || row >= numRows || column < 0 || column >= numColumns ) {
            throw new Exception("Invalid matrix position [" + row + "][" + column + "]");
        }
    }

    private static double[][] deepCopy(double[][] source)
    {
        double[][] copy = new double[source.length][];
        for ( int i = 0; i < source.length; i++ ) {
            copy[i] = Arrays.copyOf(source[i], source[i].length);
        }
        return copy;
    }

    private static double[][] buildIdentityValues(int rows, int cols)
    {
        double[][] values = new double[rows][cols];
        for ( int i = 0; i < rows; i++ ) {
            for ( int j = 0; j < cols; j++ ) {
                values[i][j] = (i == j) ? 1.0 : 0.0;
            }
        }
        return values;
    }
}
