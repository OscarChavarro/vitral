package vsdk.toolkit.common.linealAlgebra;

import java.io.Serial;
import java.util.Objects;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.FundamentalEntity;

public final class Vector4D extends FundamentalEntity
{
    @Serial
    private static final long serialVersionUID = 20061103L;

    private final double x;
    private final double y;
    private final double z;
    private final double w;

    public Vector4D(double x, double y, double z, double w) {
        this.x = x; this.y = y; this.z = z;
        this.w = w;
    }

    public Vector4D(Vector4D other) {
        this(Objects.requireNonNull(other, "Vector4D to copy cannot be null").x,
             other.y,
             other.z,
             other.w);
    }

    public Vector4D(Vector3D other) {
        this.x = other.x();
        this.y = other.y();
        this.z = other.z();
        w = 1;
    }

    public Vector4D multiply(double a) {
        return new Vector4D(a * x, a * y, a * z, a * w);
    }

    public Vector4D dividedByW() {
        if ( Math.abs(w) < VSDK.EPSILON ) return this;
        return new Vector4D(x / w, y / w, z / w, 1);
    }

    public double length() {
        return Math.sqrt(x*x + y*y + z*z + w*w);
    }

    public Vector4D add(Vector4D b)
    {
        return new Vector4D(x + b.x, y + b.y, z + b.z, w + b.w);
    }

    public Vector4D withX(double nx) { return new Vector4D(nx, y, z, w); }
    public Vector4D withY(double ny) { return new Vector4D(x, ny, z, w); }
    public Vector4D withZ(double nz) { return new Vector4D(x, y, nz, w); }
    public Vector4D withW(double nw) { return new Vector4D(x, y, z, nw); }
    public double x() { return x; }
    public double y() { return y; }
    public double z() { return z; }
    public double w() { return w; }

    /**
    Provides an object to text report conversion, optimized for human
    readability and debugging. Do not use for serialization or persistence
    purposes.
    @return human-readable representation of current vector
    */
    @Override
    public String toString()
    {
        String msg;

        msg = "<" + VSDK.formatDouble(x) + ", " + VSDK.formatDouble(y) +
              ", " + VSDK.formatDouble(z) + ", " + VSDK.formatDouble(w) + ">";

        return msg;
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj ) return true;
        if ( !(obj instanceof Vector4D other) ) return false;
        return Double.compare(x, other.x) == 0 &&
               Double.compare(y, other.y) == 0 &&
               Double.compare(z, other.z) == 0 &&
               Double.compare(w, other.w) == 0;
    }

    @Override
    public int hashCode() {
        int result = Double.hashCode(x);
        result = 31 * result + Double.hashCode(y);
        result = 31 * result + Double.hashCode(z);
        result = 31 * result + Double.hashCode(w);
        return result;
    }
}
