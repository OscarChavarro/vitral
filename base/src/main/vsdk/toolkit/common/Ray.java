package vsdk.toolkit.common;
import java.io.Serial;
import java.util.Objects;

import vsdk.toolkit.common.linealAlgebra.Vector3D;

/**
 This class models a mathematical RAY.
 */
public final class Ray extends FundamentalEntity
{
    @Serial
    private static final long serialVersionUID = 20060502L;

    private final Vector3D origin;
    private final Vector3D direction;
    private final double t;

    public Ray()
    {
        this(new Vector3D(0, 0, 0), new Vector3D(1, 0, 0), 0.0);
    }

    public Ray(Vector3D origin, Vector3D direction)
    {
        this(origin, direction, 0.0);
    }

    public Ray(Vector3D origin, Vector3D direction, double t)
    {
        this.origin = Vector3D.copyOf(Objects.requireNonNull(origin, "Ray origin cannot be null"));
        this.direction = normalizeDirection(direction);
        this.t = t;
    }

    public Ray(Ray b)
    {
        this(Objects.requireNonNull(b, "Ray to copy cannot be null").origin,
             b.direction,
             b.t);
    }

    public static Ray copyOf(Ray other)
    {
        return new Ray(other);
    }

    public Ray withOrigin(Vector3D newOrigin)
    {
        return new Ray(newOrigin, direction, t);
    }

    public Ray withDirection(Vector3D newDirection)
    {
        return new Ray(origin, newDirection, t);
    }

    public Ray withT(double newT)
    {
        return new Ray(origin, direction, newT);
    }

    public Vector3D origin()
    {
        return origin;
    }

    public Vector3D direction()
    {
        return direction;
    }

    public double t()
    {
        return t;
    }

    private static Vector3D normalizeDirection(Vector3D direction)
    {
        return Vector3D.copyOf(
            Objects.requireNonNull(direction, "Ray direction cannot be null")).normalized();
    }

    @Override
    public boolean equals(Object obj)
    {
        if ( this == obj ) {
            return true;
        }
        if ( !(obj instanceof Ray other) ) {
            return false;
        }
        return Double.compare(t, other.t) == 0 &&
               origin.equals(other.origin) &&
               direction.equals(other.direction);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(origin, direction, t);
    }

    /**
    Provides an object to text report conversion, optimized for human
    readability and debugging. Do not use for serialization or persistence
    purposes.
    @return human-readable representation of current Ray
    */
    @Override
    public String toString()
    {
        return "Ray Origin: " + origin + "; Direction: " + direction + " T: " + VSDK.formatDouble(t);
    }
}
