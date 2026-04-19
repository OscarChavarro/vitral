package vsdk.toolkit.common.linealAlgebra;

import java.io.Serial;
import java.util.Objects;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.FundamentalEntity;

/**
Class Vector3D represents a one dimensional array of three values, usually
to be interpreted as:
  - A column vector of 1x3 positions, useful in linear algebra computations.
  - A point in the R3 Euclidean space
As current class is supposed to be used in the context of computer graphics,
array elements are not indexed, to say from 0 to 2, but are instead named
with the usual 3D axis labels `x`, `y` and `z`.
This is one of the most fundamental classes in VitralSDK toolkit, and its
attributes are usually accessed in the inner loops of computational intensive
calculations.

Lack of get/set method enforces a direct attribute access programming style
which will lend to shorter code.
*/
public final class Vector3D extends FundamentalEntity
{
    // Check the general attribute description in superclass Entity.
    @Serial
    private static final long serialVersionUID = 20260419L;

    private final double x;
    private final double y;
    private final double z;

    /**
    The default Vector3D value is the zero value
    */
    public Vector3D() {
        this(0, 0, 0);
    }

    /**
    @param x X coordinate
    @param y Y coordinate
    @param z Z coordinate
    */
    public Vector3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3D(Vector3D other) {
        this(Objects.requireNonNull(other, "Vector3D to copy cannot be null").x,
             other.y,
             other.z);
    }

    public static Vector3D copyOf(Vector3D other) {
        return new Vector3D(Objects.requireNonNull(other, "Vector3D to copy cannot be null"));
    }

    public Vector3D multiply(double a) {
        return new Vector3D(a * x, a * y, a * z);
    }

    /**
    @param other the second vector in cross product
    @return Vector3D, the result of the operation
    */
    public Vector3D crossProduct(Vector3D other) {
        return new Vector3D(y*other.z - z*other.y, z*other.x - x*other.z, x*other.y - y*other.x);
    }

    /**
    @param other Vector3D
    @return dot product between two vectors
    */
    public double dotProduct(Vector3D other) {
        return (x * other.x + y * other.y + z * other.z);
    }

    public Vector3D normalized() {
        double t = x*x + y*y + z*z;
        if ( Math.abs(t) < VSDK.EPSILON ) return this;
        if (t != 0 && t != 1) t = (1.0 / Math.sqrt(t));
        return new Vector3D(x * t, y * t, z * t);
    }

    /**
    @return current vector length
    */
    public double length() {
        return Math.sqrt(x*x + y*y + z*z);
    }

    public Vector3D add(Vector3D b)
    {
        return new Vector3D(x + b.x, y + b.y, z + b.z);
    }

    public Vector3D subtract(Vector3D b)
    {
        return new Vector3D(x - b.x, y - b.y, z - b.z);
    }

    public float[] exportToFloatArrayVector()
    {
        return new float[]{(float)x, (float)y, (float)z, 1};
    }

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

        msg = "<" + VSDK.formatDouble(x) + ", " + VSDK.formatDouble(y) +
              ", " + VSDK.formatDouble(z) + ">";

        return msg;
    }

    /**
    Taking current vector as tail-anchored to the origin, this method
    calculates the theta angle (in radians) of the tip, corresponding
    to tip coordinate <x, y, z>, in spherical coordinates <r, theta, phi>.
    Note that theta goes from 0 to 2*PI, and correspond to an axis of
    rotation <0, 0, 1>.
    POST: 0 <= theta <= 2*PI
    @return an angle with theta angle spherical coordinate for the point at
    the tip of current vector, when vector tail is anchored at the origin
    */
    public double obtainSphericalThetaAngle()
    {
        double val;
        if ( Math.abs(x) > VSDK.EPSILON ) {
            if ( x > 0 ) val = Math.atan(y/x);
            else val = Math.PI + Math.atan(y/x);
        }
        else if ( y > 0 ) {
            val = Math.PI/2;
        }
        else {
            val = Math.PI + Math.PI/2;
        }
        while ( val < 0 ) val += 2*Math.PI;
        while ( val > 2*Math.PI ) val -= 2*Math.PI;
        return val;
    }

    /**
    Taking current vector as tail-anchored to the origin, this method
    calculates the phi angle (in radians) of the tip, corresponding
    to tip coordinate <x, y, z>, in spherical coordinates <r, theta, phi>.
    Note phi goes from 0 to PI.
    @return an angle with phi angle spherical coordinate for the point at
    the tip of current vector, when vector tail is anchored at the origin
    */
    public double obtainSphericalPhiAngle()
    {
        double r = length();

        if ( r < VSDK.EPSILON ) return 0;

        return Math.acos(z/r);
    }

    public static Vector3D fromSpherical(double r, double theta, double phi)
    {
        return new Vector3D(
            r * Math.sin(phi) * Math.cos(theta),
            r * Math.sin(phi) * Math.sin(theta),
            r * Math.cos(phi)
        );
    }

    public Vector3D withX(double nx) {
        return new Vector3D(nx, y, z);
    }

    public Vector3D withY(double ny) {
        return new Vector3D(x, ny, z);
    }

    public Vector3D withZ(double nz) {
        return new Vector3D(x, y, nz);
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj ) {
            return true;
        }
        if ( !(obj instanceof Vector3D other) ) {
            return false;
        }
        return Double.compare(x, other.x) == 0 &&
               Double.compare(y, other.y) == 0 &&
               Double.compare(z, other.z) == 0;
    }

    @Override
    public int hashCode() {
        int result = Double.hashCode(x);
        result = 31 * result + Double.hashCode(y);
        result = 31 * result + Double.hashCode(z);
        return result;
    }
}
