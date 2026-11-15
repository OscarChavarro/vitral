package vsdk.toolkit.environment.geometry.boundingVolumeHierarchy;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.element.GeometryBounds;
import vsdk.toolkit.environment.geometry.element.Ray;

/**
Axis aligned box `[min, max]`. Coordinates beyond `1e29` stand for an
unbounded box. The ray tests are the slab method of Kay and Kajiya (1986);
they and the point test are the hottest filters of a render.
Instances are immutable.
*/
public final class AxisAlignedBoundingBox {
    private final Vector3Dd min;
    private final Vector3Dd max;

    public AxisAlignedBoundingBox()
    {
        min = new Vector3Dd(0.0, 0.0, 0.0);
        max = new Vector3Dd(0.0, 0.0, 0.0);
    }

    public AxisAlignedBoundingBox(Vector3Dd min, Vector3Dd max)
    {
        this.min = min;
        this.max = max;
    }

    public Vector3Dd getMin()
    {
        return min;
    }

    public Vector3Dd getMax()
    {
        return max;
    }

    public static AxisAlignedBoundingBox unbounded()
    {
        final double INF = 1e30;
        return new AxisAlignedBoundingBox(new Vector3Dd(-INF, -INF, -INF), new Vector3Dd(INF, INF, INF));
    }

    public static AxisAlignedBoundingBox empty()
    {
        final double INF = 1e30;
        return new AxisAlignedBoundingBox(new Vector3Dd(INF, INF, INF), new Vector3Dd(-INF, -INF, -INF));
    }

    public boolean isUnbounded()
    {
        final double THRESHOLD = 1e29;
        return min.x() <= -THRESHOLD || max.x() >= THRESHOLD
            || min.y() <= -THRESHOLD || max.y() >= THRESHOLD
            || min.z() <= -THRESHOLD || max.z() >= THRESHOLD;
    }

    public AxisAlignedBoundingBox enclosing(AxisAlignedBoundingBox other)
    {
        return new AxisAlignedBoundingBox(
            new Vector3Dd(
                min.x() < other.min.x() ? min.x() : other.min.x(),
                min.y() < other.min.y() ? min.y() : other.min.y(),
                min.z() < other.min.z() ? min.z() : other.min.z()),
            new Vector3Dd(
                max.x() > other.max.x() ? max.x() : other.max.x(),
                max.y() > other.max.y() ? max.y() : other.max.y(),
                max.z() > other.max.z() ? max.z() : other.max.z()));
    }

    public AxisAlignedBoundingBox intersection(AxisAlignedBoundingBox other)
    {
        return new AxisAlignedBoundingBox(
            new Vector3Dd(
                min.x() > other.min.x() ? min.x() : other.min.x(),
                min.y() > other.min.y() ? min.y() : other.min.y(),
                min.z() > other.min.z() ? min.z() : other.min.z()),
            new Vector3Dd(
                max.x() < other.max.x() ? max.x() : other.max.x(),
                max.y() < other.max.y() ? max.y() : other.max.y(),
                max.z() < other.max.z() ? max.z() : other.max.z()));
    }

    public AxisAlignedBoundingBox expandedBy(Vector3Dd point)
    {
        return new AxisAlignedBoundingBox(
            new Vector3Dd(
                min.x() < point.x() ? min.x() : point.x(),
                min.y() < point.y() ? min.y() : point.y(),
                min.z() < point.z() ? min.z() : point.z()),
            new Vector3Dd(
                max.x() > point.x() ? max.x() : point.x(),
                max.y() > point.y() ? max.y() : point.y(),
                max.z() > point.z() ? max.z() : point.z()));
    }

    public Vector3Dd centroid()
    {
        return new Vector3Dd(
            (min.x() + max.x()) * 0.5,
            (min.y() + max.y()) * 0.5,
            (min.z() + max.z()) * 0.5);
    }

    public double extent(int axis)
    {
        if (axis == 0) return max.x() - min.x();
        if (axis == 1) return max.y() - min.y();
        return max.z() - min.z();
    }

    /** World box around the eight corners of `[lo, hi]` transformed by
    `matrix` (column vectors, as `GeometryBounds.transformed`); without a
    matrix (null), the box `[lo, hi]` itself. */
    public static AxisAlignedBoundingBox fromTransformedCorners(
        Vector3Dd lo, Vector3Dd hi, Matrix4x4d matrix)
    {
        GeometryBounds local = new GeometryBounds();
        if (!local.setFinite(lo, hi)) return unbounded();
        final GeometryBounds result = matrix != null ? local.transformed(matrix) : local;
        if (result.getKind() != GeometryBounds.Kind.Finite) return unbounded();
        return new AxisAlignedBoundingBox(result.getMinimum(), result.getMaximum());
    }

    /** Box of finite geometry bounds; any other kind (empty, unbounded,
    indeterminate, unsupported) gives the unbounded box, which is always
    conservative. */
    public static AxisAlignedBoundingBox fromGeometryBounds(GeometryBounds bounds)
    {
        if (bounds.getKind() != GeometryBounds.Kind.Finite) return unbounded();
        return new AxisAlignedBoundingBox(bounds.getMinimum(), bounds.getMaximum());
    }

    /** Slab test for ray parameters in [0, +inf). */
    public boolean intersectsRayForward(Ray ray)
    {
        return intersectsRayForward(ray, new RaySlabReciprocals(ray.getDirection()));
    }

    /** Slab test for ray parameters in [0, +inf), with the reciprocals of
    the ray direction computed beforehand. */
    public boolean intersectsRayForward(Ray ray, RaySlabReciprocals reciprocals)
    {
        final Vector3Dd origin = ray.getOrigin();
        double[] tMin = {0.0};
        double[] tMax = {1e30};

        return
            updateAxisForward(tMin, tMax, origin.x(), reciprocals.getInverseX(), reciprocals.isDegenerateX(),
                       min.x(), max.x()) &&
            updateAxisForward(tMin, tMax, origin.y(), reciprocals.getInverseY(), reciprocals.isDegenerateY(),
                       min.y(), max.y()) &&
            updateAxisForward(tMin, tMax, origin.z(), reciprocals.getInverseZ(), reciprocals.isDegenerateZ(),
                       min.z(), max.z()) &&
            tMax[0] >= 0.0;
    }

    /** cpp lambda `updateAxis` of `intersectsRayForward`. */
    private static boolean updateAxisForward(double[] tMin, double[] tMax,
        double originCoord, double invDir, boolean degenerate,
        double minCoord, double maxCoord)
    {
        if (degenerate) {
            return originCoord >= minCoord && originCoord <= maxCoord;
        }
        double nearT = (minCoord - originCoord) * invDir;
        double farT = (maxCoord - originCoord) * invDir;
        if (nearT > farT) {
            final double tmp = nearT;
            nearT = farT;
            farT = tmp;
        }
        tMin[0] = nearT > tMin[0] ? nearT : tMin[0];
        tMax[0] = farT < tMax[0] ? farT : tMax[0];
        return tMin[0] <= tMax[0];
    }

    /** Slab test for ray parameters in [0, maxT]. */
    public boolean intersectsRayBefore(Ray ray, double maxT)
    {
        final Vector3Dd origin = ray.getOrigin();
        final Vector3Dd direction = ray.getDirection();
        double[] tMin = {0.0};
        double[] tMax = {maxT};

        return
            updateAxisBefore(tMin, tMax, origin.x(), direction.x(), min.x(), max.x()) &&
            updateAxisBefore(tMin, tMax, origin.y(), direction.y(), min.y(), max.y()) &&
            updateAxisBefore(tMin, tMax, origin.z(), direction.z(), min.z(), max.z()) &&
            tMax[0] >= 0.0;
    }

    /** cpp lambda `updateAxis` of `intersectsRayBefore`. */
    private static boolean updateAxisBefore(double[] tMin, double[] tMax,
        double originCoord, double directionCoord,
        double minCoord, double maxCoord)
    {
        if (directionCoord > -1e-12 && directionCoord < 1e-12) {
            return originCoord >= minCoord && originCoord <= maxCoord;
        }
        final double invDir = 1.0 / directionCoord;
        double nearT = (minCoord - originCoord) * invDir;
        double farT = (maxCoord - originCoord) * invDir;
        if (nearT > farT) {
            final double tmp = nearT;
            nearT = farT;
            farT = tmp;
        }
        tMin[0] = nearT > tMin[0] ? nearT : tMin[0];
        tMax[0] = farT < tMax[0] ? farT : tMax[0];
        return tMin[0] <= tMax[0];
    }

    public boolean containsPointWithTolerance(Vector3Dd point, double tolerance)
    {
        return
            point.x() >= min.x() - tolerance &&
            point.x() <= max.x() + tolerance &&
            point.y() >= min.y() - tolerance &&
            point.y() <= max.y() + tolerance &&
            point.z() >= min.z() - tolerance &&
            point.z() <= max.z() + tolerance;
    }
}
