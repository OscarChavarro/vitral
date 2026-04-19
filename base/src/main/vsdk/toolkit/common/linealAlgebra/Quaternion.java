package vsdk.toolkit.common.linealAlgebra;

import java.io.Serial;
import java.util.Objects;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.FundamentalEntity;

/**
Represents a quaternion in vector-scalar form.

<p>For rotation use cases the quaternion is expected to have unit length.
*/
public final class Quaternion extends FundamentalEntity
{
    @Serial
    private static final long serialVersionUID = 20260419L;

    private final Vector3D direction;
    private final double magnitude;

    public Quaternion()
    {
        this(new Vector3D(0, 0, 0), 0);
    }

    public Quaternion(Vector3D direction, double magnitude)
    {
        this.direction = Vector3D.copyOf(Objects.requireNonNull(direction, "Quaternion direction cannot be null"));
        this.magnitude = magnitude;
    }

    public Quaternion(Quaternion other)
    {
        this(Objects.requireNonNull(other, "Quaternion to copy cannot be null").direction,
             other.magnitude);
    }

    public static Quaternion copyOf(Quaternion other)
    {
        return new Quaternion(Objects.requireNonNull(other, "Quaternion to copy cannot be null"));
    }

    /**
    @return the squared Euclidean norm of this quaternion
    */
    public double lengthSquared()
    {
        return magnitude * magnitude + direction.dotProduct(direction);
    }

    /**
    @return the Euclidean norm of this quaternion
    */
    public double length()
    {
        return Math.sqrt(lengthSquared());
    }

    /**
    @return a normalized quaternion, or this quaternion if its norm is near zero
    */
    public Quaternion normalized()
    {
        double l = length();
        if ( Math.abs(l) < VSDK.EPSILON ) {
            return this;
        }
        return new Quaternion(direction.multiply(1/l), magnitude * (1/l));
    }

    /**
    @return the quaternion conjugate
    */
    public Quaternion conjugated()
    {
        return new Quaternion(direction.multiply(-1), magnitude);
    }

    /**
    Rotates the given vector by this quaternion.

    <p>This method assumes the quaternion has unit length. Callers on hot
    paths should normalize once when caching the quaternion and reuse it.

    @param vector vector to rotate
    @return rotated vector
    */
    public Vector3D rotate(Vector3D vector)
    {
        Vector3D normalizedAxis = direction;
        Vector3D uv = normalizedAxis.crossProduct(
            Objects.requireNonNull(vector, "Vector to rotate cannot be null"));
        Vector3D uuv = normalizedAxis.crossProduct(uv);

        return vector.add(uv.multiply(2 * magnitude)).add(uuv.multiply(2));
    }

    public Quaternion withDirection(Vector3D newDirection)
    {
        return new Quaternion(newDirection, magnitude);
    }

    public Quaternion withMagnitude(double newMagnitude)
    {
        return new Quaternion(direction, newMagnitude);
    }

    public Vector3D direction()
    {
        return direction;
    }

    public double magnitude()
    {
        return magnitude;
    }

    @Override
    public String toString()
    {
        String msg;
        msg = direction + " / " + VSDK.formatDouble(magnitude);
        return msg;
    }
}
