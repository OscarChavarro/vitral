package vsdk.toolkit.common.linealAlgebra;

import java.io.Serial;
import java.util.Arrays;
import java.util.Objects;

import vsdk.toolkit.common.FundamentalEntity;
import vsdk.toolkit.common.VSDK;

public final class Matrix4x4f extends FundamentalEntity
{
    @Serial
    private static final long serialVersionUID = 20260514L;

    private static final int SIZE = 4;

    private final float[][] m;

    public Matrix4x4f() { this(buildIdentityValues(), false); }

    public Matrix4x4f(Matrix4x4f other) {
        this(Objects.requireNonNull(other, "Matrix to copy cannot be null").m, true);
    }

    public Matrix4x4f(float[][] values) { this(values, true); }

    private Matrix4x4f(float[][] values, boolean deepCopy) {
        validate4x4(values);
        this.m = deepCopy ? deepCopy(values) : values;
    }

    public static Matrix4x4f copyOf(Matrix4x4f other) { return new Matrix4x4f(other); }
    public static Matrix4x4f copyOf(float[][] values) { return new Matrix4x4f(values); }
    public static Matrix4x4f identityMatrix() { return new Matrix4x4f(); }
    public Matrix4x4f identity() { return identityMatrix(); }

    public float get(int row, int column) { validatePosition(row, column); return m[row][column]; }

    public Matrix4x4f withVal(int row, int column, float val) {
        validatePosition(row, column);
        float[][] r = deepCopy(m);
        r[row][column] = val;
        return new Matrix4x4f(r, false);
    }

    public float[][] toArrayCopy() { return deepCopy(m); }

    public Matrix4x4f withoutTranslation() {
        return this.withVal(0, 3, 0.0f).withVal(1, 3, 0.0f).withVal(2, 3, 0.0f)
            .withVal(3, 0, 0.0f).withVal(3, 1, 0.0f).withVal(3, 2, 0.0f).withVal(3, 3, 1.0f);
    }

    public Vector3Df extractTranslation() { return new Vector3Df(get(0, 3), get(1, 3), get(2, 3)); }

    public Matrix4x4f withTranslation(Vector3Df t) {
        Objects.requireNonNull(t, "Translation vector cannot be null");
        return this.withVal(0, 3, t.x()).withVal(1, 3, t.y()).withVal(2, 3, t.z());
    }

    public Matrix4x4f orthogonalProjection(float l, float r, float d, float u, float n, float f) {
        return fromDouble(new Matrix4x4d().orthogonalProjection(l, r, d, u, n, f));
    }

    public Matrix4x4f canonicalPerspectiveProjection() {
        return fromDouble(new Matrix4x4d().canonicalPerspectiveProjection());
    }

    public Matrix4x4f frustumProjection(float l, float r, float d, float u, float n, float f) {
        return fromDouble(new Matrix4x4d().frustumProjection(l, r, d, u, n, f));
    }

    public Matrix4x4f translation(float x, float y, float z) {
        return fromDouble(new Matrix4x4d().translation(x, y, z));
    }

    public Matrix4x4f scale(float sx, float sy, float sz) {
        return fromDouble(new Matrix4x4d().scale(sx, sy, sz));
    }

    public Matrix4x4f scale(Vector3Df s) { return scale(s.x(), s.y(), s.z()); }
    public Matrix4x4f translation(Vector3Df t) { return translation(t.x(), t.y(), t.z()); }

    public Matrix4x4f eulerAnglesRotation(float yaw, float pitch, float roll) {
        return fromDouble(new Matrix4x4d().eulerAnglesRotation(yaw, pitch, roll));
    }

    public Matrix4x4f axisRotation(float angle, Vector3Df axis) {
        return axisRotation(angle, axis.x(), axis.y(), axis.z());
    }

    public Matrix4x4f axisRotation(float angle, float x, float y, float z) {
        return fromDouble(new Matrix4x4d().axisRotation(angle, x, y, z));
    }

    public Matrix4x4f inverse() { return invert(); }
    public Matrix4x4f invert() { return fromDouble(asDouble().invert()); }
    public Matrix4x4f cofactors() { return fromDouble(asDouble().cofactors()); }
    public Matrix4x4f transpose() { return fromDouble(asDouble().transpose()); }
    public Matrix4x4f multiply(float a) { return fromDouble(asDouble().multiply(a)); }

    public Vector3Df multiply(Vector3Df e) {
        Vector3Dd r = asDouble().multiply(new Vector3Dd(e.x(), e.y(), e.z()));
        return new Vector3Df((float) r.x(), (float) r.y(), (float) r.z());
    }

    public Vector4Df multiply(Vector4Df e) {
        Vector4Dd r = asDouble().multiply(new Vector4Dd(e.x(), e.y(), e.z(), e.w()));
        return new Vector4Df((float) r.x(), (float) r.y(), (float) r.z(), (float) r.w());
    }

    public Matrix4x4f multiply(Matrix4x4f second) { return fromDouble(asDouble().multiply(second.asDouble())); }
    public float determinant() { return (float) asDouble().determinant(); }

    @Override
    public String toString() {
        StringBuilder msg = new StringBuilder();
        msg.append("\n------------------------------\n");
        for ( int i = 0; i < SIZE; i++ ) {
            for ( int j = 0; j < SIZE; j++ ) {
                msg.append(VSDK.formatDouble(m[i][j])).append(' ');
            }
            msg.append('\n');
        }
        msg.append("------------------------------\n");
        return msg.toString();
    }

    public double[] exportToDoubleArrayRowOrder() {
        double[] array = new double[16];
        for ( int i = 0, k = 0; i < SIZE; i++ ) {
            for ( int j = 0; j < SIZE; j++, k++ ) array[k] = m[i][j];
        }
        return array;
    }

    public float[] exportToFloatArrayRowOrder() {
        float[] array = new float[16];
        for ( int i = 0, k = 0; i < SIZE; i++ ) {
            for ( int j = 0; j < SIZE; j++, k++ ) array[k] = m[i][j];
        }
        return array;
    }

    public double[] exportToDoubleArrayColumnOrder() {
        double[] array = new double[16];
        for ( int j = 0, k = 0; j < SIZE; j++ ) {
            for ( int i = 0; i < SIZE; i++, k++ ) array[k] = m[i][j];
        }
        return array;
    }

    public float[] exportToFloatArrayColumnOrder() {
        float[] array = new float[16];
        for ( int j = 0, k = 0; j < SIZE; j++ ) {
            for ( int i = 0; i < SIZE; i++, k++ ) array[k] = m[i][j];
        }
        return array;
    }

    public Quaterniond exportToQuaternion() { return asDouble().exportToQuaternion(); }

    public Matrix4x4f importFromQuaternion(Quaterniond a) {
        return fromDouble(new Matrix4x4d().importFromQuaternion(a));
    }

    public float obtainEulerYawAngle() { return (float) asDouble().obtainEulerYawAngle(); }
    public float obtainEulerPitchAngle() { return (float) asDouble().obtainEulerPitchAngle(); }
    public float obtainEulerRollAngle() { return (float) asDouble().obtainEulerRollAngle(); }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj ) return true;
        if ( !(obj instanceof Matrix4x4f other) ) return false;
        for ( int i = 0; i < SIZE; i++ ) {
            if ( !Arrays.equals(m[i], other.m[i]) ) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = 1;
        for ( int i = 0; i < SIZE; i++ ) result = 31 * result + Arrays.hashCode(m[i]);
        return result;
    }

    public boolean epsilonEquals(Matrix4x4f other) { return epsilonEquals(other, (float) VSDK.EPSILON); }

    public boolean epsilonEquals(Matrix4x4f other, float epsilon) {
        if ( other == null ) return false;
        if ( epsilon < 0.0f ) throw new IllegalArgumentException("epsilon must be >= 0");
        for ( int i = 0; i < SIZE; i++ ) {
            for ( int j = 0; j < SIZE; j++ ) {
                if ( Math.abs(m[i][j] - other.m[i][j]) > epsilon ) return false;
            }
        }
        return true;
    }

    private Matrix4x4d asDouble() { return new Matrix4x4d(toDouble(m)); }

    private static Matrix4x4f fromDouble(Matrix4x4d source) {
        return new Matrix4x4f(toFloat(source.toArrayCopy()), false);
    }

    private static float[][] buildIdentityValues() {
        return new float[][] {
            {1.0f, 0.0f, 0.0f, 0.0f},
            {0.0f, 1.0f, 0.0f, 0.0f},
            {0.0f, 0.0f, 1.0f, 0.0f},
            {0.0f, 0.0f, 0.0f, 1.0f}
        };
    }

    private static void validate4x4(float[][] values) {
        if ( values == null || values.length != SIZE ) throw new IllegalArgumentException("Matrix must have 4 rows");
        for ( int i = 0; i < SIZE; i++ ) {
            if ( values[i] == null || values[i].length != SIZE ) {
                throw new IllegalArgumentException("Matrix row " + i + " must have 4 columns");
            }
        }
    }

    private static void validatePosition(int row, int column) {
        if ( row < 0 || row >= SIZE || column < 0 || column >= SIZE ) {
            throw new IndexOutOfBoundsException("Matrix position out of bounds: (" + row + ", " + column + ")");
        }
    }

    private static float[][] deepCopy(float[][] source) {
        float[][] copy = new float[SIZE][SIZE];
        for ( int i = 0; i < SIZE; i++ ) {
            System.arraycopy(source[i], 0, copy[i], 0, SIZE);
        }
        return copy;
    }

    private static double[][] toDouble(float[][] source) {
        double[][] out = new double[SIZE][SIZE];
        for ( int i = 0; i < SIZE; i++ ) {
            for ( int j = 0; j < SIZE; j++ ) out[i][j] = source[i][j];
        }
        return out;
    }

    private static float[][] toFloat(double[][] source) {
        float[][] out = new float[SIZE][SIZE];
        for ( int i = 0; i < SIZE; i++ ) {
            for ( int j = 0; j < SIZE; j++ ) out[i][j] = (float) source[i][j];
        }
        return out;
    }
}
