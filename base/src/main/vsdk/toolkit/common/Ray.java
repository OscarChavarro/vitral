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
    private static final double UNIT_DIRECTION_TOLERANCE = 1e-12;

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
        this(origin, direction, t, false);
    }

    public Ray(Ray b)
    {
        this(Objects.requireNonNull(b, "Ray to copy cannot be null").origin,
             b.direction,
             b.t,
             true);
    }

    public static Ray copyOf(Ray other)
    {
        return Objects.requireNonNull(other, "Ray to copy cannot be null");
    }

    public Ray withOrigin(Vector3D newOrigin)
    {
        return new Ray(newOrigin, direction, t, true);
    }

    public Ray withDirection(Vector3D newDirection)
    {
        return new Ray(origin, newDirection, t, false);
    }

    public Ray withT(double newT)
    {
        RaytraceStatistics.recordRayWithT();
        if ( Double.compare(newT, t) == 0 ) {
            return this;
        }
        return new Ray(origin, direction, newT, true);
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

    private Ray(
        Vector3D origin,
        Vector3D direction,
        double t,
        boolean directionAlreadyNormalized)
    {
        this.origin = Objects.requireNonNull(origin, "Ray origin cannot be null");
        this.direction = directionAlreadyNormalized ?
            Objects.requireNonNull(direction, "Ray direction cannot be null") :
            normalizeDirection(direction);
        this.t = t;
    }

    private static Vector3D normalizeDirection(Vector3D direction)
    {
        Vector3D candidate =
            Objects.requireNonNull(direction, "Ray direction cannot be null");
        double lengthSquared = candidate.dotProduct(candidate);
        if ( lengthSquared <= VSDK.EPSILON ) {
            return candidate;
        }
        if ( Math.abs(lengthSquared - 1.0) <= UNIT_DIRECTION_TOLERANCE ) {
            return candidate;
        }
        return candidate.multiply(1.0 / Math.sqrt(lengthSquared));
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
