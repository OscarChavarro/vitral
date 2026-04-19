package vsdk.toolkit.common.linealAlgebra;

import java.io.Serial;
import java.util.Objects;
import vsdk.toolkit.common.FundamentalEntity;
import vsdk.toolkit.common.VSDK;

/**
Class Vector2D represents a one dimensional array of two values, usually
to be interpreted as:
  - A column vector of 1x2 positions, useful in linear algebra computations.
  - A point in the R2 Euclidean space
As current class is supposed to be used in the context of computer graphics,
array elements are not indexed, to say from 0 to 2, but are instead named
with the usual 2D axis labels `x` and `y`.
This is one of the most fundamental classes in VitralSDK toolkit, and its
attributes are usually accessed in the inner loops of computational intensive
calculations. As such, the attributes are promoted to be public, yes, 
breaking encapsulation and converting current class to a mere non-evolvable
structure.
Lack of get/set method enforces a direct attribute access programming style
which will lend to shorter code.
*/
public final class Vector2D extends FundamentalEntity {
    @Serial
    private static final long serialVersionUID = 20060502L;

    private final double x;
    private final double y;

    /**
    The default Vector3D value is the zero value
    */
    public Vector2D() {
        this(0, 0);
    }

    /**
    @param x X coordinate
    @param y Y coordinate
    */
    public Vector2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Vector2D(Vector2D other) {
        this(Objects.requireNonNull(other, "Vector2D to copy cannot be null").x,
             other.y);
    }

    public static Vector2D copyOf(Vector2D other) {
        return new Vector2D(Objects.requireNonNull(other, "Vector2D to copy cannot be null"));
    }
    
    public final Vector2D multiply(double a) {
        return new Vector2D(a * x, a * y);
    }

    /**
    @return current vector length
    */
    public final double length() {
        return Math.sqrt(x*x + y*y);
    }

    public final Vector2D add(Vector2D b)
    {
        return new Vector2D(x + b.x, y + b.y);
    }
    
    public Vector2D withX(double nx) { return new Vector2D(nx, y); }
    public Vector2D withY(double ny) { return new Vector2D(x, ny); }
    public double x() { return x; }
    public double y() { return y; }

    /**
    Provides an object to text report a convert, optimized for human
    readability and debugging. Do not use for serialization or persistence
    purposes.
    @return human-readable representation of current vector
    */
    @Override
    public String toString()
    {
        String msg;

        msg = "<" + VSDK.formatDouble(x) + ", " + VSDK.formatDouble(y) + ">";

        return msg;
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj ) return true;
        if ( !(obj instanceof Vector2D other) ) return false;
        return Double.compare(x, other.x) == 0 &&
               Double.compare(y, other.y) == 0;
    }

    @Override
    public int hashCode() {
        int result = Double.hashCode(x);
        result = 31 * result + Double.hashCode(y);
        return result;
    }
}
