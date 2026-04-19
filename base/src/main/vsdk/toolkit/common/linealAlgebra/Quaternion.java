package vsdk.toolkit.common.linealAlgebra;

import java.io.Serial;
import java.util.Objects;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.FundamentalEntity;

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

    public double length()
    {
        return magnitude * magnitude + direction.dotProduct(direction);
    }

    public Quaternion normalized()
    {
        double l;

        l = length();
        if ( Math.abs(l) < VSDK.EPSILON ) {
            return this;
        }
        return new Quaternion(direction.multiply(1/l), magnitude * (1/l));
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
